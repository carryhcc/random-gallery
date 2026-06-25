package com.example.randomgallery.android.ui.piclist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.randomgallery.android.AppContainer
import com.example.randomgallery.android.ui.common.SimpleViewModelFactory
import com.example.randomgallery.android.ui.theme.RandomGalleryTheme

class PicListFragment : Fragment() {

    private val viewModel: PicListViewModel by viewModels {
        SimpleViewModelFactory { PicListViewModel(AppContainer.repository(requireContext())) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val groupId = arguments?.getLong("groupId") ?: 0L
        val groupName = arguments?.getString("groupName") ?: "套图详情"

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                RandomGalleryTheme {
                    PicListScreen(
                        viewModel = viewModel,
                        groupId = groupId,
                        groupName = groupName,
                        onBack = { findNavController().navigateUp() }
                    )
                }
            }
        }
    }
}
