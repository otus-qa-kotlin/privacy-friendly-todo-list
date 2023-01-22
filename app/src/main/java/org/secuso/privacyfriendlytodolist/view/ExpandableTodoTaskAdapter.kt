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

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import com.google.android.material.snackbar.Snackbar
import org.secuso.privacyfriendlytodolist.R
import org.secuso.privacyfriendlytodolist.model.BaseTodo
import org.secuso.privacyfriendlytodolist.model.Helper.getDate
import org.secuso.privacyfriendlytodolist.model.Helper.getDeadlineColor
import org.secuso.privacyfriendlytodolist.model.Helper.priority2String
import org.secuso.privacyfriendlytodolist.model.TodoSubTask
import org.secuso.privacyfriendlytodolist.model.TodoTask
import org.secuso.privacyfriendlytodolist.model.Tuple
import org.secuso.privacyfriendlytodolist.model.Tuple.Companion.makePair
import org.secuso.privacyfriendlytodolist.model.database.DBQueryHandler.Companion.saveTodoSubTaskInDb
import org.secuso.privacyfriendlytodolist.model.database.DBQueryHandler.Companion.saveTodoTaskInDb
import org.secuso.privacyfriendlytodolist.model.database.DatabaseHelper.Companion.getInstance
import org.secuso.privacyfriendlytodolist.view.dialog.ProcessTodoSubTaskDialog
import java.util.Collections

/**
 * Created by Sebastian Lutz on 06.03.2018
 *
 * This class manages the To-Do task expandableList items.
 */
class ExpandableTodoTaskAdapter(// OTHERS
    private val context: Context, tasks: ArrayList<TodoTask>
) : BaseExpandableListAdapter() {
    private val prefs: SharedPreferences

    // left item: task that was long clicked
    // right item: subtask that was long clicked
    var longClickedTodo: Tuple<TodoTask?, TodoSubTask?>? = null
        private set

    enum class Filter {
        ALL_TASKS, COMPLETED_TASKS, OPEN_TASKS
    }

    enum class SortTypes(val value: Int) {
        PRIORITY(0x1), DEADLINE(0x2);

    }

    // FILTER AND SORTING OPTIONS MADE BY THE USER
    private var filterMeasure: Filter? = null
    private var queryString: String? = null
    private var sortType =
        0 // encodes sorting (1. bit high -> sort by priority, 2. bit high --> sort by deadline)

    // DATA TO DISPLAY
    private val rawData // data from database in original order
            : ArrayList<TodoTask>
    private val filteredTasks = ArrayList<TodoTask>() // data after filtering process
    private val prioBarPositions = HashMap<TodoTask.Priority?, Int>()

    // Normally the toolbar title contains the list name. However, it all tasks are displayed in a dummy list it is not obvious to what list a tasks belongs. This missing information is then added to each task in an additional text view.
    private var showListName = false

    init {
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
        rawData = tasks
        val filterString = prefs.getString("FILTER", "ALL_TASKS")
        val filter: Filter
        filter = try {
            Filter.valueOf(
                filterString!!
            )
        } catch (e: IllegalArgumentException) {
            Filter.ALL_TASKS
        }

        // default values
        if (prefs.getBoolean("PRIORITY", false)) {
            addSortCondition(SortTypes.PRIORITY)
        }
        if (prefs.getBoolean("DEADLINE", false)) {
            addSortCondition(SortTypes.DEADLINE)
        }
        setFilter(filter)
        setQueryString(null)
        filterTasks()
    }

    fun setLongClickedTaskByPos(position: Int) {
        longClickedTodo = makePair(getTaskByPosition(position), null)
    }

    fun setListNames(flag: Boolean) {
        showListName = flag
    }

    fun setLongClickedSubTaskByPos(groupPosition: Int, childPosition: Int) {
        val task = getTaskByPosition(groupPosition)
        if (task != null) {
            val subTask = task.subTasks[childPosition - 1]!!
            longClickedTodo = makePair(task, subTask)
        }
    }

    // interface to outer world
    fun setFilter(filter: Filter?) {
        filterMeasure = filter
    }

    fun setQueryString(query: String?) {
        queryString = query
    }

    /**
     * Sets the n-th bit of [ExpandableTodoTaskAdapter.sortType] whereas n is the value of {@param type}
     * After having changed the sorting conditions, you must call [ExpandableTodoTaskAdapter.sortTasks]
     *
     * @param type condition by what tasks will be sorted (one-hot encoding)
     */
    fun addSortCondition(type: SortTypes) {
        sortType = sortType or type.value // set n-th bit
    }

    /**
     * Sets the n-th bit of [ExpandableTodoTaskAdapter.sortType] whereas n is the value of {@param type}
     * After having changed the sorting conditions, you must call [ExpandableTodoTaskAdapter.sortTasks]
     *
     * @param type condition by what tasks will be sorted (one-hot encoding)
     */
    fun removeSortCondition(type: SortTypes) {
        sortType = sortType and (1 shl type.value - 1).inv()
    }

    /**
     * filter tasks by "done" criterion (show "all", only "open" or only "completed" tasks)
     * If the user changes the filter, it is crucial to call "sortTasks" again.
     */
    private fun filterTasks() {
        filteredTasks.clear()
        val notOpen = filterMeasure != Filter.OPEN_TASKS
        val notCompleted = filterMeasure != Filter.COMPLETED_TASKS
        for (task in rawData) if (notOpen && task.done || notCompleted && !task.done) if (task.checkQueryMatch(
                queryString
            )
        ) filteredTasks.add(task)

        // Call this method even if sorting is disabled. In the case of enabled sorting, all
        // sorting patterns are automatically employed after having changed the filter on tasks.
        sortTasks()
    }

    private val isPriorityGroupingEnabled: Boolean
        private get() = sortType and SortTypes.PRIORITY.value == 1

    /**
     * Sort tasks by selected criteria (priority and/or deadline)
     * This method works on [ExpandableTodoTaskAdapter.filteredTasks]. For that reason it is
     * important to keep [ExpandableTodoTaskAdapter.filteredTasks] up-to-date.
     */
    fun sortTasks() {
        val prioSorting = isPriorityGroupingEnabled
        val deadlineSorting = sortType and SortTypes.DEADLINE.value != 0
        Collections.sort(filteredTasks, object : Comparator<TodoTask> {
            private fun compareDeadlines(d1: Long, d2: Long): Int {
                // tasks with deadlines always first
                if (d1 == -1L && d2 == -1L) return 0
                if (d1 == -1L) return 1
                if (d2 == -1L) return -1
                if (d1 < d2) return -1
                return if (d1 == d2) 0 else 1
            }

            override fun compare(t1: TodoTask, t2: TodoTask): Int {
                return if (prioSorting) {
                    val p1 = t1.priority
                    val p2 = t2.priority
                    val comp = p1!!.compareTo(p2!!)
                    if (comp == 0 && deadlineSorting) {
                        compareDeadlines(t1.deadline, t2.deadline)
                    } else comp
                } else if (deadlineSorting) {
                    compareDeadlines(t1.deadline, t2.deadline)
                } else t1.listPosition - t2.listPosition
            }
        })
        if (prioSorting) countTasksPerPriority()
    }
    // count how many tasks belong to each priority group (tasks are now sorted by priority)
    /**
     * If [ExpandableTodoTaskAdapter.sortTasks] sorted by the priority, this method must be
     * called. It computes the position of the dividing bars between the priority ranges. These
     * positions are necessary to distinguish of what group type the current row is.
     */
    private fun countTasksPerPriority() {
        prioBarPositions.clear()
        if (filteredTasks.size != 0) {
            var pos = 0
            var currentPrio: TodoTask.Priority
            val prioAlreadySeen = HashSet<TodoTask.Priority?>()
            for (task in filteredTasks) {
                currentPrio = task.priority!!
                if (!prioAlreadySeen.contains(currentPrio)) {
                    prioAlreadySeen.add(currentPrio)
                    prioBarPositions[currentPrio] = pos
                    pos++ // skip the current prio-line
                }
                pos++
            }
        }
    }

    /***
     * @param groupPosition position of current row. For that reason the offset to the task must be
     * computed taking into account all preceding dividing priority bars
     * @return null if there is no task at @param groupPosition (but a divider row) or the wanted task
     */
    private fun getTaskByPosition(groupPosition: Int): TodoTask? {
        var seenPrioBars = 0
        if (isPriorityGroupingEnabled) {
            for (priority in TodoTask.Priority.values()) {
                if (prioBarPositions.containsKey(priority)) {
                    if (groupPosition < prioBarPositions[priority]!!) break
                    seenPrioBars++
                }
            }
        }
        val pos = groupPosition - seenPrioBars
        return if (pos < filteredTasks.size && pos >= 0) filteredTasks[pos] else null
        // should never be the case
    }

    override fun getGroupCount(): Int {
        return if (isPriorityGroupingEnabled) filteredTasks.size + prioBarPositions.size else filteredTasks.size
    }

    override fun getChildrenCount(groupPosition: Int): Int {
        val task = getTaskByPosition(groupPosition) ?: return 0
        return task.subTasks.size + 2
    }

    override fun getGroupType(groupPosition: Int): Int {
        return if (isPriorityGroupingEnabled && prioBarPositions.values.contains(groupPosition)) GR_PRIO_ROW else GR_TASK_ROW
    }

    override fun getGroupTypeCount(): Int {
        return 2
    }

    override fun getChildType(groupPosition: Int, childPosition: Int): Int {
        if (childPosition == 0) return CH_TASK_DESCRIPTION_ROW else if (childPosition == getTaskByPosition(
                groupPosition
            )!!.subTasks.size + 1
        ) return CH_SETTING_ROW
        return CH_SUBTASK_ROW
    }

    override fun getChildTypeCount(): Int {
        return 3
    }

    override fun getGroup(groupPosition: Int): Any {
        return filteredTasks[groupPosition]
    }

    override fun getChild(groupPosition: Int, childPosition: Int): Any {
        return childPosition
    }

    override fun getGroupId(groupPosition: Int): Long {
        return groupPosition.toLong()
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long {
        return childPosition.toLong()
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    private fun getPriorityNameByBarPos(groupPosition: Int): String {
        for ((key, value) in prioBarPositions) {
            if (value == groupPosition) {
                return priority2String(context, key)
            }
        }
        return context.getString(R.string.unknown_priority)
    }

    override fun notifyDataSetChanged() {
        filterTasks()
        super.notifyDataSetChanged()
    }

    private val defaultReminderTime: Long
        private get() = prefs.getString(
            Settings.DEFAULT_REMINDER_TIME_KEY,
            context.resources.getInteger(R.integer.one_day).toString()
        )?.toLongOrNull() ?: 0

    override fun getGroupView(
        groupPosition: Int,
        isExpanded: Boolean,
        convertView: View?,
        parent: ViewGroup
    ): View {
        var cView = convertView
        val type = getGroupType(groupPosition)
        when (type) {
            GR_PRIO_ROW -> {
                val vh1: GroupPrioViewHolder
                if (cView == null) {
                    cView =
                        LayoutInflater.from(context).inflate(R.layout.exlv_prio_bar, parent, false)
                    vh1 = GroupPrioViewHolder()
                    vh1.prioFlag =
                        cView.findViewById<View>(R.id.tv_exlv_priority_bar) as TextView
                    cView.tag = vh1
                } else {
                    vh1 = cView.tag as GroupPrioViewHolder
                }
                vh1.prioFlag!!.text = getPriorityNameByBarPos(groupPosition)
                cView?.isClickable = true
            }

            GR_TASK_ROW -> {
                val currentTask = getTaskByPosition(groupPosition)
                val vh2: GroupTaskViewHolder
                if (cView == null) {
                    cView = LayoutInflater.from(context)
                        .inflate(R.layout.exlv_tasks_group, parent, false)
                    vh2 = GroupTaskViewHolder()
                    vh2.name = cView.findViewById<View>(R.id.tv_exlv_task_name) as TextView
                    vh2.done = cView.findViewById<View>(R.id.cb_task_done) as CheckBox
                    vh2.deadline =
                        cView.findViewById<View>(R.id.tv_exlv_task_deadline) as TextView
                    vh2.listName =
                        cView.findViewById<View>(R.id.tv_exlv_task_list_name) as TextView
                    vh2.progressBar =
                        cView.findViewById<View>(R.id.pb_task_progress) as ProgressBar
                    vh2.seperator = cView.findViewById(R.id.v_exlv_header_separator)
                    vh2.deadlineColorBar = cView.findViewById(R.id.v_urgency_task)
                    vh2.done!!.tag = currentTask!!.id
                    vh2.done!!.isChecked = currentTask.done
                    cView.tag = vh2
                } else {
                    vh2 = cView.tag as GroupTaskViewHolder
                }
                vh2.name!!.text = currentTask!!.name
                getProgressDone(currentTask, hasAutoProgress())
                vh2.progressBar!!.progress = currentTask.progress
                val deadline: String
                deadline =
                    if (currentTask.deadline <= 0) context.resources.getString(R.string.no_deadline) else context.resources.getString(
                        R.string.deadline_dd
                    ) + " " + getDate(
                        currentTask.deadline
                    )
                if (showListName) {
                    vh2.listName!!.visibility = View.VISIBLE
                    vh2.listName!!.text = currentTask.listName
                } else {
                    vh2.listName!!.visibility = View.GONE
                }
                vh2.deadline!!.text = deadline
                vh2.deadlineColorBar!!.setBackgroundColor(
                    getDeadlineColor(
                        context, currentTask.getDeadlineColor(
                            defaultReminderTime
                        )
                    )
                )
                vh2.done!!.isChecked = currentTask.done
                vh2.done!!.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (buttonView.isPressed) {
                        val snackbar =
                            Snackbar.make(buttonView, R.string.snack_check, Snackbar.LENGTH_LONG)
                        snackbar.setAction(R.string.snack_undo) {
                            if (isChecked) {
                                buttonView.isChecked = false
                                currentTask.done = buttonView.isChecked
                                currentTask.setAllSubTasksDone(false)
                                getProgressDone(currentTask, hasAutoProgress())
                                currentTask.setChanged()
                                notifyDataSetChanged()
                                saveTodoTaskInDb(
                                    getInstance(
                                        context
                                    )!!.writableDatabase, currentTask
                                )
                                for (st in currentTask.subTasks) {
                                    st!!.done = false
                                    saveTodoSubTaskInDb(
                                        getInstance(
                                            context
                                        )!!.writableDatabase, st
                                    )
                                }
                            } else {
                                buttonView.isChecked = true
                                currentTask.done = buttonView.isChecked
                                currentTask.setAllSubTasksDone(true)
                                getProgressDone(currentTask, hasAutoProgress())
                                currentTask.setChanged()
                                notifyDataSetChanged()
                                saveTodoTaskInDb(
                                    getInstance(
                                        context
                                    )!!.writableDatabase, currentTask
                                )
                                for (st in currentTask.subTasks) {
                                    st!!.done = true
                                    saveTodoSubTaskInDb(
                                        getInstance(
                                            context
                                        )!!.writableDatabase, st
                                    )
                                }
                            }
                        }
                        snackbar.show()
                        currentTask.done = buttonView.isChecked
                        currentTask.setAllSubTasksDone(buttonView.isChecked)
                        getProgressDone(currentTask, hasAutoProgress())
                        currentTask.setChanged()
                        notifyDataSetChanged()
                        saveTodoTaskInDb(getInstance(context)!!.writableDatabase, currentTask)
                        var i = 0
                        while (i < currentTask.subTasks.size) {
                            currentTask.subTasks[i]!!.setChanged()
                            notifyDataSetChanged()
                            i++
                        }
                    }
                }
            }

            else -> {}
        }
        return cView!!
    }

    override fun getChildView(
        groupPosition: Int,
        childPosition: Int,
        isLastChild: Boolean,
        convertView: View?,
        parent: ViewGroup
    ): View {
        var cView = convertView
        val type = getChildType(groupPosition, childPosition)
        val currentTask = getTaskByPosition(groupPosition)
        when (type) {
            CH_TASK_DESCRIPTION_ROW -> {
                var vh1 = TaskDescriptionViewHolder()
                if (cView == null) {
                    cView = LayoutInflater.from(parent.context)
                        .inflate(R.layout.exlv_task_description_row, parent, false)
                    vh1.taskDescription =
                        cView.findViewById<View>(R.id.tv_exlv_task_description) as TextView
                    vh1.deadlineColorBar =
                        cView.findViewById(R.id.v_task_description_deadline_color_bar)
                    cView.tag = vh1
                } else {
                    vh1 = cView.tag as TaskDescriptionViewHolder
                }
                val description = currentTask!!.description
                if (description != null && description != "") {
                    vh1.taskDescription!!.visibility = View.VISIBLE
                    vh1.taskDescription!!.text = description
                } else {
                    vh1.taskDescription!!.visibility = View.GONE
                    // vh1.taskDescription.setText("KEINE BESCHREIBUNG"); //context.getString(R.string.no_task_description));
                }
                vh1.deadlineColorBar!!.setBackgroundColor(
                    getDeadlineColor(
                        context, currentTask.getDeadlineColor(
                            defaultReminderTime
                        )
                    )
                )
            }

            CH_SETTING_ROW -> {
                var vh2 = SettingViewHolder()
                if (cView == null) {
                    cView = LayoutInflater.from(parent.context)
                        .inflate(R.layout.exlv_setting_row, parent, false)
                    //vh2.addSubTaskButton = (ImageView) cView.findViewById(R.id.iv_add_subtask);
                    vh2.addSubTaskButton =
                        cView.findViewById<View>(R.id.rl_add_subtask) as RelativeLayout
                    vh2.deadlineColorBar =
                        cView.findViewById(R.id.v_setting_deadline_color_bar)
                    cView.tag = vh2
                    if (currentTask!!.isInTrash) cView.visibility = View.GONE
                } else {
                    vh2 = cView.tag as SettingViewHolder
                }
                vh2.addSubTaskButton!!.setOnClickListener {
                    val dialog = ProcessTodoSubTaskDialog(context)
                    dialog.setDialogResult(object : TodoCallback {
                        override fun finish(b: BaseTodo?) {
                            if (b is TodoSubTask) {
                                val newSubTask = b
                                currentTask!!.subTasks.add(newSubTask)
                                newSubTask.taskId = currentTask.id.toLong()
                                saveTodoSubTaskInDb(
                                    getInstance(
                                        context
                                    )!!.writableDatabase, newSubTask
                                )
                                notifyDataSetChanged()
                            }
                        }
                    })
                    dialog.show()
                }
                vh2.deadlineColorBar!!.setBackgroundColor(
                    getDeadlineColor(
                        context, currentTask!!.getDeadlineColor(
                            defaultReminderTime
                        )
                    )
                )
            }

            else -> {
                val currentSubTask = currentTask!!.subTasks[childPosition - 1]!!
                var vh3 = SubTaskViewHolder()
                if (cView == null) {
                    cView = LayoutInflater.from(parent.context)
                        .inflate(R.layout.exlv_subtask_row, parent, false)
                    vh3.subtaskName =
                        cView.findViewById<View>(R.id.tv_subtask_name) as TextView
                    vh3.deadlineColorBar =
                        cView.findViewById(R.id.v_subtask_deadline_color_bar)
                    vh3.done = cView.findViewById<View>(R.id.cb_subtask_done) as CheckBox
                    cView.tag = vh3
                } else {
                    vh3 = cView.tag as SubTaskViewHolder
                }
                vh3.done!!.isChecked = currentSubTask.done
                vh3.done!!.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (buttonView.isPressed) {
                        currentSubTask.done = buttonView.isChecked
                        currentTask.doneStatusChanged() // check if entire task is now (when all subtasks are done)
                        currentSubTask.setChanged()
                        saveTodoSubTaskInDb(
                            getInstance(context)!!.writableDatabase, currentSubTask
                        )
                        getProgressDone(currentTask, hasAutoProgress())
                        saveTodoTaskInDb(getInstance(context)!!.writableDatabase, currentTask)
                        notifyDataSetChanged()
                    }
                }
                vh3.subtaskName!!.text = currentSubTask.name
                vh3.deadlineColorBar!!.setBackgroundColor(
                    getDeadlineColor(
                        context, currentTask.getDeadlineColor(
                            defaultReminderTime
                        )
                    )
                )
            }
        }
        return cView!!
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
        return childPosition > 0 && childPosition < getTaskByPosition(groupPosition)!!.subTasks.size + 1
    }

    fun getProgressDone(t: TodoTask?, autoProgress: Boolean) {
        if (autoProgress) {
            var progress: Int
            var help = 0
            val subs = t!!.subTasks
            for (st in subs) {
                if (st!!.done) {
                    help++
                }
            }
            val computedProgress = help.toDouble() / t.subTasks.size.toDouble() * 100
            progress = computedProgress.toInt()
            t.progress = progress
        } else t!!.progress = t.progress
    }

    inner class GroupTaskViewHolder {
        var name: TextView? = null
        var deadline: TextView? = null
        var listName: TextView? = null
        var done: CheckBox? = null
        var deadlineColorBar: View? = null
        @JvmField
        var seperator: View? = null
        var progressBar: ProgressBar? = null
    }

    inner class GroupPrioViewHolder {
        var prioFlag: TextView? = null
    }

    private inner class SubTaskViewHolder {
        var subtaskName: TextView? = null
        var done: CheckBox? = null
        var deadlineColorBar: View? = null
    }

    private inner class TaskDescriptionViewHolder {
        var taskDescription: TextView? = null
        var deadlineColorBar: View? = null
    }

    private inner class SettingViewHolder {
        var addSubTaskButton: RelativeLayout? = null
        var deadlineColorBar: View? = null
    }

    private fun hasAutoProgress(): Boolean {
        //automatic-progress enabled?
        return if (!PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean("pref_progress", false)
        ) false else true
    }

    companion object {
        // ROW TYPES FOR USED TO CREATE DIFFERENT VIEWS DEPENDING ON ITEM TO SHOW
        private const val GR_TASK_ROW = 0 // gr == group type
        private const val GR_PRIO_ROW = 1
        private const val CH_TASK_DESCRIPTION_ROW = 0 // ch == child type
        private const val CH_SETTING_ROW = 1
        private const val CH_SUBTASK_ROW = 2
    }
}