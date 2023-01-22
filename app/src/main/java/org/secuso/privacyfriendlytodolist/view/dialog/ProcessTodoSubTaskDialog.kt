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
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import org.secuso.privacyfriendlytodolist.R
import org.secuso.privacyfriendlytodolist.model.TodoSubTask

/**
 * This class shows a dialog that lets the user create/edit a subtask.
 */
class ProcessTodoSubTaskDialog : FullScreenDialog {
    private var etSubtaskName: EditText? = null
    private var cancelButton: Button? = null
    private var subtask: TodoSubTask
    private var dialogTitleNew: TextView? = null
    private var dialogTitleEdit: TextView? = null

    constructor(context: Context?) : super(context, R.layout.add_subtask_dialog) {
        initGui()
        subtask = TodoSubTask()
        subtask.setCreated()
        //this.subtask.setDbState(DBQueryHandler.ObjectStates.INSERT_TO_DB);
    }

    constructor(context: Context?, subTask: TodoSubTask) : super(
        context,
        R.layout.add_subtask_dialog
    ) {
        initGui()
        subtask = subTask
        subtask.setChanged()
        //this.subtask.setDbState(DBQueryHandler.ObjectStates.UPDATE_DB);
        etSubtaskName!!.setText(subTask.name)
    }

    private fun initGui() {
        etSubtaskName = findViewById<View>(R.id.et_new_subtask_name) as EditText
        val okButton = findViewById<View>(R.id.bt_new_subtask_ok) as Button
        cancelButton = findViewById<View>(R.id.bt_new_subtask_cancel) as Button

        //initialize titles of the dialog
        dialogTitleEdit = findViewById<View>(R.id.dialog_edit_sub) as TextView
        dialogTitleNew = findViewById<View>(R.id.dialog_subtitle) as TextView
        okButton.setOnClickListener {
            val name = etSubtaskName!!.text.toString()
            if (name == "") {
                Toast.makeText(
                    context,
                    context.getString(R.string.todo_name_must_not_be_empty),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                subtask.name = name
                callback!!.finish(subtask)
                dismiss()
            }
        }
        val cancelButton = findViewById<View>(R.id.bt_new_subtask_cancel) as Button
        cancelButton.setOnClickListener { dismiss() }
    }

    fun titleEdit() {
        dialogTitleNew!!.visibility = View.GONE
        dialogTitleEdit!!.visibility = View.VISIBLE
    }
}