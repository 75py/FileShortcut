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
import android.graphics.Bitmap
import android.os.Build
import android.support.v4.content.pm.ShortcutInfoCompat
import android.support.v4.content.pm.ShortcutManagerCompat
import android.support.v4.graphics.drawable.IconCompat
import android.widget.Toast
import timber.log.Timber

class ShortcutCreator {

    fun create(activity: Activity
               , pathString: String
               , shortcutName: String
               , mimeType: String
               , icon: Bitmap) {
        Timber.d("path: %s\nname: %s\nmimeType: %s", pathString, shortcutName, mimeType)

        val shortcutIntent = Intent(activity, LaunchShortcutActivity::class.java)
                .setAction(Intent.ACTION_MAIN)
                .putExtra(LaunchShortcutActivity.EXTRA_PATH, pathString)
                .putExtra(LaunchShortcutActivity.EXTRA_MIMETYPE, mimeType)
        val shortcutInfo = ShortcutInfoCompat.Builder(activity, "id_" + System.currentTimeMillis())
                .setLongLabel(shortcutName)
                .setShortLabel(shortcutName)
                .setIcon(IconCompat.createWithBitmap(icon))
                .setIntent(shortcutIntent)
                .build()
        if (activity.intent?.action == Intent.ACTION_CREATE_SHORTCUT) {
            val shortcutResultIntent = ShortcutManagerCompat.createShortcutResultIntent(activity, shortcutInfo)
            Timber.d("setResult %s", shortcutResultIntent)
            activity.setResult(Activity.RESULT_OK, shortcutResultIntent)
            activity.finish()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    && !ShortcutManagerCompat.isRequestPinShortcutSupported(activity)) {
                // Oreo>= and pinned shortcuts are not supported
                Toast.makeText(activity, R.string.msg_not_supported_pinned_shortcuts, Toast.LENGTH_LONG).show()
            } else {
                ShortcutManagerCompat.requestPinShortcut(activity, shortcutInfo, null)
                activity.finish()
            }
        }
    }

}
