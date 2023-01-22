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
package org.secuso.privacyfriendlytodolist.view.dialog

import android.content.Context
import android.preference.PreferenceManager
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import android.widget.Toast
import org.secuso.privacyfriendlytodolist.R
import org.secuso.privacyfriendlytodolist.model.Helper.getDate
import org.secuso.privacyfriendlytodolist.model.Helper.getDateTime
import org.secuso.privacyfriendlytodolist.model.Helper.priority2String
import org.secuso.privacyfriendlytodolist.model.TodoList
import org.secuso.privacyfriendlytodolist.model.TodoTask
import org.secuso.privacyfriendlytodolist.model.database.DBQueryHandler.Companion.getAllToDoLists
import org.secuso.privacyfriendlytodolist.model.database.DatabaseHelper
import org.secuso.privacyfriendlytodolist.model.database.DatabaseHelper.Companion.getInstance
import org.secuso.privacyfriendlytodolist.view.dialog.DeadlineDialog.DeadlineCallback
import org.secuso.privacyfriendlytodolist.view.dialog.ReminderDialog.ReminderCallback

/**
 * Created by Sebastian Lutz on 12.03.2018.
 *
 * This class creates a dialog that lets the user create/edit a task.
 */
class ProcessTodoTaskDialog : FullScreenDialog {
    private var prioritySelector: TextView? = null
    private var deadlineTextView: TextView? = null
    private var reminderTextView: TextView? = null
    private var listSelector: TextView? = null
    private var dialogTitleNew: TextView? = null
    private var dialogTitleEdit: TextView? = null
    private var progressText: TextView? = null
    private var progressPercent: TextView? = null
    private var progress_layout: RelativeLayout? = null
    private var progressSelector: SeekBar? = null
    private var taskName: EditText? = null
    private var taskDescription: EditText? = null
    private var taskPriority: TodoTask.Priority? = null
    private var selectedListID = 0
    private var lists: List<TodoList> = ArrayList()
    private var dbHelper: DatabaseHelper? = null
    private var taskProgress = 0
    private val name: String? = null
    private val description: String? = null
    private var deadline: Long = -1
    private var reminderTime: Long = -1
    private val defaultPriority = TodoTask.Priority.MEDIUM
    private var task: TodoTask

    constructor(context: Context?) : super(context, R.layout.add_task_dialog) {
        initGui()
        task = TodoTask()
        task.setCreated()
        //task.setDbState(DBQueryHandler.ObjectStates.INSERT_TO_DB);
    }

    constructor(context: Context, task: TodoTask) : super(context, R.layout.add_task_dialog) {
        initGui()
        task.setChanged()
        //task.setDbState(DBQueryHandler.ObjectStates.UPDATE_DB);
        deadline = task.deadline
        reminderTime = task.reminderTime
        taskName!!.setText(task.name)
        taskDescription!!.setText(task.description)
        prioritySelector!!.text =
            priority2String(context, task.priority)
        taskPriority = task.priority
        progressSelector!!.progress = task.progress
        if (task.deadline <= 0) deadlineTextView!!.text =
            context.getString(R.string.no_deadline) else deadlineTextView!!.text = getDate(deadline)
        if (task.reminderTime <= 0) reminderTextView!!.text =
            context.getString(R.string.reminder) else reminderTextView!!.text =
            getDateTime(reminderTime)
        this.task = task
    }

    private fun initGui() {

        // initialize textview that displays the selected priority
        prioritySelector = findViewById<View>(R.id.tv_new_task_priority) as TextView
        prioritySelector!!.setOnClickListener(View.OnClickListener {
            registerForContextMenu((prioritySelector)!!)
            openContextMenu((prioritySelector)!!)
        })
        prioritySelector!!.setOnCreateContextMenuListener(this)
        taskPriority = defaultPriority
        prioritySelector!!.text = priority2String(
            context, taskPriority
        )

        //initialize titles of the dialog
        dialogTitleNew = findViewById<View>(R.id.dialog_title) as TextView
        dialogTitleEdit = findViewById<View>(R.id.dialog_edit) as TextView


        //initialize textview that displays selected list
        listSelector = findViewById<View>(R.id.tv_new_task_listchoose) as TextView
        listSelector!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                registerForContextMenu((listSelector)!!)
                openContextMenu((listSelector)!!)
            }
        })
        listSelector!!.setOnCreateContextMenuListener(this)
        progressText = findViewById<View>(R.id.tv_task_progress) as TextView
        progressPercent = findViewById<View>(R.id.new_task_progress) as TextView
        progress_layout = findViewById<View>(R.id.progress_relative) as RelativeLayout

        // initialize seekbar that allows to select the progress
        val selectedProgress = findViewById<View>(R.id.new_task_progress) as TextView
        progressSelector = findViewById<View>(R.id.sb_new_task_progress) as SeekBar
        if (!hasAutoProgress()) {
            progressSelector!!.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    taskProgress = progress
                    selectedProgress.text = "$progress%"
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {}
                override fun onStopTrackingTouch(seekBar: SeekBar) {}
            })
        } else {
            makeProgressGone()
        }


        // initialize buttons
        val okayButton = findViewById<View>(R.id.bt_new_task_ok) as Button
        okayButton.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                val name = taskName!!.text.toString()
                val description = taskDescription!!.text.toString()
                val listName = listSelector!!.text.toString()
                if ((name == "")) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.todo_name_must_not_be_empty),
                        Toast.LENGTH_SHORT
                    ).show()
                } /* else if (listName.equals(getContext().getString(R.string.click_to_choose))) {
                    Toast.makeText(getContext(), getContext().getString(R.string.to_choose_list), Toast.LENGTH_SHORT).show();
                } */ else {
                    task.name = name
                    task.description = description
                    task.deadline = deadline
                    task.priority = taskPriority
                    task.listId = selectedListID
                    task.progress = taskProgress
                    task.reminderTime = reminderTime
                    callback!!.finish(task)
                    dismiss()
                }
            }
        })
        val cancelButton = findViewById<View>(R.id.bt_new_task_cancel) as Button
        cancelButton.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                dismiss()
            }
        })

        // initialize textviews to get deadline and reminder time
        deadlineTextView = findViewById<View>(R.id.tv_todo_list_deadline) as TextView
        deadlineTextView!!.setTextColor(okayButton.currentTextColor)
        deadlineTextView!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                val deadlineDialog = DeadlineDialog(context, deadline)
                deadlineDialog.deadlineCallback = object : DeadlineCallback {
                    override fun setDeadline(d: Long) {
                        deadline = d
                        deadlineTextView!!.text = getDate(deadline)
                    }

                    override fun removeDeadline() {
                        deadline = -1
                        deadlineTextView!!.text = context.resources.getString(R.string.deadline)
                    }
                }
                deadlineDialog.show()
            }
        })
        reminderTextView = findViewById<View>(R.id.tv_todo_list_reminder) as TextView
        reminderTextView!!.setTextColor(okayButton.currentTextColor)
        reminderTextView!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                val reminderDialog = ReminderDialog(context, reminderTime, deadline)
                reminderDialog.reminderCallback  = object : ReminderCallback {
                    override fun setReminder(r: Long) {

                        /*if(deadline == -1) {
                            Toast.makeText(getContext(), getContext().getString(R.string.set_deadline_before_reminder), Toast.LENGTH_SHORT).show();
                            return;
                        }*/
                        if (deadline != -1L && deadline < r) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.deadline_smaller_reminder),
                                Toast.LENGTH_SHORT
                            ).show()
                            return
                        }
                        reminderTime = r
                        reminderTextView!!.text = getDateTime(reminderTime)
                    }

                    override fun removeReminder() {
                        reminderTime = -1
                        val reminderTextView =
                            findViewById<View>(R.id.tv_todo_list_reminder) as TextView
                        reminderTextView.text = context.resources.getString(R.string.reminder)
                    }
                }
                reminderDialog.show()
            }
        })
        taskName = findViewById<View>(R.id.et_new_task_name) as EditText
        taskDescription = findViewById<View>(R.id.et_new_task_description) as EditText
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo) {
        when (v.id) {
            R.id.tv_new_task_priority -> {
                menu.setHeaderTitle(R.string.select_priority)
                for (prio: TodoTask.Priority in TodoTask.Priority.values()) {
                    menu.add(
                        Menu.NONE, prio.value, Menu.NONE, priority2String(
                            context, prio
                        )
                    )
                }
            }

            R.id.tv_new_task_listchoose -> {
                menu.setHeaderTitle(R.string.select_list)
                updateLists()
                menu.add(Menu.NONE, -3, Menu.NONE, R.string.select_no_list)
                for (tl: TodoList in lists) {
                    //+3 so that IDs are non-overlapping with prio-IDs
                    menu.add(Menu.NONE, tl.id + 3, Menu.NONE, tl.name)
                }
            }
        }
    }

    override fun onMenuItemSelected(featureId: Int, item: MenuItem): Boolean {
        val numValues = TodoTask.Priority.values().size
        if ((item.itemId < numValues) && (item.itemId >= 0)) {
            taskPriority = TodoTask.Priority.values()[item.itemId]
            prioritySelector!!.text = priority2String(
                context, taskPriority
            )
        }
        for (tl: TodoList in lists) {
            if (item.itemId - 3 == tl.id) {
                selectedListID = tl.id
                listSelector!!.text = tl.name
            } else if (item.title === context.getString(R.string.to_choose_list) || item.title === context.getString(
                    R.string.select_no_list
                )
            ) {
                selectedListID = -3
                listSelector!!.text = item.title
            }
        }
        return super.onMenuItemSelected(featureId, item)
    }

    //updates the lists array
    fun updateLists() {
        dbHelper = getInstance(context)
        lists = getAllToDoLists(dbHelper!!.readableDatabase)
    }

    //change the dialogtitle from "new task" to "edit task"
    fun titleEdit() {
        dialogTitleNew!!.visibility = View.GONE
        dialogTitleEdit!!.visibility = View.VISIBLE
    }

    //sets the textview either to listname in context or if no context to default
    fun setListSelector(id: Int, idExists: Boolean) {
        updateLists()
        for (tl: TodoList in lists) {
            if (id == tl.id && idExists == true) {
                listSelector!!.text = tl.name
                selectedListID = tl.id
            } else if (!idExists) {
                listSelector!!.text = context.getString(R.string.click_to_choose)
                selectedListID = -3
            }
        }
    }

    private fun hasAutoProgress(): Boolean {
        //automatic-progress enabled?
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean("pref_progress", false)
    }

    //Make progress-selectionbar disappear
    private fun makeProgressGone() {
        progress_layout!!.visibility = View.GONE
    }

}