package com.example.randomgallery.android.ui

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.randomgallery.android.AppContainer
import com.example.randomgallery.android.R
import com.example.randomgallery.android.ui.download.DownloadManageScreen
import com.example.randomgallery.android.ui.download.DownloadManageViewModel
import com.example.randomgallery.android.ui.downloaddetail.DownloadDetailScreen
import com.example.randomgallery.android.ui.downloaddetail.DownloadDetailViewModel
import com.example.randomgallery.android.ui.downloadlist.DownloadListScreen
import com.example.randomgallery.android.ui.downloadlist.DownloadListViewModel
import com.example.randomgallery.android.ui.gallery.RandomGalleryScreen
import com.example.randomgallery.android.ui.gallery.RandomGalleryViewModel
import com.example.randomgallery.android.ui.gif.RandomGifScreen
import com.example.randomgallery.android.ui.gif.RandomGifViewModel
import com.example.randomgallery.android.ui.group.GroupListScreen
import com.example.randomgallery.android.ui.group.GroupListViewModel
import com.example.randomgallery.android.ui.home.HomeScreen
import com.example.randomgallery.android.ui.home.HomeViewModel
import com.example.randomgallery.android.ui.pic.RandomPicScreen
import com.example.randomgallery.android.ui.pic.RandomPicViewModel
import com.example.randomgallery.android.ui.piclist.PicListScreen
import com.example.randomgallery.android.ui.piclist.PicListViewModel
import com.example.randomgallery.android.util.showTopMessage

/**
 * 纯 Compose 导航宿主。替代原 Fragment + nav_graph.xml + BottomNavigationView 体系。
 * 整个 App 只有一个 MainActivity，页面切换为 Composable 之间的导航。
 */
object Routes {
    const val HOME = "home"
    const val RANDOM_PIC = "random_pic"
    const val RANDOM_GALLERY = "random_gallery"
    const val GROUP_LIST = "group_list"
    const val DOWNLOAD_LIST = "download_list"
    const val DOWNLOAD_MANAGE = "download_manage"
    const val RANDOM_GIF = "random_gif"
    const val PIC_LIST = "pic_list"
    const val DOWNLOAD_DETAIL = "download_detail"
}

private data class BottomTab(val route: String, val labelRes: Int, val iconRes: Int)

private val bottomTabs = listOf(
    BottomTab(Routes.HOME, R.string.nav_home, R.drawable.ic_nav_home),
    BottomTab(Routes.RANDOM_GALLERY, R.string.home_random_gallery, R.drawable.ic_nav_stack),
    BottomTab(Routes.GROUP_LIST, R.string.nav_group, R.drawable.ic_nav_group),
    BottomTab(Routes.DOWNLOAD_LIST, R.string.nav_download, R.drawable.ic_nav_download)
)

// 底栏可见的路由（4 个 tab + 套图列表二级页），与旧 navVisibleIds 一致
private val bottomBarBases = setOf(
    Routes.HOME, Routes.RANDOM_GALLERY, Routes.GROUP_LIST, Routes.DOWNLOAD_LIST, Routes.PIC_LIST
)

private fun routeBase(route: String?): String? =
    route?.substringBefore("/")?.substringBefore("?")

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val context = LocalContext.current

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentBase = routeBase(backStackEntry?.destination?.route)
    val showBottomBar = currentBase in bottomBarBases

    // 在主页时拦截返回键：双击退出
    val activity = context as? Activity
    var backPressedAt by rememberSaveable { mutableLongStateOf(0L) }
    BackHandler(enabled = currentBase == Routes.HOME) {
        val now = System.currentTimeMillis()
        if (now - backPressedAt < 2000) {
            activity?.finish()
        } else {
            backPressedAt = now
            activity?.showTopMessage("再按一次返回键退出", 2000)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentBase == tab.route,
                            onClick = { navController.switchTab(tab.route) },
                            icon = { Icon(painterResource(tab.iconRes), contentDescription = null) },
                            label = { Text(stringResource(tab.labelRes)) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.HOME) {
                val vm: HomeViewModel = viewModel { HomeViewModel(context.applicationContext) }
                HomeScreen(
                    viewModel = vm,
                    onGroupClick = { _ -> },
                    onNavigateToPicList = { groupId, groupName ->
                        if (groupId > 0L) navController.toPicList(groupId, groupName)
                    },
                    onNavigateToRandomPic = { navController.navigate(Routes.RANDOM_PIC) },
                    onNavigateToRandomGif = { navController.navigate(Routes.RANDOM_GIF) },
                    onNavigateToDownloadManage = { navController.navigate(Routes.DOWNLOAD_MANAGE) },
                    onNavigateToRandomGallery = { navController.switchTab(Routes.RANDOM_GALLERY) },
                    onNavigateToGroupList = { navController.switchTab(Routes.GROUP_LIST) },
                    onNavigateToDownloadList = { navController.switchTab(Routes.DOWNLOAD_LIST) }
                )
            }

            composable(Routes.RANDOM_PIC) {
                val vm: RandomPicViewModel = viewModel { RandomPicViewModel(AppContainer.repository(context)) }
                RandomPicScreen(
                    viewModel = vm,
                    onBack = { navController.navigateUp() },
                    onGroupClick = { groupId, groupName -> navController.toPicList(groupId, groupName) }
                )
            }

            composable(Routes.RANDOM_GALLERY) {
                val vm: RandomGalleryViewModel = viewModel { RandomGalleryViewModel(AppContainer.repository(context)) }
                RandomGalleryScreen(
                    viewModel = vm,
                    onGroupClick = { group -> navController.toPicList(group.groupId ?: 0L, group.groupName ?: "套图详情") },
                    onBack = { navController.navigateUp() }
                )
            }

            composable(Routes.GROUP_LIST) {
                val vm: GroupListViewModel = viewModel { GroupListViewModel(AppContainer.repository(context)) }
                GroupListScreen(
                    viewModel = vm,
                    onGroupClick = { group -> navController.toPicList(group.groupId ?: 0L, group.groupName ?: "套图详情") },
                    onBack = { navController.navigateUp() }
                )
            }

            composable(Routes.RANDOM_GIF) {
                val vm: RandomGifViewModel = viewModel { RandomGifViewModel(AppContainer.repository(context)) }
                RandomGifScreen(
                    onBack = { navController.navigateUp() },
                    onDetail = { workId -> navController.toDownloadDetail(workId) },
                    onAuthor = { authorId -> navController.toDownloadList(authorId = authorId) },
                    viewModel = vm
                )
            }

            composable(Routes.DOWNLOAD_MANAGE) {
                val vm: DownloadManageViewModel = viewModel { DownloadManageViewModel(AppContainer.repository(context)) }
                DownloadManageScreen(
                    viewModel = vm,
                    onBack = { navController.navigateUp() }
                )
            }

            composable(
                route = "${Routes.PIC_LIST}/{groupId}/{groupName}",
                arguments = listOf(
                    navArgument("groupId") { type = NavType.LongType },
                    navArgument("groupName") { type = NavType.StringType }
                )
            ) { entry ->
                val vm: PicListViewModel = viewModel { PicListViewModel(AppContainer.repository(context)) }
                PicListScreen(
                    viewModel = vm,
                    groupId = entry.arguments?.getLong("groupId") ?: 0L,
                    groupName = entry.arguments?.getString("groupName") ?: "套图详情",
                    onBack = { navController.navigateUp() }
                )
            }

            composable(
                route = "${Routes.DOWNLOAD_DETAIL}/{workId}?coverImageUrl={coverImageUrl}",
                arguments = listOf(
                    navArgument("workId") { type = NavType.StringType },
                    navArgument("coverImageUrl") {
                        type = NavType.StringType; defaultValue = ""
                    }
                )
            ) { entry ->
                val vm: DownloadDetailViewModel = viewModel { DownloadDetailViewModel(AppContainer.repository(context)) }
                DownloadDetailScreen(
                    viewModel = vm,
                    workId = entry.arguments?.getString("workId") ?: "",
                    coverImageUrl = entry.arguments?.getString("coverImageUrl") ?: "",
                    onBack = { navController.navigateUp() },
                    onAuthorClick = { authorId, _ -> navController.toDownloadList(authorId = authorId) },
                    onTagClick = { tag -> navController.toDownloadList(keyword = tag) }
                )
            }

            composable(
                route = "${Routes.DOWNLOAD_LIST}?filterAuthorId={filterAuthorId}&filterKeyword={filterKeyword}",
                arguments = listOf(
                    navArgument("filterAuthorId") {
                        type = NavType.StringType; nullable = true; defaultValue = null
                    },
                    navArgument("filterKeyword") {
                        type = NavType.StringType; nullable = true; defaultValue = null
                    }
                )
            ) { entry ->
                val vm: DownloadListViewModel = viewModel { DownloadListViewModel(AppContainer.repository(context)) }
                // 首次进入前注入筛选参数，与旧 Fragment 行为一致
                if (!vm.hasStarted) {
                    entry.arguments?.getString("filterAuthorId")?.let { vm.authorId = it }
                    entry.arguments?.getString("filterKeyword")?.let { vm.keyword = it }
                }
                DownloadListScreen(
                    viewModel = vm,
                    onWorkClick = { workId, coverImageUrl -> navController.toDownloadDetail(workId, coverImageUrl) },
                    onBack = { navController.navigateUp() }
                )
            }
        }
    }
}

// ── 导航辅助 ──────────────────────────────────────────────────────────

/** 切换底部 tab：保存/恢复返回栈状态，单实例 */
private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavHostController.toPicList(groupId: Long, groupName: String) {
    val name = Uri.encode(groupName.ifBlank { "套图详情" })
    navigate("${Routes.PIC_LIST}/$groupId/$name")
}

private fun NavHostController.toDownloadDetail(workId: String, coverImageUrl: String = "") {
    navigate("${Routes.DOWNLOAD_DETAIL}/${Uri.encode(workId)}?coverImageUrl=${Uri.encode(coverImageUrl)}")
}

private fun NavHostController.toDownloadList(authorId: String? = null, keyword: String? = null) {
    val params = buildList {
        authorId?.let { add("filterAuthorId=${Uri.encode(it)}") }
        keyword?.let { add("filterKeyword=${Uri.encode(it)}") }
    }
    val suffix = if (params.isEmpty()) "" else "?" + params.joinToString("&")
    navigate("${Routes.DOWNLOAD_LIST}$suffix")
}
