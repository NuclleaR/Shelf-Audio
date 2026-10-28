@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.playbackScreen.view

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventInput
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val CLOSE_FRACTION = 1F / 3
private const val FLING_CLOSE_FRACTION = 1F / 8
private val FlingVelocity = 1000.dp

/**
 * Dragging the player down plays the same transition as the back gesture, so the cover and play button fly back
 * into the library while the finger moves.
 */
@Stable
internal class SwipeDownToCloseState(
  private val scope: CoroutineScope,
  private val settleSpec: AnimationSpec<Float>,
) {

  internal var offset by mutableFloatStateOf(0F)
    private set

  internal val input = BackInput()
  private var dragging = false
  private var settle: Job? = null

  fun drag(
    delta: Float,
    height: Int,
    timeMillis: Long,
  ) {
    settle?.cancel()
    offset = (offset + delta).coerceAtLeast(0F)
    if (!dragging && offset > 0F) {
      dragging = true
      input.start(event(height, timeMillis))
    }
    if (dragging) input.progress(event(height, timeMillis))
  }

  fun release(
    velocity: Float,
    flingVelocity: Float,
    height: Int,
  ) {
    if (!dragging) return
    dragging = false
    val close = when {
      velocity > flingVelocity -> offset > height * FLING_CLOSE_FRACTION
      velocity < -flingVelocity -> false
      else -> offset > height * CLOSE_FRACTION
    }
    if (close) {
      // the player stays where the finger left it, so the cover flies on from there
      input.complete()
    } else {
      input.cancel()
      settleBack()
    }
  }

  fun cancel() {
    if (!dragging) return
    dragging = false
    input.cancel()
    settleBack()
  }

  private fun settleBack() {
    settle = scope.launch {
      animate(initialValue = offset, targetValue = 0F, animationSpec = settleSpec) { value, _ ->
        offset = value
      }
    }
  }

  private fun event(
    height: Int,
    timeMillis: Long,
  ) = NavigationEvent(
    touchY = offset,
    progress = (offset / height).coerceIn(0F, 1F),
    frameTimeMillis = timeMillis,
  )

  internal class BackInput : NavigationEventInput() {
    fun start(event: NavigationEvent) = dispatchOnBackStarted(event)

    fun progress(event: NavigationEvent) = dispatchOnBackProgressed(event)

    fun complete() = dispatchOnBackCompleted()

    fun cancel() = dispatchOnBackCancelled()
  }
}

@Composable
internal fun rememberSwipeDownToCloseState(): SwipeDownToCloseState {
  val scope = rememberCoroutineScope()
  val settleSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
  val state = remember { SwipeDownToCloseState(scope, settleSpec) }
  val dispatcher = LocalNavigationEventDispatcherOwner.current?.navigationEventDispatcher
  DisposableEffect(dispatcher) {
    dispatcher?.addInput(state.input)
    onDispose { dispatcher?.removeInput(state.input) }
  }
  return state
}

internal fun Modifier.swipeDownToClose(state: SwipeDownToCloseState): Modifier = this
  // before the layer, so the finger isn't measured against the player it moves
  .pointerInput(state) {
    val velocityTracker = VelocityTracker()
    detectVerticalDragGestures(
      onDragStart = { velocityTracker.resetTracking() },
      onDragEnd = {
        state.release(
          velocity = velocityTracker.calculateVelocity().y,
          flingVelocity = FlingVelocity.toPx(),
          height = size.height,
        )
      },
      onDragCancel = state::cancel,
      onVerticalDrag = { change, dragAmount ->
        velocityTracker.addPosition(change.uptimeMillis, change.position)
        state.drag(dragAmount, size.height, change.uptimeMillis)
      },
    )
  }
  .graphicsLayer { translationY = state.offset }
