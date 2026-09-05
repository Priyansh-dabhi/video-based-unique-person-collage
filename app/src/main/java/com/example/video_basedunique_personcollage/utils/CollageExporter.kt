package com.example.video_basedunique_personcollage.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object CollageExporter {

    /**
     * Saves the collage bitmap into the device's public photo gallery using MediaStore.
     * Complies with Scoped Storage (no permissions required on modern Android).
     */
    fun saveToGallery(context: Context, bitmap: Bitmap): Result<Uri> {
        return runCatching {
            val filename = "collage_${System.currentTimeMillis()}.jpg"

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/UniquePersonCollage")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(collection, contentValues)
                ?: throw IllegalStateException("Could not create MediaStore entry")

            resolver.openOutputStream(uri).use { outputStream ->
                if (outputStream == null) throw IllegalStateException("Could not open output stream")
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }

            uri
        }
    }

    /**
     * Shares the collage image using Android's native share sheet.
     */
    fun shareCollage(context: Context, bitmap: Bitmap) {
        try {
            val cacheFolder = File(context.cacheDir, "shared").apply { mkdirs() }
            val file = File(cacheFolder, "collage_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Unique Person Collage").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
