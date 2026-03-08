package com.beatsense.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatsense.ui.BeatSenseTheme as T

@Composable
fun BeatSenseScreen(
    bpm: Float,
    rootNote: String,
    musicalMode: String,
    isCapturing: Boolean,
    audioLevel: Float,
    bpmConfidence: Float,
    keyConfidence: Float,
    captureMode: CaptureMode,
    onModeChanged: (CaptureMode) -> Unit,
    onStartCapture: () -> Unit,
    onStopCapture: () -> Unit
) {
    // Animated values for smooth transitions
    val animatedBpm by animateFloatAsState(
        targetValue = bpm,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "bpm"
    )

    val animatedLevel by animateFloatAsState(
        targetValue = if (isCapturing) audioLevel else 0f,
        animationSpec = tween(durationMillis = 100),
        label = "level"
    )

    // Subtle pulse when capturing
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(T.surface0)
    ) {
        // Subtle radial gradient from accent — the "glow" of the signal
        if (isCapturing && animatedLevel > 0.05f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    T.accent.copy(alpha = animatedLevel * 0.08f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.5f, size.height * 0.3f),
                                radius = size.width * 0.8f
                            )
                        )
                    }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = T.spaceL, vertical = T.spaceXL),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // — Header —
            Spacer(modifier = Modifier.height(T.spaceM))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Status dot
                if (isCapturing) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(T.accent.copy(alpha = pulseAlpha))
                    )
                    Spacer(modifier = Modifier.width(T.spaceS))
                }
                Text(
                    text = if (isCapturing) "LISTENING" else "BEATSENSE",
                    fontSize = T.textLabel,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCapturing) T.accent else T.textTertiary,
                    letterSpacing = 3.sp,
                    fontFamily = FontFamily.SansSerif
                )
            }

            Spacer(modifier = Modifier.weight(0.1f))

            // — BPM Card (the hero moment) —
            Card(
                shape = RoundedCornerShape(T.radiusL),
                colors = CardDefaults.cardColors(containerColor = T.surface1),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (isCapturing && bpm > 0f) T.accent.copy(alpha = 0.15f) else T.borderSubtle,
                        shape = RoundedCornerShape(T.radiusL)
                    )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(
                        top = T.spaceXL,
                        bottom = T.spaceXL + T.spaceS, // asymmetric: heavier bottom
                        start = T.spaceL,
                        end = T.spaceL
                    )
                ) {
                    Text(
                        text = "BPM",
                        fontSize = T.textCaption,
                        fontWeight = FontWeight.Medium,
                        color = T.textTertiary,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(T.spaceS))
                    Text(
                        text = if (animatedBpm > 0f) "%.0f".format(animatedBpm) else "—",
                        fontSize = T.textHero,
                        fontWeight = FontWeight.Bold,
                        color = if (animatedBpm > 0f) T.accent else T.textTertiary,
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily.Monospace
                    )
                    // Confidence indicator
                    if (bpm > 0f) {
                        Spacer(modifier = Modifier.height(T.spaceM))
                        ConfidenceBar(confidence = bpmConfidence, label = "confidence")
                    }
                }
            }

            Spacer(modifier = Modifier.height(T.spaceM))

            // — Key Card (root + mode separated) —
            Card(
                shape = RoundedCornerShape(T.radiusL),
                colors = CardDefaults.cardColors(containerColor = T.surface1),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = T.borderSubtle,
                        shape = RoundedCornerShape(T.radiusL)
                    )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(
                        top = T.spaceL,
                        bottom = T.spaceL + T.spaceS,
                        start = T.spaceL,
                        end = T.spaceL
                    )
                ) {
                    Text(
                        text = "KEY",
                        fontSize = T.textCaption,
                        fontWeight = FontWeight.Medium,
                        color = T.textTertiary,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(T.spaceS))
                    // Root note — large, prominent
                    Text(
                        text = rootNote,
                        fontSize = T.textDisplay,
                        fontWeight = FontWeight.Bold,
                        color = T.textPrimary,
                        textAlign = TextAlign.Center
                    )
                    // Mode — smaller, secondary
                    if (musicalMode.isNotEmpty()) {
                        Text(
                            text = musicalMode,
                            fontSize = T.textBody,
                            fontWeight = FontWeight.Normal,
                            color = T.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                    if (rootNote != "—" && keyConfidence > 0f) {
                        Spacer(modifier = Modifier.height(T.spaceM))
                        ConfidenceBar(confidence = keyConfidence, label = "confidence")
                    }
                }
            }

            Spacer(modifier = Modifier.height(T.spaceM))

            // — Audio Level Meter —
            if (isCapturing) {
                LevelMeter(level = animatedLevel)
                Spacer(modifier = Modifier.height(T.spaceM))
            }

            Spacer(modifier = Modifier.weight(1f))

            // — Mode Selector (only when not capturing) —
            if (!isCapturing) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(T.radiusM))
                        .background(T.surface1)
                        .border(1.dp, T.borderSubtle, RoundedCornerShape(T.radiusM))
                        .padding(T.spaceXS),
                    horizontalArrangement = Arrangement.spacedBy(T.spaceXS)
                ) {
                    CaptureMode.entries.forEach { mode ->
                        val selected = captureMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) T.surface3 else Color.Transparent)
                                .clickable { onModeChanged(mode) }
                                .padding(vertical = T.spaceS),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode.label,
                                fontSize = T.textLabel,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) T.textPrimary else T.textTertiary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(T.spaceM))

                // — Hint text —
                Text(
                    text = if (captureMode == CaptureMode.MICROPHONE)
                        "Sing, hum, or play near the phone"
                    else
                        "Play music in any app, then tap Start",
                    fontSize = T.textLabel,
                    color = T.textTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = T.spaceM)
                )
            }

            // — Action Button (bottom of screen, thumb zone) —
            val buttonColor by animateColorAsState(
                targetValue = if (isCapturing) T.surface2 else T.accent,
                animationSpec = tween(300),
                label = "buttonColor"
            )

            Button(
                onClick = { if (isCapturing) onStopCapture() else onStartCapture() },
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                shape = RoundedCornerShape(T.radiusM),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = if (isCapturing) "Stop Analyzing" else "Start Analyzing",
                    fontSize = T.textBody,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCapturing) T.textSecondary else Color.White
                )
            }

            // — Version (discreet) —
            Text(
                text = "v0.3.1",
                fontSize = T.textCaption,
                color = T.textTertiary.copy(alpha = 0.4f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(T.spaceS))
        }
    }
}

@Composable
private fun ConfidenceBar(confidence: Float, label: String) {
    val animatedConf by animateFloatAsState(
        targetValue = confidence,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "conf"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(3.dp)
                .clip(RoundedCornerShape(T.radiusPill))
                .background(T.surface3)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedConf.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(T.radiusPill))
                    .background(
                        when {
                            animatedConf > 0.6f -> T.success.copy(alpha = 0.7f)
                            animatedConf > 0.3f -> T.warning.copy(alpha = 0.7f)
                            else -> T.textTertiary
                        }
                    )
            )
        }
        Spacer(modifier = Modifier.height(T.spaceXS))
        Text(
            text = label,
            fontSize = T.textCaption,
            color = T.textTertiary,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun LevelMeter(level: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        val segments = 32
        for (i in 0 until segments) {
            val threshold = i.toFloat() / segments
            val active = level > threshold
            val segmentColor = when {
                !active -> T.surface2
                i < segments * 0.6f -> T.accent.copy(alpha = 0.6f)
                i < segments * 0.85f -> T.warning.copy(alpha = 0.6f)
                else -> T.error.copy(alpha = 0.7f)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(1.dp))
                    .background(segmentColor)
            )
        }
    }
}

