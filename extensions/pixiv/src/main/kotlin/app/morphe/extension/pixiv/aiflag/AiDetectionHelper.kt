package app.morphe.extension.pixiv.aiflag

import android.content.Context
import java.lang.reflect.Field

object AiDetectionHelper {

    @Volatile
    var appContext: Context? = null

    // Reflection caches
    private var cachedIllustClass: Class<*>? = null
    private var cachedIllustAiTypeField: Field? = null
    private var cachedUserField: Field? = null
    private var cachedTagsField: Field? = null

    private var cachedUserClass: Class<*>? = null
    private var cachedUserIdField: Field? = null

    private var cachedTagClass: Class<*>? = null
    private var cachedTagNameField: Field? = null
    private var cachedTagTranslatedNameField: Field? = null

    // Fallback used when the obfuscated tag class no longer keeps its field names.
    // Pixiv data classes print their original Kotlin property names in toString().
    private val TAG_NAME_PATTERN = Regex("""(?:translatedName|name)=([^,)\]]+)""")

    fun getContext(): Context? {
        if (appContext != null) return appContext
        try {
            val activityThreadClass = Class.forName("android.app.ActivityThread")
            val currentAppMethod = activityThreadClass.getMethod("currentApplication")
            val app = currentAppMethod.invoke(null) as? Context
            if (app != null) {
                appContext = app.applicationContext
                return appContext
            }
        } catch (_: Throwable) {
        }
        return null
    }

    fun unwrapIllust(item: Any?): Any? {
        if (item == null) return null
        val inner = runCatching {
            findField(item.javaClass, "d")?.get(item)
        }.getOrNull() ?: return item

        val hasAiField = findField(inner.javaClass, "illustAiType", "w") != null
        return if (hasAiField) inner else item
    }

    @JvmStatic
    fun isAi(illust: Any?): Boolean {
        if (illust == null) return false
        val actualIllust = unwrapIllust(illust) ?: illust
        val context = getContext()

        return try {
            checkIllust(context, actualIllust)
        } catch (t: Throwable) {
            // Safety fallback: never crash app
            false
        }
    }

    private fun checkIllust(context: Context?, illust: Any): Boolean {
        val clazz = illust.javaClass

        // 1. Extract illustAiType
        var illustAiType = 0
        try {
            if (cachedIllustClass != clazz) {
                cachedIllustClass = clazz
                cachedIllustAiTypeField = findField(clazz, "illustAiType", "w")
                cachedUserField = findField(clazz, "user", "d")
                cachedTagsField = findField(clazz, "tags", "e")
            }
            cachedIllustAiTypeField?.let {
                illustAiType = it.getInt(illust)
            }
        } catch (_: Throwable) {
        }

        // 2. Extract artist/user ID
        var userId = ""
        try {
            val userObj = cachedUserField?.get(illust)
            if (userObj != null) {
                val userClazz = userObj.javaClass
                if (cachedUserClass != userClazz) {
                    cachedUserClass = userClazz
                    cachedUserIdField = findField(userClazz, "id", "a")
                }
                cachedUserIdField?.let {
                    val idVal = it.get(userObj)
                    if (idVal != null) {
                        userId = idVal.toString()
                    }
                }
            }
        } catch (_: Throwable) {
        }

        // 3. Extract tags
        val tagStrings = mutableListOf<String>()
        try {
            val tagsObj = cachedTagsField?.get(illust)
            if (tagsObj is List<*>) {
                for (tag in tagsObj) {
                    if (tag == null) continue
                    if (tag is String) {
                        tagStrings.add(tag)
                        continue
                    }
                    val tagClazz = tag.javaClass
                    if (cachedTagClass != tagClazz) {
                        cachedTagClass = tagClazz
                        cachedTagNameField = findField(tagClazz, "name")
                        cachedTagTranslatedNameField = findField(tagClazz, "translatedName")
                    }
                    val name = readString(cachedTagNameField, tag)
                    val translated = readString(cachedTagTranslatedNameField, tag)
                    if (name.isNullOrEmpty()) {
                        tagStrings.addAll(parseTagNames(tag))
                    } else {
                        tagStrings.add(name)
                    }
                    if (!translated.isNullOrEmpty()) tagStrings.add(translated)
                }
            }
        } catch (_: Throwable) {
        }

        // Check Whitelist if context is available
        if (context != null) {
            val whitelist = AiFilterConfig.getWhitelist(context)
            if (whitelist.isNotEmpty()) {
                val isWhitelisted = tagStrings.any { tagStr ->
                    whitelist.any { w -> tagStr.equals(w, ignoreCase = true) }
                }
                if (isWhitelisted) {
                    return false
                }
            }
        }

        // Check if artist is already blocked
        if (context != null && userId.isNotEmpty()) {
            if (AiFilterConfig.isUserBlocked(context, userId)) {
                return true
            }
        }

        // Check native AI flag (illust_ai_type == 2)
        var isAiDetected = (illustAiType >= 2)

        // Check tag blocklists
        if (!isAiDetected && context != null && tagStrings.isNotEmpty()) {
            val exactBlocklist = AiFilterConfig.getExactBlocklist(context)
            val substringBlocklist = AiFilterConfig.getSubstringBlocklist(context)

            for (tagStr in tagStrings) {
                if (exactBlocklist.any { exact -> tagStr.equals(exact, ignoreCase = true) }) {
                    isAiDetected = true
                    break
                }
                if (substringBlocklist.any { sub -> tagStr.contains(sub, ignoreCase = true) }) {
                    isAiDetected = true
                    break
                }
            }
        }

        // Handle auto-blocking
        if (isAiDetected && context != null && userId.isNotEmpty()) {
            if (AiFilterConfig.getAutoBlockArtists(context)) {
                AiFilterConfig.addBlockedUserId(context, userId)
            }
        }

        return isAiDetected
    }

    private fun readString(field: Field?, target: Any): String? {
        if (field == null) return null
        return runCatching { field.get(target)?.toString() }.getOrNull()
    }

    private fun parseTagNames(tag: Any): List<String> {
        return runCatching {
            TAG_NAME_PATTERN.findAll(tag.toString())
                .map { it.groupValues[1].trim() }
                .filter { it.isNotEmpty() }
                .toList()
        }.getOrDefault(emptyList())
    }

    private fun findField(clazz: Class<*>, vararg fieldNames: String): Field? {
        var current: Class<*>? = clazz
        while (current != null && current != Any::class.java) {
            for (fieldName in fieldNames) {
                try {
                    val f = current.getDeclaredField(fieldName)
                    f.isAccessible = true
                    return f
                } catch (_: NoSuchFieldException) {
                    // try next alias
                }
            }
            current = current.superclass
        }
        return null
    }
}
