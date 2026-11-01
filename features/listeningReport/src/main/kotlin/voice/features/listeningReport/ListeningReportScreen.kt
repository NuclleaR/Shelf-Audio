@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package voice.features.listeningReport

import android.icu.text.DisplayContext
import android.icu.text.MeasureFormat
import android.icu.text.RelativeDateTimeFormatter
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import android.icu.util.ULocale
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import coil.compose.AsyncImage
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import voice.core.common.rootGraphAs
import voice.core.data.BookId
import voice.core.ui.CenteredContent
import voice.core.ui.ShapedIcon
import voice.core.ui.VoiceTheme
import voice.core.ui.bookText
import voice.core.ui.icons.VoiceIcons
import voice.core.ui.rememberCoverThumbnailRequest
import voice.navigation.Destination
import voice.navigation.NavEntryProvider
import java.time.DayOfWeek
import java.time.format.TextStyle
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import voice.core.strings.R as StringsR
import voice.core.ui.R as UiR

@ContributesTo(AppScope::class)
interface ListeningReportGraph {
  val listeningReportViewModel: ListeningReportViewModel
}

@BindingContainer
@ContributesTo(AppScope::class)
object ListeningReportProvider {

  @Provides
  @IntoSet
  fun listeningReportNavEntryProvider(): NavEntryProvider<*> = NavEntryProvider<Destination.ListeningReport> { key ->
    NavEntry(key) {
      ListeningReportScreen()
    }
  }
}

@Composable
fun ListeningReportScreen() {
  val viewModel = retain { rootGraphAs<ListeningReportGraph>().listeningReportViewModel }
  ListeningReport(
    viewState = viewModel.viewState(),
    onClose = viewModel::onCloseClick,
    onPeriodChange = viewModel::onPeriodChange,
    onBookClick = viewModel::onBookClick,
  )
}

@Composable
private fun ListeningReport(
  viewState: ListeningReportViewState?,
  onClose: () -> Unit,
  onPeriodChange: (ReportPeriod) -> Unit,
  onBookClick: (BookId) -> Unit,
) {
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
  Scaffold(
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      LargeFlexibleTopAppBar(
        title = { Text(stringResource(StringsR.string.listening_report_title)) },
        navigationIcon = {
          val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
          IconButton(onClick = onClose) {
            Icon(
              modifier = Modifier.graphicsLayer { scaleX = if (rtl) -1F else 1F },
              imageVector = VoiceIcons.ArrowBack,
              contentDescription = stringResource(StringsR.string.common_action_close),
            )
          }
        },
        scrollBehavior = scrollBehavior,
      )
    },
  ) { contentPadding ->
    val layoutDirection = LocalLayoutDirection.current
    CenteredContent(
      modifier = Modifier
        .fillMaxSize()
        .padding(
          start = contentPadding.calculateStartPadding(layoutDirection),
          top = contentPadding.calculateTopPadding(),
          end = contentPadding.calculateEndPadding(layoutDirection),
        ),
    ) { horizontalPadding ->
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
          start = horizontalPadding,
          end = horizontalPadding,
          top = 8.dp,
          bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        item(key = "period") {
          PeriodToggle(
            period = viewState?.period ?: ReportPeriod.Today,
            onPeriodChange = onPeriodChange,
          )
        }
        if (viewState != null) {
          item(key = "total") {
            Text(
              modifier = Modifier
                .fillMaxWidth()
                .semantics { heading() },
              text = listenedText(viewState.listened),
              style = MaterialTheme.typography.displaySmall.copy(fontFamily = FontFamily.Default),
              textAlign = TextAlign.Center,
            )
          }
          if (viewState.days.isNotEmpty()) {
            item(key = "days") {
              DayBars(viewState.days)
            }
          }
          item(key = "books") {
            if (viewState.books.isEmpty()) {
              NothingListened()
            } else {
              Books(viewState.books, onBookClick)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PeriodToggle(
  period: ReportPeriod,
  onPeriodChange: (ReportPeriod) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
  ) {
    ReportPeriod.entries.forEachIndexed { index, option ->
      ToggleButton(
        checked = option == period,
        onCheckedChange = { onPeriodChange(option) },
        modifier = Modifier.weight(1F),
        shapes = if (index == 0) {
          ButtonGroupDefaults.connectedLeadingButtonShapes()
        } else {
          ButtonGroupDefaults.connectedTrailingButtonShapes()
        },
      ) {
        Text(
          text = periodText(option),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun DayBars(days: List<ReportDay>) {
  val locale = LocalConfiguration.current.locales[0]
  val longest = days.maxOf { it.listened }
  Surface(
    shape = RoundedCornerShape(28.dp),
    color = MaterialTheme.colorScheme.surfaceContainer,
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(180.dp)
        .padding(16.dp),
    ) {
      days.forEach { day ->
        val description = "${day.day.getDisplayName(TextStyle.FULL, locale)}: ${listenedText(day.listened)}"
        val fraction by animateFloatAsState(
          targetValue = if (longest > Duration.ZERO) (day.listened / longest).toFloat() else 0F,
          animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
          label = "dayBar",
        )
        Column(
          modifier = Modifier
            .weight(1F)
            .fillMaxHeight()
            .clearAndSetSemantics { contentDescription = description },
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          // the label takes its space first, so even the longest bar leaves room for it
          Column(
            modifier = Modifier
              .weight(1F)
              .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
          ) {
            if (day.listened > Duration.ZERO) {
              Text(
                text = listenedText(day.listened, hoursAndMinutesOnTwoLines = true),
                style = MaterialTheme.typography.labelSmall,
                color = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
              )
              Spacer(Modifier.height(4.dp))
            }
            Box(
              modifier = Modifier
                .width(20.dp)
                .heightIn(min = 4.dp)
                .fillMaxHeight(fraction)
                .clip(RoundedCornerShape(8.dp))
                .background(
                  when {
                    day.listened == Duration.ZERO -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12F)
                    day.isToday -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45F)
                  },
                ),
            )
          }
          Spacer(Modifier.height(8.dp))
          Text(
            text = day.day.getDisplayName(TextStyle.SHORT, locale),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (day.isToday) FontWeight.Bold else null,
            color = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
          )
        }
      }
    }
  }
}

@Composable
private fun Books(
  books: List<ReportBook>,
  onBookClick: (BookId) -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(28.dp),
    color = MaterialTheme.colorScheme.surfaceContainer,
  ) {
    Column(Modifier.padding(vertical = 8.dp)) {
      books.forEach { book ->
        BookRow(book = book, onClick = { onBookClick(book.id) })
      }
    }
  }
}

@Composable
private fun BookRow(
  book: ReportBook,
  onClick: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 8.dp)
      .clip(RoundedCornerShape(20.dp))
      .clickable(onClick = onClick)
      .padding(8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Box {
      AsyncImage(
        modifier = Modifier
          .size(56.dp)
          .clip(RoundedCornerShape(12.dp)),
        model = rememberCoverThumbnailRequest(book.cover),
        placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceContainerHighest),
        error = painterResource(id = UiR.drawable.album_art),
        contentScale = ContentScale.Crop,
        contentDescription = null,
      )
      if (book.finished) {
        FinishedBadge(
          Modifier
            .align(Alignment.BottomEnd)
            .offset(x = 4.dp, y = 4.dp),
        )
      }
    }
    Column(Modifier.weight(1F)) {
      Text(
        text = book.name,
        style = MaterialTheme.typography.bodyLarge.bookText(),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      if (book.author != null) {
        Text(
          text = book.author,
          style = MaterialTheme.typography.bodyMedium.bookText(),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
    Text(
      text = listenedText(book.listened),
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun FinishedBadge(modifier: Modifier = Modifier) {
  val description = stringResource(StringsR.string.listening_report_finished)
  Box(
    modifier = modifier
      .size(24.dp)
      .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
      .padding(2.dp)
      .background(MaterialTheme.colorScheme.primary, CircleShape)
      .semantics { contentDescription = description },
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      modifier = Modifier.size(14.dp),
      imageVector = VoiceIcons.Check,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onPrimary,
    )
  }
}

@Composable
private fun NothingListened() {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    ShapedIcon(
      icon = VoiceIcons.Headphones,
      shape = MaterialShapes.Cookie9Sided,
      containerColor = MaterialTheme.colorScheme.secondaryContainer,
      contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
      size = 56.dp,
    )
    Spacer(Modifier.height(12.dp))
    Text(
      text = stringResource(StringsR.string.listening_report_empty),
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
  }
}

@Composable
private fun periodText(period: ReportPeriod): String {
  val locale = LocalConfiguration.current.locales[0]
  val format = remember(locale) {
    RelativeDateTimeFormatter.getInstance(
      ULocale.forLocale(locale),
      null,
      RelativeDateTimeFormatter.Style.LONG,
      DisplayContext.CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE,
    )
  }
  val unit = when (period) {
    ReportPeriod.Today -> RelativeDateTimeFormatter.AbsoluteUnit.DAY
    ReportPeriod.Week -> RelativeDateTimeFormatter.AbsoluteUnit.WEEK
  }
  return format.format(RelativeDateTimeFormatter.Direction.THIS, unit)
}

/**
 * Hours and minutes as people say them, like "1 hr, 29 min", or seconds below a minute, in the user's language.
 */
@Composable
private fun listenedText(
  duration: Duration,
  hoursAndMinutesOnTwoLines: Boolean = false,
): String {
  val locale = LocalConfiguration.current.locales[0]
  val format = remember(locale) { MeasureFormat.getInstance(locale, MeasureFormat.FormatWidth.SHORT) }
  return duration.toComponents { hours, minutes, seconds, _ ->
    when {
      hours > 0 && minutes > 0 && hoursAndMinutesOnTwoLines -> {
        format.format(Measure(hours, MeasureUnit.HOUR)) + "\n" + format.format(Measure(minutes, MeasureUnit.MINUTE))
      }
      hours > 0 && minutes > 0 -> format.formatMeasures(Measure(hours, MeasureUnit.HOUR), Measure(minutes, MeasureUnit.MINUTE))
      hours > 0 -> format.format(Measure(hours, MeasureUnit.HOUR))
      minutes == 0 && seconds > 0 -> format.format(Measure(seconds, MeasureUnit.SECOND))
      else -> format.format(Measure(minutes, MeasureUnit.MINUTE))
    }
  }
}

private val previewBooks = listOf(
  ReportBook(
    id = BookId("hobbit"),
    name = "The Hobbit",
    author = "J. R. R. Tolkien",
    cover = null,
    listened = 1.hours + 5.minutes,
    finished = false,
  ),
  ReportBook(
    id = BookId("pippi"),
    name = "Pippi Longstocking",
    author = "Astrid Lindgren",
    cover = null,
    listened = 25.minutes,
    finished = true,
  ),
)

@Preview
@Composable
private fun ListeningReportTodayPreview() {
  VoiceTheme {
    ListeningReport(
      viewState = ListeningReportViewState(
        period = ReportPeriod.Today,
        listened = 1.hours + 30.minutes,
        days = emptyList(),
        books = previewBooks,
      ),
      onClose = {},
      onPeriodChange = {},
      onBookClick = {},
    )
  }
}

@Preview
@Composable
private fun ListeningReportWeekPreview() {
  val listened = listOf(40.minutes, 1.hours + 10.minutes, Duration.ZERO, 1.hours + 30.minutes, 20.minutes, 45.seconds, Duration.ZERO)
  VoiceTheme {
    ListeningReport(
      viewState = ListeningReportViewState(
        period = ReportPeriod.Week,
        listened = 3.hours + 40.minutes,
        days = DayOfWeek.entries.mapIndexed { index, day ->
          ReportDay(day = day, listened = listened[index], isToday = day == DayOfWeek.THURSDAY)
        },
        books = previewBooks,
      ),
      onClose = {},
      onPeriodChange = {},
      onBookClick = {},
    )
  }
}

@Preview
@Composable
private fun ListeningReportEmptyPreview() {
  VoiceTheme {
    ListeningReport(
      viewState = ListeningReportViewState(
        period = ReportPeriod.Today,
        listened = Duration.ZERO,
        days = emptyList(),
        books = emptyList(),
      ),
      onClose = {},
      onPeriodChange = {},
      onBookClick = {},
    )
  }
}
