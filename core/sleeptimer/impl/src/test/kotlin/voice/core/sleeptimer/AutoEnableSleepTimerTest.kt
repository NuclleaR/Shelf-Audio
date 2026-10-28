package voice.core.sleeptimer

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import voice.core.playback.playstate.PlayStateManager
import voice.core.sleeptimer.impl.MemoryDataStore
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class AutoEnableSleepTimerMinimalTest {
  private val testDispatcher = StandardTestDispatcher()
  private val testScope = TestScope(testDispatcher)
  private var startedAt = Instant.parse("2020-01-01T23:00:00Z")
  private val clock = object : Clock() {
    override fun instant(): Instant = startedAt.plusMillis(testDispatcher.scheduler.currentTime)

    override fun getZone(): ZoneId = ZoneId.of("UTC")

    override fun withZone(zone: ZoneId?): Clock = this
  }
  private val sleepTimerPreferenceStore = MemoryDataStore(SleepTimerPreference.Default)
  private val playStateManager = PlayStateManager()
  private val sleepTimer = mockk<SleepTimer>(relaxUnitFun = true) {
    every { state } returns MutableStateFlow(SleepTimerState.Disabled)
  }

  private fun prefs(
    enabled: Boolean,
    start: LocalTime = LocalTime.of(22, 0),
    end: LocalTime = LocalTime.of(6, 0),
    duration: Duration = 30.minutes,
  ) = SleepTimerPreference(
    autoSleepTimerEnabled = enabled,
    autoSleepStartTime = start,
    autoSleepEndTime = end,
    duration = duration,
  )

  private val sut = AutoEnableSleepTimer(
    sleepTimerPreferenceStore = SleepTimerPreferenceStore(sleepTimerPreferenceStore),
    playStateManager = playStateManager,
    sleepTimer = sleepTimer,
    clock = clock,
    scope = testScope.backgroundScope,
  )

  @Test
  fun `enables when playing and in time window`() = testScope.runTest {
    sleepTimerPreferenceStore.updateData { prefs(enabled = true) }

    sut.onAppStart(mockk())
    playStateManager.playState = PlayStateManager.PlayState.Playing
    runCurrent()

    coVerify { sleepTimer.enable(SleepTimerMode.TimedWithDefault) }
  }

  @Test
  fun `does nothing when sleeptimer is disabled`() = testScope.runTest {
    sleepTimerPreferenceStore.updateData { prefs(enabled = false) }

    sut.onAppStart(mockk())
    playStateManager.playState = PlayStateManager.PlayState.Playing
    runCurrent()

    coVerify(exactly = 0) { sleepTimer.enable(any()) }
  }

  @Test
  fun `enables when the time window starts during playback`() = testScope.runTest {
    startedAt = Instant.parse("2020-01-01T21:59:00Z")
    sleepTimerPreferenceStore.updateData { prefs(enabled = true) }

    sut.onAppStart(mockk())
    playStateManager.playState = PlayStateManager.PlayState.Playing
    advanceTimeBy(59.seconds)
    runCurrent()
    coVerify(exactly = 0) { sleepTimer.enable(any()) }

    advanceTimeBy(2.seconds)
    runCurrent()
    coVerify(exactly = 1) { sleepTimer.enable(SleepTimerMode.TimedWithDefault) }
  }

  @Test
  fun `does nothing when playback pauses before the time window starts`() = testScope.runTest {
    startedAt = Instant.parse("2020-01-01T21:59:00Z")
    sleepTimerPreferenceStore.updateData { prefs(enabled = true) }

    sut.onAppStart(mockk())
    playStateManager.playState = PlayStateManager.PlayState.Playing
    advanceTimeBy(30.seconds)
    playStateManager.playState = PlayStateManager.PlayState.Paused
    advanceTimeBy(2.minutes)
    runCurrent()

    coVerify(exactly = 0) { sleepTimer.enable(any()) }
  }
}
