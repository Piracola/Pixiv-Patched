package app.morphe.patches.pixiv.ui

import app.morphe.patches.pixiv.shared.PixivConstants

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch

/**
 * Opens the Pixiv-Patched page from the settings row renamed by [pixivPatchedLabelPatch].
 *
 * The row's own screen (work auto-translation) is replaced at the start of onCreate, so the page owns
 * the whole window and behaves like a normal settings screen.
 */
val pixivPatchedSettingsPatch: BytecodePatch = bytecodePatch(
    name = "Pixiv-Patched Settings Page",
    description = "Adds a Pixiv-Patched row to Pixiv's settings that opens a page with the state of every patch and the browsing history recorded on this device.",
    default = true
) {
    compatibleWith(PixivConstants.PIXIV)

    dependsOn(pixivPatchedLabelPatch)

    extendWith("extensions/pixiv.mpe")

    execute {
        val hostActivity =
            mutableClassDefBy("Ljp/pxv/android/feature/setting/worktranslation/WorkTranslationSettingActivity;")

        val onCreate = hostActivity.methods.firstOrNull {
            it.name == "onCreate" && it.parameterTypes.size == 1
        } ?: throw IllegalStateException("WorkTranslationSettingActivity.onCreate not found")

        onCreate.addInstructions(
            1,
            """
            invoke-static/range {p0 .. p0}, Lapp/morphe/extension/pixiv/ui/PatchedSettings;->setup(Landroid/app/Activity;)V
            return-void
            """.trimIndent()
        )
    }
}
