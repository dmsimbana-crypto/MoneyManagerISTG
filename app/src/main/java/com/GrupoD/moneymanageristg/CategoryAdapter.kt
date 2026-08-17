package com.GrupoD.moneymanageristg

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class CategoryAdapter(
    private var categories: List<CategoryWithSubs>,
    private val listener: CategoryActionListener
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    // Interfaz para comunicar acciones al fragmento
    interface CategoryActionListener {
        fun onEditCategory(categoryId: Long, currentName: String)
        fun onDeleteCategory(categoryId: Long)
        fun onAddSubcategory(categoryId: Long)
        fun onEditSubcategory(subcategoryId: Long, currentName: String, categoryId: Long)
        fun onDeleteSubcategory(subcategoryId: Long, categoryId: Long)
    }

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCategoryName: TextView = itemView.findViewById(R.id.tv_category_name)
        val btnExpand: ImageButton = itemView.findViewById(R.id.btn_expand)
        val btnEditCategory: ImageButton = itemView.findViewById(R.id.btn_edit_category)
        val btnDeleteCategory: ImageButton = itemView.findViewById(R.id.btn_delete_category)
        val llSubcategoriesContainer: LinearLayout = itemView.findViewById(R.id.ll_subcategories_container)
        val rvSubcategories: RecyclerView = itemView.findViewById(R.id.rv_subcategories)
        val btnAddSubcategory: Button = itemView.findViewById(R.id.btn_add_subcategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]

        holder.tvCategoryName.text = category.nombre

        // Configurar RecyclerView de subcategorías
        val subAdapter = SubcategoryAdapter(
            category.subcategorias,
            object : SubcategoryAdapter.SubcategoryActionListener {
                override fun onEditSubcategory(subId: Long, name: String) {
                    listener.onEditSubcategory(subId, name, category.id)
                }

                override fun onDeleteSubcategory(subId: Long) {
                    listener.onDeleteSubcategory(subId, category.id)
                }
            }
        )
        holder.rvSubcategories.layoutManager = LinearLayoutManager(holder.itemView.context)
        holder.rvSubcategories.adapter = subAdapter

        // Estado de expansión (colapsado por defecto)
        var isExpanded = false
        holder.llSubcategoriesContainer.visibility = View.GONE
        holder.btnExpand.setImageResource(android.R.drawable.ic_menu_more)

        // Click para expandir/colapsar
        holder.btnExpand.setOnClickListener {
            isExpanded = !isExpanded
            if (isExpanded) {
                holder.llSubcategoriesContainer.visibility = View.VISIBLE
                holder.btnExpand.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            } else {
                holder.llSubcategoriesContainer.visibility = View.GONE
                holder.btnExpand.setImageResource(android.R.drawable.ic_menu_more)
            }
        }

        // Editar categoría
        holder.btnEditCategory.setOnClickListener {
            listener.onEditCategory(category.id, category.nombre)
        }

        // Eliminar categoría
        holder.btnDeleteCategory.setOnClickListener {
            listener.onDeleteCategory(category.id)
        }

        // Agregar subcategoría
        holder.btnAddSubcategory.setOnClickListener {
            listener.onAddSubcategory(category.id)
        }
    }

    override fun getItemCount(): Int = categories.size

    fun updateData(newCategories: List<CategoryWithSubs>) {
        categories = newCategories
        notifyDataSetChanged()
    }
}