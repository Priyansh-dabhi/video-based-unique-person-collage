package com.example.video_basedunique_personcollage.data.collage

import android.graphics.*
import com.example.video_basedunique_personcollage.data.model.CollageStyle
import com.example.video_basedunique_personcollage.data.model.PersonCluster
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * High-performance hardware Canvas rendering engine for generating
 * exportable, print-quality collages of unique video persons.
 */
object CollageGenerator {

    /**
     * Renders a complete collage bitmap for the given clusters in the requested style.
     */
    fun generateCollage(
        clusters: List<PersonCluster>,
        style: CollageStyle
    ): Bitmap {
        // Collect best photo for each cluster
        val items = clusters.mapNotNull { cluster ->
            val bestFace = BestShotSelector.selectBestShot(cluster)
            val bitmap = bestFace?.croppedBitmap ?: cluster.representativeBitmap
            if (bitmap != null && !bitmap.isRecycled) {
                CollageItem(cluster.id, cluster.appearanceCount, bitmap)
            } else null
        }

        if (items.isEmpty()) {
            return createPlaceholderBitmap("No faces available to create collage.")
        }

        return when (style) {
            CollageStyle.MODERN_GRID -> renderModernGrid(items)
            CollageStyle.POLAROID -> renderPolaroidBoard(items)
            CollageStyle.STORY_POSTER -> renderStoryPoster(items)
        }
    }

    private data class CollageItem(val id: Int, val appearances: Int, val bitmap: Bitmap)

    // -------------------------------------------------------------------------
    // STYLE 1: MODERN GRID
    // -------------------------------------------------------------------------
    private fun renderModernGrid(items: List<CollageItem>): Bitmap {
        val count = items.size
        val cols = when {
            count == 1 -> 1
            count in 2..4 -> 2
            else -> 3
        }
        val rows = ceil(count.toFloat() / cols).toInt()

        val canvasWidth = 1080
        val headerHeight = 160
        val footerHeight = 60
        val padding = 32f
        val spacing = 20f

        val availableWidth = canvasWidth - (padding * 2) - (spacing * (cols - 1))
        val cellWidth = availableWidth / cols
        val cellHeight = cellWidth * 1.05f // slightly portrait

        val canvasHeight = (headerHeight + footerHeight + (padding * 2) + (rows * cellHeight) + ((rows - 1) * spacing)).toInt()

        val output = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // Background: Deep sleek charcoal gradient
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, canvasHeight.toFloat(),
                Color.parseColor("#141518"), Color.parseColor("#0C0D0E"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, canvasWidth.toFloat(), canvasHeight.toFloat(), bgPaint)

        // Header Title
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F5F5F7")
            textSize = 44f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Unique Person Collage", padding, 70f, titlePaint)

        // Subtitle stats
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#9E9EA7")
            textSize = 24f
            typeface = Typeface.DEFAULT
        }
        val totalAppearances = items.sumOf { it.appearances }
        canvas.drawText("$count Unique People • $totalAppearances Total Appearances", padding, 115f, subPaint)

        // Draw each face card
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2C2D35")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val overlayPaint = Paint().apply {
            color = Color.parseColor("#CC000000")
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E0E0E0")
            textSize = 20f
        }

        val cornerRadius = 24f

        for (i in items.indices) {
            val item = items[i]
            val r = i / cols
            val c = i % cols

            val left = padding + c * (cellWidth + spacing)
            val top = headerHeight + padding + r * (cellHeight + spacing)
            val right = left + cellWidth
            val bottom = top + cellHeight
            val rect = RectF(left, top, right, bottom)

            // Clip rounded rectangle for photo
            canvas.save()
            val clipPath = Path().apply { addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW) }
            canvas.clipPath(clipPath)

            // Draw cropped photo centered
            drawBitmapFitCenter(canvas, item.bitmap, rect, cardPaint)

            // Gradient overlay at bottom of card
            val overlayHeight = 70f
            val gradPaint = Paint().apply {
                shader = LinearGradient(
                    0f, bottom - overlayHeight, 0f, bottom,
                    Color.TRANSPARENT, Color.parseColor("#D9000000"),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(left, bottom - overlayHeight, right, bottom, gradPaint)

            // Person label & appearance count
            canvas.drawText("Person #${item.id}", left + 14f, bottom - 34f, labelPaint)
            canvas.drawText("${item.appearances} appearances", left + 14f, bottom - 12f, badgePaint)

            canvas.restore()

            // Outer border
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)
        }

        return output
    }

    // -------------------------------------------------------------------------
    // STYLE 2: POLAROID BOARD
    // -------------------------------------------------------------------------
    private fun renderPolaroidBoard(items: List<CollageItem>): Bitmap {
        val count = items.size
        val cols = when {
            count == 1 -> 1
            count in 2..4 -> 2
            else -> 3
        }
        val rows = ceil(count.toFloat() / cols).toInt()

        val canvasWidth = 1080
        val headerHeight = 150
        val footerHeight = 60
        val padding = 40f
        val spacing = 28f

        val availableWidth = canvasWidth - (padding * 2) - (spacing * (cols - 1))
        val cardWidth = availableWidth / cols
        val cardHeight = cardWidth * 1.25f // classic polaroid proportion

        val canvasHeight = (headerHeight + footerHeight + (padding * 2) + (rows * cardHeight) + ((rows - 1) * spacing)).toInt()

        val output = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // Studio warm dark slate background
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, canvasHeight.toFloat(),
                Color.parseColor("#1B1E22"), Color.parseColor("#101214"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, canvasWidth.toFloat(), canvasHeight.toFloat(), bgPaint)

        // Header Title
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F5E6CC")
            textSize = 42f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        canvas.drawText("Moments & Faces", padding, 72f, titlePaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#8A8F99")
            textSize = 22f
            typeface = Typeface.DEFAULT
        }
        canvas.drawText("${items.size} unique individuals captured", padding, 112f, subPaint)

        // Polaroid card styling
        val polaroidBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FAF8F5")
            setShadowLayer(14f, 0f, 8f, Color.parseColor("#50000000"))
        }
        val photoMargin = 16f
        val photoBottomMargin = cardHeight * 0.22f
        val polaroidText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2A2C30")
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val polaroidSub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#7A7E85")
            textSize = 18f
            textAlign = Paint.Align.CENTER
        }
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }

        // Slight organic rotations for Polaroid realism
        val rotations = floatArrayOf(-1.5f, 1.8f, -2.0f, 1.2f, -1.0f, 2.2f, -1.8f, 1.5f)

        for (i in items.indices) {
            val item = items[i]
            val r = i / cols
            val c = i % cols

            val left = padding + c * (cardWidth + spacing)
            val top = headerHeight + padding + r * (cardHeight + spacing)
            val centerX = left + cardWidth / 2
            val centerY = top + cardHeight / 2

            val rot = rotations[i % rotations.size]

            canvas.save()
            canvas.rotate(rot, centerX, centerY)

            // Draw Polaroid White Card with Soft Shadow
            val cardRect = RectF(left, top, left + cardWidth, top + cardHeight)
            canvas.drawRoundRect(cardRect, 8f, 8f, polaroidBg)

            // Inner Photo Rect (Square)
            val photoRect = RectF(
                left + photoMargin,
                top + photoMargin,
                left + cardWidth - photoMargin,
                top + cardHeight - photoBottomMargin
            )
            drawBitmapFitCenter(canvas, item.bitmap, photoRect, cardPaint)

            // Caption text
            val textY = top + cardHeight - (photoBottomMargin * 0.45f)
            canvas.drawText("Person #${item.id}", centerX, textY, polaroidText)
            canvas.drawText("${item.appearances} appearances", centerX, textY + 24f, polaroidSub)

            canvas.restore()
        }

        return output
    }

    // -------------------------------------------------------------------------
    // STYLE 3: STORY POSTER (9:16 vertical)
    // -------------------------------------------------------------------------
    private fun renderStoryPoster(items: List<CollageItem>): Bitmap {
        val canvasWidth = 1080
        val canvasHeight = 1920

        val output = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // Gradient poster background: Deep Indigo to Obsidian
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, canvasWidth.toFloat(), canvasHeight.toFloat(),
                Color.parseColor("#0F172A"), Color.parseColor("#020617"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, canvasWidth.toFloat(), canvasHeight.toFloat(), bgPaint)

        // Header Category Pill
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
        }
        val pillText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8")
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val pillRect = RectF(canvasWidth / 2f - 140f, 100f, canvasWidth / 2f + 140f, 150f)
        canvas.drawRoundRect(pillRect, 25f, 25f, pillPaint)
        canvas.drawText("AI VIDEO CAST", canvasWidth / 2f, 134f, pillText)

        // Main Poster Title
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 58f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Unique Faces", canvasWidth / 2f, 220f, titlePaint)

        val totalAppearances = items.sumOf { it.appearances }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 26f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("${items.size} People Identified • $totalAppearances Appearances", canvasWidth / 2f, 265f, subPaint)

        // Grid of photos
        val count = items.size
        val cols = if (count <= 4) 2 else 3
        val rows = ceil(count.toFloat() / cols).toInt()

        val padding = 50f
        val spacing = 24f
        val availableWidth = canvasWidth - (padding * 2) - (spacing * (cols - 1))
        val cellWidth = availableWidth / cols
        val cellHeight = cellWidth * 1.15f

        val gridStartY = 330f
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#334155")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val countPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8")
            textSize = 20f
            typeface = Typeface.DEFAULT
        }

        for (i in items.indices) {
            val item = items[i]
            val r = i / cols
            val c = i % cols

            val left = padding + c * (cellWidth + spacing)
            val top = gridStartY + r * (cellHeight + spacing)
            val right = left + cellWidth
            val bottom = top + cellHeight
            val rect = RectF(left, top, right, bottom)

            canvas.save()
            val clipPath = Path().apply { addRoundRect(rect, 28f, 28f, Path.Direction.CW) }
            canvas.clipPath(clipPath)

            // Draw Face
            drawBitmapFitCenter(canvas, item.bitmap, rect, cardPaint)

            // Bottom gradient
            val gradPaint = Paint().apply {
                shader = LinearGradient(
                    0f, bottom - 75f, 0f, bottom,
                    Color.TRANSPARENT, Color.parseColor("#E6020617"),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(left, bottom - 75f, right, bottom, gradPaint)

            // Text
            canvas.drawText("Person #${item.id}", left + 16f, bottom - 34f, namePaint)
            canvas.drawText("${item.appearances} scenes", left + 16f, bottom - 12f, countPaint)

            canvas.restore()
            canvas.drawRoundRect(rect, 28f, 28f, borderPaint)
        }

        // Footer Branding
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 20f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Generated with Unique Person Collage", canvasWidth / 2f, canvasHeight - 60f, footerPaint)

        return output
    }

    // -------------------------------------------------------------------------
    // Helper: Scales and crops a bitmap to fill a destination rectangle
    // -------------------------------------------------------------------------
    private fun drawBitmapFitCenter(
        canvas: Canvas,
        src: Bitmap,
        dst: RectF,
        paint: Paint
    ) {
        val srcW = src.width.toFloat()
        val srcH = src.height.toFloat()
        val dstW = dst.width()
        val dstH = dst.height()

        val scale = max(dstW / srcW, dstH / srcH)
        val scaledW = srcW * scale
        val scaledH = srcH * scale

        val dx = dst.left + (dstW - scaledW) / 2f
        val dy = dst.top + (dstH - scaledH) / 2f

        val matrix = Matrix().apply {
            postScale(scale, scale)
            postTranslate(dx, dy)
        }
        canvas.drawBitmap(src, matrix, paint)
    }

    private fun createPlaceholderBitmap(msg: String): Bitmap {
        val bmp = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.DKGRAY)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 32f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(msg, 400f, 300f, paint)
        return bmp
    }
}
