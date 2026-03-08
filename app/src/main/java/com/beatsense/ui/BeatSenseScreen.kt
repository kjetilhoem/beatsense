package com.beatsense.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.beatsense.analyzer.AnalyzerResult
import com.beatsense.ui.BeatSenseTheme as T

@Composable
fun BeatSenseScreen(
    bpm: Float,
    musicalKey: String,
    isCapturing: Boolean,
    audioLevel: Float,
    bpmConfidence: Float,
    keyConfidence: Float,
    captureMode: CaptureMode,
    analyzerResults: List<Pair<String, AnalyzerResult>> = emptyList(),
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

    // Filter out results already rendered as hero cards
    val heroIds = setOf("bpm", "key", "level")
    val additionalResults = analyzerResults.filter { it.first !in heroIds }

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

            Spacer(modifier = Modifier.weight(0.05f))

            // — Scrollable content area —
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
                            bottom = T.spaceXL + T.spaceS,
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
                        if (bpm > 0f) {
                            Spacer(modifier = Modifier.height(T.spaceM))
                            ConfidenceBar(confidence = bpmConfidence, label = "confidence")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(T.spaceM))

                // — Key Card —
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
                        Text(
                            text = musicalKey,
                            fontSize = T.textDisplay,
                            fontWeight = FontWeight.Bold,
                            color = T.textPrimary,
                            textAlign = TextAlign.Center
                        )
                        if (musicalKey != "—" && keyConfidence > 0f) {
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

                // — Dynamic analyzer cards (rendered by result type) —
                if (isCapturing && additionalResults.isNotEmpty()) {
                    for ((_, result) in additionalResults) {
                        when (result) {
                            is AnalyzerResult.Bands -> BandsCard(result)
                            is AnalyzerResult.ValueGroup -> ValueGroupCard(result)
                            is AnalyzerResult.HeroValue -> SecondaryHeroCard(result)
                            is AnalyzerResult.Pending -> PendingCard(result)
                            is AnalyzerResult.Meter -> {} // Level meter handled above
                        }
                        Spacer(modifier = Modifier.height(T.spaceS))
                    }
                }

                Spacer(modifier = Modifier.height(T.spaceM))
            }

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

// — Dynamic card renderers (one per AnalyzerResult subtype) —

@Composable
private fun BandsCard(result: AnalyzerResult.Bands) {
    Card(
        shape = RoundedCornerShape(T.radiusM),
        colors = CardDefaults.cardColors(containerColor = T.surface1),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, T.borderSubtle, RoundedCornerShape(T.radiusM))
    ) {
        Column(
            modifier = Modifier.padding(T.spaceM),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = result.label,
                fontSize = T.textCaption,
                fontWeight = FontWeight.Medium,
                color = T.textTertiary,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(T.spaceS))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                for (band in result.bands) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.6f)
                                    .fillMaxHeight(band.level.coerceIn(0.02f, 1f))
                                    .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                                    .background(T.accent.copy(alpha = 0.4f + band.level * 0.5f))
                            )
                        }
                        Text(
                            text = band.name,
                            fontSize = 9.sp,
                            color = T.textTertiary,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ValueGroupCard(result: AnalyzerResult.ValueGroup) {
    Card(
        shape = RoundedCornerShape(T.radiusM),
        colors = CardDefaults.cardColors(containerColor = T.surface1),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, T.borderSubtle, RoundedCornerShape(T.radiusM))
    ) {
        Column(
            modifier = Modifier.padding(T.spaceM),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = result.label,
                fontSize = T.textCaption,
                fontWeight = FontWeight.Medium,
                color = T.textTertiary,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(T.spaceS))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (lv in result.values) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = lv.value,
                            fontSize = T.textTitle,
                            fontWeight = FontWeight.Bold,
                            color = if (lv.value == "—") T.textTertiary else T.textPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (lv.unit.isNotEmpty()) "${lv.label} ${lv.unit}" else lv.label,
                            fontSize = T.textCaption,
                            color = T.textTertiary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SecondaryHeroCard(result: AnalyzerResult.HeroValue) {
    Card(
        shape = RoundedCornerShape(T.radiusM),
        colors = CardDefaults.cardColors(containerColor = T.surface1),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, T.borderSubtle, RoundedCornerShape(T.radiusM))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(
                top = T.spaceM,
                bottom = T.spaceM + T.spaceXS,
                start = T.spaceM,
                end = T.spaceM
            )
        ) {
            Text(
                text = result.label,
                fontSize = T.textCaption,
                fontWeight = FontWeight.Medium,
                color = T.textTertiary,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(T.spaceXS))
            Text(
                text = result.value,
                fontSize = T.textSubtitle,
                fontWeight = FontWeight.Bold,
                color = if (result.accentColor) T.accent
                       else if (result.value == "—") T.textTertiary
                       else T.textPrimary,
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.Monospace
            )
            if (result.confidence > 0f && result.value != "—") {
                Spacer(modifier = Modifier.height(T.spaceS))
                ConfidenceBar(confidence = result.confidence, label = "confidence")
            }
        }
    }
}

@Composable
private fun PendingCard(result: AnalyzerResult.Pending) {
    Card(
        shape = RoundedCornerShape(T.radiusM),
        colors = CardDefaults.cardColors(containerColor = T.surface1),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, T.borderSubtle, RoundedCornerShape(T.radiusM))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(T.spaceM)
        ) {
            Text(
                text = result.label,
                fontSize = T.textCaption,
                fontWeight = FontWeight.Medium,
                color = T.textTertiary,
                letterSpacing = 2.sp
            )
            if (result.reason.isNotEmpty()) {
                Spacer(modifier = Modifier.height(T.spaceXS))
                Text(
                    text = result.reason,
                    fontSize = T.textLabel,
                    color = T.textTertiary
                )
            }
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
