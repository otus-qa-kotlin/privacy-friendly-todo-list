package org.secuso.privacyfriendlytodolist.view.calendar

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ExpandableListView
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import org.secuso.privacyfriendlytodolist.R
import org.secuso.privacyfriendlytodolist.model.TodoTask
import org.secuso.privacyfriendlytodolist.view.ExpandableTodoTaskAdapter

/**
 * Created by sebbi on 07.03.2018.
 *
 * This class helps to show the tasks that are on a specific deadline
 */
class CalendarPopup : AppCompatActivity() {
    private var lv: ExpandableListView? = null
    private var rl: RelativeLayout? = null
    private var expandableTodoTaskAdapter: ExpandableTodoTaskAdapter? = null
    private var tasks: ArrayList<TodoTask>? = ArrayList()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.calendar_popup)
        rl = findViewById<View>(R.id.relative_deadline) as RelativeLayout
        lv = findViewById<View>(R.id.deadline_tasks) as ExpandableListView
        val toolbar = findViewById<View>(R.id.toolbar_deadlineTasks) as Toolbar
        toolbar.setTitle(R.string.deadline)
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white))
        setSupportActionBar(toolbar)
        supportActionBar!!.setHomeAsUpIndicator(R.drawable.arrow)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setDisplayShowHomeEnabled(true)
        }
        val b = intent.extras
        if (b != null) tasks = b.getParcelableArrayList("Deadlines")
        updateAdapter()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun updateAdapter() {
        expandableTodoTaskAdapter = ExpandableTodoTaskAdapter(this, tasks!!)
        lv!!.setAdapter(expandableTodoTaskAdapter)
    }
}