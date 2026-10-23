package com.example.openthermalprint.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
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
    // Standard-Repository auf GitHub
    const val GITHUB_REPO = "Daddelgreis74/PocketPrint"
    const val CURRENT_VERSION = "1.0.0"

    /**
     * Prüft die GitHub Releases API nach der neuesten veröffentlichten Version.
     */
    suspend fun checkForUpdate(currentVer: String = CURRENT_VERSION): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        try {
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

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val jsonString = reader.readText()
            reader.close()

            val json = JSONObject(jsonString)
            val tagName = json.optString("tag_name", "").trim().removePrefix("v").removePrefix("V")
            val body = json.optString("body", "Neue Funktionen und Verbesserungen.")
            
            // Finde das erste APK-Asset
            var apkUrl: String? = null
            val assets = json.optJSONArray("assets")
            if (assets != null) {
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
     * Lädt die APK über den Android DownloadManager herunter und stößt anschließend die Installation an.
     */
    fun downloadAndInstallApk(context: Context, downloadUrl: String, version: String, onProgress: (String) -> Unit) {
        try {
            val fileName = "PocketPrint-v$version.apk"
            val destinationFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
            if (destinationFile.exists()) {
                destinationFile.delete()
            }

            val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                setTitle("PocketPrint Update v$version")
                setDescription("Lade neueste App-Version herunter...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationUri(Uri.fromFile(destinationFile))
                setMimeType("application/vnd.android.package-archive")
            }

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = downloadManager.enqueue(request)
            onProgress("Download gestartet...")

            val receiver = object : BroadcastReceiver() {
                override fun onReceive(recvContext: Context?, intent: Intent?) {
                    val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        try {
                            context.unregisterReceiver(this)
                        } catch (_: Exception) {}
                        installApk(context, destinationFile)
                    }
                }
            }

            val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }
        } catch (e: Exception) {
            onProgress("Fehler beim Starten des Downloads: ${e.message}")
        }
    }

    /**
     * Startet den nativen Android Paket-Installer für die heruntergeladene APK via FileProvider.
     */
    fun installApk(context: Context, apkFile: File) {
        if (!apkFile.exists()) return

        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = IntentIntentFlags()
        }

        context.startActivity(installIntent)
    }

    private fun IntentIntentFlags(): Int {
        return Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
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
