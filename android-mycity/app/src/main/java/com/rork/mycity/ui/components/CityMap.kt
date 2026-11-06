package com.rork.mycity.ui.components

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Point
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.rork.mycity.data.City
import com.rork.mycity.data.CurrentCity
import com.rork.mycity.ui.theme.CityColors
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import kotlin.math.roundToInt

data class MapPinUi(
    val id: String,
    val lat: Double,
    val lng: Double,
    val label: String,
    val sub: String?,
    val color: Color,
    val icon: ImageVector,
    val visible: Boolean,
    val onClick: () -> Unit,
)

/** Imperative handle for camera moves on the OpenStreetMap view. */
class MapController {
    internal var view: MapView? = null

    fun flyTo(lat: Double, lng: Double, zoom: Double? = null) {
        val v = view ?: return
        v.controller.animateTo(GeoPoint(lat, lng), zoom ?: v.zoomLevelDouble.coerceAtLeast(15.0), 650L)
    }

    fun zoomIn() { view?.controller?.zoomIn(250L) }
    fun zoomOut() { view?.controller?.zoomOut(250L) }
}

private fun MapView.applyCityLimits(c: City) {
    val padLat = (c.north - c.south) * 0.15
    val padLng = (c.east - c.west) * 0.15
    setScrollableAreaLimitLatitude(c.north + padLat, c.south - padLat, 0)
    setScrollableAreaLimitLongitude(c.west - padLng, c.east + padLng, 0)
}

@Composable
fun rememberMapController() = remember { MapController() }

/**
 * Real OpenStreetMap of the chosen city. Pins, the user dot and any dropped pin are Compose overlays
 * projected onto the map, so they share the app's visual language.
 */
@Composable
fun CityMap(
    pins: List<MapPinUi>,
    controller: MapController,
    userLat: Double,
    userLng: Double,
    modifier: Modifier = Modifier,
    dropped: Pair<Double, Double>? = null,
    onLongPress: (Double, Double) -> Unit = { _, _ -> },
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val longPress by rememberUpdatedState(onLongPress)
    var tick by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 0.6f, targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "pulseScale",
    )

    Box(modifier.background(CityColors.MapBase).onSizeChanged { tick++ }) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                    isTilesScaledToDpi = true
                    minZoomLevel = 10.0
                    maxZoomLevel = 19.0
                    applyCityLimits(CurrentCity.city)
                    overlayManager.tilesOverlay.apply {
                        loadingBackgroundColor = CityColors.MapBase.toArgb()
                        loadingLineColor = CityColors.MapBlock.toArgb()
                        // Midnight navy tiles: grayscale, invert, then tint toward navy.
                        val m = ColorMatrix().apply { setSaturation(0f) }
                        val tint = ColorMatrix(
                            floatArrayOf(
                                -0.62f, 0f, 0f, 0f, 170f,
                                0f, -0.72f, 0f, 0f, 203f,
                                0f, 0f, -0.95f, 0f, 280f,
                                0f, 0f, 0f, 1f, 0f,
                            ),
                        )
                        m.postConcat(tint)
                        setColorFilter(ColorMatrixColorFilter(m))
                    }
                    this.controller.setZoom(15.0)
                    this.controller.setCenter(GeoPoint(userLat, userLng))
                    overlays.add(
                        MapEventsOverlay(object : MapEventsReceiver {
                            override fun singleTapConfirmedHelper(p: GeoPoint?) = false
                            override fun longPressHelper(p: GeoPoint?): Boolean {
                                p ?: return false
                                longPress(p.latitude, p.longitude)
                                return true
                            }
                        }),
                    )
                    addMapListener(object : MapListener {
                        override fun onScroll(event: ScrollEvent?): Boolean { tick++; return false }
                        override fun onZoom(event: ZoomEvent?): Boolean { tick++; return false }
                    })
                    addOnFirstLayoutListener { _, _, _, _, _ -> tick++ }
                    this@apply.contentDescription = "Map of ${CurrentCity.city.name}. Pinch to zoom, drag to pan, long-press to drop a pin."
                }.also { controller.view = it }
            },
        )

        val activeCity = CurrentCity.city
        LaunchedEffect(activeCity.id) {
            controller.view?.let { v ->
                v.applyCityLimits(activeCity)
                v.controller.animateTo(GeoPoint(userLat, userLng), 14.5, 900L)
            }
        }

        DisposableEffect(lifecycle) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> controller.view?.onResume()
                    Lifecycle.Event.ON_PAUSE -> controller.view?.onPause()
                    else -> Unit
                }
            }
            lifecycle.addObserver(observer)
            onDispose {
                lifecycle.removeObserver(observer)
                controller.view?.onDetach()
                controller.view = null
            }
        }

        // ---- Compose overlays projected onto the map ----
        // Reading tick re-projects overlays on every pan / zoom frame.
        val view = if (tick >= 0) controller.view else null
        if (view != null) {
            val w = view.width
            val h = view.height
            val zoom = view.zoomLevelDouble
            val showLabels = zoom >= 14.5
            val pt = Point()
            fun project(lat: Double, lng: Double): Point? {
                if (w == 0 || h == 0) return null
                view.projection.toPixels(GeoPoint(lat, lng), pt)
                return Point(pt.x, pt.y)
            }

            // User location dot
            project(userLat, userLng)?.let { p ->
                val r = with(density) { 26.dp.toPx() }
                Canvas(
                    Modifier.size(52.dp).offset { IntOffset((p.x - r).roundToInt(), (p.y - r).roundToInt()) },
                ) {
                    val c = center
                    drawCircle(CityColors.Indigo.copy(alpha = 0.22f * (1.6f - pulseScale)), radius = 22.dp.toPx() * pulseScale, center = c)
                    drawCircle(Color.White, radius = 10.dp.toPx(), center = c)
                    drawCircle(CityColors.Indigo, radius = 7.dp.toPx(), center = c)
                }
            }

            val markerPx = with(density) { 36.dp.toPx() }
            val margin = with(density) { 120.dp.toPx() }
            pins.forEach { pin ->
                val p = project(pin.lat, pin.lng) ?: return@forEach
                val onScreen = p.x > -margin && p.x < w + margin && p.y > -margin && p.y < h + margin
                if (!onScreen) return@forEach
                AnimatedVisibility(
                    visible = pin.visible,
                    enter = scaleIn(initialScale = 0.4f) + fadeIn(),
                    exit = scaleOut(targetScale = 0.4f) + fadeOut(),
                    modifier = Modifier.offset { IntOffset((p.x - markerPx / 2).roundToInt(), (p.y - markerPx).roundToInt()) },
                ) {
                    MapMarker(pin, showLabels)
                }
            }

            dropped?.let { (lat, lng) ->
                project(lat, lng)?.let { p ->
                    val s = with(density) { 44.dp.toPx() }
                    Icon(
                        Icons.Filled.Place, contentDescription = "Selected place",
                        tint = CityColors.Saffron,
                        modifier = Modifier.size(44.dp)
                            .offset { IntOffset((p.x - s / 2).roundToInt(), (p.y - s).roundToInt()) },
                    )
                }
            }
        }

        Text(
            "© OpenStreetMap contributors",
            style = MaterialTheme.typography.labelSmall,
            color = CityColors.Muted,
            modifier = Modifier.align(Alignment.BottomStart).padding(6.dp)
                .background(CityColors.Glass, RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun MapMarker(pin: MapPinUi, showLabel: Boolean) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClickLabel = "Open ${pin.label}") { pin.onClick() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .shadow(4.dp, CircleShape)
                .background(pin.color, CircleShape)
                .border(2.dp, CityColors.Canvas, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(pin.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        if (showLabel) {
            Spacer(Modifier.width(4.dp))
            Column(
                modifier = Modifier
                    .shadow(2.dp, RoundedCornerShape(12.dp))
                    .background(CityColors.SurfaceHigh, RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(pin.label, style = MaterialTheme.typography.labelMedium, color = CityColors.Ink, maxLines = 1)
                if (pin.sub != null) {
                    Text(pin.sub, style = MaterialTheme.typography.bodySmall, color = CityColors.Muted, maxLines = 1)
                }
            }
        }
    }
}

/** Stylised base used by the schematic route previews. */
internal fun drawCityBase(scope: DrawScope) = with(scope) {
    val w = size.width
    val h = size.height
    drawRect(CityColors.MapBase, topLeft = Offset(-w, -h), size = Size(w * 3, h * 3))

    rotate(degrees = -8f, pivot = center) {
        val step = 54.dp.toPx()
        val gap = 6.dp.toPx()
        var y = -h
        var row = 0
        while (y < h * 2) {
            var x = -w
            var col = 0
            while (x < w * 2) {
                val isPark = (row * 7 + col * 3) % 17 == 0
                drawRoundRect(
                    color = if (isPark) CityColors.MapPark else CityColors.MapBlock,
                    topLeft = Offset(x + gap, y + gap),
                    size = Size(step - gap * 2, step * 0.8f - gap * 2),
                    cornerRadius = CornerRadius(6.dp.toPx()),
                )
                x += step
                col++
            }
            y += step * 0.8f
            row++
        }
    }

    val roadEdge = CityColors.IndigoMist
    fun road(a: Offset, b: Offset, width: Float) {
        drawLine(roadEdge, a, b, strokeWidth = width + 3.dp.toPx(), cap = StrokeCap.Round)
        drawLine(Color.White, a, b, strokeWidth = width, cap = StrokeCap.Round)
    }
    road(Offset(-w * 0.2f, h * 0.22f), Offset(w * 1.2f, h * 0.78f), 14.dp.toPx())
    road(Offset(w * 0.52f, -h * 0.2f), Offset(w * 0.4f, h * 1.2f), 10.dp.toPx())
    road(Offset(w * 0.86f, -h * 0.2f), Offset(w * 0.7f, h * 1.2f), 8.dp.toPx())
    road(Offset(-w * 0.2f, h * 0.7f), Offset(w * 1.2f, h * 0.6f), 7.dp.toPx())
    var t = 0f
    val a = Offset(-w * 0.2f, h * 0.05f)
    val b = Offset(w * 1.2f, h * 0.3f)
    while (t < 1f) {
        val s = a + (b - a) * t
        val e = a + (b - a) * (t + 0.02f).coerceAtMost(1f)
        drawLine(CityColors.Indigo.copy(alpha = 0.25f), s, e, strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
        t += 0.04f
    }
    drawOval(Color(0xFFCFE3F7), topLeft = Offset(w * 0.02f, h * 0.55f), size = Size(w * 0.14f, h * 0.12f))
}
