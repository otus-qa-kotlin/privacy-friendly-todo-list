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
package org.secuso.privacyfriendlytodolist.view.calendar

import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import org.secuso.privacyfriendlytodolist.R
import org.secuso.privacyfriendlytodolist.model.Helper
import org.secuso.privacyfriendlytodolist.model.TodoTask
import org.secuso.privacyfriendlytodolist.model.database.DBQueryHandler.Companion.getAllToDoLists
import org.secuso.privacyfriendlytodolist.model.database.DBQueryHandler.Companion.getAllToDoTasks
import org.secuso.privacyfriendlytodolist.model.database.DatabaseHelper
import org.secuso.privacyfriendlytodolist.model.database.DatabaseHelper.Companion.getInstance
import org.secuso.privacyfriendlytodolist.view.MainActivity
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Created by Sebastian Lutz on 31.01.2018.
 *
 * This Activity creates a calendar using CalendarGripAdapter to show deadlines of a task.
 */
class CalendarActivity : AppCompatActivity() {
    private var calendarView: CalendarView? = null
    private var calendarGridAdapter: CalendarGridAdapter? = null
    protected var containerActivity: MainActivity? = null
    private val tasksPerDay = HashMap<String, ArrayList<TodoTask>>()
    private var dbHelper: DatabaseHelper? = null
    private var todaysTasks: ArrayList<TodoTask>? = null

    /*  private ExpandableListView expandableListView;
    private ExpandableTodoTaskAdapter taskAdapter; */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_calendar)
        val toolbar = findViewById<View>(R.id.toolbar_calendar) as Toolbar
        if (toolbar != null) {
            toolbar.setTitle(R.string.calendar)
            toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white))
            setSupportActionBar(toolbar)
            supportActionBar!!.setHomeAsUpIndicator(R.drawable.arrow)
        }
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setDisplayShowHomeEnabled(true)
        }
        calendarView = findViewById<View>(R.id.calendar_view) as CalendarView
        calendarGridAdapter = CalendarGridAdapter(this, R.layout.calendar_day)
        calendarView!!.setGridAdapter(calendarGridAdapter)
        //expandableListView = (ExpandableListView) findViewById(R.id.exlv_tasks);
        dbHelper = getInstance(this)
        todaysTasks = ArrayList()
        updateDeadlines()
        calendarView!!.setNextMonthOnClickListener {
            calendarView!!.incMonth(1)
            calendarView!!.refresh()
        }
        calendarView!!.setPrevMontOnClickListener {
            calendarView!!.incMonth(-1)
            calendarView!!.refresh()
        }
        calendarView!!.setDayOnClickListener { parent, view, position, id -> //todaysTasks.clear();
            updateDeadlines()
            val selectedDate = calendarGridAdapter!!.getItem(position)
            val key = absSecondsToDate(selectedDate!!.time / 1000)
            todaysTasks = tasksPerDay[key]
            if (todaysTasks == null) {
                Toast.makeText(
                    applicationContext,
                    getString(R.string.no_deadline_today),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                showDeadlineTasks(todaysTasks!!)
            }
        }
    }

    private fun updateDeadlines() {
        val todoLists = getAllToDoLists(dbHelper!!.readableDatabase)
        val todoTasks = getAllToDoTasks(dbHelper!!.readableDatabase)
        tasksPerDay.clear()
        //for (TodoList list : todoLists){
        for (task in todoTasks) {
            val deadline = task.deadline
            val key = absSecondsToDate(deadline)
            if (!tasksPerDay.containsKey(key)) {
                tasksPerDay[key] = ArrayList()
            }
            tasksPerDay[key]!!.add(task)
            //}
        }
        calendarGridAdapter!!.setTodoTasks(tasksPerDay)
        calendarGridAdapter!!.notifyDataSetChanged()
        //containerActivity.getSupportActionBar().setTitle(R.string.calendar);
    }

    private fun absSecondsToDate(seconds: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = TimeUnit.SECONDS.toMillis(seconds)
        return DateFormat.format(Helper.DATE_FORMAT, cal).toString()
    }

    private fun showDeadlineTasks(tasks: ArrayList<TodoTask>) {
        val intent = Intent(this, CalendarPopup::class.java)
        val b = Bundle()
        b.putParcelableArrayList("Deadlines", tasks)
        intent.putExtras(b)
        startActivity(intent)
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
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        super.onBackPressed()
    }
}