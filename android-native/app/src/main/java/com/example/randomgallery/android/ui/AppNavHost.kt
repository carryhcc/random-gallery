package com.example.randomgallery.android.ui

import android.app.Activity
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
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.randomgallery.android.ui.common.bouncyClickable
import com.example.randomgallery.android.ui.common.isExpandedWidth
import com.example.randomgallery.android.ui.common.fresnelBorderBrush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import com.example.randomgallery.android.R
import com.example.randomgallery.android.ui.download.DownloadManageScreen
import com.example.randomgallery.android.ui.download.DownloadManageViewModel
import com.example.randomgallery.android.ui.downloaddetail.DownloadDetailScreen
import com.example.randomgallery.android.ui.downloaddetail.DownloadDetailViewModel
import com.example.randomgallery.android.ui.downloadlist.DownloadListScreen
import com.example.randomgallery.android.ui.downloadlist.DownloadListViewModel
import com.example.randomgallery.android.ui.favorite.FavoriteScreen
import com.example.randomgallery.android.ui.favorite.FavoriteViewModel
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
/**
 * REQ-11：类型安全路由。
 *
 * 迁移前是 `const val` 字符串 + 手写 `"$PIC_LIST/$groupId/${Uri.encode(name)}"` 拼接：
 * 参数顺序、转义、缺参都只能靠人工保证，写错要到运行期才发现。
 * 换成 @Serializable 后，路由串与转义由 Navigation 生成，参数即类型。
 *
 * 注意：数据类的属性名就是导航参数名，也就是各 ViewModel 从 SavedStateHandle
 * 读取的 key（`groupId` / `filterAuthorId` / `filterKeyword`），改名会静默破坏筛选与分页。
 */
@Serializable
sealed interface Routes {
    @Serializable data object Home : Routes
    @Serializable data object RandomPic : Routes
    @Serializable data object RandomGallery : Routes
    @Serializable data object GroupList : Routes
    @Serializable data object DownloadManage : Routes
    @Serializable data object RandomGif : Routes
    @Serializable data object Favorite : Routes

    @Serializable data class PicList(val groupId: Long, val groupName: String) : Routes

    @Serializable data class DownloadDetail(val workId: String, val coverImageUrl: String = "") : Routes

    @Serializable data class DownloadList(
        val filterAuthorId: String? = null,
        val filterKeyword: String? = null
    ) : Routes
}

private data class BottomTab(val route: Routes, val labelRes: Int, val iconRes: Int)

// ── 双模态动态底栏配置 ──────────────────────────────────────────────
private val exploreBottomTabs = listOf(
    BottomTab(Routes.Home, R.string.nav_explore, R.drawable.ic_nav_home),
    BottomTab(Routes.RandomGif, R.string.home_random_gif, R.drawable.ic_nav_stack),
    BottomTab(Routes.Favorite, R.string.nav_favorite, R.drawable.ic_nav_favorite),
    BottomTab(Routes.DownloadList(), R.string.nav_download, R.drawable.ic_nav_download),
    BottomTab(Routes.DownloadManage, R.string.home_download_manage_short, R.drawable.ic_nav_group)
)

private val galleryBottomTabs = listOf(
    BottomTab(Routes.Home, R.string.nav_home, R.drawable.ic_nav_home),
    BottomTab(Routes.RandomGallery, R.string.home_random_gallery, R.drawable.ic_nav_stack),
    BottomTab(Routes.GroupList, R.string.nav_group, R.drawable.ic_nav_group),
    BottomTab(Routes.Favorite, R.string.nav_favorite, R.drawable.ic_nav_favorite),
    BottomTab(Routes.RandomPic, R.string.home_random_pic, R.drawable.ic_nav_download)
)

// 顶级 Tab 显示底部导航栏。按路由「类型」判定，故带参路由（DownloadList）在任意筛选态下都命中
private val bottomBarRouteClasses = setOf(
    Routes.Home::class, Routes.RandomGallery::class, Routes.GroupList::class,
    Routes.DownloadList::class, Routes.RandomGif::class, Routes.DownloadManage::class,
    Routes.RandomPic::class, Routes.Favorite::class
)

private fun NavDestination?.isBottomBarRoute(): Boolean =
    this != null && bottomBarRouteClasses.any { hasRoute(it) }

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val context = LocalContext.current
    // REQ-06：改用 WindowSizeClass，替代 ad-hoc 的 screenWidthDp 阈值
    val isWideScreen = isExpandedWidth()

    val appPrefs = remember { com.example.randomgallery.android.data.local.AppPrefs(context.applicationContext) }
    val spaceMode by appPrefs.spaceModeFlow.collectAsState(initial = "explore")
    val currentTabs = if (spaceMode == "gallery") galleryBottomTabs else exploreBottomTabs

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = currentDestination.isBottomBarRoute()

    // 在主页时拦截返回键：双击退出
    val activity = context as? Activity
    var backPressedAt by rememberSaveable { mutableLongStateOf(0L) }
    BackHandler(enabled = currentDestination?.hasRoute(Routes.Home::class) == true) {
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
                            selected = currentDestination?.hasRoute(tab.route::class) == true,
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
                            currentDestination = currentDestination,
                            onTabSelected = { route -> navController.switchTab(route) }
                        )
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = Routes.Home,
                    modifier = Modifier.padding(innerPadding),
                    enterTransition = { slideInHorizontally(initialOffsetX = { it / 4 }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(280)) + scaleIn(initialScale = 0.95f) },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 6 }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut(tween(220)) + scaleOut(targetScale = 0.96f) },
                    popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 6 }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(280)) + scaleIn(initialScale = 0.96f) },
                    popExitTransition = { slideOutHorizontally(targetOffsetX = { it / 4 }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut(tween(220)) + scaleOut(targetScale = 0.95f) }
                ) {
                composable<Routes.Home> {
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
                        onNavigateToRandomPic = { navController.navigate(Routes.RandomPic) },
                        onNavigateToRandomGif = { navController.navigate(Routes.RandomGif) },
                        onNavigateToDownloadManage = { navController.navigate(Routes.DownloadManage) },
                        onNavigateToRandomGallery = { navController.switchTab(Routes.RandomGallery) },
                        onNavigateToGroupList = { navController.switchTab(Routes.GroupList) },
                        onNavigateToDownloadList = { navController.switchTab(Routes.DownloadList()) },
                        onNavigateToPicList = { groupId, groupName -> navController.toPicList(groupId, groupName) }
                    )
                }

                composable<Routes.RandomPic> {
                    val vm: RandomPicViewModel = viewModel { RandomPicViewModel(context.applicationContext) }
                    RandomPicScreen(
                        viewModel = vm,
                        onBack = { navController.navigateUp() },
                        onGroupClick = { groupId, groupName -> navController.toPicList(groupId, groupName) }
                    )
                }

                composable<Routes.RandomGallery> {
                    val vm: RandomGalleryViewModel = viewModel { RandomGalleryViewModel(context.applicationContext) }
                    RandomGalleryScreen(
                        viewModel = vm,
                        onGroupClick = { group -> navController.toPicList(group.groupId ?: 0L, group.groupName ?: context.getString(R.string.group_detail_fallback)) },
                        onBack = { navController.navigateUp() }
                    )
                }

                composable<Routes.GroupList> {
                    val vm: GroupListViewModel = viewModel { GroupListViewModel(context.applicationContext) }
                    GroupListScreen(
                        viewModel = vm,
                        onGroupClick = { group -> navController.toPicList(group.groupId ?: 0L, group.groupName ?: context.getString(R.string.group_detail_fallback)) },
                        onBack = { navController.navigateUp() }
                    )
                }

                composable<Routes.RandomGif> {
                    val vm: RandomGifViewModel = viewModel { RandomGifViewModel(context.applicationContext) }
                    RandomGifScreen(
                        onBack = { navController.navigateUp() },
                        onDetail = { workId -> navController.toDownloadDetail(workId) },
                        onAuthor = { authorId -> navController.toDownloadList(authorId = authorId) },
                        viewModel = vm
                    )
                }

                composable<Routes.Favorite> {
                    val vm: FavoriteViewModel = viewModel { FavoriteViewModel(context.applicationContext) }
                    FavoriteScreen(
                        viewModel = vm,
                        onWorkClick = { workId -> navController.toDownloadDetail(workId) },
                        onBack = { navController.navigateUp() }
                    )
                }

                composable<Routes.DownloadManage> {
                    val vm: DownloadManageViewModel = viewModel { DownloadManageViewModel(context.applicationContext) }
                    DownloadManageScreen(
                        viewModel = vm,
                        onBack = { navController.navigateUp() },
                        onViewDetail = { workId -> navController.toDownloadDetail(workId) }
                    )
                }

                composable<Routes.PicList> { entry ->
                    val vm: PicListViewModel = viewModel {
                        PicListViewModel(context.applicationContext, createSavedStateHandle())
                    }
                    PicListScreen(
                        viewModel = vm,
                        groupName = entry.toRoute<Routes.PicList>().groupName,
                        onBack = { navController.navigateUp() }
                    )
                }

                composable<Routes.DownloadDetail> { entry ->
                    val vm: DownloadDetailViewModel = viewModel { DownloadDetailViewModel(context.applicationContext) }
                    val args = entry.toRoute<Routes.DownloadDetail>()
                    DownloadDetailScreen(
                        viewModel = vm,
                        workId = args.workId,
                        coverImageUrl = args.coverImageUrl,
                        onBack = { navController.navigateUp() },
                        onWorkDeleted = { deletedId ->
                            navController.previousBackStackEntry?.savedStateHandle?.set("deleted_work_id", deletedId)
                        },
                        onAuthorClick = { authorId, _ -> navController.toDownloadList(authorId = authorId) },
                        onTagClick = { tag -> navController.toDownloadList(keyword = tag) }
                    )
                }

                composable<Routes.DownloadList> { entry ->
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
private fun NavHostController.switchTab(route: Routes) {
    val current = currentBackStackEntry?.destination
    if (route == Routes.Home) {
        // 点击主页：直接回退栈顶到根页面
        popBackStack(Routes.Home, inclusive = false)
    } else if (current?.hasRoute(route::class) == true) {
        // 已经在该 Tab（如在带参数的作者筛选列表页再次点击底栏【作品】），按路由类型回退到该 Tab 根，清掉参数
        popBackStack(route::class, inclusive = false)
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

private fun NavHostController.toPicList(groupId: Long, groupName: String) {
    // 空分组名回退为固定文案（迁移前这段逻辑内联在手工拼接的路由串里）
    val name = groupName.ifBlank { "套图详情" }
    // launchSingleTop：目的地已在栈顶时不重复入栈，防止连点造成多次跳转
    navigate(Routes.PicList(groupId, name)) { launchSingleTop = true }
}

private fun NavHostController.toDownloadDetail(workId: String, coverImageUrl: String = "") {
    navigate(Routes.DownloadDetail(workId, coverImageUrl))
}

private fun NavHostController.toDownloadList(authorId: String? = null, keyword: String? = null) {
    navigate(Routes.DownloadList(filterAuthorId = authorId, filterKeyword = keyword))
}

/**
 * iOS / macOS 26 风格悬浮胶囊底栏 (Floating Capsule Navigation Bar)
 * 悬浮长条、两端半圆 (CircleShape)、高质感阴影与动态淡入高亮
 */
@Composable
private fun FloatingCapsuleNavigationBar(
    tabs: List<BottomTab>,
    currentDestination: NavDestination?,
    onTabSelected: (Routes) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        // macOS 26 磨砂玻璃：半透明渐变底（上亮下暗）+ 顶部菲涅尔高光带 + 菲涅尔描边 + 柔和阴影
        val isDark = isSystemInDarkTheme()
        val glassTop = if (isDark) {
            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.68f)
        } else {
            Color.White.copy(alpha = 0.80f)
        }
        val glassBase = if (isDark) {
            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.50f)
        } else {
            Color.White.copy(alpha = 0.64f)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .shadow(elevation = 12.dp, shape = CircleShape, clip = false)
                .clip(CircleShape)
                .background(Brush.verticalGradient(colors = listOf(glassTop, glassBase)))
                .border(BorderStroke(1.dp, fresnelBorderBrush(isDark)), CircleShape)
        ) {
            // 顶部菲涅尔高光带：随胶囊上缘弧度自然收口，模拟玻璃表面捕捉顶部光线
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.45f)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isDark) 0.10f else 0.28f),
                                Color.White.copy(alpha = 0f)
                            )
                        )
                    )
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    val selected = currentDestination?.hasRoute(tab.route::class) == true

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
