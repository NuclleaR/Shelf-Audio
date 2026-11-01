package voice.features.listeningReport

import voice.core.data.BookId
import java.time.DayOfWeek
import kotlin.time.Duration

internal enum class ReportPeriod {
  Today,
  Week,
}

internal data class ListeningReportViewState(
  val period: ReportPeriod,
  val listened: Duration,
  /** Monday to Sunday for the week, empty for today. */
  val days: List<ReportDay>,
  val books: List<ReportBook>,
)

internal data class ReportDay(
  val day: DayOfWeek,
  val listened: Duration,
  val isToday: Boolean,
)

internal data class ReportBook(
  val id: BookId,
  val name: String,
  val author: String?,
  val cover: String?,
  val listened: Duration,
  val finished: Boolean,
)
