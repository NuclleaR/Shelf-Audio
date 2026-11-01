package voice.core.data.repo.internals.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import voice.core.data.BookId
import voice.core.data.ListeningSession

@Dao
public interface ListeningSessionDao {

  @Upsert
  public suspend fun upsert(session: ListeningSession): Long

  @Query("SELECT COALESCE(SUM(listenedMillis), 0) FROM listening_session WHERE startedAtMillis >= :from AND startedAtMillis < :to")
  public suspend fun listenedMillis(
    from: Long,
    to: Long,
  ): Long

  @Query(
    """
    SELECT COUNT(DISTINCT (startedAtMillis / 1000 + utcOffsetSeconds) / 86400) FROM listening_session
    WHERE startedAtMillis >= :from AND startedAtMillis < :to AND listenedMillis > 0
    """,
  )
  public suspend fun listeningDays(
    from: Long,
    to: Long,
  ): Int

  @Query(
    """
    SELECT bookId, MAX(endedAtMillis) AS atMillis FROM listening_session
    WHERE startedAtMillis >= :from AND startedAtMillis < :to AND reachedEnd = 1
    GROUP BY bookId ORDER BY atMillis DESC
    """,
  )
  public suspend fun finishedBooks(
    from: Long,
    to: Long,
  ): List<FinishedBookRow>

  @Query(
    """
    SELECT bookId, SUM(listenedMillis) AS listenedMillis, MAX(reachedEnd) AS reachedEnd FROM listening_session
    WHERE startedAtMillis >= :from AND startedAtMillis < :to
    GROUP BY bookId HAVING SUM(listenedMillis) > 0 ORDER BY SUM(listenedMillis) DESC
    """,
  )
  public suspend fun listenedPerBook(
    from: Long,
    to: Long,
  ): List<BookListeningRow>

  @Query(
    """
    SELECT (startedAtMillis / 1000 + utcOffsetSeconds) / 86400 AS epochDay, SUM(listenedMillis) AS listenedMillis
    FROM listening_session
    WHERE startedAtMillis >= :from AND startedAtMillis < :to
    GROUP BY epochDay HAVING SUM(listenedMillis) > 0
    """,
  )
  public suspend fun listenedPerDay(
    from: Long,
    to: Long,
  ): List<DayListeningRow>

  @Query("UPDATE listening_session SET bookId = :to WHERE bookId IN (:from)")
  public suspend fun moveToBook(
    from: List<@JvmSuppressWildcards BookId>,
    to: BookId,
  )
}

public data class FinishedBookRow(
  val bookId: BookId,
  val atMillis: Long,
)

public data class BookListeningRow(
  val bookId: BookId,
  val listenedMillis: Long,
  val reachedEnd: Boolean,
)

public data class DayListeningRow(
  val epochDay: Long,
  val listenedMillis: Long,
)
