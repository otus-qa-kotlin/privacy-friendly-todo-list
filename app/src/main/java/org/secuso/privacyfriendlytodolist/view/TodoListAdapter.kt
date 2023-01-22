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

import android.app.Activity
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnCreateContextMenuListener
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.secuso.privacyfriendlytodolist.R
import org.secuso.privacyfriendlytodolist.model.Helper.getDate
import org.secuso.privacyfriendlytodolist.model.Helper.getDeadlineColor
import org.secuso.privacyfriendlytodolist.model.Helper.getMenuHeader
import org.secuso.privacyfriendlytodolist.model.TodoList

class TodoListAdapter(ac: Activity, data: ArrayList<TodoList>?) :
    RecyclerView.Adapter<TodoListAdapter.ViewHolder>() {
    private val contextActivity: MainActivity
    private val prefs: SharedPreferences
    private var allLists: ArrayList<TodoList>? = null
    private var queryString: String? = null
    private var filteredLists: ArrayList<TodoList>? = null
    var position = 0

    init {
        updateList(data)
        contextActivity = ac as MainActivity
        prefs = PreferenceManager.getDefaultSharedPreferences(ac)
    }

    private val defaultReminderTime: Long
        private get() = prefs.getString(
            Settings.DEFAULT_REMINDER_TIME_KEY,
            contextActivity.resources.getInteger(R.integer.one_day).toString()
        )?.toLongOrNull() ?: 0

    // invoked by the layout manager
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.todo_list_entry, parent, false)
        return ViewHolder(v)
    }

    // replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: ViewHolder, pos: Int) {
        val list = filteredLists!![filteredLists!!.size - 1 - pos]
        holder.title.text = list.name
        if (list.nextDeadline <= 0) holder.deadline.text =
            contextActivity.resources.getString(R.string.no_next_deadline) else holder.deadline.text =
            contextActivity.resources.getString(
                R.string.next_deadline_dd,
                getDate(list.nextDeadline)
            )
        holder.done.text = String.format("%d/%d", list.doneTodos, list.size)
        holder.urgency.setBackgroundColor(
            getDeadlineColor(
                contextActivity, list.getDeadlineColor(
                    defaultReminderTime
                )
            )
        )
        holder.itemView.setOnLongClickListener {
            position = holder.adapterPosition
            false
        }
    }

    override fun getItemCount(): Int {
        return filteredLists!!.size
    }

    override fun onViewRecycled(holder: ViewHolder) {
        holder.itemView.setOnLongClickListener(null)
        super.onViewRecycled(holder)
    }

    fun updateList(todoLists: ArrayList<TodoList>?) {
        allLists = todoLists
        applyFilter()
    }

    fun setQueryString(query: String?) {
        queryString = query
        applyFilter()
    }

    private fun applyFilter() {
        filteredLists = ArrayList(allLists!!.size)
        for (i in allLists!!.indices) {
            if (allLists!![i].checkQueryMatch(queryString)) {
                filteredLists!!.add(allLists!![i])
            }
        }
    }

    fun getToDoListFromPosition(index: Int): TodoList? {
        return if (index < 0 || index >= filteredLists!!.size) null else filteredLists!![filteredLists!!.size - index - 1]
    }

    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v), View.OnClickListener,
        OnCreateContextMenuListener {
        var title: TextView
        var deadline: TextView
        var done: TextView
        var urgency: View

        init {
            title = v.findViewById<View>(R.id.tv_todo_list_title) as TextView
            deadline = v.findViewById<View>(R.id.tv_todo_list_next_deadline) as TextView
            done = v.findViewById<View>(R.id.tv_todo_list_status) as TextView
            urgency = v.findViewById(R.id.v_urgency_indicator)
            v.setOnClickListener(this)
            v.setOnCreateContextMenuListener(this)
        }

        override fun onClick(v: View) {
            val bundle = Bundle()

            // It is important to save the clicked list, because it is possible that it was not yet written to the database and thus cannot be identified by its id.
            contextActivity.clickedList =
                filteredLists!![filteredLists!!.size - 1 - adapterPosition]
            bundle.putBoolean(TodoTasksFragment.SHOW_FLOATING_BUTTON, true)
            val fragment = TodoTasksFragment()
            fragment.arguments = bundle

            //contextActivity.setFragment(fragment);
        }

        override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo) {
            //TODO ask touchlistener for swipe action
            menu.setHeaderView(
                getMenuHeader(
                    contextActivity,
                    contextActivity.getString(R.string.select_option)
                )
            )
            val inflater = contextActivity.menuInflater
            inflater.inflate(R.menu.todo_list_long_click, menu)
        }
    }

    companion object {
        private val TAG = TodoListAdapter::class.java.simpleName
    }
}