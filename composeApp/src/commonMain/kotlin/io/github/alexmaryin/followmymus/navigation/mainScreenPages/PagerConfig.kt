package io.github.alexmaryin.followmymus.navigation.mainScreenPages

import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.favorites_icon
import followmymus.composeapp.generated.resources.favorites_page_title
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


}
enum class MainPages(
    val index: Int,
    val titleRes: StringResource,
    val iconRes: DrawableResource
) {
    RELEASES(0, Res.string.releases_page_title, Res.drawable.releases_icon),
    FAVORITES(1, Res.string.favorites_page_title, Res.drawable.favorites_icon)
}