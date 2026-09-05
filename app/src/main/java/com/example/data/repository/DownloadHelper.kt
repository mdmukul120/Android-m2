package com.example.data.repository

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import com.example.data.model.DownloadedMedia
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DownloadHelper {

    fun downloadMovie(context: Context, title: String, downloadUrl: String) {
        if (downloadUrl.isEmpty()) {
            Toast.makeText(context, "ডাউনলোড লিংক পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val sanitized = title.replace(Regex("[^a-zA-Z0-9.-]"), "_")
            val fileName = "${sanitized.take(40)}.mp4"
            val targetDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }
            val targetFile = File(targetDir, fileName)

            val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                setTitle("Mukul plus: $title")
                setDescription("ইন-অ্যাপ ডাউনলোড চলছে...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationUri(Uri.fromFile(targetFile))
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            dm?.enqueue(request)
            Toast.makeText(context, "ডাউনলোড শুরু হয়েছে: $title", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "ডাউনলোড এরর: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun getDownloadedFiles(context: Context): List<DownloadedMedia> {
        val targetDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
        if (!targetDir.exists()) return emptyList()

        val files = targetDir.listFiles()?.filter {
            it.isFile && (it.name.endsWith(".mp4") || it.name.endsWith(".mkv") || it.name.endsWith(".webm"))
        } ?: emptyList()

        val dateFormat = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
        return files.map { file ->
            val sizeMb = file.length() / (1024 * 1024)
            val formattedSize = if (sizeMb > 1024) String.format(Locale.US, "%.1f GB", sizeMb / 1024.0) else "$sizeMb MB"
            DownloadedMedia(
                id = file.name,
                title = file.nameWithoutExtension.replace("_", " "),
                filePath = file.absolutePath,
                fileSize = formattedSize,
                dateAdded = dateFormat.format(Date(file.lastModified()))
            )
        }.sortedByDescending { it.dateAdded }
    }

    fun deleteDownloadedFile(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (file.exists()) file.delete() else false
        } catch (e: Exception) {
            false
        }
    }

    fun isMovieDownloaded(context: Context, title: String): Boolean {
        val sanitized = title.replace(Regex("[^a-zA-Z0-9.-]"), "_")
        val fileName = "${sanitized.take(40)}.mp4"
        val targetDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
        val targetFile = File(targetDir, fileName)
        return targetFile.exists() && targetFile.length() > 0
    }
}
