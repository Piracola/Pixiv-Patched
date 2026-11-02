package app.morphe.patches.pixiv.viewer

import app.morphe.patches.pixiv.shared.PixivConstants

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch

val pixivEnhancedViewerPatch: BytecodePatch = bytecodePatch(
    name = "Pixiv Enhanced Viewer & Instant Zoom",
    description = "Displays the standard-resolution artwork as an instant placeholder while the full-resolution image loads, allows immediate pinch-to-zoom/pan preserving zoom coordinates upon high-res load, and shows a discreet loading indicator.",
    default = true
) {
    compatibleWith(PixivConstants.PIXIV)
    extendWith("extensions/pixiv.mpe")

    execute {
        // --- Hook 1: DetailImageViewHolder.bind$lambda$0 (Capture detail bitmap on image click) ---
        val holderClass = mutableClassDefBy("Ljp/pxv/android/feature/illustviewer/detail/DetailImageViewHolder;")
        val clickMethod = holderClass.methods.first { it.name.contains("lambda") && it.parameterTypes.size == 4 }
        clickMethod.addInstructions(
            0,
            "invoke-static {p1, p0}, Lapp/morphe/extension/pixiv/viewer/EnhancedViewerHelper;->onDetailImageClicked(Ljava/lang/Object;Ljava/lang/Object;)V"
        )

        // --- Hook 2: fullscreen pager adapter instantiateItem (instant placeholder & loading badge) ---
        // The adapter is the only PagerAdapter subclass in the fullscreen artwork viewer:
        // instantiateItem() builds the photo layout (v3) and its attacher (v4) before returning v3.
        val pagerAdapterClass = mutableClassDefBy("Lea4;")
        val instantiateMethod = pagerAdapterClass.methods.first { it.name == "instantiateItem" }
        val instantiateReturnIdx = instantiateMethod.implementation?.instructions?.indexOfLast {
            it.opcode.name.startsWith("return")
        } ?: -1
        if (instantiateReturnIdx >= 0) {
            instantiateMethod.addInstructions(
                instantiateReturnIdx,
                "invoke-static {v3, v4}, Lapp/morphe/extension/pixiv/viewer/EnhancedViewerHelper;->onFullScreenItemCreated(Ljava/lang/Object;Ljava/lang/Object;)V"
            )
        }

        // --- Hook 3: Glide full-res target callback (matrix & zoom preservation, badge dismissal) ---
        val fullResTargetClass = mutableClassDefBy("Lda4;")
        val onResourceReady = fullResTargetClass.methods.first {
            it.name == "c" && it.parameterTypes.size == 2 && it.returnType == "V"
        }
        onResourceReady.addInstructions(
            1,
            "invoke-static {p0}, Lapp/morphe/extension/pixiv/viewer/EnhancedViewerHelper;->onFullResLoaded(Ljava/lang/Object;)V"
        )
    }
}
