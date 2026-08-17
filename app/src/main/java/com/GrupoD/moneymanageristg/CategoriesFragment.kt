package com.GrupoD.moneymanageristg

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class CategoriesFragment : Fragment(), CategoryAdapter.CategoryActionListener {

    private lateinit var rvCategories: RecyclerView
    private lateinit var fabAddCategory: FloatingActionButton
    private lateinit var adapter: CategoryAdapter
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var sharedPref: SharedPreferences
    private var categoriesList = mutableListOf<CategoryWithSubs>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_categories, container, false)

        dbHelper = DatabaseHelper(requireContext())
        sharedPref = requireActivity().getSharedPreferences("login_prefs", Context.MODE_PRIVATE)

        rvCategories = view.findViewById(R.id.rv_categories)
        fabAddCategory = view.findViewById(R.id.fab_add_category)

        rvCategories.layoutManager = LinearLayoutManager(requireContext())
        adapter = CategoryAdapter(categoriesList, this)
        rvCategories.adapter = adapter

        cargarCategorias()

        fabAddCategory.setOnClickListener {
            mostrarDialogoAgregarCategoria()
        }

        return view
    }


    // CARGAR CATEGORÍAS DESDE BD
    private fun cargarCategorias() {
        val categorias = dbHelper.getCategorias()
        val listaConSubs = mutableListOf<CategoryWithSubs>()

        for ((id, nombre) in categorias) {
            val subcategorias = dbHelper.getSubcategorias(id).map { (subId, subNombre) ->
                Subcategory(subId, subNombre)
            }
            listaConSubs.add(CategoryWithSubs(id, nombre, subcategorias))
        }

        categoriesList.clear()
        categoriesList.addAll(listaConSubs)
        adapter.updateData(categoriesList)
    }


    // CRUD DE CATEGORÍAS

    // Agregar categoría
    private fun mostrarDialogoAgregarCategoria() {
        val input = EditText(requireContext())
        input.hint = "Nombre de la categoría"
        AlertDialog.Builder(requireContext())
            .setTitle("Agregar categoría")
            .setView(input)
            .setPositiveButton("Guardar") { _, _ ->
                val nombre = input.text.toString().trim()
                if (nombre.isNotEmpty()) {
                    val result = dbHelper.insertCategory(nombre)
                    if (result != -1L) {
                        Toast.makeText(requireContext(), "Categoría agregada", Toast.LENGTH_SHORT).show()
                        cargarCategorias()
                    } else {
                        Toast.makeText(requireContext(), "Error o categoría duplicada", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Ingresa un nombre", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // Editar categoría
    override fun onEditCategory(categoryId: Long, currentName: String) {
        val input = EditText(requireContext())
        input.setText(currentName)
        AlertDialog.Builder(requireContext())
            .setTitle("Editar categoría")
            .setView(input)
            .setPositiveButton("Actualizar") { _, _ ->
                val nuevoNombre = input.text.toString().trim()
                if (nuevoNombre.isNotEmpty() && nuevoNombre != currentName) {
                    val success = dbHelper.updateCategory(categoryId, nuevoNombre)
                    if (success) {
                        Toast.makeText(requireContext(), "Categoría actualizada", Toast.LENGTH_SHORT).show()
                        cargarCategorias()
                    } else {
                        Toast.makeText(requireContext(), "Error o nombre duplicado", Toast.LENGTH_SHORT).show()
                    }
                } else if (nuevoNombre == currentName) {
                    Toast.makeText(requireContext(), "Sin cambios", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Ingresa un nombre válido", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // Eliminar categoría (con confirmación y manejo de transacciones)
    override fun onDeleteCategory(categoryId: Long) {
        val tieneTransacciones = dbHelper.categoriaTieneTransacciones(categoryId)
        if (tieneTransacciones) {
            mostrarDialogoEliminarCategoriaConTransacciones(categoryId)
        } else {
            AlertDialog.Builder(requireContext())
                .setTitle("Eliminar categoría")
                .setMessage("¿Estás seguro de que deseas eliminar esta categoría? También se eliminarán sus subcategorías.")
                .setPositiveButton("Eliminar") { _, _ ->
                    val success = dbHelper.deleteCategory(categoryId)
                    if (success) {
                        Toast.makeText(requireContext(), "Categoría eliminada", Toast.LENGTH_SHORT).show()
                        cargarCategorias()
                    } else {
                        Toast.makeText(requireContext(), "Error al eliminar", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun mostrarDialogoEliminarCategoriaConTransacciones(categoryId: Long) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar categoría")
            .setMessage("Esta categoría tiene transacciones asociadas. ¿Qué deseas hacer?")
            .setPositiveButton("Reasignar a 'Otros'") { _, _ ->
                // Reasignar transacciones a categoría "Otros" (id = 1) y eliminar categoría
                val success = dbHelper.reasignarCategoria(categoryId, 1)
                if (success) {
                    Toast.makeText(requireContext(), "Transacciones reasignadas y categoría eliminada", Toast.LENGTH_SHORT).show()
                    cargarCategorias()
                } else {
                    Toast.makeText(requireContext(), "Error", Toast.LENGTH_SHORT).show()
                }
            }
            .setNeutralButton("Eliminar todo") { _, _ ->
                val success = dbHelper.deleteCategoriaYTransacciones(categoryId)
                if (success) {
                    Toast.makeText(requireContext(), "Categoría y transacciones eliminadas", Toast.LENGTH_SHORT).show()
                    cargarCategorias()
                } else {
                    Toast.makeText(requireContext(), "Error", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }


    // CRUD DE SUBCATEGORÍAS

    override fun onAddSubcategory(categoryId: Long) {
        val input = EditText(requireContext())
        input.hint = "Nombre de la subcategoría"
        AlertDialog.Builder(requireContext())
            .setTitle("Agregar subcategoría")
            .setView(input)
            .setPositiveButton("Guardar") { _, _ ->
                val nombre = input.text.toString().trim()
                if (nombre.isNotEmpty()) {
                    val success = dbHelper.insertSubcategoria(nombre, categoryId)
                    if (success) {
                        Toast.makeText(requireContext(), "Subcategoría agregada", Toast.LENGTH_SHORT).show()
                        cargarCategorias()
                    } else {
                        Toast.makeText(requireContext(), "Error o subcategoría duplicada", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Ingresa un nombre", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onEditSubcategory(subcategoryId: Long, currentName: String, categoryId: Long) {
        val input = EditText(requireContext())
        input.setText(currentName)
        AlertDialog.Builder(requireContext())
            .setTitle("Editar subcategoría")
            .setView(input)
            .setPositiveButton("Actualizar") { _, _ ->
                val nuevoNombre = input.text.toString().trim()
                if (nuevoNombre.isNotEmpty() && nuevoNombre != currentName) {
                    val success = dbHelper.updateSubcategoria(subcategoryId, nuevoNombre)
                    if (success) {
                        Toast.makeText(requireContext(), "Subcategoría actualizada", Toast.LENGTH_SHORT).show()
                        cargarCategorias()
                    } else {
                        Toast.makeText(requireContext(), "Error o nombre duplicado", Toast.LENGTH_SHORT).show()
                    }
                } else if (nuevoNombre == currentName) {
                    Toast.makeText(requireContext(), "Sin cambios", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Ingresa un nombre válido", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDeleteSubcategory(subcategoryId: Long, categoryId: Long) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar subcategoría")
            .setMessage("¿Estás seguro de que deseas eliminar esta subcategoría?")
            .setPositiveButton("Eliminar") { _, _ ->
                val success = dbHelper.deleteSubcategoria(subcategoryId)
                if (success) {
                    Toast.makeText(requireContext(), "Subcategoría eliminada", Toast.LENGTH_SHORT).show()
                    cargarCategorias()
                } else {
                    Toast.makeText(requireContext(), "Error al eliminar", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}