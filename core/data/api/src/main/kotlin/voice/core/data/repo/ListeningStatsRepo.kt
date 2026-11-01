package voice.core.data.repo

import voice.core.data.BookId
import voice.core.data.ListeningSession
import java.time.Instant
import java.time.LocalDate
import kotlin.time.Duration

/**
 * Listening sessions, kept on the device to tell listeners about their listening.
 */
public interface ListeningStatsRepo {

  /**
   * Stores a new session, or updates it if it has an id. Returns the id.
   */
  public suspend fun save(session: ListeningSession): Long

  /**
   * Sums up the sessions that started in between [from] (inclusive) and [to] (exclusive).
   */
  public suspend fun summary(
    from: Instant = Instant.EPOCH,
    to: Instant = Instant.ofEpochMilli(Long.MAX_VALUE),
  ): ListeningSummary

  /**
   * The time spent playing each book in the sessions that started in between [from] (inclusive) and [to]
   * (exclusive), the longest first.
   */
  public suspend fun listenedPerBook(
    from: Instant,
    to: Instant,
  ): List<BookListening>

  /**
   * The time spent playing on each local day with listening, in the sessions that started in between [from]
   * (inclusive) and [to] (exclusive). A session counts towards the day it started on.
   */
  public suspend fun listenedPerDay(
    from: Instant,
    to: Instant,
  ): Map<LocalDate, Duration>

  /**
   * Hands the sessions over when a book shows up under a new id.
   */
  public suspend fun moveToBook(
    from: List<BookId>,
    to: BookId,
  )
}

public data class ListeningSummary(
  val listened: Duration,
  /**
   * The number of local days with listening.
   */
  val listeningDays: Int,
  /**
   * Newest first, each book once.
   */
  val finishedBooks: List<FinishedBook>,
)

public data class FinishedBook(
  val bookId: BookId,
  val at: Instant,
)

public data class BookListening(
  val bookId: BookId,
  val listened: Duration,
  /**
   * Whether playback reached the end of the book in one of the sessions.
   */
  val finished: Boolean,
)
