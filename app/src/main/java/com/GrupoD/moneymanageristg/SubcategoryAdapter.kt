package com.GrupoD.moneymanageristg

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SubcategoryAdapter(
    private var subcategories: List<Subcategory>,
    private val listener: SubcategoryActionListener
) : RecyclerView.Adapter<SubcategoryAdapter.SubcategoryViewHolder>() {

    interface SubcategoryActionListener {
        fun onEditSubcategory(subId: Long, name: String)
        fun onDeleteSubcategory(subId: Long)
    }

    inner class SubcategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvSubcategoryName: TextView = itemView.findViewById(R.id.tv_subcategory_name)
        val btnEditSubcategory: ImageButton = itemView.findViewById(R.id.btn_edit_subcategory)
        val btnDeleteSubcategory: ImageButton = itemView.findViewById(R.id.btn_delete_subcategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubcategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_subcategory, parent, false)
        return SubcategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubcategoryViewHolder, position: Int) {
        val sub = subcategories[position]
        holder.tvSubcategoryName.text = sub.nombre

        holder.btnEditSubcategory.setOnClickListener {
            listener.onEditSubcategory(sub.id, sub.nombre)
        }

        holder.btnDeleteSubcategory.setOnClickListener {
            listener.onDeleteSubcategory(sub.id)
        }
    }

    override fun getItemCount(): Int = subcategories.size

    fun updateData(newSubs: List<Subcategory>) {
        subcategories = newSubs
        notifyDataSetChanged()
    }
}