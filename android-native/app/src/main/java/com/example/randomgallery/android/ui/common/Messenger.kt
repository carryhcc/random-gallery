package com.example.randomgallery.android.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.randomgallery.android.ui.theme.XhsRed
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow

/** 全 App 唯一的顶部消息通道。UI 层挂 [TopMessageHost]，任何层调 [show]。 */
object Messenger {
    data class TopMessage(val text: String, val isError: Boolean = false)

    private val _messages = Channel<TopMessage>(Channel.BUFFERED)
    internal val messages = _messages.receiveAsFlow()

    fun show(text: String, isError: Boolean = false) {
        _messages.trySend(TopMessage(text, isError))
    }
}

/** 顶部品牌红 pill 提示（错误消息为深色底），从状态栏下方滑入/淡出。 */
@Composable
fun TopMessageHost(modifier: Modifier = Modifier) {
    var current by remember { mutableStateOf<Messenger.TopMessage?>(null) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        Messenger.messages.collect { msg ->
            current = msg
            visible = true
            delay(3000)
            visible = false
            delay(250)
            current = null
        }
    }

    Box(modifier.fillMaxWidth().statusBarsPadding(), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it }
        ) {
            current?.let { msg ->
                Text(
                    text = msg.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    maxLines = 2,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth(0.8f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (msg.isError) Color(0xF01A1A1A) else XhsRed)
                        .padding(horizontal = 20.dp, vertical = 11.dp)
                )
            }
        }
    }
}
