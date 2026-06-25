package com.example.randomgallery.android.ui.group

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

class GroupListFragment : Fragment() {

    private val viewModel: GroupListViewModel by viewModels {
        SimpleViewModelFactory { GroupListViewModel(AppContainer.repository(requireContext())) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                RandomGalleryTheme {
                    GroupListScreen(
                        viewModel = viewModel,
                        onGroupClick = { group ->
                            val bundle = Bundle().apply {
                                putLong("groupId", group.groupId ?: 0)
                                putString("groupName", group.groupName ?: "套图详情")
                            }
                            findNavController().navigate(R.id.picListFragment, bundle)
                        },
                        onBack = { findNavController().navigateUp() }
                    )
                }
            }
        }
    }
}
