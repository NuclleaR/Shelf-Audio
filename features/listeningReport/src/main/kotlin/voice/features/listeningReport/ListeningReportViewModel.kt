package voice.features.listeningReport

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.zacsweers.metro.Inject
import voice.core.data.BookId
import voice.core.data.repo.BookRepository
import voice.core.data.repo.ListeningStatsRepo
import voice.navigation.Destination
import voice.navigation.Navigator
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlin.time.Duration

@Inject
class ListeningReportViewModel(
  private val listeningStatsRepo: ListeningStatsRepo,
  private val bookRepository: BookRepository,
  private val navigator: Navigator,
  private val clock: Clock,
) {

  private var period by mutableStateOf(ReportPeriod.Today)

  @Composable
  internal fun viewState(): ListeningReportViewState? {
    val period = period
    var viewState by remember { mutableStateOf<ListeningReportViewState?>(null) }
    LaunchedEffect(period) {
      viewState = report(period)
    }
    return viewState
  }

  internal fun onPeriodChange(period: ReportPeriod) {
    this.period = period
  }

  internal fun onBookClick(id: BookId) {
    navigator.goTo(Destination.Bookmarks(id))
  }

  internal fun onCloseClick() {
    navigator.goBack()
  }

  private suspend fun report(period: ReportPeriod): ListeningReportViewState {
    val today = LocalDate.now(clock)
    val days = when (period) {
      ReportPeriod.Today -> listOf(today)
      ReportPeriod.Week -> {
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        (0L until 7L).map(monday::plusDays)
      }
    }
    val from = days.first().atStartOfDay(clock.zone).toInstant()
    val to = days.last().plusDays(1).atStartOfDay(clock.zone).toInstant()
    val listening = listeningStatsRepo.listenedPerBook(from, to)
    val perDay = if (period == ReportPeriod.Week) listeningStatsRepo.listenedPerDay(from, to) else emptyMap()
    return ListeningReportViewState(
      period = period,
      // books deleted since still count towards the time, they just aren't listed
      listened = listening.fold(Duration.ZERO) { sum, book -> sum + book.listened },
      days = if (period == ReportPeriod.Week) {
        days.map { day ->
          ReportDay(day = day.dayOfWeek, listened = perDay[day] ?: Duration.ZERO, isToday = day == today)
        }
      } else {
        emptyList()
      },
      books = listening.mapNotNull { bookListening ->
        val content = bookRepository.get(bookListening.bookId)?.content ?: return@mapNotNull null
        ReportBook(
          id = content.id,
          name = content.name,
          author = content.metadata.author,
          cover = content.coverUrl,
          listened = bookListening.listened,
          finished = bookListening.finished,
        )
      },
    )
  }
}
