package voice.features.bookOverview.overview

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import voice.core.data.Book
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.repo.BookRepository
import voice.core.data.store.CurrentBookStore
import voice.core.playback.LivePlaybackState
import voice.core.playback.PlayOnBookTapStore
import voice.core.playback.PlayerController
import voice.features.bookOverview.MemoryDataStore
import voice.features.bookOverview.book
import voice.features.bookOverview.chapter
import kotlin.test.Test
import kotlin.test.assertEquals

class PlayOnBookTapTest {

  private val lastChapter = chapter(duration = 7_000)
  private val inProgress = book(time = 1_000)
  private val finished = book(chapters = listOf(chapter(duration = 5_000), lastChapter), currentChapter = lastChapter.id, time = 7_000)

  private val repo = mockk<BookRepository> {
    coEvery { get(inProgress.id) } returns inProgress
    coEvery { get(finished.id) } returns finished
    coEvery { updateBook(any(), any()) } just runs
  }
  private val playerController = mockk<PlayerController>(relaxed = true) {
    coEvery { livePlaybackState(any()) } returns null
  }
  private val currentBookStore = MemoryDataStore<BookId?>(BookId("other"))
  private val enabled = MemoryDataStore(true)

  private val playOnBookTap = PlayOnBookTap(
    playOnBookTapStore = PlayOnBookTapStore(enabled),
    repo = repo,
    currentBookStore = CurrentBookStore(currentBookStore),
    playerController = playerController,
  )

  @Test
  fun `a tapped book plays from where it stopped`() = runTest {
    playOnBookTap.onBookTap(inProgress.id)

    assertEquals(inProgress.id, currentBookStore.data.first())
    verifyOrder {
      playerController.pauseIfCurrentBookDifferentFrom(inProgress.id)
      playerController.play(any())
    }
    coVerify(exactly = 0) { repo.updateBook(any(), any()) }
    verify(exactly = 0) { playerController.setPosition(any(), any(), any()) }
  }

  @Test
  fun `nothing plays while it's turned off`() = runTest {
    enabled.updateData { false }

    playOnBookTap.onBookTap(inProgress.id)

    assertEquals(BookId("other"), currentBookStore.data.first())
    verify(exactly = 0) { playerController.play(any()) }
  }

  @Test
  fun `a finished book starts over`() = runTest {
    playOnBookTap.onBookTap(finished.id)

    val update = slot<(BookContent) -> BookContent>()
    coVerify { repo.updateBook(finished.id, capture(update)) }
    val updated = update.captured(finished.content)
    assertEquals(finished.chapters.first().id, updated.currentChapter)
    assertEquals(0, updated.positionInChapter)
    verify(exactly = 0) { playerController.setPosition(any(), any(), any()) }
    verify { playerController.play(any()) }
  }

  @Test
  fun `a finished book the player holds starts over in the player too`() = runTest {
    playerHolds(finished)

    playOnBookTap.onBookTap(finished.id)

    verifyOrder {
      playerController.setPosition(0, finished.chapters.first().id, any())
      playerController.play(any())
    }
  }

  private fun playerHolds(book: Book) {
    coEvery { playerController.livePlaybackState(book.id) } returns LivePlaybackState(
      bookId = book.id,
      chapterId = book.content.currentChapter,
      positionMs = book.content.positionInChapter,
      isPlaying = false,
      playbackSpeed = 1F,
    )
  }
}
