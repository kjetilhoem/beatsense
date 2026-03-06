package com.beatsense.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * BeatSense Design System
 *
 * Visual identity: "Luminous" — a bright signal emerging from darkness.
 * Studio aesthetic: layered dark surfaces, one warm accent, monospace numbers.
 *
 * Palette: cool dark base with coral accent.
 * All neutrals carry a whisper of blue to feel alive, never dead grey.
 */
object BeatSenseTheme {

    // — Surfaces (dark, layered, cool blue undertone) —
    val void = Color(0xFF08081A)           // deepest background
    val surface0 = Color(0xFF0D0D1F)       // base background
    val surface1 = Color(0xFF141428)       // cards, panels
    val surface2 = Color(0xFF1C1C36)       // raised elements
    val surface3 = Color(0xFF252545)       // hover, active states

    // — Accent (warm coral — the beacon in darkness) —
    val accent = Color(0xFFE94560)          // primary accent: CTAs, BPM number, key highlights
    val accentDim = Color(0x33E94560)       // accent at 20% — glow, subtle backgrounds
    val accentSoft = Color(0x66E94560)      // accent at 40% — pressed states

    // — Text hierarchy (never pure white) —
    val textPrimary = Color(0xDEFFFFFF)     // 87% white — headings, key data
    val textSecondary = Color(0x99FFFFFF)   // 60% white — labels, descriptions
    val textTertiary = Color(0x61FFFFFF)    // 38% white — hints, disabled, captions

    // — Semantic —
    val success = Color(0xFF4ADE80)         // green, tinted slightly cool
    val warning = Color(0xFFF59E0B)         // amber
    val error = Color(0xFFEF4444)           // red

    // — Borders —
    val borderSubtle = Color(0x14FFFFFF)    // 8% white — card edges, dividers

    // — Spacing scale (8dp base grid) —
    val spaceXS = 4.dp
    val spaceS = 8.dp
    val spaceM = 16.dp
    val spaceL = 24.dp
    val spaceXL = 32.dp
    val spaceXXL = 48.dp
    val spaceHero = 64.dp

    // — Type scale (modular, ~1.25 ratio) —
    val textCaption = 12.sp
    val textLabel = 14.sp
    val textBody = 16.sp
    val textSubtitle = 20.sp
    val textTitle = 24.sp
    val textHeadline = 32.sp
    val textHero = 80.sp       // BPM number
    val textDisplay = 40.sp    // Key display

    // — Corner radius (one philosophy: medium-round, consistent everywhere) —
    val radiusM = 16.dp
    val radiusL = 24.dp
    val radiusPill = 50.dp
}
