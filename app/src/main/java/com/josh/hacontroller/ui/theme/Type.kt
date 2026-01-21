package com.josh.hacontroller.ui.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.josh.hacontroller.R

val BahnschriftFontFamily = FontFamily(
    Font(R.font.bahnschrift_light, FontWeight.Light),
    Font(R.font.bahnschrift_bold, FontWeight.Bold),
    Font(R.font.bahnschrift_semibold, FontWeight.Normal),
    Font(R.font.bahnschrift_boldcondensed, FontWeight.SemiBold),
    Font(R.font.bahnschrift_lightcondensed, FontWeight.Thin),
    Font(R.font.bahnschrift_condensed, FontWeight.Medium)
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    titleLarge = TextStyle(
        fontFamily = BahnschriftFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = BahnschriftFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp
    ),
    titleMediumEmphasized = TextStyle(
        fontFamily = BahnschriftFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    titleSmall = TextStyle(
        fontFamily = BahnschriftFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelSmall = TextStyle(
        fontFamily = BahnschriftFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelMediumEmphasized =
    TextStyle(
        fontFamily = BahnschriftFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmallEmphasized =
    TextStyle(
        fontFamily = BahnschriftFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = BahnschriftFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineMediumEmphasized =
    TextStyle(
        fontFamily = BahnschriftFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.5.sp
    ),
    labelLarge = TextStyle(
        fontFamily = BahnschriftFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 42.sp,
        letterSpacing = 0.sp
    ),
    labelLargeEmphasized =
    TextStyle(
        fontFamily = BahnschriftFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 42.sp,
        letterSpacing = 0.5.sp
    ),
    headlineLargeEmphasized =
    TextStyle(
        fontFamily = BahnschriftFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 48.sp,
        letterSpacing = 0.5.sp
    )
)
