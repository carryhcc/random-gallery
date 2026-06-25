package com.example.randomgallery.android.ui.downloaddetail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.randomgallery.android.data.model.XhsWorkDetailVO
import com.example.randomgallery.android.data.repository.GalleryRepository
import com.example.randomgallery.android.ui.common.SingleLiveEvent
import kotlinx.coroutines.launch

class DownloadDetailViewModel(
    private val repository: GalleryRepository
) : ViewModel() {

    private val _detail = MutableLiveData<Result<XhsWorkDetailVO>>()
    val detail: LiveData<Result<XhsWorkDetailVO>> = _detail

    private val _deleteResult = SingleLiveEvent<Result<String>>()
    val deleteResult: LiveData<Result<String>> = _deleteResult

    private val _deleteMediaResult = SingleLiveEvent<Result<String>>()
    val deleteMediaResult: LiveData<Result<String>> = _deleteMediaResult

    fun load(workId: String) {
        viewModelScope.launch {
            _detail.value = repository.getWorkDetail(workId)
        }
    }

    fun deleteWork(workId: String) {
        viewModelScope.launch {
            _deleteResult.value = repository.deleteWork(workId)
        }
    }

    fun deleteMedia(mediaId: Long, workId: String) {
        viewModelScope.launch {
            val result = repository.deleteMedia(mediaId)
            _deleteMediaResult.value = result
            if (result.isSuccess) {
                _detail.value = repository.getWorkDetail(workId)
            }
        }
    }
}
