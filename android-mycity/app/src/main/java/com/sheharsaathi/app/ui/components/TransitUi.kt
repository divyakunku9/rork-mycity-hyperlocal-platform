package com.sheharsaathi.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsSubway
import androidx.compose.material.icons.filled.ElectricRickshaw
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.PedalBike
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sheharsaathi.app.data.Leg
import com.sheharsaathi.app.data.LegMode
import com.sheharsaathi.app.data.LineColors
import com.sheharsaathi.app.ui.theme.CityColors

fun legIcon(mode: LegMode): ImageVector = when (mode) {
    LegMode.WALK -> Icons.AutoMirrored.Filled.DirectionsWalk
    LegMode.AUTO -> Icons.Filled.ElectricRickshaw
    LegMode.METRO -> Icons.Filled.DirectionsSubway
    LegMode.BUS -> Icons.Filled.DirectionsBus
    LegMode.CYCLE -> Icons.Filled.PedalBike
    LegMode.CAB -> Icons.Filled.LocalTaxi
}

fun legColor(leg: Leg): Color = when (leg.mode) {
    LegMode.WALK -> CityColors.Muted
    LegMode.AUTO -> Color(0xFF2E7D32)
    LegMode.CAB -> CityColors.Ink
    else -> Color(leg.lineColor)
}

/** Container / content colours for a leg's tile. */
fun legTile(leg: Leg): Pair<Color, Color> = when (leg.mode) {
    LegMode.WALK -> CityColors.Canvas to CityColors.Ink
    LegMode.AUTO -> CityColors.SaffronSoft to CityColors.SaffronText
    LegMode.CAB -> CityColors.SurfaceHigh to CityColors.Ink
    LegMode.METRO, LegMode.BUS, LegMode.CYCLE -> Color(leg.lineColor) to Color.White
}

@Composable
fun LineBadge(text: String, color: Color, modifier: Modifier = Modifier) {
    val dark = color == Color(LineColors.YELLOW)
    Text(
        text,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = if (dark) CityColors.Ink else CityColors.Surface,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color)
            .padding(horizontal = 6.dp, vertical = 1.dp),
    )
}

/** Citymapper-style chain: [walk 4] › [Blue] › [walk 8] */
@Composable
fun LegChain(legs: List<Leg>, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        legs.forEachIndexed { i, leg ->
            if (i > 0) Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                tint = CityColors.Muted.copy(alpha = 0.6f), modifier = Modifier.size(16.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Icon(legIcon(leg.mode), null, tint = legColor(leg), modifier = Modifier.size(18.dp))
                if (leg.lineLabel != null && leg.mode != LegMode.CYCLE) {
                    LineBadge(leg.lineLabel, Color(leg.lineColor))
                } else if (leg.mode == LegMode.WALK) {
                    Text("${leg.minutes}", style = MaterialTheme.typography.labelMedium, color = CityColors.Muted)
                }
            }
        }
    }
}

@Composable
fun LegTile(leg: Leg, size: Int = 36) {
    val (bg, fg) = legTile(leg)
    Box(
        Modifier.size(size.dp).clip(RoundedCornerShape(10.dp)).background(bg),
        contentAlignment = Alignment.Center,
    ) { Icon(legIcon(leg.mode), null, tint = fg, modifier = Modifier.size((size * 0.55f).dp)) }
}

/**
 * Stylised route drawn on top of the city base map. Legs are coloured by mode; walking legs dashed.
 * [progress] 0..1 places a live position puck along the route.
 */
@Composable
fun RouteMap(legs: List<Leg>, modifier: Modifier = Modifier, progress: Float? = null) {
    val colors = legs.map { legColor(it) }
    val weights = legs.map { (it.minutes + it.waitMin).coerceAtLeast(1).toFloat() }
    val walks = legs.map { it.mode == LegMode.WALK }
    Canvas(modifier.clipToBounds()) {
        drawCityBase(this)
        val w = size.width
        val h = size.height
        // A gentle S-curve across the map from bottom-left to top-right
        val pts = listOf(
            Offset(w * 0.12f, h * 0.82f), Offset(w * 0.30f, h * 0.62f), Offset(w * 0.48f, h * 0.58f),
            Offset(w * 0.62f, h * 0.38f), Offset(w * 0.80f, h * 0.30f), Offset(w * 0.88f, h * 0.16f),
        )
        fun at(t: Float): Offset {
            val seg = (t.coerceIn(0f, 1f) * (pts.size - 1))
            val i = seg.toInt().coerceAtMost(pts.size - 2)
            val f = seg - i
            return pts[i] + (pts[i + 1] - pts[i]) * f
        }
        val total = weights.sum()
        var start = 0f
        legs.indices.forEach { li ->
            val end = start + weights[li] / total
            val path = Path()
            val steps = 24
            for (s in 0..steps) {
                val p = at(start + (end - start) * s / steps)
                if (s == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            drawPath(path, Color.White, style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round))
            drawPath(
                path, colors[li],
                style = Stroke(
                    width = 6.dp.toPx(), cap = StrokeCap.Round,
                    pathEffect = if (walks[li]) PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 9.dp.toPx())) else null,
                ),
            )
            // transfer node
            if (li > 0) {
                val node = at(start)
                drawCircle(Color.White, 6.dp.toPx(), node)
                drawCircle(colors[li], 6.dp.toPx(), node, style = Stroke(2.5.dp.toPx()))
            }
            start = end
        }
        val a = at(0f)
        drawCircle(Color.White, 8.dp.toPx(), a)
        drawCircle(CityColors.Indigo, 5.dp.toPx(), a)
        val b = at(1f)
        drawCircle(Color.White, 9.dp.toPx(), b)
        drawCircle(CityColors.Saffron, 6.dp.toPx(), b)
        if (progress != null) {
            val p = at(progress)
            drawCircle(CityColors.Indigo.copy(alpha = 0.18f), 18.dp.toPx(), p)
            drawCircle(Color.White, 9.dp.toPx(), p)
            drawCircle(CityColors.Indigo, 6.5.dp.toPx(), p)
        }
    }
}

@Composable
fun DotSeparator() {
    Spacer(Modifier.width(6.dp))
    Text("•", color = CityColors.Muted, style = MaterialTheme.typography.bodySmall)
    Spacer(Modifier.width(6.dp))
}
