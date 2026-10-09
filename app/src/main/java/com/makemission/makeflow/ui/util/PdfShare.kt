package com.makemission.makeflow.ui.util

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

fun sharePdfStatement(context: Context, uri: Uri, fileName: String) {
    shareStatement(context, uri, fileName, "application/pdf")
}

fun shareCsvStatement(context: Context, uri: Uri, fileName: String) {
    shareStatement(context, uri, fileName, "text/csv")
}

private fun shareStatement(context: Context, uri: Uri, fileName: String, mimeType: String) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newUri(context.contentResolver, fileName, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(Intent.createChooser(sendIntent, "Share statement"))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "Statement saved, but no sharing app is available", Toast.LENGTH_LONG).show()
    }
}
