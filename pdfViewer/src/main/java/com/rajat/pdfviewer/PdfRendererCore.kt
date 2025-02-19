package com.rajat.pdfviewer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.Bitmap.createBitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Created by Rajat on 11,July,2020
 */

internal class PdfRendererCore(
    private val context: Context,
    pdfFile: File,
    private val pdfQuality: PdfQuality
) {

    private val cachePath = "___pdf___cache___"
    private var pdfRenderer: PdfRenderer? = null

    init {
        initCache()
        openPdfFile(pdfFile)
    }

    private fun initCache() {
        val cache = File(context.cacheDir, cachePath)
        if (cache.exists()) cache.deleteRecursively()
        cache.mkdirs()
    }

    private fun getBitmapFromCache(pageNo: Int): Bitmap? {
        val loadPath = File(File(context.cacheDir, cachePath), pageNo.toString())
        if (!loadPath.exists()) return null

        return try {
            BitmapFactory.decodeFile(loadPath.absolutePath)
        } catch (e: Exception) {
            null
        }
    }

    @Throws(IOException::class)
    private fun writeBitmapToCache(pageNo: Int, bitmap: Bitmap) {
        val savePath = File(File(context.cacheDir, cachePath), pageNo.toString())
        savePath.createNewFile()
        val fos = FileOutputStream(savePath)
        bitmap.compress(CompressFormat.PNG, 100, fos)
        fos.flush()
        fos.close()
    }

    private fun openPdfFile(pdfFile: File) {
        try {
            val fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(fileDescriptor)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getPageCount(): Int = pdfRenderer?.pageCount ?: 0

    fun renderPage(pageNo: Int, onBitmapReady: ((bitmap: Bitmap) -> Unit)) {
        if (pageNo >= getPageCount()) return

        GlobalScope.async {
            synchronized(this@PdfRendererCore) {
                buildBitmap(pageNo) { bitmap ->
                    GlobalScope.launch(Dispatchers.Main) { onBitmapReady(bitmap) }
                }
                onBitmapReady?.let {
                    // prefetchNext(pageNo + 1)
                }
            }
        }
    }

    private fun buildBitmap(pageNo: Int, onBitmap: (Bitmap) -> Unit) {
        var bitmap = getBitmapFromCache(pageNo)
        bitmap?.let {
            onBitmap(it)
            return@buildBitmap
        }

        try {
            val pdfPage = pdfRenderer!!.openPage(pageNo)
            bitmap = createBitmap(
                pdfPage.width * pdfQuality.ratio,
                pdfPage.height * pdfQuality.ratio,
                Bitmap.Config.ARGB_8888
            )
            Canvas(bitmap).drawColor(Color.WHITE)
            pdfPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            pdfPage.close()
            writeBitmapToCache(pageNo, bitmap)
            onBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun closePdfRender() {
        try {
            pdfRenderer?.close()
        } catch (e: Exception) {
            Log.e("PdfRendererCore", e.toString())
        }
    }
}
