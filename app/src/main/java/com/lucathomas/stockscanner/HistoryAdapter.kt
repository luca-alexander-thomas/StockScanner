package com.lucathomas.stockscanner

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter(context: Context, private val dataList: List<Data>) :
    ArrayAdapter<Data>(context, R.layout.item_history, dataList) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_history, parent, false)
        val data = dataList[position]

        val ivProductThumb = view.findViewById<ImageView>(R.id.ivProductThumb)
        val tvName = view.findViewById<TextView>(R.id.tvHistoryName)
        val tvBrand = view.findViewById<TextView>(R.id.tvHistoryBrand)
        val tvDate = view.findViewById<TextView>(R.id.tvHistoryDate)
        val tvNutri = view.findViewById<TextView>(R.id.tvHistoryNutri)

        tvName.text = data.name
        tvBrand.text = data.brandName
        
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        tvDate.text = "Gescann am: ${sdf.format(Date(data.timestamp))}"

        Glide.with(context)
            .load(data.image)
            .placeholder(android.R.drawable.ic_menu_report_image)
            .into(ivProductThumb)

        val score = data.nutriScore.lowercase()
        if (score.isNotEmpty() && score != "unknown") {
            tvNutri.visibility = View.VISIBLE
            tvNutri.text = score.uppercase()
            
            val colorRes = when (score) {
                "a" -> R.color.nutriGreen
                "b" -> R.color.nutriLightGreen
                "c" -> R.color.nutriYellow
                "d" -> R.color.nutriOrange
                "e" -> R.color.nutriRed
                else -> android.R.color.darker_gray
            }
            
            val background = tvNutri.background as GradientDrawable
            background.setColor(ContextCompat.getColor(context, colorRes))
        } else {
            tvNutri.visibility = View.INVISIBLE
        }

        return view
    }
}