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
        canvas.drawText("$count Unique People", padding, 115f, subPaint)

        // Draw each face card (clean, edge-to-edge photo without text overlays)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2A3342")
            style = Paint.Style.STROKE
            strokeWidth = 3f
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

            // Draw cropped photo centered - completely clean
            drawBitmapFitCenter(canvas, item.bitmap, rect, cardPaint)

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
        val photoBottomMargin = cardHeight * 0.18f
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
            canvas.drawRoundRect(cardRect, 10f, 10f, polaroidBg)

            // Inner Photo Rect (clean white polaroid border, no text)
            val photoRect = RectF(
                left + photoMargin,
                top + photoMargin,
                left + cardWidth - photoMargin,
                top + cardHeight - photoBottomMargin
            )
            drawBitmapFitCenter(canvas, item.bitmap, photoRect, cardPaint)

            canvas.restore()
        }

        return output
    }

    // -------------------------------------------------------------------------
    // STYLE 3: STORY POSTER (9:16 vertical - Cinematic Ensemble Edition)
    // -------------------------------------------------------------------------
    private fun renderStoryPoster(items: List<CollageItem>): Bitmap {
        val canvasWidth = 1080
        val canvasHeight = 1920

        val output = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // Gradient poster background: Deep Obsidian to Midnight Navy
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, canvasHeight.toFloat(),
                intArrayOf(
                    Color.parseColor("#040812"),
                    Color.parseColor("#091326"),
                    Color.parseColor("#030710")
                ),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, canvasWidth.toFloat(), canvasHeight.toFloat(), bgPaint)

        // Outer Double Editorial Framing Border
        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#253549")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRect(36f, 36f, canvasWidth - 36f, canvasHeight - 36f, framePaint)

        val innerGoldFrame = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4DFFB95F")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRect(44f, 44f, canvasWidth - 44f, canvasHeight - 44f, innerGoldFrame)

        // Header Kicker: Spaced gold editorial tag
        val kickerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFB95F")
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.25f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("✦  A  C I N E M A T I C  A I  S T U D Y  ✦", canvasWidth / 2f, 120f, kickerPaint)

        // Main Poster Title: "THE ENSEMBLE"
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F8FAFC")
            textSize = 68f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            letterSpacing = 0.08f
            textAlign = Paint.Align.CENTER
            setShadowLayer(20f, 0f, 6f, Color.parseColor("#66000000"))
        }
        canvas.drawText("THE ENSEMBLE", canvasWidth / 2f, 205f, titlePaint)

        // Subtitle: Cast summary
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 24f
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.04f
        }
        canvas.drawText("Featuring ${items.size} Unique Voices Captured from Motion", canvasWidth / 2f, 255f, subPaint)

        // Thin golden accent divider under title
        val goldLine = Paint().apply {
            shader = LinearGradient(
                canvasWidth / 2f - 200f, 0f, canvasWidth / 2f + 200f, 0f,
                Color.TRANSPARENT, Color.parseColor("#B3FFB95F"),
                Shader.TileMode.MIRROR
            )
            strokeWidth = 2f
        }
        canvas.drawLine(canvasWidth / 2f - 180f, 290f, canvasWidth / 2f + 180f, 290f, goldLine)

        // ── Editorial Photo Gallery ───────────────────────────────────────
        val count = items.size
        val paddingX = 64f
        val gridStartY = 330f
        val availableWidth = canvasWidth - (paddingX * 2)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
        val photoBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38485E")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val photoGoldAccent = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#99FFB95F")
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }

        if (count == 5) {
            // 2 on top (prominent), 3 on bottom (symmetrical)
            val spacing = 24f
            val topRowHeight = 520f
            val topCardWidth = (availableWidth - spacing) / 2f

            // Top 2 cards
            for (i in 0 until 2) {
                val left = paddingX + i * (topCardWidth + spacing)
                val top = gridStartY
                val right = left + topCardWidth
                val bottom = top + topRowHeight
                val rect = RectF(left, top, right, bottom)

                canvas.save()
                val clipPath = Path().apply { addRoundRect(rect, 20f, 20f, Path.Direction.CW) }
                canvas.clipPath(clipPath)
                drawBitmapFitCenter(canvas, items[i].bitmap, rect, cardPaint)
                canvas.restore()

                canvas.drawRoundRect(rect, 20f, 20f, photoBorder)
                canvas.drawRoundRect(RectF(left + 3f, top + 3f, right - 3f, bottom - 3f), 17f, 17f, photoGoldAccent)
            }

            // Bottom 3 cards
            val bottomStartY = gridStartY + topRowHeight + spacing
            val bottomRowHeight = 440f
            val bottomCardWidth = (availableWidth - (spacing * 2)) / 3f

            for (i in 2 until 5) {
                val col = i - 2
                val left = paddingX + col * (bottomCardWidth + spacing)
                val top = bottomStartY
                val right = left + bottomCardWidth
                val bottom = top + bottomRowHeight
                val rect = RectF(left, top, right, bottom)

                canvas.save()
                val clipPath = Path().apply { addRoundRect(rect, 18f, 18f, Path.Direction.CW) }
                canvas.clipPath(clipPath)
                drawBitmapFitCenter(canvas, items[i].bitmap, rect, cardPaint)
                canvas.restore()

                canvas.drawRoundRect(rect, 18f, 18f, photoBorder)
                canvas.drawRoundRect(RectF(left + 3f, top + 3f, right - 3f, bottom - 3f), 15f, 15f, photoGoldAccent)
            }
        } else {
            // General grid: 2 cols for <= 4, 3 cols for > 5
            val cols = if (count <= 4) 2 else 3
            val rows = ceil(count.toFloat() / cols).toInt()
            val spacing = 24f
            val cellWidth = (availableWidth - (spacing * (cols - 1))) / cols
            val cellHeight = min(cellWidth * 1.25f, (1100f / rows))

            for (i in items.indices) {
                val r = i / cols
                val c = i % cols

                val left = paddingX + c * (cellWidth + spacing)
                val top = gridStartY + r * (cellHeight + spacing)
                val right = left + cellWidth
                val bottom = top + cellHeight
                val rect = RectF(left, top, right, bottom)

                canvas.save()
                val clipPath = Path().apply { addRoundRect(rect, 18f, 18f, Path.Direction.CW) }
                canvas.clipPath(clipPath)
                drawBitmapFitCenter(canvas, items[i].bitmap, rect, cardPaint)
                canvas.restore()

                canvas.drawRoundRect(rect, 18f, 18f, photoBorder)
                canvas.drawRoundRect(RectF(left + 3f, top + 3f, right - 3f, bottom - 3f), 15f, 15f, photoGoldAccent)
            }
        }

        // ── Authentic Cinematic Billing Block ──────────────────────────────
        val creditsStartY = canvasHeight - 270f

        val creditLine = Paint().apply {
            color = Color.parseColor("#1E293B")
            strokeWidth = 2f
        }
        canvas.drawLine(70f, creditsStartY, canvasWidth - 70f, creditsStartY, creditLine)

        val billingHeader = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#C8C4D9")
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.12f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "FACECOLLAGE AI STUDIO PRESENTS AN ON-DEVICE PRODUCTION",
            canvasWidth / 2f,
            creditsStartY + 45f,
            billingHeader
        )

        val billingBody = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#828A99")
            textSize = 17f
            typeface = Typeface.DEFAULT
            letterSpacing = 0.08f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "NEURAL FACIAL EMBEDDINGS BY FACENET-512  •  UNSUPERVISED CLUSTERING BY RECIPROCAL KNN",
            canvasWidth / 2f,
            creditsStartY + 85f,
            billingBody
        )
        canvas.drawText(
            "BEST SHOT SELECTION ENGINE  •  ML KIT VISION DETECTOR  •  HARDWARE ACCELERATED RENDER",
            canvasWidth / 2f,
            creditsStartY + 120f,
            billingBody
        )

        val sealPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFB95F")
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.18f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "— OFFICIAL CAST COMPILATION —",
            canvasWidth / 2f,
            creditsStartY + 175f,
            sealPaint
        )

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
