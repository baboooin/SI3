package com.babooin.si3

import android.content.Context
import android.content.pm.ApplicationInfo
import android.telephony.cdma.CdmaCellLocation
import android.view.*
import android.widget.BaseAdapter
import android.widget.TextView

class  DocumentsAdapter(context: Context, private val dataSource: ArrayList<CDocuments>) : BaseAdapter() {
    internal class ViewHolder(view: View) {
        var idId: TextView = view.findViewById(R.id.idid) as TextView
        var stat: TextView = view.findViewById(R.id.Status) as TextView
        var num: TextView = view.findViewById(R.id.Number) as TextView
        var date: TextView = view.findViewById(R.id.Date) as TextView
        var supplier: TextView = view.findViewById(R.id.supplier) as TextView

        init {
            view.tag = this
        }
    }


    @Suppress("NAME_SHADOWING")
    public override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var convertView = convertView
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.document_item, parent, false)
            ViewHolder(convertView)
        }
        val holder = convertView!!.tag as ViewHolder
        val item: CDocuments = getItem(position) as CDocuments
        holder.date.text = item.Date
        holder.stat.text = item.Status
        holder.num.text = item.Number
        holder.idId.text = item.id
try {
    holder.supplier.text = item.Supplier
} catch (e: Exception) {
    holder.supplier.text = ""
}

        return convertView
    }
//    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
//        val cItem = getItem(position) as CDocuments
//        val rowView = inflater.inflate(R.layout.document_item, parent, false)
//
//        // Get title
////        val idId = rowView.findViewById(R.id.idid) as TextView
//        idId.text = cItem.id
//
//        val stat = rowView.findViewById(R.id.Status) as TextView
//        stat.text = cItem.Status
//
//        val num = rowView.findViewById(R.id.Number) as TextView
//        num.text = cItem.Number
//
//        val date = rowView.findViewById(R.id.Date) as TextView
//        date.text = cItem.Date
//        val supplier = rowView.findViewById(R.id.supplier) as TextView
//
//        try {
//        supplier.text = cItem.Supplier
//        } catch (e: Exception) {
//            supplier.text = ""
//}
//
//        return rowView
//    }

    override fun getItem(position: Int): Any {
        return dataSource[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return dataSource.size
    }


//    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
//        var convertView = convertView
//        if (convertView == null) {
//            convertView = LayoutInflater.from(ctx).inflate(
//                R.layout.document_item, null
//            )
//            val holder = RecyclerView.ViewHolder()
//            holder.container = convertView
//                .findViewById<View>(R.id.docItemContainer) as RelativeLayout
//            holder.userName = convertView!!.findViewById<View>(R.id.name) as TextView
//            holder.mDetector = GestureDetectorCompat(
//                ctx,
//                MyGestureListener(ctx, convertView)
//            )
//            convertView.setTag(holder)
//        }
//        val holder = convertView!!.tag as RecyclerView.ViewHolder
//        holder.userName.setText(names.get(position))
//        holder.container.setOnTouchListener(OnTouchListener { v, event ->
//            holder.mDetector.onTouchEvent(event)
//            true
//        })
//        return convertView
//    }

    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

}