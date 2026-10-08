package com.makemission.makeflow.ui.util

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

fun sharePdfStatement(context: Context, uri: Uri, fileName: String) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
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
