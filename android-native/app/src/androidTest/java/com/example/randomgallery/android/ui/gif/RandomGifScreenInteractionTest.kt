package com.example.randomgallery.android.ui.gif

import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.randomgallery.android.data.model.RandomGifVO
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * REQ-09：卡片交互回归测试。
 *
 * 覆盖两处曾经真实出过问题的地方：
 * - 双击收藏：旧实现把 `lastTapTime` 声明在 `awaitEachGesture` 内部，导致双击永远判为超时；
 * - 上滑切换后进度指示跟随索引：旧实现有两个 `pointerInput` 争抢同一串事件。
 *
 * 注意：这些用例需要真机 / 模拟器才能执行；离线环境仅保证 `assembleDebugAndroidTest` 可编译。
 */
@RunWith(AndroidJUnit4::class)
class RandomGifScreenInteractionTest {

    @get:Rule
    val rule = createComposeRule()

    private fun gif(id: Long) = RandomGifVO(
        id = id,
        workId = "w$id",
        workTitle = "作品 $id",
        mediaUrl = "https://cdn.example.com/$id.mp4"
    )

    private fun setViewer(
        gifs: List<RandomGifVO>,
        onToggleFavorite: () -> Unit = {},
        onLoadNext: () -> Unit = {}
    ) {
        rule.setContent {
            SingleCardDynamicGifViewer(
                gifList = gifs,
                loading = false,
                error = null,
                isFavorited = false,
                onBack = {},
                onDetail = {},
                onAuthor = {},
                onLoadNext = onLoadNext,
                onToggleFavorite = onToggleFavorite,
                onSwitchMode = {}
            )
        }
    }

    @Test
    fun doubleTapOnCard_invokesFavoriteToggle() {
        var toggles = 0
        setViewer(gifs = listOf(gif(1), gif(2)), onToggleFavorite = { toggles++ })

        rule.onNodeWithTag("gif_card").performTouchInput { doubleClick() }
        rule.waitForIdle()

        assertTrue("双击应收敛为一次收藏切换，实际 $toggles 次", toggles == 1)
    }

    @Test
    fun swipeUp_movesProgressIndicatorToNextIndex() {
        setViewer(gifs = listOf(gif(1), gif(2), gif(3)))

        rule.onNodeWithTag("gif_progress_0").assertExists()

        rule.onNodeWithTag("gif_card").performTouchInput { swipeUp() }
        rule.waitForIdle()

        rule.onNodeWithTag("gif_progress_1").assertExists()
    }

    @Test
    fun singleGif_rendersNoProgressIndicator() {
        setViewer(gifs = listOf(gif(1)))

        rule.onNodeWithTag("gif_card").assertExists()
        rule.onNodeWithTag("gif_progress_0").assertDoesNotExist()
    }
}
