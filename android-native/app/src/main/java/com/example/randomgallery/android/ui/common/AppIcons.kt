package com.example.randomgallery.android.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private fun icon(name: String, block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit): ImageVector {
    return ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.Black), pathBuilder = block).build()
}

val Icons.Filled.Download: ImageVector by lazy {
    icon("Filled.Download") {
        moveTo(19f, 9f)
        horizontalLineToRelative(-4f)
        verticalLineTo(3f)
        horizontalLineTo(9f)
        verticalLineToRelative(6f)
        horizontalLineTo(5f)
        lineToRelative(7f, 7f)
        lineToRelative(7f, -7f)
        close()
        moveTo(5f, 18f)
        verticalLineToRelative(2f)
        horizontalLineToRelative(14f)
        verticalLineToRelative(-2f)
        horizontalLineTo(5f)
        close()
    }
}

val Icons.Filled.FileDownload: ImageVector by lazy { Icons.Filled.Download }

val Icons.Filled.Image: ImageVector by lazy {
    icon("Filled.Image") {
        moveTo(21f, 19f)
        verticalLineTo(5f)
        curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
        horizontalLineTo(5f)
        curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
        verticalLineToRelative(14f)
        curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
        horizontalLineToRelative(14f)
        curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
        close()
        moveTo(8.5f, 13.5f)
        lineToRelative(2.5f, 3.01f)
        lineToRelative(3.5f, -4.51f)
        lineToRelative(4.5f, 6f)
        horizontalLineTo(5f)
        lineToRelative(3.5f, -4.5f)
        close()
    }
}

val Icons.Filled.ChevronLeft: ImageVector by lazy {
    icon("Filled.ChevronLeft") {
        moveTo(15.41f, 7.41f)
        lineTo(14f, 6f)
        lineToRelative(-6f, 6f)
        lineToRelative(6f, 6f)
        lineToRelative(1.41f, -1.41f)
        lineTo(10.83f, 12f)
        close()
    }
}

val Icons.Filled.ChevronRight: ImageVector by lazy {
    icon("Filled.ChevronRight") {
        moveTo(10f, 6f)
        lineTo(8.59f, 7.41f)
        lineTo(13.17f, 12f)
        lineToRelative(-4.58f, 4.59f)
        lineTo(10f, 18f)
        lineToRelative(6f, -6f)
        close()
    }
}

val Icons.Filled.ExpandLess: ImageVector by lazy {
    icon("Filled.ExpandLess") {
        moveTo(12f, 8f)
        lineToRelative(-6f, 6f)
        lineToRelative(1.41f, 1.41f)
        lineTo(12f, 10.83f)
        lineToRelative(4.59f, 4.58f)
        lineTo(18f, 14f)
        close()
    }
}

val Icons.Filled.ExpandMore: ImageVector by lazy {
    icon("Filled.ExpandMore") {
        moveTo(16.59f, 8.59f)
        lineTo(12f, 13.17f)
        lineTo(7.41f, 8.59f)
        lineTo(6f, 10f)
        lineToRelative(6f, 6f)
        lineToRelative(6f, -6f)
        close()
    }
}

val Icons.Filled.Tune: ImageVector by lazy {
    icon("Filled.Tune") {
        moveTo(3f, 17f)
        verticalLineToRelative(2f)
        horizontalLineToRelative(6f)
        verticalLineToRelative(-2f)
        horizontalLineTo(3f)
        close()
        moveTo(3f, 5f)
        verticalLineToRelative(2f)
        horizontalLineToRelative(10f)
        verticalLineTo(5f)
        horizontalLineTo(3f)
        close()
        moveTo(13f, 21f)
        verticalLineToRelative(-2f)
        horizontalLineToRelative(8f)
        verticalLineToRelative(-2f)
        horizontalLineToRelative(-8f)
        verticalLineToRelative(-2f)
        horizontalLineToRelative(-2f)
        verticalLineToRelative(6f)
        horizontalLineToRelative(2f)
        close()
        moveTo(7f, 9f)
        verticalLineToRelative(2f)
        horizontalLineTo(3f)
        verticalLineToRelative(2f)
        horizontalLineToRelative(4f)
        verticalLineToRelative(2f)
        horizontalLineToRelative(2f)
        verticalLineTo(9f)
        horizontalLineTo(7f)
        close()
        moveTo(21f, 13f)
        verticalLineToRelative(-2f)
        horizontalLineTo(11f)
        verticalLineToRelative(2f)
        horizontalLineToRelative(10f)
        close()
        moveTo(15f, 9f)
        horizontalLineToRelative(2f)
        verticalLineTo(7f)
        horizontalLineToRelative(4f)
        verticalLineTo(5f)
        horizontalLineToRelative(-4f)
        verticalLineTo(3f)
        horizontalLineToRelative(-2f)
        verticalLineToRelative(6f)
        close()
    }
}

val Icons.Filled.Tag: ImageVector by lazy {
    icon("Filled.Tag") {
        moveTo(20f, 10f)
        verticalLineTo(8f)
        horizontalLineToRelative(-4f)
        verticalLineTo(4f)
        horizontalLineToRelative(-2f)
        verticalLineToRelative(4f)
        horizontalLineToRelative(-4f)
        verticalLineTo(4f)
        horizontalLineTo(8f)
        verticalLineToRelative(4f)
        horizontalLineTo(4f)
        verticalLineToRelative(2f)
        horizontalLineToRelative(4f)
        verticalLineToRelative(4f)
        horizontalLineTo(4f)
        verticalLineToRelative(2f)
        horizontalLineToRelative(4f)
        verticalLineToRelative(4f)
        horizontalLineToRelative(2f)
        verticalLineToRelative(-4f)
        horizontalLineToRelative(4f)
        verticalLineToRelative(4f)
        horizontalLineToRelative(2f)
        verticalLineToRelative(-4f)
        horizontalLineToRelative(4f)
        verticalLineToRelative(-2f)
        horizontalLineToRelative(-4f)
        verticalLineToRelative(-4f)
        horizontalLineToRelative(4f)
        close()
        moveTo(14f, 14f)
        horizontalLineToRelative(-4f)
        verticalLineToRelative(-4f)
        horizontalLineToRelative(4f)
        verticalLineToRelative(4f)
        close()
    }
}

val Icons.AutoMirrored.Filled.OpenInNew: ImageVector by lazy {
    icon("AutoMirrored.Filled.OpenInNew") {
        moveTo(19f, 19f)
        horizontalLineTo(5f)
        verticalLineTo(5f)
        horizontalLineToRelative(7f)
        verticalLineTo(3f)
        horizontalLineTo(5f)
        curveToRelative(-1.11f, 0f, -2f, 0.9f, -2f, 2f)
        verticalLineToRelative(14f)
        curveToRelative(0f, 1.1f, 0.89f, 2f, 2f, 2f)
        horizontalLineToRelative(14f)
        curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
        verticalLineToRelative(-7f)
        horizontalLineToRelative(-2f)
        verticalLineToRelative(7f)
        close()
        moveTo(14f, 3f)
        verticalLineToRelative(2f)
        horizontalLineToRelative(3.59f)
        lineToRelative(-9.83f, 9.83f)
        lineToRelative(1.41f, 1.41f)
        lineTo(19f, 6.41f)
        verticalLineTo(10f)
        horizontalLineToRelative(2f)
        verticalLineTo(3f)
        horizontalLineToRelative(-7f)
        close()
    }
}

val Icons.Filled.Bolt: ImageVector by lazy {
    icon("Filled.Bolt") {
        moveTo(11f, 21f)
        horizontalLineToRelative(-1f)
        lineToRelative(1f, -7f)
        horizontalLineTo(7.5f)
        curveToRelative(-0.58f, 0f, -0.57f, -0.32f, -0.38f, -0.66f)
        curveToRelative(0.19f, -0.34f, 0.05f, -0.08f, 0.07f, -0.12f)
        curveTo(8.48f, 10.94f, 10.42f, 7.54f, 13f, 3f)
        horizontalLineToRelative(1f)
        lineToRelative(-1f, 7f)
        horizontalLineToRelative(3.5f)
        curveToRelative(0.49f, 0f, 0.56f, 0.33f, 0.47f, 0.51f)
        lineToRelative(-0.07f, 0.15f)
        curveTo(12.96f, 17.55f, 11f, 21f, 11f, 21f)
        close()
    }
}

val Icons.Filled.DeleteOutline: ImageVector by lazy {
    icon("Filled.DeleteOutline") {
        moveTo(6f, 19f)
        curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
        horizontalLineToRelative(8f)
        curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
        verticalLineTo(7f)
        horizontalLineTo(6f)
        verticalLineToRelative(12f)
        close()
        moveTo(8f, 9f)
        horizontalLineToRelative(8f)
        verticalLineToRelative(10f)
        horizontalLineTo(8f)
        verticalLineTo(9f)
        close()
        moveTo(15.5f, 4f)
        lineToRelative(-1f, -1f)
        horizontalLineToRelative(-5f)
        lineToRelative(-1f, 1f)
        horizontalLineTo(5f)
        verticalLineToRelative(2f)
        horizontalLineToRelative(14f)
        verticalLineTo(4f)
        close()
    }
}

val Icons.Filled.CloudDone: ImageVector by lazy {
    icon("Filled.CloudDone") {
        moveTo(19.35f, 10.04f)
        curveTo(18.67f, 6.59f, 15.64f, 4f, 12f, 4f)
        curveTo(9.11f, 4f, 6.6f, 5.64f, 5.35f, 8.04f)
        curveTo(2.34f, 8.36f, 0f, 10.91f, 0f, 14f)
        curveToRelative(0f, 3.31f, 2.69f, 6f, 6f, 6f)
        horizontalLineToRelative(13f)
        curveToRelative(2.76f, 0f, 5f, -2.24f, 5f, -5f)
        curveToRelative(0f, -2.64f, -2.05f, -4.78f, -4.65f, -4.96f)
        close()
        moveTo(10f, 17f)
        lineToRelative(-3.5f, -3.5f)
        lineToRelative(1.41f, -1.41f)
        lineTo(10f, 14.17f)
        lineToRelative(5.09f, -5.09f)
        lineTo(16.5f, 10.5f)
        lineTo(10f, 17f)
        close()
    }
}

val Icons.Filled.CloudDownload: ImageVector by lazy {
    icon("Filled.CloudDownload") {
        moveTo(19.35f, 10.04f)
        curveTo(18.67f, 6.59f, 15.64f, 4f, 12f, 4f)
        curveTo(9.11f, 4f, 6.6f, 5.64f, 5.35f, 8.04f)
        curveTo(2.34f, 8.36f, 0f, 10.91f, 0f, 14f)
        curveToRelative(0f, 3.31f, 2.69f, 6f, 6f, 6f)
        horizontalLineToRelative(13f)
        curveToRelative(2.76f, 0f, 5f, -2.24f, 5f, -5f)
        curveToRelative(0f, -2.64f, -2.05f, -4.78f, -4.65f, -4.96f)
        close()
        moveTo(17f, 13f)
        lineToRelative(-5f, 5f)
        lineToRelative(-5f, -5f)
        horizontalLineToRelative(3f)
        verticalLineTo(9f)
        horizontalLineToRelative(4f)
        verticalLineToRelative(4f)
        horizontalLineToRelative(3f)
        close()
    }
}

val Icons.Filled.ContentPaste: ImageVector by lazy {
    icon("Filled.ContentPaste") {
        moveTo(19f, 2f)
        horizontalLineToRelative(-4.18f)
        curveTo(14.4f, 0.84f, 13.3f, 0f, 12f, 0f)
        curveToRelative(-1.3f, 0f, -2.4f, 0.84f, -2.82f, 2f)
        horizontalLineTo(5f)
        curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
        verticalLineToRelative(16f)
        curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
        horizontalLineToRelative(14f)
        curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
        verticalLineTo(4f)
        curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
        close()
        moveTo(12f, 2f)
        curveToRelative(0.55f, 0f, 1f, 0.45f, 1f, 1f)
        curveToRelative(0f, 0.55f, -0.45f, 1f, -1f, 1f)
        curveToRelative(-0.55f, 0f, -1f, -0.45f, -1f, -1f)
        curveToRelative(0f, -0.55f, 0.45f, -1f, 1f, -1f)
        close()
        moveTo(19f, 20f)
        horizontalLineTo(5f)
        verticalLineTo(4f)
        horizontalLineToRelative(2f)
        verticalLineToRelative(3f)
        horizontalLineToRelative(10f)
        verticalLineTo(4f)
        horizontalLineToRelative(2f)
        verticalLineToRelative(16f)
        close()
    }
}

val Icons.Filled.RadioButtonUnchecked: ImageVector by lazy {
    icon("Filled.RadioButtonUnchecked") {
        moveTo(12f, 2f)
        curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
        curveToRelative(0f, 5.52f, 4.48f, 10f, 10f, 10f)
        curveToRelative(5.52f, 0f, 10f, -4.48f, 10f, -10f)
        curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
        close()
        moveTo(12f, 20f)
        curveToRelative(-4.42f, 0f, -8f, -3.58f, -8f, -8f)
        curveToRelative(0f, -4.42f, 3.58f, -8f, 8f, -8f)
        curveToRelative(4.42f, 0f, 8f, 3.58f, 8f, 8f)
        curveToRelative(0f, 4.42f, -3.58f, 8f, -8f, 8f)
        close()
    }
}

val Icons.Filled.Cancel: ImageVector by lazy {
    icon("Filled.Cancel") {
        moveTo(12f, 2f)
        curveTo(6.47f, 2f, 2f, 6.47f, 2f, 12f)
        curveToRelative(0f, 5.53f, 4.47f, 10f, 10f, 10f)
        curveToRelative(5.53f, 0f, 10f, -4.47f, 10f, -10f)
        curveTo(22f, 6.47f, 17.53f, 2f, 12f, 2f)
        close()
        moveTo(17f, 15.59f)
        lineTo(15.59f, 17f)
        lineTo(12f, 13.41f)
        lineTo(8.41f, 17f)
        lineTo(7f, 15.59f)
        lineTo(10.59f, 12f)
        lineTo(7f, 8.41f)
        lineTo(8.41f, 7f)
        lineTo(12f, 10.59f)
        lineTo(15.59f, 7f)
        lineTo(17f, 8.41f)
        lineTo(13.41f, 12f)
        lineTo(17f, 15.59f)
        close()
    }
}

val Icons.Filled.Update: ImageVector by lazy {
    icon("Filled.Update") {
        moveTo(21f, 10.12f)
        horizontalLineToRelative(-6.78f)
        lineToRelative(2.74f, -2.82f)
        curveToRelative(-2.73f, -2.7f, -7.15f, -2.8f, -9.88f, -0.1f)
        curveToRelative(-2.73f, 2.71f, -2.73f, 7.08f, 0f, 9.79f)
        curveToRelative(2.73f, 2.71f, 7.15f, 2.71f, 9.88f, 0f)
        curveToRelative(1.36f, -1.35f, 2.04f, -2.92f, 2.04f, -4.9f)
        horizontalLineToRelative(2f)
        curveToRelative(0f, 2.42f, -0.84f, 4.49f, -2.51f, 6.16f)
        curveToRelative(-3.5f, 3.47f, -9.18f, 3.47f, -12.68f, 0f)
        curveToRelative(-3.5f, -3.47f, -3.5f, -9.11f, 0f, -12.58f)
        curveToRelative(3.5f, -3.47f, 9.18f, -3.47f, 12.68f, 0f)
        lineTo(21f, 3f)
        verticalLineToRelative(7.12f)
        close()
        moveTo(12.5f, 8f)
        verticalLineToRelative(4.25f)
        lineToRelative(3.5f, 2.08f)
        lineToRelative(-0.72f, 1.21f)
        lineTo(11f, 13f)
        verticalLineTo(8f)
        horizontalLineToRelative(1.5f)
        close()
    }
}

val Icons.Filled.HourglassTop: ImageVector by lazy {
    icon("Filled.HourglassTop") {
        moveTo(6f, 2f)
        verticalLineToRelative(6f)
        horizontalLineToRelative(0.01f)
        lineTo(6f, 8.01f)
        lineTo(10f, 12f)
        lineToRelative(-4f, 4f)
        verticalLineToRelative(6f)
        horizontalLineToRelative(12f)
        verticalLineToRelative(-6f)
        lineToRelative(-4f, -4f)
        lineToRelative(4f, -3.99f)
        verticalLineTo(2f)
        horizontalLineTo(6f)
        close()
        moveTo(16f, 16.5f)
        verticalLineTo(20f)
        horizontalLineTo(8f)
        verticalLineToRelative(-3.5f)
        lineToRelative(4f, -4f)
        lineToRelative(4f, 4f)
        close()
    }
}

val Icons.Filled.Replay: ImageVector by lazy {
    icon("Filled.Replay") {
        moveTo(12f, 5f)
        verticalLineTo(1f)
        lineTo(7f, 6f)
        lineToRelative(5f, 5f)
        verticalLineTo(7f)
        curveToRelative(3.31f, 0f, 6f, 2.69f, 6f, 6f)
        curveToRelative(0f, 3.31f, -2.69f, 6f, -6f, 6f)
        curveToRelative(-3.31f, 0f, -6f, -2.69f, -6f, -6f)
        horizontalLineTo(4f)
        curveToRelative(0f, 4.42f, 3.58f, 8f, 8f, 8f)
        curveToRelative(4.42f, 0f, 8f, -3.58f, 8f, -8f)
        curveToRelative(0f, -4.42f, -3.58f, -8f, -8f, -8f)
        close()
    }
}

val Icons.Filled.Visibility: ImageVector by lazy {
    icon("Filled.Visibility") {
        moveTo(12f, 4.5f)
        curveTo(7f, 4.5f, 2.73f, 7.61f, 1f, 12f)
        curveToRelative(1.73f, 4.39f, 6f, 7.5f, 11f, 7.5f)
        curveToRelative(5f, 0f, 9.27f, -3.11f, 11f, -7.5f)
        curveToRelative(-1.73f, -4.39f, -6f, -7.5f, -11f, -7.5f)
        close()
        moveTo(12f, 17f)
        curveToRelative(-2.76f, 0f, -5f, -2.24f, -5f, -5f)
        curveToRelative(0f, -2.76f, 2.24f, -5f, 5f, -5f)
        curveToRelative(2.76f, 0f, 5f, 2.24f, 5f, 5f)
        curveToRelative(0f, 2.76f, -2.24f, 5f, -5f, 5f)
        close()
        moveTo(12f, 9f)
        curveToRelative(-1.66f, 0f, -3f, 1.34f, -3f, 3f)
        curveToRelative(0f, 1.66f, 1.34f, 3f, 3f, 3f)
        curveToRelative(1.66f, 0f, 3f, -1.34f, 3f, -3f)
        curveToRelative(0f, -1.66f, -1.34f, -3f, -3f, -3f)
        close()
    }
}

val Icons.Filled.ErrorOutline: ImageVector by lazy {
    icon("Filled.ErrorOutline") {
        moveTo(11f, 15f)
        horizontalLineToRelative(2f)
        verticalLineToRelative(2f)
        horizontalLineToRelative(-2f)
        close()
        moveTo(11f, 7f)
        horizontalLineToRelative(2f)
        verticalLineToRelative(6f)
        horizontalLineToRelative(-2f)
        close()
        moveTo(11.99f, 2f)
        curveTo(6.47f, 2f, 2f, 6.48f, 2f, 12f)
        curveToRelative(0f, 5.52f, 4.47f, 10f, 9.99f, 10f)
        curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
        curveTo(22f, 6.48f, 17.52f, 2f, 11.99f, 2f)
        close()
        moveTo(12f, 20f)
        curveToRelative(-4.42f, 0f, -8f, -3.58f, -8f, -8f)
        curveToRelative(0f, -4.42f, 3.58f, -8f, 8f, -8f)
        curveToRelative(4.42f, 0f, 8f, 3.58f, 8f, 8f)
        curveToRelative(0f, 4.42f, -3.58f, 8f, -8f, 8f)
        close()
    }
}

val Icons.Filled.AccessTime: ImageVector by lazy {
    icon("Filled.AccessTime") {
        moveTo(11.99f, 2f)
        curveTo(6.47f, 2f, 2f, 6.48f, 2f, 12f)
        curveToRelative(0f, 5.52f, 4.47f, 10f, 9.99f, 10f)
        curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
        curveTo(22f, 6.48f, 17.52f, 2f, 11.99f, 2f)
        close()
        moveTo(12f, 20f)
        curveToRelative(-4.42f, 0f, -8f, -3.58f, -8f, -8f)
        curveToRelative(0f, -4.42f, 3.58f, -8f, 8f, -8f)
        curveToRelative(4.42f, 0f, 8f, 3.58f, 8f, 8f)
        curveToRelative(0f, 4.42f, -3.58f, 8f, -8f, 8f)
        close()
        moveTo(12.5f, 7f)
        horizontalLineTo(11f)
        verticalLineToRelative(6f)
        lineToRelative(5.25f, 3.15f)
        lineToRelative(0.75f, -1.23f)
        lineToRelative(-4.5f, -2.67f)
        verticalLineTo(7f)
        close()
    }
}

val Icons.Filled.Bookmark: ImageVector by lazy {
    icon("Filled.Bookmark") {
        moveTo(17f, 3f)
        horizontalLineTo(7f)
        curveToRelative(-1.1f, 0f, -1.99f, 0.9f, -1.99f, 2f)
        lineTo(5f, 21f)
        lineToRelative(7f, -3f)
        lineToRelative(7f, 3f)
        verticalLineTo(5f)
        curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
        close()
    }
}

val Icons.Filled.ChatBubble: ImageVector by lazy {
    icon("Filled.ChatBubble") {
        moveTo(20f, 2f)
        horizontalLineTo(4f)
        curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
        verticalLineToRelative(18f)
        lineToRelative(4f, -4f)
        horizontalLineToRelative(14f)
        curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
        verticalLineTo(4f)
        curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
        close()
    }
}

val Icons.Filled.PlayCircle: ImageVector by lazy {
    icon("Filled.PlayCircle") {
        moveTo(12f, 2f)
        curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
        curveToRelative(0f, 5.52f, 4.48f, 10f, 10f, 10f)
        curveToRelative(5.52f, 0f, 10f, -4.48f, 10f, -10f)
        curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
        close()
        moveTo(10f, 16.5f)
        verticalLineToRelative(-9f)
        lineToRelative(6f, 4.5f)
        lineToRelative(-6f, 4.5f)
        close()
    }
}

val Icons.Filled.VideocamOff: ImageVector by lazy {
    icon("Filled.VideocamOff") {
        moveTo(21f, 6.5f)
        lineToRelative(-4f, 4f)
        verticalLineTo(7f)
        curveToRelative(0f, -0.55f, -0.45f, -1f, -1f, -1f)
        horizontalLineTo(9.82f)
        lineTo(21f, 17.18f)
        verticalLineTo(6.5f)
        close()
        moveTo(3.27f, 2f)
        lineTo(2f, 3.27f)
        lineTo(4.73f, 6f)
        horizontalLineTo(4f)
        curveToRelative(-0.55f, 0f, -1f, 0.45f, -1f, 1f)
        verticalLineToRelative(10f)
        curveToRelative(0f, 0.55f, 0.45f, 1f, 1f, 1f)
        horizontalLineToRelative(12f)
        curveToRelative(0.21f, 0f, 0.39f, -0.08f, 0.54f, -0.18f)
        lineTo(19.73f, 21f)
        lineTo(21f, 19.73f)
        lineTo(3.27f, 2f)
        close()
    }
}

val Icons.Filled.BrokenImage: ImageVector by lazy {
    icon("Filled.BrokenImage") {
        moveTo(21f, 5f)
        verticalLineToRelative(14f)
        curveToRelative(0f, 1.1f, -0.9f, 2f, -2f, 2f)
        horizontalLineTo(5f)
        curveToRelative(-1.1f, 0f, -2f, -0.9f, -2f, -2f)
        verticalLineTo(5f)
        curveToRelative(0f, -1.1f, 0.9f, -2f, 2f, -2f)
        horizontalLineToRelative(14f)
        curveToRelative(1.1f, 0f, 2f, 0.9f, 2f, 2f)
        close()
        moveTo(19f, 5f)
        horizontalLineToRelative(-3.99f)
        lineTo(14f, 6.99f)
        lineTo(10f, 3f)
        horizontalLineTo(5f)
        verticalLineToRelative(14f)
        horizontalLineToRelative(14f)
        verticalLineTo(5f)
        close()
    }
}

val Icons.Filled.Animation: ImageVector by lazy {
    icon("Filled.Animation") {
        moveTo(2f, 2f)
        horizontalLineToRelative(8f)
        verticalLineToRelative(8f)
        horizontalLineTo(2f)
        close()
        moveTo(4f, 4f)
        verticalLineToRelative(4f)
        horizontalLineToRelative(4f)
        verticalLineTo(4f)
        close()
        moveTo(6f, 10f)
        horizontalLineToRelative(8f)
        verticalLineToRelative(8f)
        horizontalLineTo(6f)
        close()
        moveTo(8f, 12f)
        verticalLineToRelative(4f)
        horizontalLineToRelative(4f)
        verticalLineToRelative(-4f)
        close()
        moveTo(10f, 18f)
        horizontalLineToRelative(12f)
        verticalLineToRelative(4f)
        horizontalLineTo(10f)
        close()
    }
}

val Icons.Filled.Shuffle: ImageVector by lazy {
    icon("Filled.Shuffle") {
        moveTo(10.59f, 9.17f)
        lineTo(5.41f, 4f)
        lineTo(4f, 5.41f)
        lineToRelative(5.17f, 5.17f)
        lineToRelative(1.42f, -1.41f)
        close()
        moveTo(14.5f, 4f)
        lineToRelative(2.04f, 2.04f)
        lineTo(4f, 18.59f)
        lineTo(5.41f, 20f)
        lineTo(17.96f, 7.46f)
        lineTo(20f, 9.5f)
        verticalLineTo(4f)
        horizontalLineToRelative(-5.5f)
        close()
        moveTo(14.83f, 13.41f)
        lineToRelative(-1.41f, 1.41f)
        lineToRelative(3.13f, 3.13f)
        lineTo(14.5f, 20f)
        horizontalLineTo(20f)
        verticalLineToRelative(-5.5f)
        lineToRelative(-2.04f, 2.04f)
        lineToRelative(-3.13f, -3.13f)
        close()
    }
}

val Icons.AutoMirrored.Filled.FormatListBulleted: ImageVector by lazy {
    icon("AutoMirrored.Filled.FormatListBulleted") {
        moveTo(4f, 10.5f)
        curveToRelative(-0.83f, 0f, -1.5f, 0.67f, -1.5f, 1.5f)
        curveToRelative(0f, 0.83f, 0.67f, 1.5f, 1.5f, 1.5f)
        curveToRelative(0.83f, 0f, 1.5f, -0.67f, 1.5f, -1.5f)
        curveToRelative(0f, -0.83f, -0.67f, -1.5f, -1.5f, -1.5f)
        close()
        moveTo(4f, 4.5f)
        curveToRelative(-0.83f, 0f, -1.5f, 0.67f, -1.5f, 1.5f)
        curveToRelative(0f, 0.83f, 0.67f, 1.5f, 1.5f, 1.5f)
        curveToRelative(0.83f, 0f, 1.5f, -0.67f, 1.5f, -1.5f)
        curveToRelative(0f, -0.83f, -0.67f, -1.5f, -1.5f, -1.5f)
        close()
        moveTo(4f, 16.5f)
        curveToRelative(-0.83f, 0f, -1.5f, 0.68f, -1.5f, 1.5f)
        curveToRelative(0f, 0.82f, 0.67f, 1.5f, 1.5f, 1.5f)
        curveToRelative(0.83f, 0f, 1.5f, -0.68f, 1.5f, -1.5f)
        curveToRelative(0f, -0.82f, -0.67f, -1.5f, -1.5f, -1.5f)
        close()
        moveTo(7f, 19f)
        horizontalLineToRelative(14f)
        verticalLineToRelative(-2f)
        horizontalLineTo(7f)
        verticalLineToRelative(2f)
        close()
        moveTo(7f, 13f)
        horizontalLineToRelative(14f)
        verticalLineToRelative(-2f)
        horizontalLineTo(7f)
        verticalLineToRelative(2f)
        close()
        moveTo(7f, 5f)
        verticalLineToRelative(2f)
        horizontalLineToRelative(14f)
        verticalLineTo(5f)
        horizontalLineTo(7f)
        close()
    }
}

val Icons.Filled.Explore: ImageVector by lazy {
    icon("Filled.Explore") {
        moveTo(12f, 2f)
        curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
        curveToRelative(0f, 5.52f, 4.48f, 10f, 10f, 10f)
        curveToRelative(5.52f, 0f, 10f, -4.48f, 10f, -10f)
        curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
        close()
        moveTo(14.19f, 14.19f)
        lineTo(6f, 18f)
        lineToRelative(3.81f, -8.19f)
        lineTo(18f, 6f)
        lineToRelative(-3.81f, 8.19f)
        close()
    }
}

val Icons.Filled.AutoAwesome: ImageVector by lazy {
    icon("Filled.AutoAwesome") {
        moveTo(19f, 9f)
        lineToRelative(1.25f, -2.75f)
        lineTo(23f, 5f)
        lineToRelative(-2.75f, -1.25f)
        lineTo(19f, 1f)
        lineToRelative(-1.25f, 2.75f)
        lineTo(15f, 5f)
        lineToRelative(2.75f, 1.25f)
        close()
        moveTo(19f, 15f)
        lineToRelative(-1.25f, 2.75f)
        lineTo(15f, 19f)
        lineToRelative(2.75f, 1.25f)
        lineTo(19f, 23f)
        lineToRelative(1.25f, -2.75f)
        lineTo(23f, 19f)
        lineToRelative(-2.75f, -1.25f)
        close()
        moveTo(11.5f, 9.5f)
        lineTo(9f, 4f)
        lineTo(6.5f, 9.5f)
        lineTo(1f, 12f)
        lineToRelative(5.5f, 2.5f)
        lineTo(9f, 20f)
        lineToRelative(2.5f, -5.5f)
        lineTo(17f, 12f)
        close()
    }
}

val Icons.Filled.PhotoLibrary: ImageVector by lazy {
    icon("Filled.PhotoLibrary") {
        moveTo(22f, 16f)
        verticalLineTo(4f)
        curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
        horizontalLineTo(8f)
        curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
        verticalLineToRelative(12f)
        curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
        horizontalLineToRelative(12f)
        curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
        close()
        moveTo(11f, 12f)
        lineToRelative(2.03f, 2.71f)
        lineTo(16f, 11f)
        lineToRelative(4f, 5f)
        horizontalLineTo(8f)
        lineToRelative(3f, -4f)
        close()
        moveTo(2f, 6f)
        verticalLineToRelative(14f)
        curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
        horizontalLineToRelative(14f)
        verticalLineToRelative(-2f)
        horizontalLineTo(4f)
        verticalLineTo(6f)
        horizontalLineTo(2f)
        close()
    }
}

val Icons.Filled.PhotoAlbum: ImageVector by lazy {
    icon("Filled.PhotoAlbum") {
        moveTo(18f, 2f)
        horizontalLineTo(6f)
        curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
        verticalLineToRelative(16f)
        curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
        horizontalLineToRelative(12f)
        curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
        verticalLineTo(4f)
        curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
        close()
        moveTo(6f, 4f)
        horizontalLineToRelative(5f)
        verticalLineToRelative(8f)
        lineToRelative(-2.5f, -1.5f)
        lineTo(6f, 12f)
        verticalLineTo(4f)
        close()
    }
}

val Icons.Filled.Dns: ImageVector by lazy {
    icon("Filled.Dns") {
        moveTo(20f, 13f)
        horizontalLineTo(4f)
        curveToRelative(-0.55f, 0f, -1f, 0.45f, -1f, 1f)
        verticalLineToRelative(6f)
        curveToRelative(0f, 0.55f, 0.45f, 1f, 1f, 1f)
        horizontalLineToRelative(16f)
        curveToRelative(0.55f, 0f, 1f, -0.45f, 1f, -1f)
        verticalLineToRelative(-6f)
        curveToRelative(0f, -0.55f, -0.45f, -1f, -1f, -1f)
        close()
        moveTo(7f, 19f)
        curveToRelative(-1.1f, 0f, -2f, -0.9f, -2f, -2f)
        curveToRelative(0f, -1.1f, 0.9f, -2f, 2f, -2f)
        curveToRelative(1.1f, 0f, 2f, 0.9f, 2f, 2f)
        curveToRelative(0f, 1.1f, -0.9f, 2f, -2f, 2f)
        close()
        moveTo(20f, 3f)
        horizontalLineTo(4f)
        curveToRelative(-0.55f, 0f, -1f, 0.45f, -1f, 1f)
        verticalLineToRelative(6f)
        curveToRelative(0f, 0.55f, 0.45f, 1f, 1f, 1f)
        horizontalLineToRelative(16f)
        curveToRelative(0.55f, 0f, 1f, -0.45f, 1f, -1f)
        verticalLineTo(4f)
        curveToRelative(0f, -0.55f, -0.45f, -1f, -1f, -1f)
        close()
        moveTo(7f, 9f)
        curveToRelative(-1.1f, 0f, -2f, -0.9f, -2f, -2f)
        curveToRelative(0f, -1.1f, 0.9f, -2f, 2f, -2f)
        curveToRelative(1.1f, 0f, 2f, 0.9f, 2f, 2f)
        curveToRelative(0f, 1.1f, -0.9f, 2f, -2f, 2f)
        close()
    }
}

val Icons.Filled.FolderSpecial: ImageVector by lazy {
    icon("Filled.FolderSpecial") {
        moveTo(20f, 6f)
        horizontalLineToRelative(-8f)
        lineToRelative(-2f, -2f)
        horizontalLineTo(4f)
        curveToRelative(-1.1f, 0f, -1.99f, 0.9f, -1.99f, 2f)
        lineTo(2f, 18f)
        curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
        horizontalLineToRelative(16f)
        curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
        verticalLineTo(8f)
        curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
        close()
        moveTo(17.94f, 13.12f)
        lineToRelative(-2.21f, 1.6f)
        lineToRelative(0.85f, 2.62f)
        lineToRelative(-2.23f, -1.62f)
        lineToRelative(-2.23f, 1.62f)
        lineToRelative(0.85f, -2.62f)
        lineToRelative(-2.21f, -1.6f)
        horizontalLineToRelative(2.74f)
        lineTo(14.35f, 10.5f)
        lineToRelative(0.85f, 2.62f)
        horizontalLineToRelative(2.74f)
        close()
    }
}

val Icons.Filled.GridView: ImageVector by lazy {
    icon("Filled.GridView") {
        moveTo(3f, 3f)
        verticalLineToRelative(8f)
        horizontalLineToRelative(8f)
        verticalLineTo(3f)
        horizontalLineTo(3f)
        close()
        moveTo(9f, 9f)
        horizontalLineTo(5f)
        verticalLineTo(5f)
        horizontalLineToRelative(4f)
        verticalLineToRelative(4f)
        close()
        moveTo(3f, 13f)
        verticalLineToRelative(8f)
        horizontalLineToRelative(8f)
        verticalLineToRelative(-8f)
        horizontalLineTo(3f)
        close()
        moveTo(9f, 19f)
        horizontalLineTo(5f)
        verticalLineToRelative(-4f)
        horizontalLineToRelative(4f)
        verticalLineToRelative(4f)
        close()
        moveTo(13f, 3f)
        verticalLineToRelative(8f)
        horizontalLineToRelative(8f)
        verticalLineTo(3f)
        horizontalLineToRelative(-8f)
        close()
        moveTo(19f, 9f)
        horizontalLineToRelative(-4f)
        verticalLineTo(5f)
        horizontalLineToRelative(4f)
        verticalLineToRelative(4f)
        close()
        moveTo(13f, 13f)
        verticalLineToRelative(8f)
        horizontalLineToRelative(8f)
        verticalLineToRelative(-8f)
        horizontalLineToRelative(-8f)
        close()
        moveTo(19f, 19f)
        horizontalLineToRelative(-4f)
        verticalLineToRelative(-4f)
        horizontalLineToRelative(4f)
        verticalLineToRelative(4f)
        close()
    }
}

val Icons.Filled.GridOn: ImageVector by lazy {
    icon("Filled.GridOn") {
        moveTo(20f, 2f)
        horizontalLineTo(4f)
        curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
        verticalLineToRelative(16f)
        curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
        horizontalLineToRelative(16f)
        curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
        verticalLineTo(4f)
        curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
        close()
        moveTo(8f, 20f)
        horizontalLineTo(4f)
        verticalLineToRelative(-4f)
        horizontalLineToRelative(4f)
        verticalLineToRelative(4f)
        close()
        moveTo(8f, 14f)
        horizontalLineTo(4f)
        verticalLineToRelative(-4f)
        horizontalLineToRelative(4f)
        verticalLineToRelative(4f)
        close()
        moveTo(8f, 8f)
        horizontalLineTo(4f)
        verticalLineTo(4f)
        horizontalLineToRelative(4f)
        verticalLineToRelative(4f)
        close()
        moveTo(14f, 20f)
        horizontalLineToRelative(-4f)
        verticalLineToRelative(-4f)
        horizontalLineToRelative(4f)
        verticalLineToRelative(4f)
        close()
        moveTo(14f, 14f)
        horizontalLineToRelative(-4f)
        verticalLineToRelative(-4f)
        horizontalLineToRelative(4f)
        verticalLineToRelative(4f)
        close()
        moveTo(14f, 8f)
        horizontalLineToRelative(-4f)
        verticalLineTo(4f)
        horizontalLineToRelative(4f)
        verticalLineToRelative(4f)
        close()
        moveTo(20f, 20f)
        horizontalLineToRelative(-4f)
        verticalLineToRelative(-4f)
        horizontalLineToRelative(4f)
        verticalLineToRelative(4f)
        close()
        moveTo(20f, 14f)
        horizontalLineToRelative(-4f)
        verticalLineToRelative(-4f)
        horizontalLineToRelative(4f)
        verticalLineToRelative(4f)
        close()
        moveTo(20f, 8f)
        horizontalLineToRelative(-4f)
        verticalLineTo(4f)
        horizontalLineToRelative(4f)
        verticalLineToRelative(4f)
        close()
    }
}

val Icons.Filled.Collections: ImageVector by lazy { Icons.Filled.PhotoLibrary }

val Icons.Filled.Palette: ImageVector by lazy {
    icon("Filled.Palette") {
        moveTo(12f, 3f)
        curveToRelative(-4.97f, 0f, -9f, 4.03f, -9f, 9f)
        curveToRelative(0f, 2.12f, 0.74f, 4.07f, 1.97f, 5.61f)
        curveToRelative(0.48f, 0.6f, 0.77f, 1.34f, 0.77f, 2.11f)
        curveToRelative(0f, 1.26f, -1.02f, 2.28f, -2.28f, 2.28f)
        horizontalLineToRelative(0.04f)
        curveToRelative(4.97f, 0f, 9f, -4.03f, 9f, -9f)
        curveToRelative(0f, -4.97f, -4.03f, -9f, -9f, -9f)
        close()
        moveTo(6.5f, 12f)
        curveToRelative(-0.83f, 0f, -1.5f, -0.67f, -1.5f, -1.5f)
        curveTo(5f, 9.67f, 5.67f, 9f, 6.5f, 9f)
        curveTo(7.33f, 9f, 8f, 9.67f, 8f, 10.5f)
        curveTo(8f, 11.33f, 7.33f, 12f, 6.5f, 12f)
        close()
        moveTo(9.5f, 8f)
        curveTo(8.67f, 8f, 8f, 7.33f, 8f, 6.5f)
        curveTo(8f, 5.67f, 8.67f, 5f, 9.5f, 5f)
        curveTo(10.33f, 5f, 11f, 5.67f, 11f, 6.5f)
        curveTo(11f, 7.33f, 10.33f, 8f, 9.5f, 8f)
        close()
        moveTo(14.5f, 8f)
        curveToRelative(-0.83f, 0f, -1.5f, -0.67f, -1.5f, -1.5f)
        curveTo(13f, 5.67f, 13.67f, 5f, 14.5f, 5f)
        curveToRelative(0.83f, 0f, 1.5f, 0.67f, 1.5f, 1.5f)
        curveToRelative(0f, 0.83f, -0.67f, 1.5f, -1.5f, 1.5f)
        close()
        moveTo(17.5f, 12f)
        curveToRelative(-0.83f, 0f, -1.5f, -0.67f, -1.5f, -1.5f)
        curveToRelative(0f, -0.83f, 0.67f, -1.5f, 1.5f, -1.5f)
        curveToRelative(0.83f, 0f, 1.5f, 0.67f, 1.5f, 1.5f)
        curveToRelative(0f, 0.83f, -0.67f, 1.5f, -1.5f, 1.5f)
        close()
    }
}

val Icons.Filled.VpnKey: ImageVector by lazy {
    icon("Filled.VpnKey") {
        moveTo(12.65f, 10f)
        curveTo(11.83f, 7.67f, 9.61f, 6f, 7f, 6f)
        curveToRelative(-3.31f, 0f, -6f, 2.69f, -6f, 6f)
        curveToRelative(0f, 3.31f, 2.69f, 6f, 6f, 6f)
        curveToRelative(2.61f, 0f, 4.83f, -1.67f, 5.65f, -4f)
        horizontalLineTo(17f)
        verticalLineToRelative(4f)
        horizontalLineToRelative(4f)
        verticalLineToRelative(-4f)
        horizontalLineToRelative(2f)
        verticalLineToRelative(-4f)
        horizontalLineTo(12.65f)
        close()
        moveTo(7f, 14f)
        curveToRelative(-1.1f, 0f, -2f, -0.9f, -2f, -2f)
        curveToRelative(0f, -1.1f, 0.9f, -2f, 2f, -2f)
        curveToRelative(1.1f, 0f, 2f, 0.9f, 2f, 2f)
        curveToRelative(0f, 1.1f, -0.9f, 2f, -2f, 2f)
        close()
    }
}

val Icons.Filled.Security: ImageVector by lazy {
    icon("Filled.Security") {
        moveTo(12f, 1f)
        lineTo(3f, 5f)
        verticalLineToRelative(6f)
        curveToRelative(0f, 5.55f, 3.84f, 10.74f, 9f, 12f)
        curveToRelative(5.16f, -1.26f, 9f, -6.45f, 9f, -12f)
        verticalLineTo(5f)
        lineToRelative(-9f, -4f)
        close()
        moveTo(12f, 11.99f)
        horizontalLineToRelative(7f)
        curveToRelative(-0.53f, 4.12f, -3.28f, 7.79f, -7f, 8.94f)
        verticalLineTo(12f)
        horizontalLineTo(5f)
        verticalLineTo(6.3f)
        lineToRelative(7f, -3.11f)
        verticalLineToRelative(8.8f)
        close()
    }
}

val Icons.Outlined.Inbox: ImageVector by lazy {
    icon("Outlined.Inbox") {
        moveTo(19f, 3f)
        horizontalLineTo(5f)
        curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
        verticalLineToRelative(14f)
        curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
        horizontalLineToRelative(14f)
        curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
        verticalLineTo(5f)
        curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
        close()
        moveTo(19f, 5f)
        verticalLineToRelative(9f)
        horizontalLineToRelative(-3.5f)
        curveToRelative(-0.83f, 0f, -1.5f, 0.67f, -1.5f, 1.5f)
        curveToRelative(0f, 0.83f, -0.67f, 1.5f, -1.5f, 1.5f)
        horizontalLineToRelative(-1f)
        curveToRelative(-0.83f, 0f, -1.5f, -0.67f, -1.5f, -1.5f)
        curveToRelative(0f, -0.83f, -0.67f, -1.5f, -1.5f, -1.5f)
        horizontalLineTo(5f)
        verticalLineTo(5f)
        horizontalLineToRelative(14f)
        close()
        moveTo(5f, 19f)
        verticalLineToRelative(-2f)
        horizontalLineToRelative(3.13f)
        curveToRelative(0.48f, 1.18f, 1.66f, 2f, 3.02f, 2f)
        horizontalLineToRelative(1.7f)
        curveToRelative(1.36f, 0f, 2.54f, -0.82f, 3.02f, -2f)
        horizontalLineTo(19f)
        verticalLineToRelative(2f)
        horizontalLineTo(5f)
        close()
    }
}
