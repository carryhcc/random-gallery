package com.example.randomgallery.android.ui.gif

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.randomgallery.android.AppContainer
import com.example.randomgallery.android.R
import com.example.randomgallery.android.ui.common.SimpleViewModelFactory
import com.example.randomgallery.android.ui.theme.RandomGalleryTheme

class RandomGifFragment : Fragment() {

    private val viewModel: RandomGifViewModel by viewModels {
        SimpleViewModelFactory { RandomGifViewModel(AppContainer.repository(requireContext())) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                RandomGalleryTheme {
                    RandomGifScreen(
                        viewModel = viewModel,
                        onBack = { findNavController().navigateUp() },
                        onDetail = { workId ->
                            findNavController().navigate(
                                R.id.downloadDetailFragment,
                                bundleOf("workId" to workId)
                            )
                        },
                        onAuthor = { authorId ->
                            findNavController().navigate(
                                R.id.downloadListFragment,
                                bundleOf("filterAuthorId" to authorId)
                            )
                        }
                    )
                }
            }
        }
    }
}
