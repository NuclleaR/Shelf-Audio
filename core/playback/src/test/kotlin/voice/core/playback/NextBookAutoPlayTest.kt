package voice.core.playback

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import voice.core.data.AUDIOBOOKSHELF_SCHEME
import voice.core.data.Book
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.Chapter
import voice.core.data.ChapterId
import voice.core.data.ListeningEvent
import voice.core.data.repo.BookRepository
import voice.core.data.store.CurrentBookStore
import voice.core.playback.session.search.book
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.uuid.Uuid

class NextBookAutoPlayTest {

  private val scope = TestScope()

  @Test
  fun `the next book is the next one by name, with numbers in order`() {
    val tale1 = book("Tale 1")
    val tale2 = book("tale 2")
    val tale10 = book("Tale 10")
    val books = listOf(tale10, tale2, tale1)

    assertEquals(expected = tale2, actual = nextBook(tale1.id, books))
    assertEquals(expected = tale10, actual = nextBook(tale2.id, books))
  }

  @Test
  fun `there is no next book after the last one`() {
    val a = book("A")
    val b = book("B")

    assertNull(nextBook(b.id, listOf(a, b)))
  }

  @Test
  fun `server books are left out`() {
    val a = book("A")
    val server = book("B", id = BookId("$AUDIOBOOKSHELF_SCHEME://item/b"))
    val c = book("C")
    val books = listOf(a, server, c)

    assertEquals(expected = c, actual = nextBook(a.id, books))
    assertNull(nextBook(server.id, books))
  }

  @Test
  fun `a finished next book starts from the beginning`() = scope.runTest {
    val current = book("A")
    val next = book("B", positionInChapter = CHAPTER_DURATION - 1_000)
    val repo = repo(current, next)
    val update = slot<(BookContent) -> BookContent>()
    coEvery { repo.updateBook(next.id, capture(update)) } just Runs
    val currentBookStore = CurrentBookStore(MemoryDataStore(current.id))
    val playerController = mockk<PlayerController>(relaxUnitFun = true)

    autoPlay(repo, currentBookStore, playerController).playNextBook()

    val restarted = update.captured(next.content)
    assertEquals(expected = 0, actual = restarted.positionInChapter)
    assertEquals(expected = next.chapters.first().id, actual = restarted.currentChapter)
    assertEquals(expected = next.id, actual = currentBookStore.data.first())
    verify { playerController.play(ListeningEvent.Source.Unknown) }
  }

  @Test
  fun `a partly heard next book goes on where it stopped`() = scope.runTest {
    val current = book("A")
    val next = book("B", positionInChapter = 4_000)
    val repo = repo(current, next)
    val currentBookStore = CurrentBookStore(MemoryDataStore(current.id))
    val playerController = mockk<PlayerController>(relaxUnitFun = true)

    autoPlay(repo, currentBookStore, playerController).playNextBook()

    coVerify(exactly = 0) { repo.updateBook(any(), any()) }
    assertEquals(expected = next.id, actual = currentBookStore.data.first())
    verify { playerController.play(ListeningEvent.Source.Unknown) }
  }

  @Test
  fun `playback stops after the last book`() = scope.runTest {
    val first = book("A")
    val last = book("B")
    val currentBookStore = CurrentBookStore(MemoryDataStore(last.id))
    val playerController = mockk<PlayerController>(relaxUnitFun = true)

    autoPlay(repo(first, last), currentBookStore, playerController).playNextBook()

    assertEquals(expected = last.id, actual = currentBookStore.data.first())
    verify(exactly = 0) { playerController.play(any()) }
  }

  @Test
  fun `nothing plays while the setting is off`() = scope.runTest {
    val current = book("A")
    val next = book("B")
    val currentBookStore = CurrentBookStore(MemoryDataStore(current.id))
    val playerController = mockk<PlayerController>(relaxUnitFun = true)

    autoPlay(repo(current, next), currentBookStore, playerController, enabled = false).playNextBook()

    assertEquals(expected = current.id, actual = currentBookStore.data.first())
    verify(exactly = 0) { playerController.play(any()) }
  }

  private fun repo(vararg books: Book): BookRepository = mockk {
    coEvery { all() } returns books.toList()
  }

  private fun autoPlay(
    repo: BookRepository,
    currentBookStore: CurrentBookStore,
    playerController: PlayerController,
    enabled: Boolean = true,
  ) = NextBookAutoPlay(
    autoPlayNextBookStore = AutoPlayNextBookStore(MemoryDataStore(enabled)),
    bookRepository = repo,
    currentBookStore = currentBookStore,
    playerController = playerController,
    sleepTimer = mockk(),
    scope = scope,
  )

  private fun book(
    name: String,
    id: BookId = BookId(Uuid.random().toString()),
    positionInChapter: Long = 0,
  ): Book {
    val chapter = Chapter(
      id = ChapterId(Uuid.random().toString()),
      name = name,
      duration = CHAPTER_DURATION,
      fileLastModified = Instant.EPOCH,
      markData = emptyList(),
      fileSize = 0,
    )
    val book = book(chapters = listOf(chapter), id = id, positionInChapter = positionInChapter)
    return book.copy(content = book.content.copy(customName = name))
  }
}

private const val CHAPTER_DURATION = 60_000L
