package com.example.randomgallery.android.ui.downloaddetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.randomgallery.android.data.model.XhsWorkDetailVO
import com.example.randomgallery.android.data.repository.GalleryRepository
import com.example.randomgallery.android.ui.common.UiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class DownloadDetailViewModel(
    private val repository: GalleryRepository
) : ViewModel() {

    private val _detail = MutableStateFlow<UiState<XhsWorkDetailVO>>(UiState.Loading)
    val detail: StateFlow<UiState<XhsWorkDetailVO>> = _detail.asStateFlow()

    private val _deleteWorkEvents = Channel<Result<String>>(Channel.BUFFERED)
    val deleteWorkEvents: Flow<Result<String>> = _deleteWorkEvents.receiveAsFlow()

    private val _deleteMediaEvents = Channel<Result<String>>(Channel.BUFFERED)
    val deleteMediaEvents: Flow<Result<String>> = _deleteMediaEvents.receiveAsFlow()

    fun load(workId: String) {
        viewModelScope.launch {
            val result = repository.getWorkDetail(workId)
            _detail.value = result.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.message ?: "加载失败") }
            )
        }
    }

    fun deleteWork(workId: String) {
        viewModelScope.launch {
            _deleteWorkEvents.trySend(repository.deleteWork(workId))
        }
    }

    fun deleteMedia(mediaId: Long, workId: String) {
        viewModelScope.launch {
            val result = repository.deleteMedia(mediaId)
            _deleteMediaEvents.trySend(result)
            if (result.isSuccess) {
                val detailResult = repository.getWorkDetail(workId)
                _detail.value = detailResult.fold(
                    onSuccess = { UiState.Success(it) },
                    onFailure = { UiState.Error(it.message ?: "加载失败") }
                )
            }
        }
    }
}
