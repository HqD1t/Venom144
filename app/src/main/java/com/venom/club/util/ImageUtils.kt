package com.venom.club.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

object ImageUtils {

    /**
     * Готовит аватарку: поворот по EXIF, центр-кроп в квадрат, сжатие до 512px JPEG.
     * Возвращает Uri готового файла в кэше (или исходный, если обработать не удалось).
     */
    fun prepareAvatar(context: Context, source: Uri): Uri = try {
        val resolver = context.contentResolver

        // Сначала узнаём размеры без загрузки в память
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(source)!!.use { BitmapFactory.decodeStream(it, null, bounds) }

        // Грубое уменьшение при декодировании (для фото с камеры 4000px+)
        val opts = BitmapFactory.Options().apply {
            inSampleSize = maxOf(1, min(bounds.outWidth, bounds.outHeight) / 1024)
        }
        var bmp = resolver.openInputStream(source)!!.use {
            BitmapFactory.decodeStream(it, null, opts)
        } ?: return source

        // Поворот по EXIF (фото с камеры часто "лежат на боку")
        val rotation = resolver.openInputStream(source)?.use { stream ->
            when (ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
        if (rotation != 0f) {
            val m = Matrix().apply { postRotate(rotation) }
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
        }

        // Центр-кроп в квадрат + масштаб до 512
        val side = min(bmp.width, bmp.height)
        var square = Bitmap.createBitmap(bmp, (bmp.width - side) / 2, (bmp.height - side) / 2, side, side)
        if (side > 512) square = Bitmap.createScaledBitmap(square, 512, 512, true)

        val out = File(context.cacheDir, "avatar_${System.currentTimeMillis()}.jpg")
        FileOutputStream(out).use { square.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        Uri.fromFile(out)
    } catch (_: Exception) {
        source
    }
}
