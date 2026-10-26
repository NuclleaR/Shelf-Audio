package voice.core.playback

import androidx.media3.common.Player
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import voice.core.data.Book
import voice.core.data.BookComparator
import voice.core.data.BookId
import voice.core.data.ListeningEvent
import voice.core.data.isRemote
import voice.core.data.repo.BookRepository
import voice.core.data.store.CurrentBookStore
import voice.core.playback.di.PlaybackScope
import voice.core.sleeptimer.SleepTimer
import voice.core.sleeptimer.SleepTimerState
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

@SingleIn(PlaybackScope::class)
@Inject
class NextBookAutoPlay(
  private val autoPlayNextBookStore: AutoPlayNextBookStore,
  private val bookRepository: BookRepository,
  private val currentBookStore: CurrentBookStore,
  private val playerController: PlayerController,
  private val sleepTimer: SleepTimer,
  private val scope: CoroutineScope,
) {

  fun attachTo(player: Player) {
    player.addListener(
      object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
          // the end of chapter sleep timer stops at the end of the book too, and its listener may run after this one
          if (playbackState == Player.STATE_ENDED &&
            player.playWhenReady &&
            sleepTimer.state.value !is SleepTimerState.Enabled.WithEndOfChapter
          ) {
            scope.launch { playNextBook() }
          }
        }
      },
    )
  }

  internal suspend fun playNextBook() {
    if (!autoPlayNextBookStore.data.first()) return
    val current = currentBookStore.data.first() ?: return
    val next = nextBook(current, bookRepository.all()) ?: return
    if (next.isFinished) {
      bookRepository.updateBook(next.id) {
        it.copy(
          currentChapter = next.chapters.first().id,
          positionInChapter = 0,
          lastPlayedAt = Instant.now(),
        )
      }
    }
    currentBookStore.updateData { next.id }
    playerController.play(ListeningEvent.Source.Unknown)
  }
}

internal fun nextBook(
  current: BookId,
  books: List<Book>,
): Book? {
  if (current.isRemote) return null
  val localBooks = books.filterNot { it.id.isRemote }.sortedWith(BookComparator.ByName)
  val index = localBooks.indexOfFirst { it.id == current }
  if (index == -1) return null
  return localBooks.getOrNull(index + 1)
}

// the same rule that puts a book among the finished ones in the library
internal val Book.isFinished: Boolean
  get() = position >= duration - 5.seconds.inWholeMilliseconds
