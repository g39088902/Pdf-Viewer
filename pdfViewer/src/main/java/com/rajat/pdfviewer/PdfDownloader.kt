package com.rajat.pdfviewer

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.File
import java.net.URL

internal class PdfDownloader(url: String, private val listener: StatusListener,id:String) {
    interface StatusListener {
        fun getContext(): Context
        fun onDownloadStart() {}
        fun onDownloadProgress(currentBytes: Long, totalBytes: Long) {}
        fun onDownloadSuccess(absolutePath: String) {}
        fun onError(error: Throwable) {}
    }

    init {
        GlobalScope.async { download(url,id) }
    }

    private fun download(downloadUrl: String,id:String) {
        GlobalScope.launch(Dispatchers.Main) { listener.onDownloadStart() }
        val outputFile = File(listener.getContext().cacheDir, "${id}.pdf")
        if (outputFile.exists())
            outputFile.delete()
        try {
            val bufferSize = 8192
            val url = URL(downloadUrl)
            val connection = url.openConnection()
            connection.connect()

            val totalLength = connection.contentLength
            val inputStream = BufferedInputStream(url.openStream(), bufferSize)
            val outputStream = outputFile.outputStream()
            var downloaded = 0

            do {
                val data = ByteArray(bufferSize)
                val count = inputStream.read(data)
                if (count == -1) break
                if (totalLength > 0) {
                    downloaded += bufferSize
                    GlobalScope.launch(Dispatchers.Main) {
                        listener.onDownloadProgress(
                            downloaded.toLong(),
                            totalLength.toLong()
                        )
                    }
                }
                outputStream.write(data, 0, count)
            } while (true)
        } catch (e: Exception) {
            GlobalScope.launch(Dispatchers.Main) { listener.onError(e) }
            return
        }
        GlobalScope.launch(Dispatchers.Main) { listener.onDownloadSuccess(outputFile.absolutePath) }
    }
}