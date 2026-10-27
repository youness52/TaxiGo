package com.example.ui.map

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.Ride
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan

@Composable
fun RealTileMapView(
    modifier: Modifier = Modifier,
    centerLat: Double = 37.7749,
    centerLng: Double = -122.4194,
    ride: Ride?,
    driverProgress: Float = 0.25f,
    speedKmh: Double = 42.0,
    currentRoadName: String = "Market Boulevard",
    isTripActive: Boolean = false,
    onCenterOnLocation: () -> Unit = {}
) {
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }
    var zoomLevel by remember { mutableIntStateOf(14) } // Web Mercator Zoom 13-17
    var isNightMode by remember { mutableStateOf(true) }

    // Pulsing radar animation
    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 12f,
        targetValue = 54f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseRadius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseAlpha"
    )
    val dashPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "DashPhase"
    )

    // Calculate center tile in Web Mercator projection
    val n = 2.0.pow(zoomLevel.toDouble())
    val centerTileX = ((centerLng + 180.0) / 360.0 * n)
    val latRad = Math.toRadians(centerLat)
    val centerTileY = ((1.0 - ln(tan(latRad) + 1.0 / kotlin.math.cos(latRad)) / PI) / 2.0 * n)

    val tileSizePx = 256
    val baseTileX = floor(centerTileX).toInt()
    val baseTileY = floor(centerTileY).toInt()

    val subTileOffsetX = ((centerTileX - baseTileX) * tileSizePx).toFloat()
    val subTileOffsetY = ((centerTileY - baseTileY) * tileSizePx).toFloat()

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    panOffsetX += dragAmount.x
                    panOffsetY += dragAmount.y
                }
            }
    ) {
        // 1. Real World Map Tiles Layer (CartoDB / OpenStreetMap 3x3 Grid)
        Box(modifier = Modifier.fillMaxSize()) {
            val context = LocalContext.current
            for (dx in -1..2) {
                for (dy in -2..2) {
                    val tileX = baseTileX + dx
                    val tileY = baseTileY + dy
                    val tileUrl = if (isNightMode) {
                        "https://basemaps.cartocdn.com/rastertiles/dark_all/$zoomLevel/$tileX/$tileY.png"
                    } else {
                        "https://basemaps.cartocdn.com/rastertiles/voyager/$zoomLevel/$tileX/$tileY.png"
                    }

                    val posX = (dx * tileSizePx - subTileOffsetX + panOffsetX + 300).toInt()
                    val posY = (dy * tileSizePx - subTileOffsetY + panOffsetY + 400).toInt()

                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(tileUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Map Tile",
                        modifier = Modifier
                            .size(tileSizePx.dp)
                            .offset { IntOffset(posX, posY) },
                        contentScale = ContentScale.FillBounds
                    )
                }
            }
        }

        // 2. High-Tech Tactical Overlay (Routes, Headlights, Vehicles, Markers)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val centerX = width * 0.5f + panOffsetX
            val centerY = height * 0.45f + panOffsetY

            // InDrive Real Route Navigation coordinates
            val routeCoords = listOf(
                Offset(-180f, -220f),
                Offset(-110f, -130f),
                Offset(-30f, -30f), // Pickup Point
                Offset(50f, 60f),
                Offset(130f, 130f),
                Offset(190f, 270f),
                Offset(230f, 350f)  // Destination Point
            )

            val screenRoutePoints = routeCoords.map { pt ->
                Offset(centerX + pt.x, centerY + pt.y)
            }

            // Draw Neon Glow Route
            drawRoutePath(screenRoutePoints, isNight = isNightMode, dashPhase = dashPhase)

            // Draw Pickup Pin (Green) with Radar Pulse
            val pickupScreen = screenRoutePoints[2]
            drawPickupMarker(
                center = pickupScreen,
                pulseRadius = pulseRadius,
                pulseAlpha = pulseAlpha
            )

            // Draw Destination Pin (Amber/Red)
            val destScreen = screenRoutePoints.last()
            drawDestinationMarker(destScreen)

            // Calculate driver car heading angle and position
            val (driverPos, headingAngle) = computePositionAlongPath(screenRoutePoints, driverProgress)

            // Draw Real-Time Driver Vehicle
            drawDriverVehicle(
                position = driverPos,
                angle = headingAngle,
                driverName = ride?.driverName?.ifBlank { "Marcus Vance" } ?: "Marcus Vance",
                isNight = isNightMode
            )
        }

        // 3. Telemetry HUD (Speedometer, Current Road)
        if (isTripActive || ride != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 16.dp, start = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                tonalElevation = 6.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Speedometer",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "${speedKmh.toInt()} km/h",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = currentRoadName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 4. Map Control Buttons (Right Side)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Day/Night Toggle
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                shadowElevation = 6.dp
            ) {
                IconButton(
                    onClick = { isNightMode = !isNightMode },
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(
                        imageVector = if (isNightMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Toggle Theme",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Recenter GPS
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                shadowElevation = 6.dp
            ) {
                IconButton(
                    onClick = {
                        panOffsetX = 0f
                        panOffsetY = 0f
                        zoomLevel = 14
                        onCenterOnLocation()
                    },
                    modifier = Modifier.size(46.dp).testTag("recenter_gps_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Center on GPS",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Zoom In
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                shadowElevation = 6.dp
            ) {
                IconButton(
                    onClick = { zoomLevel = (zoomLevel + 1).coerceAtMost(17) },
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom In",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Zoom Out
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                shadowElevation = 6.dp
            ) {
                IconButton(
                    onClick = { zoomLevel = (zoomLevel - 1).coerceAtLeast(12) },
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Zoom Out",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawRoutePath(
    points: List<Offset>,
    isNight: Boolean,
    dashPhase: Float
) {
    if (points.size < 2) return

    val path = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            lineTo(points[i].x, points[i].y)
        }
    }

    // Outer glow
    drawPath(
        path = path,
        color = Color(0xFF06B6D4).copy(alpha = if (isNight) 0.35f else 0.2f),
        style = Stroke(width = 20f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Solid base line
    drawPath(
        path = path,
        color = Color(0xFF0284C7),
        style = Stroke(width = 10f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Animated dashed overlay
    drawPath(
        path = path,
        color = Color(0xFF38BDF8),
        style = Stroke(
            width = 6f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 14f), dashPhase)
        )
    )
}

private fun DrawScope.drawPickupMarker(
    center: Offset,
    pulseRadius: Float,
    pulseAlpha: Float
) {
    drawCircle(
        color = Color(0xFF10B981).copy(alpha = pulseAlpha),
        radius = pulseRadius,
        center = center,
        style = Stroke(width = 3f)
    )
    drawCircle(
        color = Color(0xFF10B981).copy(alpha = 0.25f),
        radius = 18f,
        center = center
    )
    drawCircle(
        color = Color(0xFF10B981),
        radius = 10f,
        center = center
    )
    drawCircle(
        color = Color.White,
        radius = 4f,
        center = center
    )
}

private fun DrawScope.drawDestinationMarker(center: Offset) {
    drawCircle(
        color = Color(0xFFEF4444).copy(alpha = 0.25f),
        radius = 16f,
        center = center
    )
    drawCircle(
        color = Color(0xFFEF4444),
        radius = 10f,
        center = center
    )
    drawCircle(
        color = Color.White,
        radius = 4f,
        center = center
    )
}

private fun DrawScope.drawDriverVehicle(
    position: Offset,
    angle: Float,
    driverName: String,
    isNight: Boolean
) {
    rotate(degrees = angle, pivot = position) {
        if (isNight) {
            val beamPath = Path().apply {
                moveTo(position.x - 10f, position.y + 16f)
                lineTo(position.x - 28f, position.y + 80f)
                lineTo(position.x + 28f, position.y + 80f)
                lineTo(position.x + 10f, position.y + 16f)
                close()
            }
            drawPath(
                path = beamPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFEF08A).copy(alpha = 0.45f), Color.Transparent),
                    startY = position.y + 16f,
                    endY = position.y + 80f
                )
            )
        }

        // Vehicle shadow
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.45f),
            topLeft = Offset(position.x - 15f, position.y - 25f + 4f),
            size = Size(30f, 52f),
            cornerRadius = CornerRadius(8f, 8f)
        )

        // Cab Body (Amber / Gold)
        drawRoundRect(
            color = Color(0xFFF59E0B),
            topLeft = Offset(position.x - 14f, position.y - 25f),
            size = Size(28f, 50f),
            cornerRadius = CornerRadius(7f, 7f)
        )

        // Windshield
        drawRoundRect(
            color = Color(0xFF0F172A),
            topLeft = Offset(position.x - 10f, position.y - 12f),
            size = Size(20f, 26f),
            cornerRadius = CornerRadius(4f, 4f)
        )

        // Taxi roof light
        drawRoundRect(
            color = Color(0xFFFDE047),
            topLeft = Offset(position.x - 7f, position.y - 3f),
            size = Size(14f, 6f),
            cornerRadius = CornerRadius(2f, 2f)
        )
    }

    // Floating Driver badge
    drawRoundRect(
        color = Color(0xFF090D16).copy(alpha = 0.9f),
        topLeft = Offset(position.x - 44f, position.y - 54f),
        size = Size(88f, 22f),
        cornerRadius = CornerRadius(11f, 11f)
    )
    drawRoundRect(
        color = Color(0xFFF59E0B),
        topLeft = Offset(position.x - 44f, position.y - 54f),
        size = Size(88f, 22f),
        cornerRadius = CornerRadius(11f, 11f),
        style = Stroke(width = 1.5f)
    )
}

private fun computePositionAlongPath(points: List<Offset>, progress: Float): Pair<Offset, Float> {
    if (points.isEmpty()) return Pair(Offset.Zero, 0f)
    if (points.size == 1) return Pair(points[0], 0f)

    val segmentLengths = mutableListOf<Float>()
    var totalLength = 0f
    for (i in 0 until points.size - 1) {
        val dx = points[i + 1].x - points[i].x
        val dy = points[i + 1].y - points[i].y
        val len = kotlin.math.sqrt(dx * dx + dy * dy)
        segmentLengths.add(len)
        totalLength += len
    }

    if (totalLength == 0f) return Pair(points[0], 0f)

    val targetDist = (progress.coerceIn(0f, 1f)) * totalLength
    var accumulated = 0f

    for (i in segmentLengths.indices) {
        val segLen = segmentLengths[i]
        if (accumulated + segLen >= targetDist || i == segmentLengths.lastIndex) {
            val remaining = (targetDist - accumulated).coerceAtLeast(0f)
            val fraction = if (segLen > 0f) (remaining / segLen).coerceIn(0f, 1f) else 0f
            val p1 = points[i]
            val p2 = points[i + 1]

            val curX = p1.x + (p2.x - p1.x) * fraction
            val curY = p1.y + (p2.y - p1.y) * fraction

            val dx = p2.x - p1.x
            val dy = p2.y - p1.y
            val rad = atan2(dy, dx)
            val deg = Math.toDegrees(rad.toDouble()).toFloat() - 90f

            return Pair(Offset(curX, curY), deg)
        }
        accumulated += segLen
    }

    return Pair(points.last(), 0f)
}
