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

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.os.Build
import android.os.SystemClock
import timber.log.Timber

class ShortcutCreator {

    fun create(activity: Activity
               , pathString: String
               , shortcutName: String
               , mimeType: String
               , icon: Bitmap) {
        Timber.d("path: %s\nname: %s\nmimeType: %s", pathString, shortcutName, mimeType)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createApi26(activity, pathString, shortcutName, mimeType, icon)
        } else {
            createApi25(activity, pathString, shortcutName, mimeType, icon)
        }
        activity.finish()
    }

    @TargetApi(Build.VERSION_CODES.O)
    fun createApi26(activity: Activity
                    , pathString: String
                    , shortcutName: String
                    , mimeType: String
                    , icon: Bitmap) {
        val shortcutIntent = Intent(activity, LaunchShortcutActivity::class.java)
                .setAction(Intent.ACTION_MAIN)
                .putExtra(LaunchShortcutActivity.EXTRA_PATH, pathString)
                .putExtra(LaunchShortcutActivity.EXTRA_MIMETYPE, mimeType)

        val shortcutManager = activity.getSystemService(Context.SHORTCUT_SERVICE) as ShortcutManager
        val shortcutInfo = ShortcutInfo.Builder(activity, "id_" + SystemClock.currentThreadTimeMillis())
                .setShortLabel(shortcutName)
                .setIcon(Icon.createWithBitmap(icon))
                .setIntent(shortcutIntent)
                .build()
        if (activity.intent?.action == Intent.ACTION_CREATE_SHORTCUT) {
            val shortcutResultIntent = shortcutManager.createShortcutResultIntent(shortcutInfo)
            setResult(activity, shortcutResultIntent)
        } else {
            shortcutManager.requestPinShortcut(shortcutInfo, null)
        }
    }

    fun createApi25(activity: Activity
                    , pathString: String
                    , shortcutName: String
                    , mimeType: String
                    , icon: Bitmap) {
        val shortcutIntent = Intent(activity, LaunchShortcutActivity::class.java)
                .putExtra(LaunchShortcutActivity.EXTRA_PATH, pathString)
                .putExtra(LaunchShortcutActivity.EXTRA_MIMETYPE, mimeType)

        val intent = Intent()
                .putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
                .putExtra(Intent.EXTRA_SHORTCUT_ICON, icon)
                .putExtra(Intent.EXTRA_SHORTCUT_NAME, shortcutName)

        if (activity.intent?.action == Intent.ACTION_CREATE_SHORTCUT) {
            setResult(activity, intent)
        } else {
            sendInstallShortcutBroadcast(activity, intent)
        }
    }

    fun setResult(activity: Activity, intent: Intent) {
        Timber.d("setResult %s", intent)
        activity.setResult(Activity.RESULT_OK, intent)
    }

    fun sendInstallShortcutBroadcast(activity: Activity, intent: Intent) {
        Timber.d("sendInstallShortcutBroadcast %s", intent)
        intent.action = "com.android.launcher.action.INSTALL_SHORTCUT"
        activity.sendBroadcast(intent)
    }

}
