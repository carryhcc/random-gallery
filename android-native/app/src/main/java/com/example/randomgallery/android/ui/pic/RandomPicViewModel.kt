package com.example.randomgallery.android.ui.pic

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.randomgallery.android.data.model.GroupVO
import com.example.randomgallery.android.data.model.PicVO
import com.example.randomgallery.android.data.repository.GalleryRepository
import kotlinx.coroutines.launch

class RandomPicViewModel(
    private val repository: GalleryRepository
) : ViewModel() {

    private val _picState = MutableLiveData<Result<PicVO>>()
    val picState: LiveData<Result<PicVO>> = _picState

    private val _groupState = MutableLiveData<Result<GroupVO>>()
    val groupState: LiveData<Result<GroupVO>> = _groupState

    fun loadRandomPic() {
        viewModelScope.launch {
            val picResult = repository.getRandomPic()
            _picState.value = picResult
            picResult.getOrNull()?.groupId?.let { gid ->
                _groupState.value = repository.getRandomGroupInfo(gid)
            }
        }
    }
}
