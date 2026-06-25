package com.example.randomgallery.android.ui.downloadlist

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
import com.example.randomgallery.android.R
import com.example.randomgallery.android.ui.common.SimpleViewModelFactory
import com.example.randomgallery.android.ui.theme.RandomGalleryTheme

class DownloadListFragment : Fragment() {

    private val viewModel: DownloadListViewModel by viewModels {
        SimpleViewModelFactory { DownloadListViewModel(AppContainer.repository(requireContext())) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Apply filter args from navigation before first load
        arguments?.getString("filterAuthorId")?.let { viewModel.authorId = it }
        arguments?.getString("filterKeyword")?.let { viewModel.keyword = it }

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                RandomGalleryTheme {
                    DownloadListScreen(
                        viewModel = viewModel,
                        onWorkClick = { workId, coverImageUrl ->
                            val bundle = Bundle().apply {
                                putString("workId", workId)
                                putString("coverImageUrl", coverImageUrl)
                            }
                            findNavController().navigate(R.id.downloadDetailFragment, bundle)
                        },
                        onBack = { findNavController().navigateUp() }
                    )
                }
            }
        }
    }
}
