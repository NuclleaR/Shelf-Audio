package voice.features.settings.developer

import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import voice.core.common.DispatcherProvider
import voice.features.settings.DeveloperMenuUnlockedStore
import voice.features.settings.MemoryDataStore
import voice.navigation.Navigator
import kotlin.test.Test
import kotlin.test.assertFalse

class DeveloperSettingsViewModelTest {

  private val scope = TestScope()

  @Test
  fun `hiding the developer menu locks it and leaves it`() = scope.runTest {
    val unlocked = MemoryDataStore(true)
    val navigator = mockk<Navigator>(relaxUnitFun = true)
    val viewModel = DeveloperSettingsViewModel(
      navigator = navigator,
      fmcTokenProvider = mockk(),
      remoteConfig = mockk(),
      dispatcherProvider = DispatcherProvider(scope.coroutineContext, scope.coroutineContext, scope.coroutineContext),
      featureFlags = emptySet(),
      developerMenuUnlockedStore = DeveloperMenuUnlockedStore(unlocked),
    )

    viewModel.hideDeveloperMenu()
    runCurrent()

    assertFalse(unlocked.data.first())
    verify { navigator.goBack() }
  }
}
