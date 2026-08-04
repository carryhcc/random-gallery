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

    private val _picState = MutableStateFlow<UiState<PicVO>>(UiState.Loading)
    val picState: StateFlow<UiState<PicVO>> = _picState.asStateFlow()

    private val _groupState = MutableStateFlow<GroupVO?>(null)
    val groupState: StateFlow<GroupVO?> = _groupState.asStateFlow()

    // 换图只请求一次：套图名不阻塞图片展示，按需解析
    fun loadRandomPic() {
        _picState.value = UiState.Loading
        viewModelScope.launch {
            _picState.value = repository().getRandomPic().fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.message ?: "加载失败") }
            )
            _groupState.value = null
        }
    }

    /** 按需解析当前图的套图名（仅查看套图时触发，避免每张图两次请求） */
    suspend fun resolveGroupName(): String? {
        val gid = (_picState.value as? UiState.Success)?.data?.groupId ?: return null
        val cached = _groupState.value
        if (cached?.groupId == gid) return cached.groupName
        return repository().getRandomGroupInfo(gid).getOrNull()?.groupName?.also { name ->
            _groupState.value = GroupVO(groupId = gid, groupName = name)
        }
    }
}