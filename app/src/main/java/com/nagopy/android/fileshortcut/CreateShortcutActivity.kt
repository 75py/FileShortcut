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

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ShortcutManager
import android.databinding.DataBindingUtil
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.support.v7.app.AlertDialog
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import com.github.salomonbrys.kodein.android.KodeinAppCompatActivity
import com.github.salomonbrys.kodein.instance
import com.nagopy.android.fileshortcut.databinding.ActivityCreateShortcutBinding
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.OnNeverAskAgain
import permissions.dispatcher.OnPermissionDenied
import permissions.dispatcher.RuntimePermissions
import timber.log.Timber
import java.io.File


@RuntimePermissions
class CreateShortcutActivity : KodeinAppCompatActivity(), View.OnClickListener {

    lateinit var binding: ActivityCreateShortcutBinding

    val contentHelper: ContentHelper by instance()
    val shortcutCreator: ShortcutCreator by instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_create_shortcut)
        binding.onClickListener = this

        handleSharedIntent(intent)
    }

    fun handleSharedIntent(intent: Intent?) {
        if (intent == null || intent.action != Intent.ACTION_SEND) {
            return
        }

        val extra = intent.extras
        extra?.keySet()?.forEach {
            if (it == Intent.EXTRA_STREAM) {
                val es = extra.get(Intent.EXTRA_STREAM) ?: return@forEach
                val uri = Uri.parse(es.toString())
                val data = Intent()
                data.data = uri
                onActivityResult(REQUEST_CODE_FILE, RESULT_OK, data)
                return@forEach
            }
        }
    }

    override fun onStart() {
        super.onStart()
        requestPermissionWithPermissionCheck()
    }

    @NeedsPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
    fun requestPermission() {
        // do nothing
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }

    override fun onClick(v: View?) {
        Timber.d("onClick %d", v?.id)
        when (v?.id) {
            R.id.filePickerButton -> startFilePickerWithPermissionCheck()
            R.id.iconPickerButton -> startIconPickerWithPermissionCheck()
            R.id.createShortcutButton -> createShortcut()
        }
    }

    @NeedsPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
    fun startFilePicker() {
        Timber.d("startFilePicker")
        val intent = Intent(Intent.ACTION_GET_CONTENT).setType("*/*")
        startActivityForResult(intent, REQUEST_CODE_FILE)
    }

    @NeedsPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
    fun startIconPicker() {
        Timber.d("startIconPicker")
        val intent = Intent(Intent.ACTION_GET_CONTENT).setType("image/*")
        startActivityForResult(intent, REQUEST_CODE_ICON)
    }

    fun createShortcut() {
        val id = binding.id
        val pathString = binding.targetFilePath.text.toString()
        val shortcutName = binding.targetShortcutName.text.toString()
        val mimeType = contentHelper.getMimeType(pathString)
        val iconBitmap = getIconBitmap(binding.targetShortcutIcon)
        if (id == null) {
            shortcutCreator.create(this, pathString, shortcutName, mimeType, iconBitmap)
        } else {
            shortcutCreator.update(this, id, pathString, shortcutName, mimeType, iconBitmap)
        }
    }

    fun getIconBitmap(imageView: ImageView): Bitmap {
        val backup_isDrawingCacheEnabled = imageView.isDrawingCacheEnabled
        if (imageView.isDrawingCacheEnabled) {
            imageView.destroyDrawingCache()
        }
        imageView.isDrawingCacheEnabled = true
        val bitmap = Bitmap.createBitmap(imageView.drawingCache)
        imageView.isDrawingCacheEnabled = backup_isDrawingCacheEnabled
        return bitmap
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != RESULT_OK) {
            return
        }
        when (requestCode) {
            REQUEST_CODE_FILE -> {
                if (data == null) {
                    return
                }
                Timber.d("onActivityResult %s", data)
                val pathString = contentHelper.getPath(data.data)
                val mimeType = contentHelper.getMimeType(pathString)
                binding.filePath = pathString
                binding.mimeType = mimeType
                binding.shortcutName = if (contentHelper.isLocal(pathString)) {
                    File(pathString).name.toString()
                } else {
                    pathString
                }
                if (mimeType.startsWith("image")) {
                    binding.shortcutIcon = data.data
                } else if (mimeType.startsWith("video")) {
                    val thumbnail = ThumbnailUtils.createVideoThumbnail(pathString, MediaStore.Video.Thumbnails.MICRO_KIND)
                    binding.targetShortcutIcon.setImageBitmap(thumbnail)
                } else {
                    val bundledId = contentHelper.getBundledIconId(mimeType)
                    if (bundledId != null) {
                        binding.targetShortcutIcon.setImageResource(bundledId)
                    }
                }
            }
            REQUEST_CODE_ICON -> {
                binding.shortcutIcon = data?.data
            }
            REQUEST_CODE_HISTORY -> {
                val id = data?.getStringExtra(CreatedShortcutListActivity.EXTRA_RESULT_ID)
                val path = data?.getStringExtra(CreatedShortcutListActivity.EXTRA_RESULT_PATH)
                val name = data?.getStringExtra(CreatedShortcutListActivity.EXTRA_RESULT_NAME)
                val icon = data?.getParcelableExtra(CreatedShortcutListActivity.EXTRA_RESULT_ICON) as? Bitmap
                binding.id = id
                binding.filePath = path
                binding.shortcutName = name
                binding.mimeType = contentHelper.getMimeType(path)
                binding.targetShortcutIcon.setImageBitmap(icon)
            }
        }
    }

    @OnPermissionDenied(Manifest.permission.READ_EXTERNAL_STORAGE)
    @OnNeverAskAgain(Manifest.permission.READ_EXTERNAL_STORAGE)
    fun onPermissionDenied() {
        Timber.d("@OnNeverAskAgain")
        AlertDialog.Builder(this)
                .setTitle(R.string.need_permission)
                .setMessage(R.string.msg_need_permission)
                .setPositiveButton(R.string.app_setting, DialogInterface.OnClickListener { dialogInterface, i ->
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                })
                .setNegativeButton(R.string.close, DialogInterface.OnClickListener { dialogInterface, i -> finish() })
                .show()
    }

    companion object {
        val REQUEST_CODE_FILE = 75
        val REQUEST_CODE_ICON = 76
        val REQUEST_CODE_HISTORY = 77
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_create_shortcut, menu)

        menu?.findItem(R.id.menu_history)?.isVisible = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val shortcutManager: ShortcutManager by instance()
            if (intent?.categories?.contains(Intent.CATEGORY_LAUNCHER) == true // from home app
                    && shortcutManager.pinnedShortcuts.isNotEmpty()) {
                menu?.findItem(R.id.menu_history)?.isVisible = true
            }
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_license -> {
                startActivity(Intent(this, LicenseActivity::class.java))
            }
            R.id.menu_history -> {
                startActivityForResult(Intent(this, CreatedShortcutListActivity::class.java), REQUEST_CODE_HISTORY)
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
