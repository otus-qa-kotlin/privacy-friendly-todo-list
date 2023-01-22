/*
 This file is part of Privacy Friendly To-Do List.

 Privacy Friendly To-Do List is free software:
 you can redistribute it and/or modify it under the terms of the
 GNU General Public License as published by the Free Software Foundation,
 either version 3 of the License, or any later version.

 Privacy Friendly To-Do List is distributed in the hope
 that it will be useful, but WITHOUT ANY WARRANTY; without even
 the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Privacy Friendly To-Do List. If not, see <http://www.gnu.org/licenses/>.
 */
package org.secuso.privacyfriendlytodolist.view.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import org.secuso.privacyfriendlytodolist.R
import org.secuso.privacyfriendlytodolist.model.TodoList
import org.secuso.privacyfriendlytodolist.model.database.DBQueryHandler.Companion.getAllToDoLists
import org.secuso.privacyfriendlytodolist.model.database.DatabaseHelper
import org.secuso.privacyfriendlytodolist.model.database.DatabaseHelper.Companion.getInstance

/**
 * The configuration screen for the [TodoListWidget] AppWidget.
 * @author Sebastian Lutz
 * @version 1.0
 */
class TodoListWidgetConfigureActivity : Activity() {
    var mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var spinner: Spinner? = null
    private var selected: String? = null
    private var dbHelper: DatabaseHelper? = null
    private var lists: ArrayAdapter<String?>? = null
    var mOnClickListener: View.OnClickListener = object : View.OnClickListener {
        override fun onClick(v: View) {
            val context: Context = this@TodoListWidgetConfigureActivity

            // When the button is clicked, store the string locally
            if (!lists!!.isEmpty) {
                val listTitle: String = selectedItem
                saveTitlePref(context, mAppWidgetId, listTitle)

                // It is the responsibility of the configuration activity to update the app widget
                //AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                WidgetViewsFactory.getListName(context, mAppWidgetId)
                TodoListWidget.getListName(context, mAppWidgetId)

                // Make sure we pass back the original appWidgetId
                val resultValue = Intent()
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId)
                setResult(RESULT_OK, resultValue)
                finish()
            } else Toast.makeText(context, "No list available", Toast.LENGTH_SHORT).show()
        }
    }

    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        updateLists()

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED)
        setContentView(R.layout.todo_list_widget_configure)
        findViewById<View>(R.id.add_button).setOnClickListener(mOnClickListener)

        //initialize spinner dropdown
        spinner = findViewById<View>(R.id.spinner1) as Spinner
        lists!!.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner!!.adapter = lists

        // Find the widget id from the intent.
        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        // If this activity was started with an intent without an app widget ID, finish with an error.
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
    }

    //updates the lists array and prepare adapter for spinner
    fun updateLists() {
        dbHelper = getInstance(this)
        var tl = ArrayList<TodoList>()
        tl = getAllToDoLists(dbHelper!!.readableDatabase)
        val help = ArrayList<String?>()
        for (i in tl.indices) {
            help.add(tl[i].name)
        }
        lists = ArrayAdapter(this, android.R.layout.simple_spinner_item, help)
        lists!!.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    }

    val selectedItem: String
        get() {
            selected = spinner!!.selectedItem.toString()
            return selected!!
        }

    companion object {
        private const val PREFS_NAME =
            "org.secuso.privacyfriendlytodolist.view.widget.TodoListWidget"
        private const val PREF_PREFIX_KEY = "appwidget_"

        // Write the prefix to the SharedPreferences object for this widget
        fun saveTitlePref(context: Context, appWidgetId: Int, text: String?) {
            val prefs = context.getSharedPreferences(PREFS_NAME, 0).edit()
            prefs.putString(PREF_PREFIX_KEY + appWidgetId, text)
            prefs.apply()
        }

        // Read the prefix from the SharedPreferences object for this widget.
        // If there is no preference saved, get the default from a resource
        fun loadTitlePref(context: Context?, appWidgetId: Int): String {
            val prefs = context!!.getSharedPreferences(PREFS_NAME, 0)
            val titleValue = prefs.getString(PREF_PREFIX_KEY + appWidgetId, null)
            return titleValue ?: context.getString(R.string.appwidget_text)
        }

        fun deleteTitlePref(context: Context, appWidgetId: Int) {
            val prefs = context.getSharedPreferences(PREFS_NAME, 0).edit()
            prefs.remove(PREF_PREFIX_KEY + appWidgetId)
            prefs.apply()
        }
    }
}