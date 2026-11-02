package app.morphe.patches.pixiv.privacy

import app.morphe.patches.pixiv.shared.PixivConstants

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch

val pixivAnalyticsBlockerPatch: BytecodePatch = bytecodePatch(
    name = "Pixiv Analytics Blocker",
    description = "Neutralizes Firebase Analytics screen tracking and Google Measurement telemetry for enhanced privacy.",
    default = true
) {
    compatibleWith(PixivConstants.PIXIV)

    execute {
        // FirebaseAnalytics.setCurrentScreen -> return early
        val firebaseClass = mutableClassDefByOrNull("Lcom/google/firebase/analytics/FirebaseAnalytics;")
        firebaseClass?.let { cls ->
            val setScreenMethod = cls.methods.firstOrNull { it.name == "setCurrentScreen" }
            setScreenMethod?.addInstructions(
                1,
                "return-void"
            )
        }
    }
}
