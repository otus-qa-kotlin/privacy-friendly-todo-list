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

import android.os.Bundle
import android.util.Log
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemLongClickListener
import android.widget.ExpandableListView
import android.widget.ExpandableListView.ExpandableListContextMenuInfo
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuItemCompat
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.secuso.privacyfriendlytodolist.R
import org.secuso.privacyfriendlytodolist.model.BaseTodo
import org.secuso.privacyfriendlytodolist.model.Helper.getMenuHeader
import org.secuso.privacyfriendlytodolist.model.TodoList
import org.secuso.privacyfriendlytodolist.model.TodoTask
import org.secuso.privacyfriendlytodolist.model.database.DBQueryHandler.Companion.putSubtaskInTrash
import org.secuso.privacyfriendlytodolist.model.database.DBQueryHandler.Companion.putTaskInTrash
import org.secuso.privacyfriendlytodolist.view.ExpandableTodoTaskAdapter.GroupTaskViewHolder
import org.secuso.privacyfriendlytodolist.view.ExpandableTodoTaskAdapter.SortTypes
import org.secuso.privacyfriendlytodolist.view.dialog.ProcessTodoSubTaskDialog
import org.secuso.privacyfriendlytodolist.view.dialog.ProcessTodoTaskDialog

class TodoTasksFragment : Fragment(), SearchView.OnQueryTextListener {
    private var expandableListView: ExpandableListView? = null
    private var taskAdapter: ExpandableTodoTaskAdapter? = null
    private var containingActivity: MainActivity? = null
    private var currentList: TodoList? = null
    private var todoTasks = ArrayList<TodoTask>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        containingActivity = activity as MainActivity?
        if (containingActivity == null) {
            throw RuntimeException("TodoTasksFragment could not find containing activity.")
        }
        val showFab = requireArguments().getBoolean(SHOW_FLOATING_BUTTON)

        // This argument is only set if a dummy list is displayed (a mixture of tasks of different lists) or if
        // a list was selected by clicking on a notification. If the the user selects a list explicitly by clicking on it
        // the list object is instantly available and can be obtained using the method "getClickedList()"
        val selectedListID = requireArguments().getInt(TodoList.UNIQUE_DATABASE_ID, -1)
        var showListNamesOfTasks = false
        if (selectedListID >= 0) {
            currentList =
                containingActivity!!.getListByID(selectedListID) // MainActivity was started after a notification click
            Log.i(TAG, "List was loaded that was requested by a click on a notification.")
        } else if (selectedListID == TodoList.DUMMY_LIST_ID) {
            currentList = containingActivity!!.dummyList
            showListNamesOfTasks = true
            Log.i(TAG, "Dummy list was loaded.")
        } else {
            currentList = containingActivity!!.clickedList // get clicked list
            Log.i(TAG, "Clicked list was loaded.")
        }
        val v = inflater.inflate(R.layout.fragment_todo_tasks, container, false)
        if (currentList != null) {
            todoTasks = currentList!!.tasks
            initExListViewGUI(v)
            initFab(v, showFab)
            taskAdapter!!.setListNames(showListNamesOfTasks)

            // set toolbar title
            if ((activity as AppCompatActivity?)!!.supportActionBar != null) {
                (activity as AppCompatActivity?)!!.supportActionBar!!.title = currentList!!.name
            }
        } else {
            Log.d(TAG, "Cannot identify selected list.")
        }
        return v
    }

    private fun initFab(rootView: View, showFab: Boolean) {
        val optionFab = rootView.findViewById<View>(R.id.fab_new_task) as FloatingActionButton
        if (showFab) {
            optionFab.setOnClickListener {
                val addListDialog = ProcessTodoTaskDialog(activity)
                addListDialog.setDialogResult(object : TodoCallback {
                    override fun finish(b: BaseTodo?) {
                                            if (b is TodoTask) {
                        todoTasks.add(b)
                        saveNewTasks()
                        taskAdapter!!.notifyDataSetChanged()
                    }
                }
            })
                addListDialog.show()
            }
        } else optionFab.visibility = View.GONE
    }

    private fun initExListViewGUI(v: View) {
        taskAdapter = ExpandableTodoTaskAdapter(requireActivity(), todoTasks)
        val emptyView = v.findViewById<View>(R.id.tv_empty_view_no_tasks) as TextView
        expandableListView = v.findViewById<View>(R.id.exlv_tasks) as ExpandableListView
        expandableListView!!.onItemLongClickListener =
            OnItemLongClickListener { parent, view, position, id ->
                val groupPosition = ExpandableListView.getPackedPositionGroup(id)
                if (ExpandableListView.getPackedPositionType(id) == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
                    val childPosition = ExpandableListView.getPackedPositionChild(id)
                    taskAdapter!!.setLongClickedSubTaskByPos(groupPosition, childPosition)
                } else {
                    taskAdapter!!.setLongClickedTaskByPos(groupPosition)
                }
                false
            }


        // react when task expands
        expandableListView!!.setOnGroupClickListener { parent, v, groupPosition, id ->
            val vh = v.tag
            if (vh != null && vh is GroupTaskViewHolder) {
                val viewHolder = vh
                if (viewHolder.seperator!!.visibility == View.GONE) {
                    viewHolder.seperator!!.visibility = View.VISIBLE
                } else {
                    viewHolder.seperator!!.visibility = View.GONE
                }
            }
            false
        }

        // long click to delete or change a task
        registerForContextMenu(expandableListView!!)
        expandableListView!!.emptyView = emptyView
        expandableListView!!.setAdapter(taskAdapter)
    }

    override fun onCreateContextMenu(
        menu: ContextMenu, v: View,
        menuInfo: ContextMenuInfo?
    ) {
        val info = menuInfo as ExpandableListContextMenuInfo?
        val type = ExpandableListView.getPackedPositionType(info!!.packedPosition)
        val inflater = requireActivity().menuInflater
        menu.setHeaderView(getMenuHeader(context, requireContext().getString(R.string.select_option)))

        // context menu for child items
        if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
            inflater.inflate(R.menu.todo_subtask_long_click, menu)
        } else { // context menu for group items
            inflater.inflate(R.menu.todo_task_long_click, menu)
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val longClickedTodo = taskAdapter!!.longClickedTodo
        val affectedRows: Int
        when (item.itemId) {
            R.id.change_subtask -> {
                val dialog = ProcessTodoSubTaskDialog(containingActivity, longClickedTodo!!.right!!)
                dialog.setDialogResult(object : TodoCallback {
                    override fun finish(b: BaseTodo?) {
                        if (b is TodoTask) {
                            taskAdapter!!.notifyDataSetChanged()
                            Log.i(TAG, "subtask altered")
                        }
                    }
                })
                dialog.show()
            }

            R.id.delete_subtask -> {
                affectedRows = putSubtaskInTrash(
                    containingActivity!!.dbHelper!!.writableDatabase,
                    longClickedTodo!!.right
                )
                longClickedTodo.left!!.subTasks.remove(longClickedTodo.right)
                if (affectedRows == 1) Toast.makeText(
                    context,
                    getString(R.string.subtask_removed),
                    Toast.LENGTH_SHORT
                ).show() else Log.d(
                    TAG,
                    "Subtask was not removed from the database. Maybe it was not added beforehand (then this is no error)?"
                )
                taskAdapter!!.notifyDataSetChanged()
            }

            R.id.change_task -> {
                val editTaskDialog = ProcessTodoTaskDialog(requireActivity(), longClickedTodo!!.left!!)
                editTaskDialog.setDialogResult(object : TodoCallback {
                    override fun finish(alteredTask: BaseTodo?) {
                        if (alteredTask is TodoTask) {
                            taskAdapter!!.notifyDataSetChanged()
                        }
                    }
                })
                editTaskDialog.show()
            }

            R.id.delete_task -> {
                affectedRows = putTaskInTrash(
                    containingActivity!!.dbHelper!!.writableDatabase,
                    longClickedTodo!!.left
                )
                todoTasks.remove(longClickedTodo.left)
                if (affectedRows == 1) Toast.makeText(
                    context,
                    getString(R.string.task_removed),
                    Toast.LENGTH_SHORT
                ).show() else Log.d(
                    TAG,
                    "Task was not removed from the database. Maybe it was not added beforehand (then this is no error)?"
                )
                taskAdapter!!.notifyDataSetChanged()
            }

            else -> throw IllegalArgumentException("Invalid menu item selected.")
        }
        return super.onContextItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        retainInstance = true
    }

    override fun onPause() {
        saveNewTasks()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.main, menu)
        inflater.inflate(R.menu.search, menu)
        inflater.inflate(R.menu.add_list, menu)
        val searchItem = menu.findItem(R.id.ac_search)
        val searchView = MenuItemCompat.getActionView(searchItem) as SearchView
        searchView.setOnQueryTextListener(this)
    }

    private fun collapseAll() {
        // collapse all elements on view change.
        // the expandable list view keeps the expanded indices, so other items
        // get expanded, when they get the old expanded index
        val groupCount = taskAdapter!!.groupCount
        for (i in 0 until groupCount) expandableListView!!.collapseGroup(i)
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        collapseAll()
        taskAdapter!!.setQueryString(query)
        taskAdapter!!.notifyDataSetChanged()
        return false
    }

    override fun onQueryTextChange(query: String): Boolean {
        collapseAll()
        taskAdapter!!.setQueryString(query)
        taskAdapter!!.notifyDataSetChanged()
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val checked: Boolean
        val sortType: SortTypes
        collapseAll()
        when (item.itemId) {
            R.id.ac_show_all_tasks -> {
                taskAdapter!!.setFilter(ExpandableTodoTaskAdapter.Filter.ALL_TASKS)
                taskAdapter!!.notifyDataSetChanged()
                return true
            }

            R.id.ac_show_open_tasks -> {
                taskAdapter!!.setFilter(ExpandableTodoTaskAdapter.Filter.OPEN_TASKS)
                taskAdapter!!.notifyDataSetChanged()
                return true
            }

            R.id.ac_show_completed_tasks -> {
                taskAdapter!!.setFilter(ExpandableTodoTaskAdapter.Filter.COMPLETED_TASKS)
                taskAdapter!!.notifyDataSetChanged()
                return true
            }

            R.id.ac_group_by_prio -> {
                checked = !item.isChecked
                item.isChecked = checked
                sortType = SortTypes.PRIORITY
            }

            R.id.ac_sort_by_deadline -> {
                checked = !item.isChecked
                item.isChecked = checked
                sortType = SortTypes.DEADLINE
            }

            else -> return super.onOptionsItemSelected(item)
        }
        if (checked) taskAdapter!!.addSortCondition(sortType) else taskAdapter!!.removeSortCondition(
            sortType
        )
        taskAdapter!!.notifyDataSetChanged()
        return true
    }

    // write new tasks to the database
    fun saveNewTasks() {
        for (i in todoTasks.indices) {
            val currentTask = todoTasks[i]

            // If a dummy list is displayed, its id must not be assigned to the task.
            if (!currentList!!.isDummyList) currentTask!!.listId =
                currentList!!.id // crucial step to not lose the connection to the list
            val dbChanged = containingActivity!!.sendToDatabase(currentTask!!)
            if (dbChanged) containingActivity!!.notifyReminderService(currentTask)

            // write subtasks to the database
            for (subTask in currentTask.subTasks) {
                subTask!!.taskId =
                    currentTask.id.toLong() // crucial step to not lose the connection to the task
                containingActivity!!.sendToDatabase(subTask)
            }
        }
    }

    companion object {
        private val TAG = TodoTasksFragment::class.java.simpleName

        // The fab is used to create new tasks. However, a task can only be created if the user is inside
        // a certain list. If he chose the "show all task" view, the option to create a new task is not available.
        const val SHOW_FLOATING_BUTTON = "SHOW_FAB"
        const val KEY = "fragment_selector_key"
    }
}