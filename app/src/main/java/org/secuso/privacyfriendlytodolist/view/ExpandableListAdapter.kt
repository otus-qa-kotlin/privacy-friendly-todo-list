package org.secuso.privacyfriendlytodolist.view

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.TextView
import org.secuso.privacyfriendlytodolist.R

/**
 * Created by Sebastian Lutz on 28.02.2018.
 *
 * Helper for HelpActivity, creating a FAQ-style layout
 */
class ExpandableListAdapter(
    private val context: Context, private val expandableListTitle: List<String>,
    private val expandableListDetail: HashMap<String, List<String>>
) : BaseExpandableListAdapter() {
    override fun getChild(listPosition: Int, expandedListPosition: Int): Any {
        return expandableListDetail[expandableListTitle[listPosition]]!![expandedListPosition]
    }

    override fun getChildId(listPosition: Int, expandedListPosition: Int): Long {
        return expandedListPosition.toLong()
    }

    override fun getChildView(
        listPosition: Int, expandedListPosition: Int,
        isLastChild: Boolean, convertView: View?, parent: ViewGroup
    ): View {
        var cView = convertView
        val expandedListText = getChild(listPosition, expandedListPosition) as String
        if (cView == null) {
            val layoutInflater = context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            cView = layoutInflater.inflate(R.layout.list_item, null)
        }
        val expandedListTextView = cView?.findViewById<View>(R.id.expandedListItem) as? TextView
        expandedListTextView?.text = expandedListText
        return cView!!
    }

    override fun getChildrenCount(listPosition: Int): Int {
        return expandableListDetail[expandableListTitle[listPosition]]!!.size
    }

    override fun getGroup(listPosition: Int): Any {
        return expandableListTitle[listPosition]
    }

    override fun getGroupCount(): Int {
        return expandableListTitle.size
    }

    override fun getGroupId(listPosition: Int): Long {
        return listPosition.toLong()
    }

    override fun getGroupView(
        listPosition: Int, isExpanded: Boolean,
        convertView: View?, parent: ViewGroup
    ): View {
        var cView = convertView
        val listTitle = getGroup(listPosition) as String
        if (cView == null) {
            val layoutInflater =
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            cView = layoutInflater.inflate(R.layout.list_group, null)
        }
        val listTitleTextView = cView?.findViewById<View>(R.id.listTitle) as? TextView?
        listTitleTextView?.setTypeface(null, Typeface.BOLD)
        listTitleTextView?.text = listTitle
        return cView!!
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    override fun isChildSelectable(listPosition: Int, expandedListPosition: Int): Boolean {
        return true
    }
}