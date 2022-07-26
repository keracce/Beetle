package com.karacca.beetle.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.view.View
import androidx.annotation.WorkerThread
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

/**
 * @author karacca
 * @date 17.07.2022
 */

@Suppress("PrivatePropertyName", "SpellCheckingInspection")
internal object BitmapUtils {

    private val TAG = BitmapUtils::class.java.simpleName
    private const val FILE_NAME_TEMPLATE = "%s_%s.jpg"
    private const val BITMAP_PREFIX = "bitmap"
    private const val FILE_PROVIDER_SUFFIX = ".fileprovider"

    @Suppress("SameParameterValue")
    private fun createUniqueFilename(prefix: String?): String {
        val randomId = System.currentTimeMillis().toString()
        return String.format(Locale.US, FILE_NAME_TEMPLATE, prefix, randomId)
    }

    @WorkerThread
    private fun writeBitmapToDirectory(bitmap: Bitmap, directory: File): File? {
        if (!directory.mkdirs() && !directory.exists()) {
            Log.e(TAG, "Failed to create directory for bitmap.")
            return null
        }

        return writeBitmapToFile(bitmap, File(directory, createUniqueFilename(BITMAP_PREFIX)))
    }

    @WorkerThread
    @SuppressLint("WrongThread")
    fun writeBitmapToFile(bitmap: Bitmap, file: File): File? {
        var fileStream: FileOutputStream? = null

        try {
            val byteStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, byteStream)
            fileStream = FileOutputStream(file)
            fileStream.write(byteStream.toByteArray())
            return file
        } catch (e: IOException) {
            Log.e(TAG, e.message, e)
        } finally {
            if (fileStream != null) {
                try {
                    fileStream.close()
                } catch (e: IOException) {
                    Log.e(TAG, e.message, e)
                }
            }
        }
        return null
    }

    fun capture(view: View): Bitmap? {
        if (view.width == 0 || view.height == 0) {
            return null
        }
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.RGB_565)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    private fun getProviderUri(context: Context, file: File): Uri {
        val authority: String = context.packageName.toString() + FILE_PROVIDER_SUFFIX
        return FileProvider.getUriForFile(context, authority, file)
    }

    private fun getProviderUri(context: Context, uri: Uri): Uri {
        val file = File(uri.path!!)
        return getProviderUri(context, file)
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap? {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }

        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
