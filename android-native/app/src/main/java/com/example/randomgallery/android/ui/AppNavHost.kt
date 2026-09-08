package com.example.randomgallery.android.ui

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.randomgallery.android.ui.common.bouncyClickable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
import com.example.randomgallery.android.ui.common.Messenger
import com.example.randomgallery.android.ui.common.TopMessageHost

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

// ── 双模态动态底栏配置 ──────────────────────────────────────────────
private val exploreBottomTabs = listOf(
    BottomTab(Routes.HOME, R.string.nav_explore, R.drawable.ic_nav_home),
    BottomTab(Routes.RANDOM_GIF, R.string.home_random_gif, R.drawable.ic_nav_stack),
    BottomTab(Routes.DOWNLOAD_LIST, R.string.nav_download, R.drawable.ic_nav_download),
    BottomTab(Routes.DOWNLOAD_MANAGE, R.string.home_download_manage_short, R.drawable.ic_nav_group)
)

private val galleryBottomTabs = listOf(
    BottomTab(Routes.HOME, R.string.nav_home, R.drawable.ic_nav_home),
    BottomTab(Routes.RANDOM_GALLERY, R.string.home_random_gallery, R.drawable.ic_nav_stack),
    BottomTab(Routes.GROUP_LIST, R.string.nav_group, R.drawable.ic_nav_group),
    BottomTab(Routes.RANDOM_PIC, R.string.home_random_pic, R.drawable.ic_nav_download)
)

// 顶级 Tab 显示底部导航栏
private val bottomBarBases = setOf(
    Routes.HOME, Routes.RANDOM_GALLERY, Routes.GROUP_LIST, Routes.DOWNLOAD_LIST, Routes.RANDOM_GIF, Routes.DOWNLOAD_MANAGE, Routes.RANDOM_PIC
)

private fun routeBase(route: String?): String? =
    route?.substringBefore("/")?.substringBefore("?")

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 600

    val appPrefs = remember { com.example.randomgallery.android.data.local.AppPrefs(context.applicationContext) }
    val spaceMode by appPrefs.spaceModeFlow.collectAsState(initial = "explore")
    val currentTabs = if (spaceMode == "gallery") galleryBottomTabs else exploreBottomTabs

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
            Messenger.show(context.getString(R.string.exit_confirm_hint))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            // 宽屏模式下展示侧边 NavigationRail
            if (isWideScreen && showBottomBar) {
                NavigationRail {
                    currentTabs.forEach { tab ->
                        NavigationRailItem(
                            selected = currentBase == tab.route,
                            onClick = { navController.switchTab(tab.route) },
                            icon = { Icon(painterResource(tab.iconRes), contentDescription = null) },
                            label = { Text(stringResource(tab.labelRes)) }
                        )
                    }
                }
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    // 窄屏模式下展示底部 macOS / iOS 悬浮长条胶囊 NavigationBar
                    if (!isWideScreen && showBottomBar) {
                        FloatingCapsuleNavigationBar(
                            tabs = currentTabs,
                            currentRoute = currentBase,
                            onTabSelected = { route -> navController.switchTab(route) }
                        )
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = Routes.HOME,
                    modifier = Modifier.padding(innerPadding),
                    enterTransition = { slideInHorizontally(initialOffsetX = { it / 4 }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(280)) + scaleIn(initialScale = 0.95f) },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 6 }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut(tween(220)) + scaleOut(targetScale = 0.96f) },
                    popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 6 }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(280)) + scaleIn(initialScale = 0.96f) },
                    popExitTransition = { slideOutHorizontally(targetOffsetX = { it / 4 }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut(tween(220)) + scaleOut(targetScale = 0.95f) }
                ) {
                composable(Routes.HOME) {
                    val vm: HomeViewModel = viewModel { HomeViewModel(context.applicationContext) }
                    HomeScreen(
                        viewModel = vm,
                        onWorkClick = { work ->
                            work.workId?.let { workId ->
                                navController.toDownloadDetail(workId, work.coverImageUrl ?: "")
                            }
                        },
                        onNavigateToDownloadDetail = { workId, coverUrl ->
                            navController.toDownloadDetail(workId, coverUrl)
                        },
                        onNavigateToRandomPic = { navController.navigate(Routes.RANDOM_PIC) },
                        onNavigateToRandomGif = { navController.navigate(Routes.RANDOM_GIF) },
                        onNavigateToDownloadManage = { navController.navigate(Routes.DOWNLOAD_MANAGE) },
                        onNavigateToRandomGallery = { navController.switchTab(Routes.RANDOM_GALLERY) },
                        onNavigateToGroupList = { navController.switchTab(Routes.GROUP_LIST) },
                        onNavigateToDownloadList = { navController.switchTab(Routes.DOWNLOAD_LIST) },
                        onNavigateToPicList = { groupId, groupName -> navController.toPicList(groupId, groupName) }
                    )
                }

                composable(Routes.RANDOM_PIC) {
                    val vm: RandomPicViewModel = viewModel { RandomPicViewModel(context.applicationContext) }
                    RandomPicScreen(
                        viewModel = vm,
                        onBack = { navController.navigateUp() },
                        onGroupClick = { groupId, groupName -> navController.toPicList(groupId, groupName) }
                    )
                }

                composable(Routes.RANDOM_GALLERY) {
                    val vm: RandomGalleryViewModel = viewModel { RandomGalleryViewModel(context.applicationContext) }
                    RandomGalleryScreen(
                        viewModel = vm,
                        onGroupClick = { group -> navController.toPicList(group.groupId ?: 0L, group.groupName ?: context.getString(R.string.group_detail_fallback)) },
                        onBack = { navController.navigateUp() }
                    )
                }

                composable(Routes.GROUP_LIST) {
                    val vm: GroupListViewModel = viewModel { GroupListViewModel(context.applicationContext) }
                    GroupListScreen(
                        viewModel = vm,
                        onGroupClick = { group -> navController.toPicList(group.groupId ?: 0L, group.groupName ?: context.getString(R.string.group_detail_fallback)) },
                        onBack = { navController.navigateUp() }
                    )
                }

                composable(Routes.RANDOM_GIF) {
                    val vm: RandomGifViewModel = viewModel { RandomGifViewModel(context.applicationContext) }
                    RandomGifScreen(
                        onBack = { navController.navigateUp() },
                        onDetail = { workId -> navController.toDownloadDetail(workId) },
                        onAuthor = { authorId -> navController.toDownloadList(authorId = authorId) },
                        viewModel = vm
                    )
                }

                composable(Routes.DOWNLOAD_MANAGE) {
                    val vm: DownloadManageViewModel = viewModel { DownloadManageViewModel(context.applicationContext) }
                    DownloadManageScreen(
                        viewModel = vm,
                        onBack = { navController.navigateUp() },
                        onViewDetail = { workId -> navController.toDownloadDetail(workId) }
                    )
                }

                composable(
                    route = "${Routes.PIC_LIST}/{groupId}/{groupName}",
                    arguments = listOf(
                        navArgument("groupId") { type = NavType.LongType },
                        navArgument("groupName") { type = NavType.StringType }
                    )
                ) { entry ->
                    val vm: PicListViewModel = viewModel {
                        PicListViewModel(context.applicationContext, createSavedStateHandle())
                    }
                    PicListScreen(
                        viewModel = vm,
                        groupName = entry.arguments?.getString("groupName") ?: context.getString(R.string.group_detail_fallback),
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
                    val vm: DownloadDetailViewModel = viewModel { DownloadDetailViewModel(context.applicationContext) }
                    DownloadDetailScreen(
                        viewModel = vm,
                        workId = entry.arguments?.getString("workId") ?: "",
                        coverImageUrl = entry.arguments?.getString("coverImageUrl") ?: "",
                        onBack = { navController.navigateUp() },
                        onWorkDeleted = { deletedId ->
                            navController.previousBackStackEntry?.savedStateHandle?.set("deleted_work_id", deletedId)
                        },
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
                    val vm: DownloadListViewModel = viewModel {
                        DownloadListViewModel(context.applicationContext, createSavedStateHandle())
                    }
                    val deletedWorkId by entry.savedStateHandle.getStateFlow<String?>("deleted_work_id", null).collectAsStateWithLifecycle()
                    LaunchedEffect(deletedWorkId) {
                        deletedWorkId?.let {
                            vm.removeWork(it)
                            entry.savedStateHandle.remove<String>("deleted_work_id")
                        }
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
    TopMessageHost(Modifier.align(Alignment.TopCenter))
}
}

// ── 导航辅助 ──────────────────────────────────────────────────────────

/** 切换底部 tab：支持从任意深层页面（如详情页、作者筛选页等）平滑切换至任何顶级 Tab，彻底消除多层栈卡死问题 */
private fun NavHostController.switchTab(route: String) {
    if (route == Routes.HOME) {
        // 点击主页：直接回退栈顶到根页面
        popBackStack(Routes.HOME, inclusive = false)
    } else {
        val currentRoute = currentBackStackEntry?.destination?.route?.substringBefore("?")?.substringBefore("/")
        if (currentRoute == route) {
            // 如果已经在该 Tab（比如在带参数的作者列表页，再次点击底栏【作品】时），清空参数重置为纯净根列表
            popBackStack(route, inclusive = false)
        } else {
            navigate(route) {
                // 清理到根导航节点，不保留深层残留页面阻碍切换
                popUpTo(graph.findStartDestination().id) {
                    saveState = false
                }
                launchSingleTop = true
                restoreState = false
            }
        }
    }
}

private fun NavHostController.toPicList(groupId: Long, groupName: String) {
    val name = Uri.encode(groupName.ifBlank { "套图详情" }) // not a Composable: fallback hardcoded intentionally as URL path
    // launchSingleTop：目的地已在栈顶时不重复入栈，防止连点造成多次跳转
    navigate("${Routes.PIC_LIST}/$groupId/$name") { launchSingleTop = true }
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

/**
 * iOS / macOS 26 风格悬浮胶囊底栏 (Floating Capsule Navigation Bar)
 * 悬浮长条、两端半圆 (CircleShape)、高质感阴影与动态淡入高亮
 */
@Composable
private fun FloatingCapsuleNavigationBar(
    tabs: List<BottomTab>,
    currentRoute: String?,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    val selected = currentRoute == tab.route

                    val activeBgColor by animateColorAsState(
                        targetValue = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                        } else {
                            Color.Transparent
                        },
                        animationSpec = tween(durationMillis = 220),
                        label = "capsuleTabBg"
                    )

                    val activeContentColor by animateColorAsState(
                        targetValue = if (selected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        animationSpec = tween(durationMillis = 220),
                        label = "capsuleTabContent"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(horizontal = 3.dp, vertical = 2.dp)
                            .clip(CircleShape)
                            .background(activeBgColor)
                            .semantics { this.selected = selected }
                            .bouncyClickable { onTabSelected(tab.route) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(tab.iconRes),
                                contentDescription = stringResource(tab.labelRes),
                                tint = activeContentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(tab.labelRes),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = activeContentColor,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
