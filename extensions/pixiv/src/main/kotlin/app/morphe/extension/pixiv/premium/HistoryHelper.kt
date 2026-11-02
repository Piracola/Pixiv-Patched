package app.morphe.extension.pixiv.premium

import java.util.Collections
import java.util.LinkedList

object HistoryHelper {

    private val localIllusts = Collections.synchronizedList(LinkedList<Any>())
    private val localNovels = Collections.synchronizedList(LinkedList<Any>())
    private const val MAX_HISTORY = 100

    /**
     * Obfuscated PixivIllust class for the pinned app version. Illustrations and novels are both
     * handed to the same detail bottom bar, so they are split into two buckets by class name.
     */
    private val ILLUST_CLASS_NAMES = setOf("e97", "Le97;")

    @JvmStatic
    fun recordView(work: Any?) {
        if (work == null) return
        val bucket = if (isIllust(work)) localIllusts else localNovels
        try {
            val id = getWorkId(work) ?: return
            synchronized(bucket) {
                // Remove existing instance to move to front (most recent)
                bucket.removeAll { getWorkId(it) == id }
                bucket.add(0, work)
                if (bucket.size > MAX_HISTORY) {
                    bucket.removeAt(bucket.size - 1)
                }
            }
        } catch (_: Throwable) {
        }
    }

    /** True when the server returned nothing, so the caller should fall back to the local cache. */
    @JvmStatic
    fun isEmpty(list: List<*>?): Boolean = list.isNullOrEmpty()

    @JvmStatic
    fun illustCache(): ArrayList<Any> = synchronized(localIllusts) { ArrayList(localIllusts) }

    @JvmStatic
    fun novelCache(): ArrayList<Any> = synchronized(localNovels) { ArrayList(localNovels) }

    private fun isIllust(work: Any): Boolean = work.javaClass.name in ILLUST_CLASS_NAMES

    private fun getWorkId(work: Any?): String? {
        if (work == null) return null
        return runCatching {
            work.javaClass.getMethod("getId").invoke(work)?.toString()
        }.getOrNull() ?: runCatching {
            var c: Class<*>? = work.javaClass
            var f: java.lang.reflect.Field? = null
            while (c != null && f == null) {
                f = runCatching { c.getDeclaredField("id") }.getOrNull()
                    ?: runCatching { c.getDeclaredField("a") }.getOrNull()
                c = c.superclass
            }
            f?.apply { isAccessible = true }?.get(work)?.toString()
        }.getOrNull()
    }
}
