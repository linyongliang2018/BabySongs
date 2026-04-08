package com.swqsv.babysongs.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val MintCardShape = RoundedCornerShape(24.dp)

enum class MintGlassEmphasis {
    /** 专辑行、空提示等 */
    Default,
    /** 歌曲列表当前正在播放：更强薄荷铺色与描边 */
    Playing,
}

/**
 * 横向「左亮右薄荷」分层玻璃条，用于专辑行、歌曲行等；无图标、无模糊，仅渐变 + 高光 + 薄荷色散阴影。
 * 点击由外层 Modifier（如 clickable / combinedClickable）处理。
 */
@Composable
fun MintGlassSplitCard(
    modifier: Modifier = Modifier,
    emphasis: MintGlassEmphasis = MintGlassEmphasis.Default,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val playing = emphasis == MintGlassEmphasis.Playing
    val split = if (playing) {
        Brush.horizontalGradient(
            colorStops = arrayOf(
                0f to scheme.primaryContainer.copy(alpha = 0.92f),
                0.28f to Color.White.copy(alpha = 0.55f),
                0.5f to scheme.primaryContainer.copy(alpha = 0.65f),
                0.72f to scheme.primary.copy(alpha = 0.28f),
                1f to scheme.primary.copy(alpha = 0.38f),
            ),
        )
    } else {
        Brush.horizontalGradient(
            colorStops = arrayOf(
                0f to Color.White.copy(alpha = 0.88f),
                0.35f to scheme.primaryContainer.copy(alpha = 0.52f),
                0.52f to scheme.primary.copy(alpha = 0.07f),
                1f to scheme.primary.copy(alpha = 0.13f),
            ),
        )
    }
    val gloss = Brush.verticalGradient(
        colorStops = arrayOf(
            0f to Color.White.copy(alpha = if (playing) 0.32f else 0.26f),
            0.2f to Color.Transparent,
            1f to scheme.primary.copy(alpha = if (playing) 0.12f else 0.05f),
        ),
    )
    val innerEdge = Brush.verticalGradient(
        colorStops = arrayOf(
            0f to Color.White.copy(alpha = 0.18f),
            0.12f to Color.Transparent,
            1f to Color.Transparent,
        ),
    )
    val strokeW = if (playing) 2.dp else 1.dp
    val stroke = scheme.primary.copy(alpha = if (playing) 0.52f else 0.11f)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (playing) 14.dp else 7.dp,
                shape = MintCardShape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = if (playing) 0.08f else 0.05f),
                spotColor = scheme.primary.copy(alpha = if (playing) 0.34f else 0.13f),
            )
            .then(modifier),
        shape = MintCardShape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(strokeW, stroke),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = split, shape = MintCardShape),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(brush = gloss, shape = MintCardShape),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(brush = innerEdge, shape = MintCardShape),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 15.dp),
                content = content,
            )
        }
    }
}

/**
 * 正方形分类格：白→薄荷的柔和渐变 + 高光，与 [MintGlassSplitCard] 同系。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MintGlassCategoryTile(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val fill = Brush.linearGradient(
        colorStops = arrayOf(
            0f to Color.White.copy(alpha = if (selected) 0.72f else 0.58f),
            0.4f to scheme.primaryContainer.copy(alpha = if (selected) 0.65f else 0.5f),
            0.75f to scheme.primaryContainer.copy(alpha = 0.18f),
            1f to scheme.primary.copy(alpha = if (selected) 0.15f else 0.09f),
        ),
    )
    val gloss = Brush.verticalGradient(
        colorStops = arrayOf(
            0f to Color.White.copy(alpha = 0.24f),
            0.18f to Color.Transparent,
            1f to Color.Transparent,
        ),
    )
    val stroke = scheme.primary.copy(alpha = if (selected) 0.32f else 0.10f)
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (selected) 11.dp else 8.dp,
                shape = MintCardShape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.06f),
                spotColor = scheme.primary.copy(alpha = if (selected) 0.22f else 0.14f),
            )
            .then(modifier),
        shape = MintCardShape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(width = if (selected) 1.5.dp else 1.dp, color = stroke),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = fill, shape = MintCardShape),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(brush = gloss, shape = MintCardShape),
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                content = content,
            )
        }
    }
}
