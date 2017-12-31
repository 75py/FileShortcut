/*
 * Copyright 2017 75py
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// com.ipaulpro.afilechooser.utils.FileUtils
// https://github.com/iPaulPro/aFileChooser
/*
 * Copyright (C) 2007-2008 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nagopy.android.fileshortcut


import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.DatabaseUtils
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import timber.log.Timber
import java.util.*

/**
 * Intent経由でのファイル取得後の操作を行うヘルパークラス。<br>
 * 処理の多くを以下のクラスからコピーして使用。
 * https://github.com/samirae/aFileChooser/blob/patch-1/aFileChooser/src/com/ipaulpro/afilechooser/utils/FileUtils.java
 */
class ContentHelper(val context: Context, val contentResolver: ContentResolver) {

    /**
     * ローカルファイルか否かを判定する
     *
     * @param url URL
     * @return Whether the URI is a local one.
     */
    fun isLocal(url: String?): Boolean {
        return url != null && !url.startsWith("http://") && !url.startsWith("https://")
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    fun isGooglePhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.content" == uri.authority
    }

    fun isGoogleDriveUri(uri: Uri): Boolean {
        return "com.google.android.apps.docs.storage" == uri.authority
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    fun getDataColumn(uri: Uri, selection: String?, selectionArgs: Array<String>?): String? {
        val column = "_data"
        val projection = arrayOf(column)

        contentResolver.query(uri, projection, selection, selectionArgs, null).use { cursor ->
            if (cursor != null && cursor.moveToFirst()) {
                if (BuildConfig.DEBUG) {
                    DatabaseUtils.dumpCursor(cursor)
                }

                val columnIndex = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(columnIndex)
            }
        }
        return null
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.<br></br>
     * <br></br>
     * Callers should check whether the path is local before assuming it
     * represents a local file.
     *
     * @param uri The Uri to query.
     * @return File path
     */
    fun getPath(uri: Uri): String? {
        Timber.d("File - Authority: %s\n"
                + ", Fragment: %s\n"
                + ", Port: %s\n"
                + ", Query: %s\n"
                + ", Scheme: %s\n"
                + ", Host: %s\n"
                + ", Segments: %s"
                , uri.authority
                , uri.fragment
                , uri.port
                , uri.query
                , uri.scheme
                , uri.host
                , uri.pathSegments
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // DocumentProvider
            if (DocumentsContract.isDocumentUri(context, uri)) {
                // ExternalStorageProvider
                when {
                    isExternalStorageDocument(uri) -> {
                        val docId = DocumentsContract.getDocumentId(uri)
                        val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        val type = split[0]

                        return if ("primary".equals(type, ignoreCase = true)) {
                            Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                        } else {
                            System.getenv("SECONDARY_STORAGE") + "/" + split[1]
                        }
                    }
                    isDownloadsDocument(uri) -> {

                        val id = DocumentsContract.getDocumentId(uri)
                        val contentUri = ContentUris.withAppendedId(
                                Uri.parse("content://downloads/public_downloads"), java.lang.Long.parseLong(id))

                        return getDataColumn(contentUri, null, null)
                    }
                    isMediaDocument(uri) -> {
                        val docId = DocumentsContract.getDocumentId(uri)
                        val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        val type = split[0]

                        val contentUri = when (type) {
                            "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                            "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                            "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                            else -> {
                                throw RuntimeException("Unknown content type: $type")
                            }
                        }

                        val selection = "_id=?"
                        val selectionArgs = arrayOf(split[1])

                        return getDataColumn(contentUri, selection, selectionArgs)
                    }
                    isGoogleDriveUri(uri) -> {
                        context.showErrorMessage(R.string.msg_not_supported_on_google_drive)
                    }
                }
            }
        }

        if ("content".equals(uri.scheme, ignoreCase = true)) {
            // Return the remote address
            return if (isGooglePhotosUri(uri)) {
                uri.lastPathSegment
            } else {
                getDataColumn(uri, null, null)
            }
        } else if ("file".equals(uri.scheme, ignoreCase = true)) {
            return uri.path
        }

        return null
    }

    /**
     * ファイル名からmimetypeを取得して返す
     *
     * @param path ファイル名
     * @return mimetype。不明の場合は UNKNOWN_MIME_TYPE
     */
    fun getMimeType(path: String?): String {
        val dotPos = path?.lastIndexOf('.') ?: 0
        if (dotPos <= 0) {
            return UNKNOWN_MIME_TYPE
        }
        val extension = path!!.substring(dotPos + 1).toLowerCase(Locale.getDefault())
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: UNKNOWN_MIME_TYPE
    }

    fun getBundledIconId(mimeType: String): Int? {
        return when {
            mimeType.startsWith("text/") -> R.mipmap.ic_launcher_text
            mimeType.startsWith("audio/") -> R.mipmap.ic_launcher_audio
            MIME_TYPE_PDF.contains(mimeType) -> R.mipmap.ic_launcher_pdf
            MIME_TYPE_DOCUMENT.contains(mimeType) -> R.mipmap.ic_launcher_document
            MIME_TYPE_SPREADSHEET.contains(mimeType) -> R.mipmap.ic_launcher_spreadsheet
            MIME_TYPE_PRESENTATION.contains(mimeType) -> R.mipmap.ic_launcher_presentation
            else -> null
        }
    }

    companion object {
        val UNKNOWN_MIME_TYPE = "application/octet-stream"
        val MIME_TYPE_DOCUMENT = setOf(
                "application/msword"
                , "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                , "application/vnd.ms-word.document.macroEnabled.12"
                , "application/vnd.openxmlformats-officedocument.wordprocessingml.template"
                , "application/vnd.ms-word.template.macroEnabled.12"
                , "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
        val MIME_TYPE_SPREADSHEET = setOf(
                "application/xlc"
                , "application/msexcel"
                , "application/x-msexcel"
                , "application/vnd.ms-excel.sheet.macroEnabled.12"
                , "application/vnd.openxmlformats-officedocument.spreadsheetml.template"
                , "application/vnd.ms-excel.template.macroEnabled.12"
                , "application/vnd.ms-excel.sheet.binary.macroEnabled.12"
                , "application/vnd.ms-excel.addin.macroEnabled.12"
        )
        val MIME_TYPE_PRESENTATION = setOf(
                "application/pot"
                , "application/powerpoint"
                , "application/pps"
                , "application/ppt"
                , "application/mspowerpoint"
                , "application/vnd.ms-powerpoint"
                , "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                , "application/vnd.ms-powerpoint.presentation.macroEnabled.12"
                , "application/vnd.openxmlformats-officedocument.presentationml.slideshow"
                , "application/vnd.ms-powerpoint.slideshow.macroEnabled.12"
                , "application/vnd.openxmlformats-officedocument.presentationml.template"
                , "application/vnd.ms-powerpoint.template.macroEnabled.12"
                , "application/vnd.ms-powerpoint.addin.macroEnabled.12"
                , "application/vnd.openxmlformats-officedocument.presentationml.slide"
                , "application/vnd.ms-powerpoint.slide.macroEnabled.12"
        )
        val MIME_TYPE_PDF = setOf(
                "application/pdf"
                , "application/x-pdf"
        )
    }

}