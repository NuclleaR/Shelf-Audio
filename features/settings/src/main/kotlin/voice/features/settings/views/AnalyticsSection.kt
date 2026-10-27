@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.settings.views

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import voice.core.ui.ShapedIcon
import voice.core.ui.icons.VoiceIcons
import voice.core.strings.R as StringsR

@Composable
internal fun AnalyticsSection(
  analyticsEnabled: Boolean,
  onAnalyticsToggle: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  SettingsIsland(
    modifier = modifier,
    title = stringResource(StringsR.string.settings_analytics_consent_title),
    containerColor = colors.surfaceContainerHigh,
  ) {
    IslandSwitchRow(
      title = stringResource(StringsR.string.settings_analytics_consent_description),
      checked = analyticsEnabled,
      onCheckedChange = { onAnalyticsToggle() },
      leading = {
        ShapedIcon(
          icon = VoiceIcons.Analytics,
          shape = MaterialShapes.Square,
          containerColor = colors.surfaceContainerHighest,
          contentColor = colors.onSurfaceVariant,
        )
      },
    )
  }
}
