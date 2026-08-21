package com.example.randomgallery.android.ui.pic

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.randomgallery.android.AppContainer
import com.example.randomgallery.android.data.model.GroupVO
import com.example.randomgallery.android.data.model.PicVO
import com.example.randomgallery.android.data.repository.GalleryRepository
import com.example.randomgallery.android.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RandomPicViewModel(
    private val appContext: Context
) : ViewModel() {

    private fun repository(): GalleryRepository = AppContainer.repository(appContext)

    private val _picList = MutableStateFlow<List<PicVO>>(emptyList())
    val picList: StateFlow<List<PicVO>> = _picList.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _groupState = MutableStateFlow<GroupVO?>(null)
    val groupState: StateFlow<GroupVO?> = _groupState.asStateFlow()

    private var isLoadingNext = false

    fun loadRandomPic() {
        _picList.value = emptyList()
        _error.value = null
        loadNext()
    }

    fun loadNext() {
        if (isLoadingNext) return
        isLoadingNext = true
        if (_picList.value.isEmpty()) _loading.value = true
        viewModelScope.launch {
            repository().getRandomPic().fold(
                onSuccess = { pic ->
                    _picList.value = _picList.value + pic
                    _error.value = null
                },
                onFailure = { err ->
                    if (_picList.value.isEmpty()) _error.value = err.message ?: "加载失败"
                }
            )
            _loading.value = false
            isLoadingNext = false
        }
    }

    /** 按需解析指定 groupId 的套图名 */
    suspend fun resolveGroupName(groupId: Long?): String? {
        if (groupId == null) return null
        val cached = _groupState.value
        if (cached?.groupId == groupId) return cached.groupName
        return repository().getRandomGroupInfo(groupId).getOrNull()?.groupName?.also { name ->
            _groupState.value = GroupVO(groupId = groupId, groupName = name)
        }
    }
}