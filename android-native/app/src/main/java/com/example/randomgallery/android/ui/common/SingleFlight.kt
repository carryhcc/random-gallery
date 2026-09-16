package com.example.randomgallery.android.ui.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * In-flight 忙碌锁：同一时刻只允许一个 [launch] 协程运行。
 *
 * 用途：网络写操作（删除、提交、重试、抽选等）在 0.5~1s 往返窗口内被连点时，
 * 时间窗防抖（如 bouncyClickable 的 400ms）管不住，这里以"进行中忽略新调用"兜底。
 *
 * 等价于官方推荐的 busy 标志 + `Button(enabled = !busy)` 姿势的可复用化：
 *
 * ```
 * private val deleteFlight = SingleFlight()
 * val deleting: StateFlow<Boolean> = deleteFlight.busy
 *
 * fun deleteWork(id: String) = deleteFlight.launch(viewModelScope) {
 *     val result = repository().deleteWork(id)
 *     _events.trySend(result)
 * }
 * ```
 */
class SingleFlight {

    private val _busy = MutableStateFlow(false)

    /** 是否有任务正在执行。UI 可据此禁用按钮或显示进度。 */
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    /**
     * 若当前空闲则执行 [block]，否则忽略本次调用（不做排队）。
     * [block] 结束（成功/失败/取消）后自动复位；取消经 [kotlinx.coroutines.CancellationException]
     * 正常传播，不吞异常。
     */
    fun launch(scope: CoroutineScope, block: suspend () -> Unit) {
        // 同步检查 + 置位：主线程上无竞态，点击事件天然串行
        if (_busy.value) return
        _busy.value = true
        scope.launch {
            try {
                block()
            } finally {
                _busy.value = false
            }
        }
    }
}
