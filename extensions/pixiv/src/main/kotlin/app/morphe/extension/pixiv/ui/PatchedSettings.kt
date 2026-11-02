package app.morphe.extension.pixiv.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import app.morphe.extension.pixiv.history.HistoryStore
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * The Pixiv-Patched page: a row pinned to the top of Pixiv's own settings screen that opens a
 * native page with the state of every patch plus the browsing history recorded on this device.
 */
object PatchedSettings {

    private const val PAGE_TITLE = "Pixiv-Patched"
    private const val DETAIL_ACTIVITY = "jp.pxv.android.feature.routing.intentfilter.IntentFilterActivity"

    private val timeFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    private val imagePool = Executors.newFixedThreadPool(3)
    private val imageCache = object : LinkedHashMap<String, Bitmap>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>?) = size > 96
    }

    private class Palette(context: Context) {
        val dark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val background = if (dark) 0xFF121212.toInt() else 0xFFF5F5F7.toInt()
        val card = if (dark) 0xFF1E1E1E.toInt() else 0xFFFFFFFF.toInt()
        val primary = if (dark) 0xFFEEEEEE.toInt() else 0xFF1F2937.toInt()
        val secondary = if (dark) 0xFF9E9E9E.toInt() else 0xFF6B7280.toInt()
        val divider = if (dark) 0xFF2D2D2D.toInt() else 0xFFE5E7EB.toInt()
        val accent = 0xFF0096FA.toInt()
    }

    /**
     * Takes over the settings screen that hosts the page. It runs from that activity's onCreate
     * before its own content is inflated, so the whole window belongs to the page from the start.
     */
    @JvmStatic
    fun setup(activity: Activity) {
        try {
            val palette = Palette(activity)
            val root = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(palette.background)
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            root.addView(buildToolbar(activity, palette) { activity.finish() })
            root.addView(buildContent(activity, palette))
            activity.setContentView(root)
        } catch (t: Throwable) {
            Toast.makeText(activity, "$PAGE_TITLE: ${t.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildToolbar(context: Context, palette: Palette, onBack: () -> Unit): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(palette.card)
            val horizontal = dp(context, 16f)
            setPadding(horizontal, 0, horizontal, 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(context, 56f)
            )

            addView(
                TextView(context).apply {
                    text = "←"
                    textSize = 24f
                    setTextColor(palette.primary)
                    setPadding(0, 0, dp(context, 14f), 0)
                    setOnClickListener { onBack() }
                }
            )
            addView(
                TextView(context).apply {
                    text = PAGE_TITLE
                    textSize = 18f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(palette.primary)
                }
            )
        }
    }

    private fun buildContent(context: Context, palette: Palette): View {
        val scroll = ScrollView(context).apply {
            isFillViewport = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val padding = dp(context, 16f)
            setPadding(padding, padding, padding, dp(context, 28f))
        }
        scroll.addView(content)

        content.addView(sectionHeader(context, palette, "状态"))

        val countValue = valueText(context, palette, "0 条")
        val statusCard = card(context, palette)
        statusCard.addView(statusRow(context, palette, "高级功能模拟", valueText(context, palette, "已开启")))
        statusCard.addView(divider(context, palette))
        statusCard.addView(statusRow(context, palette, "广告拦截", valueText(context, palette, "已开启")))
        statusCard.addView(divider(context, palette))
        statusCard.addView(statusRow(context, palette, "AI 作品标记", valueText(context, palette, "已开启")))
        statusCard.addView(divider(context, palette))
        statusCard.addView(statusRow(context, palette, "本地浏览记录", countValue))
        content.addView(statusCard)

        content.addView(sectionHeader(context, palette, "本地浏览记录"))

        val historyCard = card(context, palette)
        content.addView(historyCard)

        val emptyHint = TextView(context).apply {
            text = "还没有记录。打开任意插画或小说后会自动记录在这里。"
            textSize = 13f
            setTextColor(palette.secondary)
            setPadding(0, dp(context, 10f), 0, 0)
        }
        content.addView(emptyHint)

        val clearRow = LinearLayout(context).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(context, 20f), 0, 0)
        }
        clearRow.addView(
            TextView(context).apply {
                text = "清空本地记录"
                textSize = 14f
                setTextColor(palette.secondary)
                val horizontal = dp(context, 18f)
                val vertical = dp(context, 9f)
                setPadding(horizontal, vertical, horizontal, vertical)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(context, 8f).toFloat()
                    setStroke(dp(context, 1f), palette.divider)
                }
                setOnClickListener {
                    HistoryStore.clear(context)
                    renderHistory(context, palette, historyCard, emptyHint, countValue)
                    Toast.makeText(context, "已清空", Toast.LENGTH_SHORT).show()
                }
            }
        )
        content.addView(clearRow)

        renderHistory(context, palette, historyCard, emptyHint, countValue)
        return scroll
    }

    private fun renderHistory(
        context: Context,
        palette: Palette,
        container: LinearLayout,
        emptyHint: TextView,
        countValue: TextView
    ) {
        container.removeAllViews()
        val entries = HistoryStore.entries(context)
        countValue.text = "${entries.size} 条"
        emptyHint.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE

        entries.forEachIndexed { index, entry ->
            if (index > 0) container.addView(divider(context, palette))
            container.addView(historyRow(context, palette, entry))
        }
    }

    private fun historyRow(context: Context, palette: Palette, entry: HistoryStore.Entry): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val padding = dp(context, 12f)
            setPadding(padding, padding, padding, padding)
            setOnClickListener { openWork(context, entry) }
        }

        val size = dp(context, 44f)
        val image = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(size, size)
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(context, 6f).toFloat()
                setColor(palette.divider)
            }
            clipToOutline = true
        }
        row.addView(image)
        if (entry.thumb.isNotEmpty()) loadThumb(image, entry.thumb)

        val texts = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(dp(context, 12f), 0, 0, 0)
        }
        texts.addView(
            TextView(context).apply {
                text = entry.title.ifEmpty {
                    if (entry.kind == HistoryStore.KIND_NOVEL) "小说 ${entry.id}" else "作品 ${entry.id}"
                }
                textSize = 14f
                setTextColor(palette.primary)
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            }
        )
        val detail = buildString {
            if (entry.author.isNotEmpty()) append(entry.author)
            if (entry.time > 0L) {
                if (isNotEmpty()) append(" · ")
                append(timeFormat.format(Date(entry.time)))
            }
        }
        if (detail.isNotEmpty()) {
            texts.addView(
                TextView(context).apply {
                    text = detail
                    textSize = 11f
                    setTextColor(palette.secondary)
                    setPadding(0, dp(context, 3f), 0, 0)
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                }
            )
        }
        row.addView(texts)

        row.addView(
            TextView(context).apply {
                text = if (entry.kind == HistoryStore.KIND_NOVEL) "小说" else "插画"
                textSize = 11f
                setTextColor(palette.secondary)
                setPadding(dp(context, 8f), 0, 0, 0)
            }
        )
        return row
    }

    private fun openWork(context: Context, entry: HistoryStore.Entry) {
        val url = if (entry.kind == HistoryStore.KIND_NOVEL) {
            "https://www.pixiv.net/novel/show.php?id=${entry.id}"
        } else {
            "https://www.pixiv.net/artworks/${entry.id}"
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.setClassName(context.packageName, DETAIL_ACTIVITY)
            context.startActivity(intent)
        } catch (_: Throwable) {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (_: Throwable) {
            }
        }
    }

    private fun loadThumb(image: ImageView, url: String) {
        synchronized(imageCache) { imageCache[url] }?.let {
            image.setImageBitmap(it)
            return
        }
        image.tag = url
        imagePool.execute {
            val bitmap = try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                connection.setRequestProperty("Referer", "https://www.pixiv.net/")
                connection.inputStream.use { BitmapFactory.decodeStream(it) }
            } catch (_: Throwable) {
                null
            } ?: return@execute

            synchronized(imageCache) { imageCache[url] = bitmap }
            image.post {
                if (image.tag == url) image.setImageBitmap(bitmap)
            }
        }
    }

    private fun statusRow(context: Context, palette: Palette, label: String, value: TextView): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val padding = dp(context, 16f)
            setPadding(padding, padding, padding, padding)

            addView(
                TextView(context).apply {
                    text = label
                    textSize = 15f
                    setTextColor(palette.primary)
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }
            )
            addView(value)
        }
    }

    private fun valueText(context: Context, palette: Palette, value: String): TextView {
        return TextView(context).apply {
            text = value
            textSize = 14f
            setTextColor(palette.accent)
        }
    }

    private fun sectionHeader(context: Context, palette: Palette, title: String): TextView {
        return TextView(context).apply {
            text = title
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(palette.accent)
            setPadding(dp(context, 4f), dp(context, 18f), dp(context, 4f), dp(context, 8f))
        }
    }

    private fun card(context: Context, palette: Palette): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(palette.card)
                cornerRadius = dp(context, 10f).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun divider(context: Context, palette: Palette): View {
        return View(context).apply {
            setBackgroundColor(palette.divider)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(context, 1f)
            )
        }
    }

    private fun dp(context: Context, value: Float): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        value,
        context.resources.displayMetrics
    ).toInt()
}
