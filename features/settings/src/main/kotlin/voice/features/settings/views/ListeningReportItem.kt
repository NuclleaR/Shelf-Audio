@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.settings.views

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import voice.core.ui.ShapedIcon
import voice.core.ui.icons.VoiceIcons
import voice.core.strings.R as StringsR

@Composable
internal fun ListeningReportItem(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = IslandShape,
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
  ) {
    IslandRow(
      modifier = Modifier.padding(vertical = 8.dp),
      title = stringResource(StringsR.string.listening_report_title),
      summary = { Text(stringResource(StringsR.string.listening_report_summary)) },
      onClick = onClick,
      leading = {
        ShapedIcon(
          icon = VoiceIcons.Headphones,
          shape = MaterialShapes.Cookie9Sided,
          containerColor = MaterialTheme.colorScheme.secondary,
          contentColor = MaterialTheme.colorScheme.onSecondary,
        )
      },
    )
  }
}
