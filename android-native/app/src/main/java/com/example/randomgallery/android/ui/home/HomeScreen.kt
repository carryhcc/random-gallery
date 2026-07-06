package com.example.randomgallery.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.randomgallery.android.R
import com.example.randomgallery.android.ui.common.*
import com.example.randomgallery.android.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onGroupClick: (com.example.randomgallery.android.data.model.GroupVO) -> Unit,
    onNavigateToPicList: (groupId: Long, groupName: String) -> Unit,
    onNavigateToRandomPic: () -> Unit,
    onNavigateToRandomGif: () -> Unit,
    onNavigateToDownloadManage: () -> Unit,
    onNavigateToRandomGallery: () -> Unit,
    onNavigateToGroupList: () -> Unit,
    onNavigateToDownloadList: () -> Unit
) {
    val context = LocalContext.current
    val envInfo by viewModel.envInfo.collectAsStateWithLifecycle()
    val privacy by viewModel.privacy.collectAsStateWithLifecycle()
    val localEnv by viewModel.localEnv.collectAsStateWithLifecycle()
    val baseUrl by viewModel.baseUrl.collectAsStateWithLifecycle()
    val urlList by viewModel.urlList.collectAsStateWithLifecycle()

    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadEnvInfo()
        viewModel.loadPrivacy()
        viewModel.messages.collect { msg ->
            Messenger.show(msg, isError = msg.contains("失败"))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.randomGroupEvents.collect { result ->
            result.onSuccess { group ->
                group.groupId?.let { onNavigateToPicList(it, group.groupName ?: context.getString(R.string.group_detail_fallback)) }
            }
        }
    }

    val picCount = (envInfo as? UiState.Success)?.data?.picCount ?: 0
    val groupCount = (envInfo as? UiState.Success)?.data?.groupCount ?: 0

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.home_title), fontWeight = FontWeight.SemiBold)
                },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.common_settings))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {

            // ── 第一区：本地图库 ──────────────────────────────────
            SectionCard {
                // 区块标题 + 统计
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        Icon(
                            Icons.Filled.PhotoLibrary,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(stringResource(R.string.home_section_local), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    // 加载中用骨架，有数据就显示
                    if (envInfo is UiState.Loading) {
                        XhsLoadingBox(Modifier.size(16.dp))
                    } else if (groupCount > 0 || picCount > 0) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatChip(label = stringResource(R.string.home_stat_group), value = groupCount.toString())
                            StatChip(label = stringResource(R.string.home_stat_pic), value = picCount.toString())
                        }
                    }
                }

                Spacer(Modifier.height(Spacing.md))

                // 2×2 功能格
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        FuncCard(
                            icon = Icons.Filled.Shuffle,
                            label = stringResource(R.string.home_random_pic),
                            tint = MaterialTheme.xhs.accentIndigo,
                            bg = MaterialTheme.xhs.accentIndigoSoft,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToRandomPic
                        )
                        FuncCard(
                            icon = Icons.Filled.GridView,
                            label = stringResource(R.string.home_random_gallery_label),
                            tint = MaterialTheme.colorScheme.primary,
                            bg = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToRandomGallery
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        FuncCard(
                            icon = Icons.Filled.Collections,
                            label = stringResource(R.string.home_random_group),
                            tint = MaterialTheme.xhs.accentCoral,
                            bg = MaterialTheme.xhs.accentCoralSoft,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.randomGroup() }
                        )
                        FuncCard(
                            icon = Icons.Filled.FormatListBulleted,
                            label = stringResource(R.string.home_group_list_label),
                            tint = MaterialTheme.colorScheme.secondary,
                            bg = MaterialTheme.xhs.accentGreySoft,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToGroupList
                        )
                    }
                }
            }

            // ── 第二区：精选收藏（XHS 数据）───────────────────────
            SectionCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Filled.CloudDownload,
                        contentDescription = null,
                        tint = MaterialTheme.xhs.accentBlue,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(stringResource(R.string.home_section_favorites), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(Spacing.md))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    FuncCard(
                        icon = Icons.Filled.Animation,
                        label = stringResource(R.string.home_random_gif),
                        tint = MaterialTheme.xhs.accentGreen,
                        bg = MaterialTheme.xhs.accentGreenSoft,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToRandomGif
                    )
                    FuncCard(
                        icon = Icons.Filled.PhotoAlbum,
                        label = stringResource(R.string.home_download_browse),
                        tint = MaterialTheme.xhs.accentBlue,
                        bg = MaterialTheme.xhs.accentBlueSoft,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToDownloadList
                    )
                }

                Spacer(Modifier.height(Spacing.md))

                // 全宽——下载管理
                FuncCardWide(
                    icon = Icons.Filled.Download,
                    label = stringResource(R.string.home_download_manage),
                    tint = MaterialTheme.xhs.accentOrange,
                    bg = MaterialTheme.xhs.accentOrangeSoft,
                    onClick = onNavigateToDownloadManage
                )
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            currentUrl = baseUrl,
            urlList = urlList,
            privacyEnabled = privacy,
            currentEnv = localEnv,
            onDismiss = { showSettings = false },
            onSelectUrl = { viewModel.selectBaseUrl(it) },
            onAddUrl = { viewModel.addAndSelectUrl(it) },
            onRemoveUrl = { viewModel.removeUrl(it) },
            onPrivacyToggle = { viewModel.setPrivacy(it) },
            onSwitchEnv = { viewModel.switchEnv(it) }
        )
    }
}

// ── 区块容器卡片 ───────────────────────────────────────────────────

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(Spacing.lg), content = content)
    }
}

// ── 统计小标签 ─────────────────────────────────────────────────────

@Composable
private fun StatChip(label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Text(
            "$label: ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ── 功能方格（1/2 宽）──────────────────────────────────────────────

@Composable
private fun FuncCard(
    icon: ImageVector,
    label: String,
    tint: Color,
    bg: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.background)
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.xl, horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(bg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ── 功能宽格（全宽）──────────────────────────────────────────────

@Composable
private fun FuncCardWide(
    icon: ImageVector,
    label: String,
    tint: Color,
    bg: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.background)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(Spacing.md))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ── 设置弹窗 ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDialog(
    currentUrl: String,
    urlList: List<String>,
    privacyEnabled: Boolean,
    currentEnv: String,
    onDismiss: () -> Unit,
    onSelectUrl: (String) -> Unit,
    onAddUrl: (String) -> Unit,
    onRemoveUrl: (String) -> Unit,
    onPrivacyToggle: (Boolean) -> Unit,
    onSwitchEnv: (String) -> Unit
) {
    var newUrlInput by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }
    val envOptions = listOf("dev", "test", "prod")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.common_settings), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {

                // ── 服务器地址 ─────────────────────────────────────
                SettingsRow(label = stringResource(R.string.settings_server)) {
                    // 下拉选择已保存的地址
                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { if (urlList.isNotEmpty()) dropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = currentUrl.ifBlank { stringResource(R.string.settings_url_unset) },
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall,
                            trailingIcon = {
                                if (urlList.isNotEmpty())
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                            },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            urlList.forEach { url ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = url,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (url == currentUrl) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                fontWeight = if (url == currentUrl) FontWeight.SemiBold else FontWeight.Normal,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (url == currentUrl) {
                                                Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    },
                                    trailingIcon = {
                                        IconButton(
                                            onClick = { onRemoveUrl(url) }
                                        ) {
                                            Icon(Icons.Filled.Close, stringResource(R.string.common_delete), tint = MaterialTheme.xhs.textTertiary, modifier = Modifier.size(14.dp))
                                        }
                                    },
                                    onClick = {
                                        onSelectUrl(url)
                                        dropdownExpanded = false
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(Spacing.sm))

                    // 添加新地址
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newUrlInput,
                            onValueChange = { newUrlInput = it },
                            placeholder = { Text(stringResource(R.string.settings_add_url_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        IconButton(
                            onClick = {
                                if (newUrlInput.isNotBlank()) {
                                    onAddUrl(newUrlInput.trim())
                                    newUrlInput = ""
                                    onDismiss()
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Filled.Add, stringResource(R.string.settings_add), tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                XhsDivider()

                // ── 环境选择 ───────────────────────────────────────
                SettingsRow(label = stringResource(R.string.settings_env)) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        envOptions.forEachIndexed { index, env ->
                            SegmentedButton(
                                selected = currentEnv == env,
                                onClick = { onSwitchEnv(env) },
                                shape = SegmentedButtonDefaults.itemShape(index, envOptions.size),
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    activeContentColor = MaterialTheme.colorScheme.primary,
                                    activeBorderColor = MaterialTheme.colorScheme.primary
                                ),
                                icon = {}
                            ) {
                                Text(env, style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (currentEnv == env) FontWeight.SemiBold else FontWeight.Normal)
                            }
                        }
                    }
                }

                XhsDivider()

                // ── 隐私模式 ───────────────────────────────────────
                Row(
                    Modifier.fillMaxWidth().padding(vertical = Spacing.sm),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.settings_privacy), style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = privacyEnabled,
                        onCheckedChange = onPrivacyToggle,
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary, checkedTrackColor = MaterialTheme.colorScheme.primaryContainer)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_close), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
        }
    )
}

@Composable
private fun SettingsRow(label: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = Spacing.xs))
        content()
    }
}
