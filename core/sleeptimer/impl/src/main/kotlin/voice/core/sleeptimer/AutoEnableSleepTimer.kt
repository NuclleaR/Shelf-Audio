package voice.core.sleeptimer

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import voice.core.initializer.AppInitializer
import voice.core.playback.playstate.PlayStateManager
import voice.core.playback.playstate.PlayStateManager.PlayState.Playing
import java.time.Clock
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime

@ContributesIntoSet(AppScope::class)
class AutoEnableSleepTimer(
  private val sleepTimerPreferenceStore: SleepTimerPreferenceStore,
  private val playStateManager: PlayStateManager,
  private val sleepTimer: SleepTimer,
  private val clock: Clock,
  private val scope: CoroutineScope,
) : AppInitializer {

  override fun onAppStart(application: Application) {
    scope.launch {
      playStateManager.playStateFlow.collectLatest { playState ->
        if (playState == Playing) enableWhenTimeWindowStarts()
      }
    }
  }

  private suspend fun enableWhenTimeWindowStarts() {
    while (true) {
      val autoSleepTimer = sleepTimerPreferenceStore.data.first()
      if (!autoSleepTimer.autoSleepTimerEnabled) return
      val now = clock.instant().atZone(clock.zone)
      val inTimeWindow = isTimeInRange(
        currentTime = now.toLocalTime(),
        startTime = autoSleepTimer.autoSleepStartTime,
        endTime = autoSleepTimer.autoSleepEndTime,
      )
      if (inTimeWindow) {
        if (!sleepTimer.state.value.enabled) {
          sleepTimer.enable(SleepTimerMode.TimedWithDefault)
        }
        return
      }
      delay(millisUntilTimeWindowStarts(now, autoSleepTimer.autoSleepStartTime))
    }
  }

  private fun millisUntilTimeWindowStarts(
    now: ZonedDateTime,
    startTime: LocalTime,
  ): Long {
    val start = now.with(startTime).let { if (it.isBefore(now)) it.plusDays(1) else it }
    // the window only begins after its start time
    return Duration.between(now, start).toMillis() + 1
  }
}
