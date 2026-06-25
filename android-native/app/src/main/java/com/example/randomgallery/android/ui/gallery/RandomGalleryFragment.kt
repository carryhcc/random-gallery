package com.example.randomgallery.android.ui.gallery

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

/**
 * 随机图库页：已迁移到 Jetpack Compose（小红书风瀑布流）。
 * ViewModel 仍复用原有 RandomGalleryViewModel，仅 View 层换成 Compose。
 */
class RandomGalleryFragment : Fragment() {

    private val viewModel: RandomGalleryViewModel by viewModels {
        SimpleViewModelFactory {
            RandomGalleryViewModel(AppContainer.repository(requireContext()))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                RandomGalleryTheme {
                    RandomGalleryScreen(
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
