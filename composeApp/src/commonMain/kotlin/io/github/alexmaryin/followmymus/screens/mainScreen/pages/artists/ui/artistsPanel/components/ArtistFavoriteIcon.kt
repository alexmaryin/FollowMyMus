package io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.ui.artistsPanel.components

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.add
import followmymus.composeapp.generated.resources.favorite
import org.jetbrains.compose.resources.painterResource

@Composable
fun ArtistFavoriteIcon(
    isFavorite: Boolean,
    onCLick: () -> Unit
) {

    val iconRes = if (isFavorite) Res.drawable.favorite else Res.drawable.add
    val rotation by animateFloatAsState(
        targetValue = if (isFavorite) 360f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "rotation"
    )

    AnimatedContent(
        targetState = isFavorite,
        transitionSpec = {
            (slideInVertically() + fadeIn() togetherWith slideOutVertically() + fadeOut()).using(
                SizeTransform(clip = false)
            )
        },
        label = "icon_animation"
    ) {
        IconButton(
            onClick = onCLick
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier
                    .animateContentSize()
                    .rotate(rotation)
            )
        }
    }
}