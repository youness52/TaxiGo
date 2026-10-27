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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Ride
import kotlin.math.atan2

@Composable
fun TaxiMapView(
    modifier: Modifier = Modifier,
    ride: Ride?,
    driverProgress: Float = 0.35f, // 0.0f = at pickup/start, 1.0f = destination
    isTripActive: Boolean = false,
    speedKmh: Double = 42.0,
    currentRoadName: String = "Market Boulevard",
    onCenterMap: () -> Unit = {}
) {
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var isNightMode by remember { mutableStateOf(true) }

    // Pulsing radar animation for pickup / vehicle
    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 48f,
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

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        panOffsetX += dragAmount.x
                        panOffsetY += dragAmount.y
                    }
                }
        ) {
            val width = size.width
            val height = size.height

            val bgColor = if (isNightMode) Color(0xFF0D131F) else Color(0xFFE8EEF5)
            drawRect(color = bgColor, size = size)

            // Coordinate transform with pan and zoom
            val centerX = width * 0.5f + panOffsetX
            val centerY = height * 0.45f + panOffsetY

            // City Layout rendering
            drawCityBackground(
                centerX = centerX,
                centerY = centerY,
                zoom = zoomScale,
                isNight = isNightMode,
                canvasWidth = width,
                canvasHeight = height
            )

            // Route points in normalized map coordinate space
            val routeCoords = listOf(
                Offset(-180f, -220f), // Driver starting position
                Offset(-100f, -120f),
                Offset(-40f, -40f),   // Pickup location
                Offset(40f, 60f),
                Offset(120f, 120f),
                Offset(180f, 260f),
                Offset(220f, 340f)    // Destination
            )

            // Scale to screen
            val screenRoutePoints = routeCoords.map { pt ->
                Offset(centerX + pt.x * zoomScale, centerY + pt.y * zoomScale)
            }

            // Draw full route glow and dashed path
            drawRoutePath(
                points = screenRoutePoints,
                isNight = isNightMode,
                dashPhase = dashPhase
            )

            // Draw Pickup Marker at index 2 (-40f, -40f)
            val pickupScreen = screenRoutePoints[2]
            drawPickupMarker(
                center = pickupScreen,
                pulseRadius = pulseRadius * zoomScale,
                pulseAlpha = pulseAlpha
            )

            // Draw Destination Marker at last point
            val destScreen = screenRoutePoints.last()
            drawDestinationMarker(center = destScreen)

            // Compute current Driver position and rotation along the route
            val (driverPos, headingAngle) = computePositionAlongPath(screenRoutePoints, driverProgress)

            // Draw real-time Driver vehicle
            drawDriverVehicle(
                position = driverPos,
                angle = headingAngle,
                driverName = ride?.driverName?.ifBlank { "Marcus V." } ?: "Marcus V.",
                carModel = ride?.carModel?.ifBlank { "Tesla Model Y" } ?: "Tesla Model Y",
                isNight = isNightMode,
                zoom = zoomScale
            )
        }

        // Top-Left Telemetry HUD
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

        // Floating Map Controls (Right Side)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Day/Night Toggle
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shadowElevation = 6.dp
            ) {
                IconButton(
                    onClick = { isNightMode = !isNightMode },
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(
                        imageVector = if (isNightMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Toggle Map Theme",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Recenter
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shadowElevation = 6.dp
            ) {
                IconButton(
                    onClick = {
                        panOffsetX = 0f
                        panOffsetY = 0f
                        zoomScale = 1.0f
                        onCenterMap()
                    },
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Center Map",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Zoom In
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shadowElevation = 6.dp
            ) {
                IconButton(
                    onClick = { zoomScale = (zoomScale * 1.25f).coerceAtMost(2.5f) },
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
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shadowElevation = 6.dp
            ) {
                IconButton(
                    onClick = { zoomScale = (zoomScale * 0.8f).coerceAtLeast(0.5f) },
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

private fun DrawScope.drawCityBackground(
    centerX: Float,
    centerY: Float,
    zoom: Float,
    isNight: Boolean,
    canvasWidth: Float,
    canvasHeight: Float
) {
    val blockColor = if (isNight) Color(0xFF141C2B) else Color(0xFFDCE4EE)
    val waterColor = if (isNight) Color(0xFF082032) else Color(0xFFBAE6FD)
    val parkColor = if (isNight) Color(0xFF0F291E) else Color(0xFFDCFCE7)
    val roadColor = if (isNight) Color(0xFF1F293D) else Color(0xFFFFFFFF)
    val arterialRoadColor = if (isNight) Color(0xFF2B374E) else Color(0xFFF1F5F9)

    // Waterfront / Bay on East side
    val waterPath = Path().apply {
        moveTo(centerX + 320f * zoom, 0f)
        cubicTo(
            centerX + 260f * zoom, canvasHeight * 0.4f,
            centerX + 340f * zoom, canvasHeight * 0.7f,
            centerX + 290f * zoom, canvasHeight
        )
        lineTo(canvasWidth, canvasHeight)
        lineTo(canvasWidth, 0f)
        close()
    }
    drawPath(path = waterPath, color = waterColor)

    // Central Green Park
    drawRoundRect(
        color = parkColor,
        topLeft = Offset(centerX - 160f * zoom, centerY - 90f * zoom),
        size = Size(110f * zoom, 160f * zoom),
        cornerRadius = CornerRadius(16f * zoom, 16f * zoom)
    )

    // City Building Blocks Grid
    val blockW = 70f * zoom
    val blockH = 50f * zoom
    val gap = 16f * zoom

    for (row in -4..5) {
        for (col in -4..3) {
            // Avoid drawing over park
            val bx = centerX + col * (blockW + gap)
            val by = centerY + row * (blockH + gap)
            if (!(col in -2..-1 && row in -1..1)) {
                drawRoundRect(
                    color = blockColor,
                    topLeft = Offset(bx, by),
                    size = Size(blockW, blockH),
                    cornerRadius = CornerRadius(8f * zoom, 8f * zoom)
                )
            }
        }
    }

    // Grid Roads
    for (col in -5..4) {
        val rx = centerX + col * (blockW + gap) - gap * 0.5f
        drawLine(
            color = if (col % 2 == 0) arterialRoadColor else roadColor,
            start = Offset(rx, 0f),
            end = Offset(rx, canvasHeight),
            strokeWidth = (if (col % 2 == 0) 14f else 8f) * zoom
        )
    }

    for (row in -5..6) {
        val ry = centerY + row * (blockH + gap) - gap * 0.5f
        drawLine(
            color = if (row % 2 == 0) arterialRoadColor else roadColor,
            start = Offset(0f, ry),
            end = Offset(canvasWidth, ry),
            strokeWidth = (if (row % 2 == 0) 14f else 8f) * zoom
        )
    }

    // Diagonal Waterfront Boulevard
    drawLine(
        color = Color(0xFFF59E0B).copy(alpha = if (isNight) 0.35f else 0.25f),
        start = Offset(centerX - 280f * zoom, centerY + 380f * zoom),
        end = Offset(centerX + 320f * zoom, centerY - 280f * zoom),
        strokeWidth = 18f * zoom
    )
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

    // Animated neon dashed overlay
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
    // Outer radar waves
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

    // Solid Green Core
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
    // Halo
    drawCircle(
        color = Color(0xFFEF4444).copy(alpha = 0.25f),
        radius = 16f,
        center = center
    )
    // Red pin
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
    carModel: String,
    isNight: Boolean,
    zoom: Float
) {
    rotate(degrees = angle, pivot = position) {
        // Headlights beam (if night)
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

        // Cab Body (Sleek Taxi Amber / Yellow)
        drawRoundRect(
            color = Color(0xFFF59E0B),
            topLeft = Offset(position.x - 14f, position.y - 25f),
            size = Size(28f, 50f),
            cornerRadius = CornerRadius(7f, 7f)
        )

        // Windshield and Roof
        drawRoundRect(
            color = Color(0xFF0F172A),
            topLeft = Offset(position.x - 10f, position.y - 12f),
            size = Size(20f, 26f),
            cornerRadius = CornerRadius(4f, 4f)
        )

        // Taxi Roof Sign (Luminous Bright Yellow)
        drawRoundRect(
            color = Color(0xFFFDE047),
            topLeft = Offset(position.x - 7f, position.y - 3f),
            size = Size(14f, 6f),
            cornerRadius = CornerRadius(2f, 2f)
        )

        // Headlights
        drawCircle(
            color = Color(0xFFFFFBEB),
            radius = 3f,
            center = Offset(position.x - 9f, position.y + 23f)
        )
        drawCircle(
            color = Color(0xFFFFFBEB),
            radius = 3f,
            center = Offset(position.x + 9f, position.y + 23f)
        )

        // Taillights
        drawCircle(
            color = Color(0xFFEF4444),
            radius = 2.5f,
            center = Offset(position.x - 9f, position.y - 24f)
        )
        drawCircle(
            color = Color(0xFFEF4444),
            radius = 2.5f,
            center = Offset(position.x + 9f, position.y - 24f)
        )
    }

    // Driver name & badge pill above vehicle (not rotated)
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

    // Compute segment lengths
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
            // Calculate angle in degrees (+90 because vehicle is drawn pointing down)
            val rad = atan2(dy, dx)
            val deg = Math.toDegrees(rad.toDouble()).toFloat() - 90f

            return Pair(Offset(curX, curY), deg)
        }
        accumulated += segLen
    }

    return Pair(points.last(), 0f)
}
