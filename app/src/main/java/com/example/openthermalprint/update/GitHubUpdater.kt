package com.example.openthermalprint.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val hasUpdate: Boolean,
    val latestVersion: String,
    val currentVersion: String,
    val releaseNotes: String,
    val downloadUrl: String?
)

object GitHubUpdater {
    const val GITHUB_REPO = "Daddelgreis74/PocketPrint"

    fun getCurrentVersion(context: Context): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0)).versionName ?: "1.0.0"
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
            }
        } catch (_: Exception) {
            "1.0.0"
        }
    }

    suspend fun checkForUpdate(context: Context): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val currentVer = getCurrentVersion(context)
            val apiUrl = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"
            val url = URL(apiUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "PocketPrint-Android")
                connectTimeout = 8000
                readTimeout = 8000
            }

            if (connection.responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                return@withContext Result.success(
                    UpdateInfo(
                        hasUpdate = false,
                        latestVersion = currentVer,
                        currentVersion = currentVer,
                        releaseNotes = "Noch keine Releases auf GitHub vorhanden.",
                        downloadUrl = null
                    )
                )
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(Exception("GitHub API HTTP ${connection.responseCode}"))
            }

            val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(jsonString)
            val tagName = json.optString("tag_name", "").trim().removePrefix("v").removePrefix("V")
            val body = json.optString("body", "Neue Funktionen und Verbesserungen.")

            var apkUrl: String? = null
            if (json.has("assets")) {
                val assets = json.getJSONArray("assets")
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url", null)
                        break
                    }
                }
            }

            val hasUpdate = isNewerVersion(currentVer, tagName) && apkUrl != null

            Result.success(
                UpdateInfo(
                    hasUpdate = hasUpdate,
                    latestVersion = tagName.ifBlank { currentVer },
                    currentVersion = currentVer,
                    releaseNotes = body,
                    downloadUrl = apkUrl
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Lädt die APK direkt im Hintergrund in den Cache herunter (wie bei SmartHome Kiosk)
     * und liefert die fertige Datei zurück.
     */
    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        onProgress: (Float) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            val url = URL(downloadUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 20000
                setRequestProperty("User-Agent", "PocketPrint-Android")
                connect()
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val fileLength = connection.contentLength
                val cacheFile = File(context.cacheDir, "pocketprint-update.apk")
                if (cacheFile.exists()) cacheFile.delete()

                connection.inputStream.use { input ->
                    FileOutputStream(cacheFile).use { output ->
                        val data = ByteArray(4096)
                        var total: Long = 0
                        var count: Int
                        while (input.read(data).also { count = it } != -1) {
                            total += count
                            if (fileLength > 0) {
                                onProgress(total.toFloat() / fileLength.toFloat())
                            }
                            output.write(data, 0, count)
                        }
                    }
                }
                return@withContext cacheFile
            }
        } catch (e: Exception) {
            Log.e("GitHubUpdater", "Fehler beim Herunterladen der APK", e)
        }
        return@withContext null
    }

    /**
     * Startet den nativen Android Paket-Installer für die heruntergeladene Datei (SmartHome Kiosk Methode).
     */
    fun startInstallation(context: Context, apkFile: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, apkFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("GitHubUpdater", "Fehler beim Starten der Installation", e)
        }
    }

    private fun isNewerVersion(current: String, candidate: String): Boolean {
        if (candidate.isBlank()) return false
        val currParts = current.split(".").mapNotNull { it.toIntOrNull() }
        val candParts = candidate.split(".").mapNotNull { it.toIntOrNull() }

        val length = maxOf(currParts.size, candParts.size)
        for (i in 0 until length) {
            val c = currParts.getOrElse(i) { 0 }
            val n = candParts.getOrElse(i) { 0 }
            if (n > c) return true
            if (n < c) return false
        }
        return false
    }
}
