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
package org.secuso.privacyfriendlytodolist.view

import android.content.Intent
import android.os.Bundle
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView.OnItemLongClickListener
import android.widget.ExpandableListView
import android.widget.ExpandableListView.ExpandableListContextMenuInfo
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import org.secuso.privacyfriendlytodolist.R
import org.secuso.privacyfriendlytodolist.model.Helper.getMenuHeader
import org.secuso.privacyfriendlytodolist.model.TodoTask
import org.secuso.privacyfriendlytodolist.model.database.DBQueryHandler.Companion.deleteTodoTask
import org.secuso.privacyfriendlytodolist.model.database.DBQueryHandler.Companion.getBin
import org.secuso.privacyfriendlytodolist.model.database.DBQueryHandler.Companion.recoverSubtasks
import org.secuso.privacyfriendlytodolist.model.database.DBQueryHandler.Companion.recoverTasks
import org.secuso.privacyfriendlytodolist.model.database.DatabaseHelper
import org.secuso.privacyfriendlytodolist.model.database.DatabaseHelper.Companion.getInstance
import org.secuso.privacyfriendlytodolist.view.MainActivity

/**
 * Created by Sebastian Lutz on 20.12.2017.
 *
 * This Activity handles deleted tasks in a kind of recycle bin.
 */
class RecyclerActivity : AppCompatActivity() {
    private var dbhelper: DatabaseHelper? = null
    private var tv: TextView? = null
    private var lv: ExpandableListView? = null
    var rl: RelativeLayout? = null
    private var backupTasks: List<TodoTask> = ArrayList()
    private var expandableTodoTaskAdapter: ExpandableTodoTaskAdapter? = null
    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val longClickedTodo = expandableTodoTaskAdapter!!.longClickedTodo
        when (item.itemId) {
            R.id.restore -> {
                recoverTasks(dbhelper!!.writableDatabase, longClickedTodo!!.left!!)
                val subTasks = longClickedTodo.left!!.subTasks
                for (ts in subTasks) {
                    recoverSubtasks(dbhelper!!.writableDatabase, ts)
                }
                updateAdapter()
            }
        }
        return super.onContextItemSelected(item)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                finish()
                return true
            }

            R.id.btn_clear -> {
                dbhelper = getInstance(this)
                val tasks: ArrayList<TodoTask>
                tasks = getBin(dbhelper!!.readableDatabase)
                val builder1 = AlertDialog.Builder(this)
                builder1.setMessage(R.string.alert_clear)
                builder1.setCancelable(true)
                builder1.setPositiveButton(R.string.yes) { dialog, which ->
                    for (t in tasks) {
                        deleteTodoTask(
                            getInstance(baseContext)!!.readableDatabase, t
                        )
                    }
                    dialog.cancel()
                    updateAdapter()
                }
                builder1.setNegativeButton(R.string.no) { dialog, which -> dialog.cancel() }
                val alert = builder1.create()
                alert.show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val info = menuInfo as ExpandableListContextMenuInfo
        val type = ExpandableListView.getPackedPositionType(info.packedPosition)
        val inflater = this.menuInflater
        menu.setHeaderView(
            getMenuHeader(
                baseContext,
                baseContext.getString(R.string.select_option)
            )
        )
        inflater.inflate(R.menu.deleted_task_long_click, menu)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recycle)
        rl = findViewById<View>(R.id.relative_recycle) as RelativeLayout
        lv = findViewById<View>(R.id.trash_tasks) as ExpandableListView
        tv = findViewById<View>(R.id.bin_empty) as TextView
        val toolbar = findViewById<View>(R.id.toolbar_trash) as Toolbar
        if (toolbar != null) {
            toolbar.setTitle(R.string.bin_toolbar)
            toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white))
            setSupportActionBar(toolbar)
            supportActionBar!!.setHomeAsUpIndicator(R.drawable.arrow)
        }
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setDisplayShowHomeEnabled(true)
        }
        updateAdapter()
        backupTasks = tasksInTrash
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.trash_clear, menu)
        return true
    }

    fun updateAdapter() {
        dbhelper = getInstance(this)
        val tasks: ArrayList<TodoTask>
        tasks = getBin(dbhelper!!.readableDatabase)
        expandableTodoTaskAdapter = ExpandableTodoTaskAdapter(this, tasks)
        lv!!.setAdapter(expandableTodoTaskAdapter)
        lv!!.emptyView = tv
        lv!!.onItemLongClickListener = OnItemLongClickListener { parent, view, position, id ->
            val groupPosition = ExpandableListView.getPackedPositionGroup(id)
            if (ExpandableListView.getPackedPositionType(id) == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
                val childPosition = ExpandableListView.getPackedPositionChild(id)
                expandableTodoTaskAdapter!!.setLongClickedSubTaskByPos(groupPosition, childPosition)
            } else {
                expandableTodoTaskAdapter!!.setLongClickedTaskByPos(groupPosition)
            }
            registerForContextMenu(lv)
            false
        }
    }

    val tasksInTrash: ArrayList<TodoTask>
        get() = getBin(dbhelper!!.readableDatabase)

    override fun onBackPressed() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        super.onBackPressed()
    }
}