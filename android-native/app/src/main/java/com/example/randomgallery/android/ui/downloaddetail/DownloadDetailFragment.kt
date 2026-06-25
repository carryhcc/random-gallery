package com.example.randomgallery.android.ui.downloaddetail

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

class DownloadDetailFragment : Fragment() {

    private val viewModel: DownloadDetailViewModel by viewModels {
        SimpleViewModelFactory { DownloadDetailViewModel(AppContainer.repository(requireContext())) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val workId = arguments?.getString("workId") ?: ""
        val coverImageUrl = arguments?.getString("coverImageUrl") ?: ""

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                RandomGalleryTheme {
                    DownloadDetailScreen(
                        viewModel = viewModel,
                        workId = workId,
                        coverImageUrl = coverImageUrl,
                        onBack = { findNavController().navigateUp() },
                        onAuthorClick = { authorId, _ ->
                            val bundle = Bundle().apply { putString("filterAuthorId", authorId) }
                            findNavController().navigate(R.id.downloadListFragment, bundle)
                        },
                        onTagClick = { tag ->
                            val bundle = Bundle().apply { putString("filterKeyword", tag) }
                            findNavController().navigate(R.id.downloadListFragment, bundle)
                        }
                    )
                }
            }
        }
    }
}
