package com.babooin.si3

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.babooin.si3.R.id

class  PositionsAdapter (context: Context, private val dataSource: ArrayList<CPositions>, private val type:String) : BaseAdapter() {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val cItem = getItem(position) as CPositions
        val rowView = if (type != "Плановый") inflater.inflate(R.layout.positions_list_item, parent, false) else inflater.inflate(R.layout.positions_list_item_double, parent, false)
        // Get title
        val namePosition = rowView.findViewById(id.name_position) as TextView
        namePosition.text = cItem.view

        val amount = rowView.findViewById(id.amount) as TextView
        amount.text = cItem.amount.toString()

        val barcode = rowView.findViewById(id.barcode) as TextView
        barcode.text = cItem.barcode

        if (type == "Плановый") {
            val plan = rowView.findViewById(id.plan_amount) as TextView
            if (cItem.plan_amount<cItem.amount) {
                rowView.setBackgroundResource(R.color.red)
                namePosition.setTextColor(Color.WHITE)
                barcode.setTextColor(Color.WHITE)
                amount.setTextColor(Color.WHITE)
                plan.setTextColor(Color.WHITE)


            }
            plan.text = cItem.plan_amount.toString()
        }
        return rowView
    }

    override fun getItem(position: Int): Any {
        return dataSource[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return dataSource.size
    }

    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
}