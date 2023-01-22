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
import android.content.DialogInterface
import android.preference.PreferenceManager
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import org.secuso.privacyfriendlytodolist.R

class PinDialog(context: Context?) : FullScreenDialog(context, R.layout.pin_dialog) {
    var pinCallback: PinCallback? = null
    private var wrongCounter = 0
    private var disallowReset = false

    init {
        val buttonOkay = findViewById<View>(R.id.bt_pin_ok) as Button
        buttonOkay.setOnClickListener {
            val pinExpected =
                PreferenceManager.getDefaultSharedPreferences(context).getString("pref_pin", "")
            val textEditPin = findViewById<View>(R.id.et_pin_pin) as EditText
            if (pinExpected == textEditPin.text.toString()) {
                pinCallback!!.accepted()
                setOnDismissListener(null)
                dismiss()
            } else {
                wrongCounter++
                Toast.makeText(
                    context,
                    context!!.resources.getString(R.string.wrong_pin),
                    Toast.LENGTH_SHORT
                ).show()
                textEditPin.setText("")
                textEditPin.isActivated = true
                if (wrongCounter >= 3 && !disallowReset) {
                    val buttonResetApp = findViewById<View>(R.id.bt_reset_application) as Button
                    buttonResetApp.visibility = View.VISIBLE
                }
            }
        }
        val buttonResetApp = findViewById<View>(R.id.bt_reset_application) as Button
        buttonResetApp.setOnClickListener {
            val resetDialogListener = DialogInterface.OnClickListener { dialog, which ->
                if (which == BUTTON_POSITIVE) {
                    pinCallback!!.resetApp()
                }
            }
            val builder = AlertDialog.Builder(context!!)
            builder.setMessage(context.resources.getString(R.string.reset_application_msg))
            builder.setPositiveButton(
                context.resources.getString(R.string.yes),
                resetDialogListener
            )
            builder.setNegativeButton(context.resources.getString(R.string.no), resetDialogListener)
            builder.show()
        }
        val buttonNoDeadline = findViewById<View>(R.id.bt_pin_cancel) as Button
        buttonNoDeadline.setOnClickListener {
            pinCallback!!.declined()
            dismiss()
        }
        setOnDismissListener { pinCallback!!.declined() }
        val textEditPin = findViewById<View>(R.id.et_pin_pin) as EditText
        textEditPin.isActivated = true
    }

    interface PinCallback {
        fun accepted()
        fun declined()
        fun resetApp()
    }

    fun setDisallowReset(disallow: Boolean) {
        disallowReset = disallow
    }
}