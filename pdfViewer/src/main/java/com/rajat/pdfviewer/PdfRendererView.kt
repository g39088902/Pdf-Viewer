package com.rajat.pdfviewer

import android.content.Context
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import kotlinx.android.synthetic.main.pdf_rendererview.view.*
import java.io.File
import java.lang.Integer.min

/**
 * Created by Rajat on 11,July,2020
 */

class PdfRendererView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var pdfRendererCore: PdfRendererCore
    private lateinit var pdfViewAdapter: PdfViewAdapter
    private var quality = PdfQuality.NORMAL
    private var runnable = Runnable {}
    private var pdfRendererCoreInitialised = false

    var statusListener: StatusCallBack? = null
    val totalPageCount: Int
        get() = pdfRendererCore.getPageCount()

    interface StatusCallBack {
        fun onDownloadStart() {}
        fun onDownloadProgress(progress: Int, downloadedBytes: Long, totalBytes: Long?) {}
        fun onDownloadSuccess() {}
        fun onError(error: Throwable) {}
        fun onPageChanged(currentPage: Int, totalPage: Int) {}
    }

    fun initWithUrl(
        url: String,
        pdfQuality: PdfQuality = this.quality
    ) {
        PdfDownloader(url, object : PdfDownloader.StatusListener {
            override fun getContext(): Context = context
            override fun onDownloadStart() {
                statusListener?.onDownloadStart()
            }

            override fun onDownloadProgress(currentBytes: Long, totalBytes: Long) {
                var progress = (currentBytes.toFloat() / totalBytes.toFloat() * 100F).toInt()
                progress = min(100,progress)
                statusListener?.onDownloadProgress(progress, currentBytes, totalBytes)
            }

            override fun onDownloadSuccess(absolutePath: String) {
                initWithPath(absolutePath, pdfQuality)
                statusListener?.onDownloadSuccess()
            }

            override fun onError(error: Throwable) {
                statusListener?.onError(error)
            }
        })
    }

    fun initWithPath(path: String, pdfQuality: PdfQuality = this.quality) {
        init(File(path), pdfQuality)
    }

    fun initWithFile(file: File, pdfQuality: PdfQuality = this.quality) {
        init(file, pdfQuality)
    }

    private fun init(file: File, pdfQuality: PdfQuality) {
        pdfRendererCore = PdfRendererCore(context, file, pdfQuality)
        pdfRendererCoreInitialised = true
        pdfViewAdapter = PdfViewAdapter(pdfRendererCore)
        val v = LayoutInflater.from(context).inflate(R.layout.pdf_rendererview, this, false)
        addView(v)
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.apply {
            adapter = pdfViewAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            itemAnimator = DefaultItemAnimator()
            addOnScrollListener(scrollListener)
        }

    }

    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            (recyclerView.layoutManager as LinearLayoutManager).run {
                var foundPosition = findLastVisibleItemPosition() + findFirstVisibleItemPosition()
                foundPosition /=2

                pageNo.run {
                    if (foundPosition != NO_POSITION) text = "${(foundPosition + 1)} of $totalPageCount"
                    pageNo.visibility = View.VISIBLE
                }

                if (foundPosition != NO_POSITION) {
                    statusListener?.onPageChanged(foundPosition, totalPageCount)
                    return@run
                }
                foundPosition = findFirstVisibleItemPosition()
                if (foundPosition != NO_POSITION) {
                    statusListener?.onPageChanged(foundPosition, totalPageCount)
                    return@run
                }
            }
        }

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            if (newState == RecyclerView.SCROLL_STATE_IDLE) pageNo.postDelayed(runnable, 3000)
            else pageNo.removeCallbacks(runnable)
        }

    }

    init {
        getAttrs(attrs, defStyleAttr)
    }

    private fun getAttrs(attrs: AttributeSet?, defStyle: Int) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.PdfRendererView, defStyle, 0)
        setTypeArray(typedArray)
    }

    private fun setTypeArray(typedArray: TypedArray) {
        typedArray.recycle()
    }

    fun closePdfRender() {
        if (pdfRendererCoreInitialised) pdfRendererCore.closePdfRender()
    }

}