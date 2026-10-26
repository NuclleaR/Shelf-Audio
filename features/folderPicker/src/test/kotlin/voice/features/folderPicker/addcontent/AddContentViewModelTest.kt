package voice.features.folderPicker.addcontent

import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import voice.core.common.DispatcherProvider
import voice.navigation.Destination
import voice.navigation.Navigator
import voice.navigation.Origin
import kotlin.test.Test

class AddContentViewModelTest {

  private val navigator = mockk<Navigator>(relaxUnitFun = true)

  @Test
  fun `skipping the first start opens the library`() {
    val scope = TestScope()
    val viewModel = AddContentViewModel(
      audiobookFolders = mockk(),
      navigator = navigator,
      audiobookshelfFeatureFlag = mockk(),
      serverLibrary = mockk(),
      analytics = mockk(),
      dispatcherProvider = DispatcherProvider(scope.coroutineContext, scope.coroutineContext, scope.coroutineContext),
      origin = Origin.Onboarding,
    )

    viewModel.skip()

    verify { navigator.setRoot(Destination.BookOverview) }
  }
}
