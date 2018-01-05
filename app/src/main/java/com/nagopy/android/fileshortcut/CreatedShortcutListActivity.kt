package com.nagopy.android.fileshortcut

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.databinding.DataBindingUtil
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import com.github.salomonbrys.kodein.android.KodeinAppCompatActivity
import com.github.salomonbrys.kodein.instance
import com.nagopy.android.fileshortcut.LaunchShortcutActivity.Companion.EXTRA_PATH
import com.nagopy.android.fileshortcut.databinding.ActivityCreatedShortcutListBinding
import com.nagopy.android.fileshortcut.databinding.ItemCreatedShortcutBinding
import timber.log.Timber
import java.io.ByteArrayOutputStream


@TargetApi(Build.VERSION_CODES.O)
class CreatedShortcutListActivity : KodeinAppCompatActivity(), AdapterView.OnItemClickListener {

    lateinit var binding: ActivityCreatedShortcutListBinding
    val shortcutManager: ShortcutManager by instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pinnedShortcuts = shortcutManager.pinnedShortcuts
        pinnedShortcuts.sortByDescending { it.lastChangedTimestamp }
        Timber.d("%s", pinnedShortcuts)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_created_shortcut_list)
        binding.listView.adapter = Adapter(
                this
                , pinnedShortcuts
                , shortcutManager.iconMaxWidth
                , shortcutManager.iconMaxHeight)
        binding.onItemClickListener = this
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val shortcutInfo = parent?.adapter?.getItem(position) as ShortcutInfo
        Timber.d("onItemClick %s", shortcutInfo)

        val binding = view!!.tag as ItemCreatedShortcutBinding
        Timber.d("%s", binding)
        val resultIntent = Intent()
                .putExtra(EXTRA_RESULT_ID, binding.id)
                .putExtra(EXTRA_RESULT_PATH, binding.path)
                .putExtra(EXTRA_RESULT_NAME, binding.label)
                .putExtra(EXTRA_RESULT_ICON, binding.icon)
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    class Adapter(val context: Context
                  , val pinnedShortcuts: List<ShortcutInfo>
                  , val iconMaxWidth: Int
                  , val iconMaxHeight: Int
    ) : BaseAdapter() {

        override fun getItem(position: Int): ShortcutInfo = pinnedShortcuts[position]

        override fun getItemId(position: Int): Long = position.toLong() // unused

        override fun getCount(): Int = pinnedShortcuts.size

        override fun getView(position: Int, paramConvertView: View?, parent: ViewGroup?): View {
            val binding: ItemCreatedShortcutBinding
            val convertView: View
            if (paramConvertView == null) {
                val inflater = LayoutInflater.from(context)
                binding = DataBindingUtil.inflate(inflater, R.layout.item_created_shortcut, parent, false)
                convertView = binding.root
                convertView.tag = binding
            } else {
                convertView = paramConvertView
                binding = convertView.tag as ItemCreatedShortcutBinding
            }

            val shortcutInfo = getItem(position)
            binding.id = shortcutInfo.id
            binding.icon = convertToBitmap(shortcutInfo.intent.getStringExtra(EXTRA_ICON))
            binding.label = shortcutInfo.longLabel.toString()
            binding.path = shortcutInfo.intent.getStringExtra(EXTRA_PATH)
            binding.iconMaxWidth = iconMaxWidth
            binding.iconMaxHeight = iconMaxHeight

            return convertView
        }
    }

    companion object {
        val EXTRA_ICON = "com.nagopy.android.fileshortcut.EXTRA_ICON"

        val EXTRA_RESULT_ID = "com.nagopy.android.fileshortcut.EXTRA_RESULT_ID"
        val EXTRA_RESULT_PATH = "com.nagopy.android.fileshortcut.EXTRA_RESULT_PATH"
        val EXTRA_RESULT_NAME = "com.nagopy.android.fileshortcut.EXTRA_RESULT_NAME"
        val EXTRA_RESULT_ICON = "com.nagopy.android.fileshortcut.EXTRA_RESULT_ICON"

        // https://stackoverflow.com/questions/4989182/converting-java-bitmap-to-byte-array
        /**
         * @param bitmap
         * @return converting bitmap and return a string
         */
        fun convertToString(bitmap: Bitmap): String {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val b = stream.toByteArray()
            return Base64.encodeToString(b, Base64.DEFAULT)
        }

        /**
         * @param encodedString
         * @return bitmap (from given string)
         */
        fun convertToBitmap(encodedString: String): Bitmap? {
            return try {
                val encodeByte = Base64.decode(encodedString, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.size)
            } catch (e: Exception) {
                Timber.e(e)
                null
            }

        }
    }

}
