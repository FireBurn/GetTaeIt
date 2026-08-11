package uk.co.fireburn.gettaeit.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Brand accents ────────────────────────────────────────────────────────────
// Same trio the mockup and the app icon are built on. Kept as fixed brand colours
// rather than derived, so "personal = thistle" / "work = loch" reads consistently
// wherever it's used.

val ThistleLight = Color(0xFF7C4F96)
val ThistleDark = Color(0xFFC79FE0)

val LochLight = Color(0xFF0065BD)
val LochDark = Color(0xFF5CA6EE)

val IrnBruLight = Color(0xFFE85E10)
val IrnBruDark = Color(0xFFFF8A3D)

val RustLight = Color(0xFFA73824)
val RustDark = Color(0xFFE2694F)

val MossLight = Color(0xFF3E6B4F)
val MossDark = Color(0xFF7FAE8C)

// Back-compat aliases used around the app for context accents.
val PersonalAccent = ThistleLight
val WorkAccent = LochLight

// ─── Neutrals ─────────────────────────────────────────────────────────────────
// A warm, purple-biased neutral ramp instead of stock Material grey.

val InkLight = Color(0xFF211A26)
val InkSoftLight = Color(0xFF6C6072)
val InkFaintLight = Color(0xFF9C90A2)
val BgLight = Color(0xFFF5EFEC)
val SurfaceLight = Color(0xFFFFFDFB)
val SurfaceQuietLight = Color(0xFFF3ECEA)
val LineLight = Color(0x1A211A26)

val InkDark = Color(0xFFF2ECF4)
val InkSoftDark = Color(0xFFB7A9BF)
val InkFaintDark = Color(0xFF7C6F84)
val BgDark = Color(0xFF17131C)
val SurfaceDark = Color(0xFF211A28)
val SurfaceQuietDark = Color(0xFF271F2F)
val LineDark = Color(0x17F2ECF4)

// Retained for anything still referencing the old names directly.
val ThistlePurple = ThistleLight
val IrnBruOrange = IrnBruLight
val LochBlue = LochLight
val MistGrey = BgLight
