package com.dejongdevelopment.golfps.models

import androidx.annotation.DrawableRes
import com.dejongdevelopment.golfps.GolfApplication
import com.dejongdevelopment.golfps.R
import kotlin.math.min

open class Badge(
    val id: String,
    var title: String = "ACHIEVEMENT TITLE",
    var description: String = "",
    @DrawableRes var iconResource: Int? = null,
    @DrawableRes var backgroundResource: Int = R.drawable.golf_ball_blank
) {
    protected val me: Me
        get() = GolfApplication.me

    open val progress: Float
        get() = 0f

    open val isUnlocked: Boolean
        get() = false
}

class ExplorerBadge(id: String) : Badge(
    id = id,
    title = "EXPLORER",
    description = "Go to 5 or more unique courses and explore using the app! Spectating a round from a distance doesn't count...",
    iconResource = R.drawable.golf_hole
) {
    override val progress: Float
        get() = 100f * me.numUniqueCourses / 5f

    override val isUnlocked: Boolean
        get() = me.numUniqueCourses >= 5
}

class LocalBadge(id: String) : Badge(
    id = id,
    title = "LOCAL",
    description = "Play at your first course using the app.",
    iconResource = R.drawable.golf_hole
) {
    override val progress: Float
        get() = min(100f, 100f * me.numUniqueCourses)

    override val isUnlocked: Boolean
        get() = me.numUniqueCourses >= 1
}

class RoadTripperBadge(id: String) : Badge(
    id = id,
    title = "ROAD TRIPPER",
    description = "Visit 10 unique courses and build some range with the app.",
    iconResource = R.drawable.golf_hole
) {
    override val progress: Float
        get() = min(100f, 100f * me.numUniqueCourses / 10f)

    override val isUnlocked: Boolean
        get() = me.numUniqueCourses >= 10
}

class LongDriveBadge(id: String) : Badge(
    id = id,
    title = "DRIVER",
    description = "Play a course with a valid long drive hole, tap the Longest Drive button, tap Mark, and successfully record a long drive.",
    iconResource = R.drawable.golf_hole
) {
    override val progress: Float
        get() = if (me.didLogLongDrive) 100f else 0f

    override val isUnlocked: Boolean
        get() = me.didLogLongDrive
}

class CustomizerBadge(id: String) : Badge(
    id = id,
    title = "CUSTOMIZER",
    description = "Update your golf bag with your clubs and distances. Get better club suggestions.",
    iconResource = R.drawable.golf_bag
) {
    override val progress: Float
        get() = if (me.didCustomizeBag) 100f else 0f

    override val isUnlocked: Boolean
        get() = me.didCustomizeBag
}

class BroadcasterBadge(id: String) : Badge(
    id = id,
    title = "BROADCASTER",
    description = "Turn on location sharing so other golfers can see you on the map.",
    iconResource = R.drawable.player_marker
) {
    override val progress: Float
        get() = if (me.shareLocation) 100f else 0f

    override val isUnlocked: Boolean
        get() = me.shareLocation
}

class BitmojiLiveBadge(id: String) : Badge(
    id = id,
    title = "BITMOJI LIVE",
    description = "Share your Bitmoji on the course map.",
    iconResource = R.drawable.player_marker
) {
    override val progress: Float
        get() = if (me.shareBitmoji) 100f else 0f

    override val isUnlocked: Boolean
        get() = me.shareBitmoji
}

class AmbassadorBadge(id: String) : Badge(
    id = id,
    title = "AMBASSADOR",
    description = "Become a course ambassador by convincing the app developer that you are worthy.",
    iconResource = R.drawable.flag_marker
) {
    override val progress: Float
        get() = if (me.didSeeAmbassadorMessage) 100f else 0f

    override val isUnlocked: Boolean
        get() = me.didSeeAmbassadorMessage
}

class CartographerBadge(id: String) : Badge(
    id = id,
    title = "CARTOGRAPHER",
    description = "Become an ambassador for a course and help keep the map accurate.",
    iconResource = R.drawable.flag_marker
) {
    override val progress: Float
        get() = min(100f, 100f * me.ambassadorCourses.size)

    override val isUnlocked: Boolean
        get() = me.ambassadorCourses.isNotEmpty()
}

class ActiveAmbassadorBadge(id: String) : Badge(
    id = id,
    title = "EDITOR",
    description = "Contribute to the community by keeping your course data up to date. Update the position of a tee, pin, or bunker.",
    iconResource = R.drawable.tee_marker
) {
    override val progress: Float
        get() = if (me.didModifyAmbassadorCourse) 100f else 0f

    override val isUnlocked: Boolean
        get() = me.didModifyAmbassadorCourse
}
