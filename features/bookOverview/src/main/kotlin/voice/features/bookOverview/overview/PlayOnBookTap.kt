package voice.features.bookOverview.overview

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first
import voice.core.data.BookId
import voice.core.data.repo.BookRepository
import voice.core.data.store.CurrentBookStore
import voice.core.playback.PlayOnBookTapStore
import voice.core.playback.PlayerController
import java.time.Instant

@Inject
class PlayOnBookTap(
  private val playOnBookTapStore: PlayOnBookTapStore,
  private val repo: BookRepository,
  private val currentBookStore: CurrentBookStore,
  private val playerController: PlayerController,
) {

  suspend fun onBookTap(id: BookId) {
    if (!playOnBookTapStore.data.first()) return
    val book = repo.get(id) ?: return
    playerController.pauseIfCurrentBookDifferentFrom(id)
    val startOver = book.category == BookOverviewCategory.FINISHED
    val firstChapter = book.chapters.first().id
    if (startOver) {
      repo.updateBook(id) {
        it.copy(
          currentChapter = firstChapter,
          positionInChapter = 0,
          lastPlayedAt = Instant.now(),
        )
      }
    }
    currentBookStore.updateData { id }
    // The player keeps the position of the book it has loaded and would write it back.
    if (startOver && playerController.livePlaybackState(id) != null) {
      playerController.setPosition(0, firstChapter)
    }
    playerController.play()
  }
}
