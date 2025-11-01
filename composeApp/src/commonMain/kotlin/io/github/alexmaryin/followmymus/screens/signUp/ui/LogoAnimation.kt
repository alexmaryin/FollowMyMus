package io.github.alexmaryin.followmymus.screens.signUp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun BoxScope.LogoAnimation(
    imageRes: DrawableResource,
    text: String,
) {
    var showImage by remember { mutableStateOf(false) }
    var showText by remember { mutableStateOf(false) }

    // Start animation automatically when composable appears
    LaunchedEffect(Unit) {
        delay(400L)
        showImage = true
        delay(400L)
        showText = true
    }

    // --- Step 2: Text appears (slide in from left, end-aligned above image) ---
    // inverted order to let image overlap the text
    AnimatedVisibility(
        visible = showText,
        enter = slideInHorizontally(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            ),
            initialOffsetX = { fullWidth -> -fullWidth * 2 } // from left
        ),
        exit = ExitTransition.None,
        modifier = Modifier
            .align(Alignment.BottomStart)
            .fillMaxWidth(0.5f),
        label = "TextAnimation"
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.tertiary,
            textAlign = TextAlign.End,
            modifier = Modifier
                .offset(y = (-30).dp / 1.35f)
                .fillMaxWidth()
        )
    }

    // --- Step 1: Image appears (slide in from right) ---
    AnimatedVisibility(
        visible = showImage,
        enter = slideInHorizontally(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            ),
            initialOffsetX = { fullWidth -> fullWidth * 2 } // from right
        ),
        exit = ExitTransition.None,
        modifier = Modifier.align(Alignment.BottomCenter),
        label = "ImageAnimation"
    ) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = null,
            modifier = Modifier.size(191.dp, 30.dp)
        )
    }
}
