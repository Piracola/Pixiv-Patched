package app.morphe.patches.pixiv.ui

import app.morphe.patches.pixiv.shared.PixivConstants

import app.morphe.patcher.patch.ResourcePatch
import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Element

private const val ROW_STRING = "feature_setting_settings_work_translation"
private const val ROW_LABEL = "Pixiv-Patched"

private val LABEL_FILES = listOf(
    "res/values/strings.xml",
    "res/values-ja/strings.xml",
    "res/values-ko/strings.xml",
    "res/values-ms/strings.xml",
    "res/values-th/strings.xml",
    "res/values-zh-rCN/strings.xml",
    "res/values-zh-rTW/strings.xml"
)

/**
 * Gives the Pixiv-Patched page a row of its own in the settings list.
 *
 * The settings screen is built with Compose, so a row cannot be appended from the outside; the
 * "auto-translate submitted works" row is repurposed instead, because it is always present and is a
 * server-side setting rather than a display preference. Renaming its label is all the row needs to
 * read as the patch entry.
 */
val pixivPatchedLabelPatch: ResourcePatch = resourcePatch(
    name = "Pixiv-Patched Settings Row",
    description = "Turns the work auto-translation row of Pixiv's settings into the Pixiv-Patched entry that opens the patch page.",
    default = true
) {
    compatibleWith(PixivConstants.PIXIV)

    execute {
        for (path in LABEL_FILES) {
            runCatching {
                document(path).use { document ->
                    val nodes = document.getElementsByTagName("string")
                    for (index in 0 until nodes.length) {
                        val node = nodes.item(index) as? Element ?: continue
                        if (node.getAttribute("name") == ROW_STRING) {
                            node.textContent = ROW_LABEL
                        }
                    }
                }
            }
        }
    }
}
