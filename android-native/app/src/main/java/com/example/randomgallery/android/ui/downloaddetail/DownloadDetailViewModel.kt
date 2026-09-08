package com.example.randomgallery.android.ui.downloaddetail

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.randomgallery.android.AppContainer
import com.example.randomgallery.android.data.model.XhsWorkDetailVO
import com.example.randomgallery.android.data.repository.GalleryRepository
import com.example.randomgallery.android.ui.common.SingleFlight
import com.example.randomgallery.android.ui.common.UiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class DownloadDetailViewModel(
    private val appContext: Context
) : ViewModel() {

    private fun repository(): GalleryRepository = AppContainer.repository(appContext)

    private val _detail = MutableStateFlow<UiState<XhsWorkDetailVO>>(UiState.Loading)
    val detail: StateFlow<UiState<XhsWorkDetailVO>> = _detail.asStateFlow()

    private val _deleteWorkEvents = Channel<Result<String>>(Channel.BUFFERED)
    val deleteWorkEvents: Flow<Result<String>> = _deleteWorkEvents.receiveAsFlow()

    private val _deleteMediaEvents = Channel<Result<String>>(Channel.BUFFERED)
    val deleteMediaEvents: Flow<Result<String>> = _deleteMediaEvents.receiveAsFlow()

    private val _refetchEvents = Channel<Result<String>>(Channel.BUFFERED)
    val refetchEvents: Flow<Result<String>> = _refetchEvents.receiveAsFlow()

    // 写操作 in-flight 忙碌锁：防止 0.5~1s 网络窗口内连点重复提交（删除/解析不可逆）
    private val writeFlight = SingleFlight()

    /** 任一写操作（删除作品/删除媒体/重新解析）进行中。UI 据此禁用确认按钮。 */
    val writeBusy: StateFlow<Boolean> = writeFlight.busy

    fun refetchWork(workId: String, workUrl: String?) {
        writeFlight.launch(viewModelScope) {
            val urlToSubmit = workUrl?.takeIf { it.isNotBlank() } ?: "https://www.xiaohongshu.com/explore/$workId"
            val result = repository().addDownloadTask(urlToSubmit)
            _refetchEvents.trySend(result)
            if (result.isSuccess) {
                val detailResult = repository().getWorkDetail(workId)
                _detail.value = detailResult.fold(
                    onSuccess = { UiState.Success(it) },
                    onFailure = { UiState.Error(it.message ?: "加载失败") }
                )
            }
        }
    }

    fun load(workId: String) {
        viewModelScope.launch {
            val result = repository().getWorkDetail(workId)
            _detail.value = result.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.message ?: "加载失败") }
            )
        }
    }

    fun deleteWork(workId: String) {
        writeFlight.launch(viewModelScope) {
            _deleteWorkEvents.trySend(repository().deleteWork(workId))
        }
    }

    fun deleteMedia(mediaId: Long, workId: String) {
        writeFlight.launch(viewModelScope) {
            val result = repository().deleteMedia(mediaId)
            _deleteMediaEvents.trySend(result)
            if (result.isSuccess) {
                val current = (_detail.value as? UiState.Success)?.data
                if (current != null) {
                    val updatedImages = current.images.filter { it.id != mediaId }
                    val updatedGifs = current.gifs.filter { it.id != mediaId }
                    _detail.value = UiState.Success(current.copy(images = updatedImages, gifs = updatedGifs))
                }
                val detailResult = repository().getWorkDetail(workId)
                _detail.value = detailResult.fold(
                    onSuccess = { UiState.Success(it) },
                    onFailure = {
                        current?.let { c ->
                            val updatedImages = c.images.filter { it.id != mediaId }
                            val updatedGifs = c.gifs.filter { it.id != mediaId }
                            UiState.Success(c.copy(images = updatedImages, gifs = updatedGifs))
                        } ?: UiState.Error(it.message ?: "加载失败")
                    }
                )
            }
        }
    }
}
