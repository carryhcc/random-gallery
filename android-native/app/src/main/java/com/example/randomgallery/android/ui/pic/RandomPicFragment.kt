package com.example.randomgallery.android.ui.pic

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

class RandomPicFragment : Fragment() {

    private val viewModel: RandomPicViewModel by viewModels {
        SimpleViewModelFactory { RandomPicViewModel(AppContainer.repository(requireContext())) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                RandomGalleryTheme {
                    RandomPicScreen(
                        viewModel = viewModel,
                        onBack = { findNavController().navigateUp() },
                        onGroupClick = { groupId, groupName ->
                            val bundle = android.os.Bundle().apply {
                                putLong("groupId", groupId)
                                putString("groupName", groupName)
                            }
                            findNavController().navigate(R.id.picListFragment, bundle)
                        }
                    )
                }
            }
        }
    }
}
