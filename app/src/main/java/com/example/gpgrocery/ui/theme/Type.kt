package com.example.gpgrocery.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.gpgrocery.R

/*
 * Two typefaces, both bundled (SIL Open Font License, see app/licenses):
 * Bricolage Grotesque for numbers and headings, Figtree for everything else.
 * Both are variable fonts; each weight used here is a named instance of the
 * one file. Bricolage also has an optical-size axis, set larger for the big
 * numbers so they keep their tighter, display cut.
 */

@OptIn(ExperimentalTextApi::class)
private fun bricolage(weight: Int, opticalSize: Float) = Font(
    R.font.bricolage_grotesque,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight),
        FontVariation.Setting("opsz", opticalSize),
    ),
)

@OptIn(ExperimentalTextApi::class)
private fun figtree(weight: Int) = Font(
    R.font.figtree,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** Headings and section titles. */
val BricolageTitle = FontFamily(bricolage(600, 20f), bricolage(700, 20f), bricolage(800, 20f))

/** The big numbers: today's sales, a product's stock, the receipt total. */
val BricolageDisplay = FontFamily(bricolage(600, 48f), bricolage(700, 48f), bricolage(800, 48f))

val Figtree = FontFamily(figtree(400), figtree(500), figtree(600), figtree(700))

private val tight = (-0.02).em

val CrateTypography = Typography(
    displayLarge = TextStyle(fontFamily = BricolageDisplay, fontWeight = FontWeight.Bold, fontSize = 46.sp, lineHeight = 50.sp, letterSpacing = tight),
    displayMedium = TextStyle(fontFamily = BricolageDisplay, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 44.sp, letterSpacing = tight),
    displaySmall = TextStyle(fontFamily = BricolageDisplay, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = tight),
    headlineLarge = TextStyle(fontFamily = BricolageTitle, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 32.sp, letterSpacing = tight),
    headlineMedium = TextStyle(fontFamily = BricolageTitle, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 30.sp, letterSpacing = tight),
    headlineSmall = TextStyle(fontFamily = BricolageTitle, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = tight),
    titleLarge = TextStyle(fontFamily = BricolageTitle, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = tight),
    titleMedium = TextStyle(fontFamily = BricolageTitle, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = tight),
    titleSmall = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 16.sp),
)

/** Figures that line up in columns: prices, totals, counts. */
fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")
