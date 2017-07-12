package e.chakritrakhuang.kotlinmusicbobber

import android.content.Context
import android.support.v4.content.AsyncTaskLoader

internal abstract class BaseAsyncTaskLoader<T>(context : Context) : AsyncTaskLoader<T>(context) {
    protected var mData : T? = null

    override fun deliverResult(data : T?) {
        if (isReset) {

            if (data != null) {
                onReleaseResources()
            }
        }
        val oldData = mData
        mData = data

        if (isStarted) {

            super.deliverResult(data)
        }

        if (oldData != null) {
            onReleaseResources()
        }
    }

    override fun onStartLoading() {
        if (mData != null) {

            deliverResult(mData)
        }

        if (takeContentChanged() || mData == null) {

            forceLoad()
        }
    }

    override fun onStopLoading() {

        cancelLoad()
    }

    override fun onCanceled(data : T?) {
        super.onCanceled(data)

        onReleaseResources()
    }

    override fun onReset() {
        super.onReset()

        onStopLoading()

        if (mData != null) {
            onReleaseResources()
            mData = null
        }
    }

    protected fun onReleaseResources() {

    }
}