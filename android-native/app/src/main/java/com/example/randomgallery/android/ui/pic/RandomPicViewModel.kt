package com.example.randomgallery.android.ui.pic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.randomgallery.android.data.model.GroupVO
import com.example.randomgallery.android.data.model.PicVO
import com.example.randomgallery.android.data.repository.GalleryRepository
import com.example.randomgallery.android.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RandomPicViewModel(
    private val repository: GalleryRepository
) : ViewModel() {

    private val _picState = MutableStateFlow<UiState<PicVO>>(UiState.Loading)
    val picState: StateFlow<UiState<PicVO>> = _picState.asStateFlow()

    private val _groupState = MutableStateFlow<GroupVO?>(null)
    val groupState: StateFlow<GroupVO?> = _groupState.asStateFlow()

    fun loadRandomPic() {
        viewModelScope.launch {
            val picResult = repository.getRandomPic()
            _picState.value = picResult.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.message ?: "加载失败") }
            )
            picResult.getOrNull()?.groupId?.let { gid ->
                _groupState.value = repository.getRandomGroupInfo(gid).getOrNull()
            }
        }
    }
}
