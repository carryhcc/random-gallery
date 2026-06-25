package com.example.randomgallery.android.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.randomgallery.android.R
import com.example.randomgallery.android.ui.common.SimpleViewModelFactory
import com.example.randomgallery.android.ui.theme.RandomGalleryTheme

class HomeFragment : Fragment() {

    private val viewModel: HomeViewModel by viewModels {
        SimpleViewModelFactory { HomeViewModel(requireContext().applicationContext) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                RandomGalleryTheme {
                    HomeScreen(
                        viewModel = viewModel,
                        onGroupClick = { _ -> },
                        onNavigateToPicList = { groupId, groupName ->
                            if (groupId > 0L) {
                                val bundle = Bundle().apply {
                                    putLong("groupId", groupId)
                                    putString("groupName", groupName)
                                }
                                findNavController().navigate(R.id.picListFragment, bundle)
                            }
                        },
                        onNavigateToRandomPic = {
                            findNavController().navigate(R.id.randomPicFragment)
                        },
                        onNavigateToRandomGif = {
                            findNavController().navigate(R.id.randomGifFragment)
                        },
                        onNavigateToDownloadManage = {
                            findNavController().navigate(R.id.downloadManageFragment)
                        },
                        onNavigateToRandomGallery = {
                            findNavController().navigate(R.id.randomGalleryFragment)
                        },
                        onNavigateToGroupList = {
                            findNavController().navigate(R.id.groupListFragment)
                        },
                        onNavigateToDownloadList = {
                            findNavController().navigate(R.id.downloadListFragment)
                        }
                    )
                }
            }
        }
    }
}
