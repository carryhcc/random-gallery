package com.example.randomgallery.android

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.radzivon.bartoshyk.avif.coder.HeifCoder
import com.radzivon.bartoshyk.avif.coder.PreferredColorConfig
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer

@RunWith(AndroidJUnit4::class)
class HeicDecodeProbeTest {

    @Test
    fun probeDecode() {
        val dir = File("/data/local/tmp/heic")
        val files = dir.listFiles { f -> f.extension in listOf("heic", "heif", "avif") }
            ?.sortedBy { it.name }
            ?: emptyArray()
        Log.i(TAG, "found ${files.size} files in $dir")

        files.forEach { f ->
            val bytes = f.readBytes()
            var sys: String? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                sys = runCatching {
                    val bmp = ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(bytes)))
                    bmp.width.toString() + "x" + bmp.height
                }.fold(
                    onSuccess = { "OK($it)" },
                    onFailure = { "FAIL(${it.javaClass.simpleName}: ${it.message})" }
                )
            }
            var lib = runCatching {
                val coder = HeifCoder()
                val bmp = coder.decode(bytes, PreferredColorConfig.RGBA_8888)
                bmp.width.toString() + "x" + bmp.height
            }.fold(
                onSuccess = { "OK($it)" },
                onFailure = { "FAIL(${it.javaClass.simpleName}: ${it.message})" }
            )
            Log.i(TAG, "RESULT ${f.name} size=${bytes.size} system=$sys libheif=$lib")
        }
        Log.i(TAG, "probe done")
    }

    private companion object {
        const val TAG = "HEIC_PROBE"
    }
}
