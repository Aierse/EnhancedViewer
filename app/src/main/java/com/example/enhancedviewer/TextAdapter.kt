package com.example.enhancedviewer

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TextAdapter(private val context: Context, private val textHeight: Int) : RecyclerView.Adapter<TextAdapter.ViewHolder>() {

    lateinit var data: ArrayList<String>

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.recycler_item,parent,false)

        return ViewHolder(view)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position == data.lastIndex) {
            val params = holder.itemView.layoutParams as RecyclerView.LayoutParams
            params.bottomMargin = textHeight
            holder.itemView.layoutParams = params
        }
        else {
            val params = holder.itemView.layoutParams as RecyclerView.LayoutParams
            params.bottomMargin = 0
            holder.itemView.layoutParams = params
        }

        holder.bind(data[position])
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val text: TextView = itemView.findViewById(R.id.textView)

        fun bind(item: String) {
            text.text = item
        }
    }
}