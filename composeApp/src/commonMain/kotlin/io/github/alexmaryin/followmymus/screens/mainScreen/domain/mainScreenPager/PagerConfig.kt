package io.github.alexmaryin.followmymus.screens.mainScreen.domain.mainScreenPager

import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.account_active_icon
import followmymus.composeapp.generated.resources.account_icon
import followmymus.composeapp.generated.resources.account_page_title
import followmymus.composeapp.generated.resources.favorites_active_icon
import followmymus.composeapp.generated.resources.favorites_icon
import followmymus.composeapp.generated.resources.favorites_page_title
import followmymus.composeapp.generated.resources.releases_active_icon
import followmymus.composeapp.generated.resources.releases_icon
import followmymus.composeapp.generated.resources.releases_page_title
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

@Serializable
sealed class PagerConfig {
    @Serializable
    data object Releases : PagerConfig()

    @Serializable
    data object Favorites : PagerConfig()

    @Serializable
    data object Account : PagerConfig()
}
enum class MainPages(
    val index: Int,
    val titleRes: StringResource,
    val iconRes: DrawableResource,
    val iconActiveRes: DrawableResource
) {
    RELEASES(
        0,
        Res.string.releases_page_title,
        Res.drawable.releases_icon,
        Res.drawable.releases_active_icon
    ),
    FAVORITES(
        1,
        Res.string.favorites_page_title,
        Res.drawable.favorites_icon,
        Res.drawable.favorites_active_icon
    ),
    ACCOUNT(
        2,
        Res.string.account_page_title,
        Res.drawable.account_icon,
        Res.drawable.account_active_icon
    )
}