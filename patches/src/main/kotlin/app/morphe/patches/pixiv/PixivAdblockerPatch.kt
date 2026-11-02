package app.morphe.patches.pixiv

import app.morphe.patches.pixiv.shared.PixivConstants

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch

val pixivAdblockerPatch: BytecodePatch = bytecodePatch(
    name = "Pixiv Adblocker",
    description = "Eliminates bottom advertising banners and interstitials across all screens without layout padding, suppresses rate-this-app dialogs, and hides sponsored works in feeds.",
    default = true
) {
    compatibleWith(PixivConstants.PIXIV)
    extendWith("extensions/pixiv.mpe")

    execute {
        // --- Hook 1: Neutralize bottom overlay banner ad constructors and collapse containers ---
        val bannerOverlayClasses = listOf(
            "Ljp/pxv/android/feature/advertisement/view/OverlayAppLovinView;",
            "Ljp/pxv/android/feature/advertisement/view/OverlayADGAutoRotationView;",
            "Ljp/pxv/android/feature/advertisement/view/OverlayAdgTamView;",
            "Ljp/pxv/android/feature/advertisement/view/YufulightOverlayAdView;"
        )

        for (overlayClassName in bannerOverlayClasses) {
            val overlayClass = mutableClassDefByOrNull(overlayClassName) ?: continue
            val constructors = overlayClass.methods.filter { it.name == "<init>" }
            for (ctor in constructors) {
                val instructions = ctor.implementation?.instructions
                val superInitIndex = instructions?.indexOfFirst {
                    it is com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction &&
                    (it.reference as? com.android.tools.smali.dexlib2.iface.reference.MethodReference)?.name == "<init>"
                } ?: -1

                val insertIndex = if (superInitIndex != -1) superInitIndex + 1 else 1
                ctor.addInstructions(
                    insertIndex,
                    "invoke-static/range {p0 .. p0}, Lapp/morphe/extension/pixiv/adblock/AdblockHelper;->hideBanner(Landroid/view/View;)V"
                )
            }
        }


        // --- Hook 3: AppLovin interstitial display -> return early ---
        // Ported from De-Vanced (https://github.com/RookieEnough/De-Vanced), GPL-3.0.
        // The interstitial manager is the MaxAd listener whose display method gates on isReady().
        val interstitialManagerClass = mutableClassDefByOrNull("Liw;")
        interstitialManagerClass?.let { cls ->
            val showMethod = cls.methods.firstOrNull { method ->
                method.returnType == "V" &&
                    method.parameterTypes.size == 1 &&
                    method.implementation?.instructions?.any { insn ->
                        val reference = (insn as? com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction)?.reference
                        reference is com.android.tools.smali.dexlib2.iface.reference.MethodReference &&
                            reference.definingClass == "Lcom/applovin/mediation/ads/MaxInterstitialAd;" &&
                            reference.name == "isReady"
                    } == true
            }
            showMethod?.addInstructions(0, "return-void")
        }

        // --- Hook 4: Hide sponsored works and promotional cards in feeds and detail screen ---
        val sponsoredClasses = listOf(
            "Ljp/pxv/android/feature/commonlist/recyclerview/baserecycler/SelfServeItemViewHolder;",
            "Ljp/pxv/android/feature/commonlist/recyclerview/baserecycler/RectangleAdViewHolder;",
            "Ljp/pxv/android/feature/illustviewer/detail/IllustDetailAdvertisementSolidItem;"
        )

        for (cardClassName in sponsoredClasses) {
            val cardClass = mutableClassDefByOrNull(cardClassName) ?: continue
            val showMethod = cardClass.methods.firstOrNull { it.name == "show" }
            showMethod?.addInstructions(
                1,
                """
                invoke-static/range {p0 .. p0}, Lapp/morphe/extension/pixiv/adblock/AdblockHelper;->hideSponsoredItem(Ljava/lang/Object;)V
                return-void
                """.trimIndent()
            )
        }
    }
}
