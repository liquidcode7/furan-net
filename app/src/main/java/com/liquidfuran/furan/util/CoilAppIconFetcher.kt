package com.liquidfuran.furan.util

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import javax.inject.Inject

/** Wraps a package name so Coil routes it to [AppIconFetcher] instead of treating it as a URL. */
data class AppIconModel(val packageName: String)

class AppIconFetcher(
    private val model: AppIconModel,
    private val packageManager: PackageManager
) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val drawable: Drawable = try {
            packageManager.getApplicationIcon(model.packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            packageManager.defaultActivityIcon
        }
        return DrawableResult(
            drawable = drawable,
            isSampled = false,
            dataSource = DataSource.MEMORY
        )
    }

    class Factory @Inject constructor(
        private val packageManager: PackageManager
    ) : Fetcher.Factory<AppIconModel> {
        override fun create(
            data: AppIconModel,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher = AppIconFetcher(data, packageManager)
    }
}
