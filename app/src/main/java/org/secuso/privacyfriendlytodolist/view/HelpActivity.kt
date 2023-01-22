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
import android.preference.PreferenceFragment
import android.view.MenuItem
import android.view.View
import android.widget.ExpandableListView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import org.secuso.privacyfriendlytodolist.R

/**
 * Created by Sebastian Lutz on 15.01.2018
 *
 * Activity that gives the user some help regarding the handling of the app.
 */
class HelpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)
        val expandableListAdapter: ExpandableListAdapter
        val toolbar = findViewById<View>(R.id.toolbar_help) as Toolbar
        val generalExpandableListView =
            findViewById<View>(R.id.generalExpandableListView) as ExpandableListView
        val helpDataDump = HelpDataDump(this)
        val expandableListDetail = helpDataDump.dataGeneral
        val expandableListTitleGeneral: List<String> = ArrayList(expandableListDetail.keys)
        expandableListAdapter =
            ExpandableListAdapter(this, expandableListTitleGeneral, expandableListDetail)
        generalExpandableListView.setAdapter(expandableListAdapter)
        toolbar.setTitle(R.string.help)
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white))
        setSupportActionBar(toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setDisplayShowHomeEnabled(true)
            supportActionBar!!.setHomeAsUpIndicator(R.drawable.arrow)
        }

        //getFragmentManager().beginTransaction().replace(R.id.help_fragment_container, new HelpFragment()).commit();
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

    class HelpFragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.help)
        }
    }
}