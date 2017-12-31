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

package com.nagopy.android.fileshortcut

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import java.io.File

class LaunchShortcutActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pathString = intent.getStringExtra(EXTRA_PATH)
        val mimeType = intent.getStringExtra(EXTRA_MIMETYPE)

        val shortcutIntent = Intent(Intent.ACTION_VIEW)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val uri = Uri.parse(pathString)
        if (uri.scheme.isNullOrEmpty()) {
            val file = File(pathString)
            shortcutIntent.setDataAndType(Uri.fromFile(file), mimeType)
        } else {
            shortcutIntent.data = uri
        }
        startActivityIfLaunchable(shortcutIntent)
        finish()
    }

    companion object {
        val EXTRA_PATH = "com.nagopy.android.fileshortcut.EXTRA_PATH"
        val EXTRA_MIMETYPE = "com.nagopy.android.fileshortcut.EXTRA_MIMETYPE"
    }
}