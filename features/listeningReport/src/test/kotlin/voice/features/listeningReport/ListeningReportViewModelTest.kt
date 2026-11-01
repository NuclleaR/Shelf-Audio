package voice.features.listeningReport

import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import voice.core.data.Book
import voice.core.data.BookId
import voice.core.data.repo.BookListening
import voice.core.data.repo.BookRepository
import voice.core.data.repo.ListeningStatsRepo
import voice.navigation.Destination
import voice.navigation.Navigator
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class ListeningReportViewModelTest {

  private val scope = TestScope()

  // a Wednesday
  private val clock = Clock.fixed(Instant.parse("2026-10-28T10:00:00Z"), ZoneOffset.ofHours(2))
  private val hobbit = BookId("hobbit")
  private val pippi = BookId("pippi")
  private val deleted = BookId("deleted")
  private val statsRepo = mockk<ListeningStatsRepo>()
  private val bookRepository = mockk<BookRepository> {
    coEvery { get(hobbit) } returns book(hobbit, "The Hobbit")
    coEvery { get(pippi) } returns book(pippi, "Pippi Longstocking")
    coEvery { get(deleted) } returns null
  }
  private val navigator = mockk<Navigator>(relaxUnitFun = true)
  private val viewModel = ListeningReportViewModel(
    listeningStatsRepo = statsRepo,
    bookRepository = bookRepository,
    navigator = navigator,
    clock = clock,
  )

  private fun book(
    id: BookId,
    name: String,
  ): Book = mockk {
    every { content.id } returns id
    every { content.name } returns name
    every { content.metadata.author } returns null
    every { content.coverUrl } returns null
  }

  private fun test(validate: suspend ReceiveTurbine<ListeningReportViewState?>.() -> Unit) = scope.runTest {
    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.viewState()
    }.test {
      validate()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @IgnorableReturnValue
  private suspend fun ReceiveTurbine<ListeningReportViewState?>.awaitState(period: ReportPeriod): ListeningReportViewState {
    while (true) {
      val state = awaitItem()
      if (state != null && state.period == period) return state
    }
  }

  @Test
  fun `today lists the books listened to today and adds up the time`() {
    coEvery {
      statsRepo.listenedPerBook(from = Instant.parse("2026-10-27T22:00:00Z"), to = Instant.parse("2026-10-28T22:00:00Z"))
    } returns listOf(
      BookListening(hobbit, listened = 40.minutes, finished = false),
      BookListening(deleted, listened = 15.minutes, finished = false),
      BookListening(pippi, listened = 10.minutes, finished = true),
    )

    test {
      val today = awaitState(ReportPeriod.Today)

      // a deleted book isn't listed, but its time still counts
      assertEquals(expected = 65.minutes, actual = today.listened)
      assertEquals(expected = listOf("The Hobbit", "Pippi Longstocking"), actual = today.books.map { it.name })
      assertEquals(expected = listOf(false, true), actual = today.books.map { it.finished })
      assertEquals(expected = emptyList(), actual = today.days)
    }
  }

  @Test
  fun `the week runs from Monday to Sunday with the time of each day`() {
    val monday = Instant.parse("2026-10-25T22:00:00Z")
    val nextMonday = Instant.parse("2026-11-01T22:00:00Z")
    coEvery { statsRepo.listenedPerBook(any(), any()) } returns emptyList()
    coEvery { statsRepo.listenedPerBook(monday, nextMonday) } returns listOf(
      BookListening(hobbit, listened = 2.hours, finished = false),
    )
    coEvery { statsRepo.listenedPerDay(monday, nextMonday) } returns mapOf(
      LocalDate.of(2026, 10, 26) to 30.minutes,
      LocalDate.of(2026, 10, 28) to 90.minutes,
    )

    test {
      awaitState(ReportPeriod.Today)
      viewModel.onPeriodChange(ReportPeriod.Week)
      val week = awaitState(ReportPeriod.Week)

      assertEquals(expected = 2.hours, actual = week.listened)
      assertEquals(
        expected = DayOfWeek.entries.map { day ->
          ReportDay(
            day = day,
            listened = when (day) {
              DayOfWeek.MONDAY -> 30.minutes
              DayOfWeek.WEDNESDAY -> 90.minutes
              else -> Duration.ZERO
            },
            isToday = day == DayOfWeek.WEDNESDAY,
          )
        },
        actual = week.days,
      )
    }
  }

  @Test
  fun `tapping a book opens its bookmarks and history`() {
    viewModel.onBookClick(hobbit)

    verify { navigator.goTo(Destination.Bookmarks(hobbit)) }
  }
}
