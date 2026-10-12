package com.makemission.makeflow.core.update

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.makemission.makeflow.BuildConfig
import com.makemission.makeflow.MainActivity
import com.makemission.makeflow.R
import com.makemission.makeflow.core.logging.AppLogger
import com.makemission.makeflow.core.notifications.NotificationChannels
import com.makemission.makeflow.domain.AppReleaseVersion
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

private const val GITHUB_LATEST_RELEASE_URL =
    "https://api.github.com/repos/makemissionllc/MakeFlow/releases/latest"
private const val UPDATE_CHECK_STARTUP_WORK = "github_app_update_startup"
private const val UPDATE_CHECK_PERIODIC_WORK = "github_app_update_periodic"
private const val UPDATE_CHECK_MANUAL_WORK = "github_app_update_manual"
private const val INPUT_NOTIFY_EXISTING = "notify_existing_download"
private const val APP_UPDATE_NOTIFICATION_ID = 20_261_012
private const val MAX_APK_BYTES = 250L * 1024L * 1024L

object GithubAppUpdateScheduler {
    fun schedule(context: Context) {
        if (!isCommunityInstall(context)) return
        val workManager = WorkManager.getInstance(context.applicationContext)
        val connectedNetwork = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val startupRequest = OneTimeWorkRequestBuilder<GithubAppUpdateWorker>()
            .setConstraints(connectedNetwork)
            .build()
        workManager.enqueueUniqueWork(
            UPDATE_CHECK_STARTUP_WORK,
            ExistingWorkPolicy.KEEP,
            startupRequest
        )

        val periodicRequest = PeriodicWorkRequestBuilder<GithubAppUpdateWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(1, TimeUnit.DAYS)
            .setConstraints(connectedNetwork)
            .build()
        workManager.enqueueUniquePeriodicWork(
            UPDATE_CHECK_PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )
    }

    fun checkNow(context: Context) {
        if (!isCommunityInstall(context)) return
        val request = OneTimeWorkRequestBuilder<GithubAppUpdateWorker>()
            .setInputData(workDataOf(INPUT_NOTIFY_EXISTING to true))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            UPDATE_CHECK_MANUAL_WORK,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    fun isCommunityInstall(context: Context): Boolean =
        context.packageName == "com.makemission.makeflow.community" && !BuildConfig.DEBUG
}

class GithubAppUpdateWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        if (!GithubAppUpdateScheduler.isCommunityInstall(applicationContext)) return Result.success()
        return try {
            val release = fetchNewerRelease() ?: return Result.success()
            val updateDirectory = File(applicationContext.filesDir, "updates")
            if (!updateDirectory.exists() && !updateDirectory.mkdirs()) {
                throw IOException("Could not create the update download directory")
            }
            val apkFile = File(updateDirectory, "makeflow-update.apk")
            if (isValidUpdate(apkFile, release.version)) {
                if (inputData.getBoolean(INPUT_NOTIFY_EXISTING, false)) {
                    notifyUpdateAvailable(applicationContext, release.version, apkFile.name)
                }
                return Result.success()
            }

            val temporaryFile = File(updateDirectory, "makeflow-update.tmp.apk")
            downloadAsset(release, temporaryFile)
            if (!isValidUpdate(temporaryFile, release.version)) {
                temporaryFile.delete()
                AppLogger.warn(
                    "GithubAppUpdate",
                    "Skipped a release APK that does not match this package, version, and signing certificate"
                )
                return Result.success()
            }
            if (apkFile.exists() && !apkFile.delete()) {
                temporaryFile.delete()
                throw IOException("Could not replace the previous update download")
            }
            if (!temporaryFile.renameTo(apkFile)) {
                temporaryFile.delete()
                throw IOException("Could not finalize the update download")
            }
            notifyUpdateAvailable(applicationContext, release.version, apkFile.name)
            Result.success()
        } catch (error: Exception) {
            AppLogger.error("GithubAppUpdate", "Could not check or download the Community update", error)
            Result.retry()
        }
    }

    private fun fetchNewerRelease(): GithubRelease? {
        val connection = URL(GITHUB_LATEST_RELEASE_URL).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("X-GitHub-Api-Version", "2026-03-10")
            connection.setRequestProperty("User-Agent", "Mindful-Wallet-Android")
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                if (connection.responseCode == HttpURLConnection.HTTP_NOT_FOUND) return null
                throw IOException("GitHub Releases returned HTTP ${connection.responseCode}")
            }
            val release = org.json.JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            if (release.optBoolean("draft") || release.optBoolean("prerelease")) return null
            val tag = release.optString("tag_name").takeIf(String::isNotBlank) ?: return null
            if (!AppReleaseVersion.isNewer(tag, BuildConfig.VERSION_NAME)) return null
            val version = Regex("^v?(\\d+\\.\\d+\\.\\d+(?:[-+][0-9A-Za-z.-]+)?)$")
                .matchEntire(tag.trim())?.groupValues?.getOrNull(1)
                ?: return null
            val assets = release.optJSONArray("assets") ?: return null
            val candidates = (0 until assets.length()).mapNotNull { index ->
                val asset = assets.optJSONObject(index) ?: return@mapNotNull null
                val name = asset.optString("name")
                val size = asset.optLong("size", -1L)
                val url = asset.optString("browser_download_url")
                if (!name.endsWith(".apk", ignoreCase = true) ||
                    name.contains("debug", ignoreCase = true) ||
                    name.contains("play", ignoreCase = true) ||
                    size !in 1..MAX_APK_BYTES ||
                    !url.startsWith(
                        "https://github.com/makemissionllc/MakeFlow/releases/download/",
                        ignoreCase = true
                    )
                ) return@mapNotNull null
                GithubReleaseAsset(
                    name = name,
                    size = size,
                    url = url,
                    digest = asset.optString("digest").takeUnless { it.isBlank() || it == "null" }
                )
            }
            val communityAssets = candidates.filter { it.name.contains("community", ignoreCase = true) }
            val asset = when {
                communityAssets.size == 1 -> communityAssets.single()
                communityAssets.size > 1 -> communityAssets.firstOrNull { it.name.contains("universal", ignoreCase = true) }
                candidates.size == 1 -> candidates.single()
                else -> candidates.firstOrNull { it.name.contains("universal", ignoreCase = true) }
            } ?: return null
            return GithubRelease(version = version, asset = asset)
        } finally {
            connection.disconnect()
        }
    }

    private fun downloadAsset(release: GithubRelease, destination: File) {
        val connection = URL(release.asset.url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 60_000
            connection.setRequestProperty("User-Agent", "Mindful-Wallet-Android")
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("GitHub APK download returned HTTP ${connection.responseCode}")
            }
            val contentLength = connection.contentLengthLong
            if ((contentLength > MAX_APK_BYTES) ||
                (contentLength > 0 && contentLength != release.asset.size)
            ) throw IOException("GitHub APK download size did not match the release metadata")

            val digest = MessageDigest.getInstance("SHA-256")
            var totalBytes = 0L
            connection.inputStream.use { input ->
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        totalBytes += count
                        if (totalBytes > MAX_APK_BYTES || totalBytes > release.asset.size) {
                            throw IOException("GitHub APK download exceeded the published asset size")
                        }
                        digest.update(buffer, 0, count)
                        output.write(buffer, 0, count)
                    }
                }
            }
            if (totalBytes != release.asset.size) throw IOException("GitHub APK download was incomplete")
            release.asset.digest?.let { expected ->
                val expectedHex = expected.removePrefix("sha256:")
                val actualHex = digest.digest().joinToString("") { byte -> "%02x".format(byte) }
                if (expectedHex.length != 64 || !actualHex.equals(expectedHex, ignoreCase = true)) {
                    throw SecurityException("GitHub APK SHA-256 digest did not match the release metadata")
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    @Suppress("DEPRECATION")
    private fun isValidUpdate(file: File, expectedVersionName: String): Boolean {
        if (!file.isFile || file.length() !in 1..MAX_APK_BYTES) return false
        val packageManager = applicationContext.packageManager
        val packageInfo = packageManager.getPackageArchiveInfo(
            file.absolutePath,
            PackageManager.GET_SIGNING_CERTIFICATES
        )
            ?: return false
        if (packageInfo.packageName != applicationContext.packageName ||
            packageInfo.longVersionCode <= BuildConfig.VERSION_CODE ||
            packageInfo.versionName != expectedVersionName
        ) return false

        val installedPackage = packageManager.getPackageInfo(
            applicationContext.packageName,
            PackageManager.GET_SIGNING_CERTIFICATES
        )
        val installedSigning = installedPackage.signingInfo ?: return false
        val updateSigning = packageInfo.signingInfo ?: return false
        val installedSigners = installedSigning.apkContentsSigners.map { it.toCharsString() }.toSet()
        val updateSigners = updateSigning.apkContentsSigners.map { it.toCharsString() }.toSet()
        if (installedSigning.hasMultipleSigners() || updateSigning.hasMultipleSigners()) {
            return installedSigners == updateSigners
        }
        val installedSigner = installedSigners.singleOrNull() ?: return false
        return updateSigning.signingCertificateHistory.orEmpty()
            .any { it.toCharsString() == installedSigner }
    }
}

private data class GithubRelease(
    val version: String,
    val asset: GithubReleaseAsset
)

private data class GithubReleaseAsset(
    val name: String,
    val size: Long,
    val url: String,
    val digest: String?
)

private fun notifyUpdateAvailable(context: Context, version: String, fileName: String) {
    if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) return

    NotificationChannels.createChannels(context)
    val installIntent = Intent(context, MainActivity::class.java).apply {
        action = MainActivity.ACTION_INSTALL_APP_UPDATE
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        data = Uri.parse("makeflow://updates/$version")
        putExtra(MainActivity.EXTRA_UPDATE_FILE_NAME, fileName)
    }
    val pendingIntent = PendingIntent.getActivity(
        context,
        APP_UPDATE_NOTIFICATION_ID,
        installIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_APP_UPDATES)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(context.getString(R.string.update_ready_title))
        .setContentText(context.getString(R.string.update_ready_body, version))
        .setContentIntent(pendingIntent)
        .setCategory(NotificationCompat.CATEGORY_STATUS)
        .setAutoCancel(true)
        .build()
    NotificationManagerCompat.from(context).notify(APP_UPDATE_NOTIFICATION_ID, notification)
}

object GithubAppUpdateInstaller {
    fun hasInstallPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    fun launchInstaller(context: Context, fileName: String): Boolean {
        if (!GithubAppUpdateScheduler.isCommunityInstall(context)) return false
        if (!fileName.matches(Regex("makeflow-[A-Za-z0-9._-]+\\.apk"))) return false
        val updateDirectory = File(context.filesDir, "updates").canonicalFile
        val apk = File(updateDirectory, fileName).canonicalFile
        if (apk.parentFile != updateDirectory || !apk.isFile) return false
        val packageInfo = context.packageManager.getPackageArchiveInfo(
            apk.absolutePath,
            PackageManager.GET_SIGNING_CERTIFICATES
        ) ?: return false
        if (packageInfo.packageName != context.packageName || packageInfo.longVersionCode <= BuildConfig.VERSION_CODE) {
            return false
        }

        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apk
            )
            @Suppress("DEPRECATION")
            val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(installIntent)
            true
        } catch (error: Exception) {
            AppLogger.error("GithubAppUpdate", "Could not launch the Android package installer", error)
            false
        }
    }
}
