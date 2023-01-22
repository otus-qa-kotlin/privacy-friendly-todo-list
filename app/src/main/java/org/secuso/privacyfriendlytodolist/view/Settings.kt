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
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.preference.EditTextPreference
import android.preference.ListPreference
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.PreferenceGroup
import android.preference.SwitchPreference
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import org.secuso.privacyfriendlytodolist.R
import org.secuso.privacyfriendlytodolist.view.MainActivity

/**
 * Created by Sebastian Lutz on 15.03.2018
 *
 *
 * Activity that can enable/disable particular functionalities.
 */
class Settings : AppCompatActivity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings)
        val toolbar = findViewById<View>(R.id.toolbarr) as Toolbar
        setSupportActionBar(toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setDisplayShowHomeEnabled(true)
            //final Drawable upArrow = ContextCompat.getDrawable(this, R.drawable.abc_ic_ab_back_mtrl_am_alpha);
            //upArrow.setColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.SRC_ATOP);
            //getSupportActionBar().setHomeAsUpIndicator(upArrow);
        }
        fragmentManager.beginTransaction().replace(R.id.fragment_container, MyPreferenceFragment())
            .commit()
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

    class MyPreferenceFragment : PreferenceFragment(), OnSharedPreferenceChangeListener {
        var ignoreChanges = false
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.settings)

            // initializes
            initSummary(preferenceScreen)
        }

        private fun initSummary(p: Preference) {
            if (p is PreferenceGroup) {
                val pGrp = p
                for (i in 0 until pGrp.preferenceCount) {
                    initSummary(pGrp.getPreference(i))
                }
            } else {
                updatePrefSummary(p)
            }
        }

        private fun updatePrefSummary(p: Preference) {
            if (p is ListPreference) {
                p.setSummary(p.entry)
            }
        }

        override fun onResume() {
            super.onResume()
            preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onPause() {
            // uncheck pin if pin is invalid
            val sharedPreferences = preferenceManager.sharedPreferences
            val pinEnabled = sharedPreferences.getBoolean("pref_pin_enabled", false)
            if (pinEnabled) {
                val pin = sharedPreferences.getString("pref_pin", null)
                if (pin == null || pin.length < 4) {
                    // pin invalid: uncheck
                    ignoreChanges = true
                    (findPreference("pref_pin_enabled") as SwitchPreference).isChecked = false
                    ignoreChanges = false
                }
            }
            preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
            super.onPause()
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
            if (!ignoreChanges) {
                if (key == "pref_pin") {
                    val pin = sharedPreferences.getString(key, null)
                    if (pin != null) {
                        if (pin.length < 4) {
                            ignoreChanges = true
                            (findPreference("pref_pin") as EditTextPreference).text = ""
                            ignoreChanges = false
                            Toast.makeText(
                                activity,
                                getString(R.string.invalid_pin),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } else if (key == "pref_pin_enabled") {
                    val pinEnabled = sharedPreferences.getBoolean("pref_pin_enabled", false)
                    if (pinEnabled) {
                        ignoreChanges = true
                        (findPreference("pref_pin") as EditTextPreference).text = ""
                        ignoreChanges = false
                    }
                }
            }
            updatePrefSummary(findPreference(key))
        }
    }

    override fun onBackPressed() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        super.onBackPressed()
    }

    companion object {
        private val TAG = Settings::class.java.simpleName
        const val DEFAULT_REMINDER_TIME_KEY = "pref_default_reminder_time"
    }
}