package com.example.criminalintent.utils

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.os.Build
import android.util.Log
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.roundToInt

fun getScaledBitmap(pathName: String, activity: Activity): Bitmap {
    val outSize = Point()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val display = activity.display
        display?.getRealSize(outSize)
    } else {
        @Suppress("DEPRECATION")
        activity.windowManager.defaultDisplay.getSize(outSize)
    }
    return getScaledBitmap(pathName, outSize.x, outSize.y)
}

fun getScaledBitmap(pathName: String, destWidth: Int, destHeight: Int): Bitmap {
    var options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeFile(pathName, options)

    val srcWidth = options.outWidth.toFloat()
    val srcHeight = options.outHeight.toFloat()

    var inSampleSize = 1
    Log.d("getScaledBitmap", "DST W:$destWidth, H:$destWidth")
    Log.d("getScaledBitmap", "SRC W:$srcWidth, H:$srcHeight")
    if (srcWidth > destWidth || srcHeight > destHeight) {
        val widthScale = srcWidth / destWidth
        val heightScale = srcHeight / destHeight
        val sampleScale = if (widthScale > heightScale) widthScale else heightScale
        inSampleSize = sampleScale.roundToInt()
    }
    options = BitmapFactory.Options().apply {
        inJustDecodeBounds = false
        this.inSampleSize = inSampleSize
    }

    return BitmapFactory.decodeFile(pathName, options)
}

//fun getCompressedImage(pathName: String, quality: Int) {
//    val bitmap = BitmapFactory.decodeFile(pathName)
//    val byteArrayOutputStream = ByteArrayOutputStream()
//    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
//    val inputStream: InputStream = ByteArrayInputStream(byteArrayOutputStream.toByteArray())
//    val compressedImage = InputStreamB()
//}