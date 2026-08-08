package com.GrupoD.moneymanageristg

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class TransactionAdapter(
    private var transactions: List<Transaction>
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.findViewById(R.id.card_view)
        val tvConcepto: TextView = itemView.findViewById(R.id.tv_concepto)
        val tvCategoria: TextView = itemView.findViewById(R.id.tv_categoria)
        val tvSubcategoria: TextView = itemView.findViewById(R.id.tv_subcategoria)
        val tvMonto: TextView = itemView.findViewById(R.id.tv_monto)
        val tvFecha: TextView = itemView.findViewById(R.id.tv_fecha)
        val tvMedio: TextView = itemView.findViewById(R.id.tv_medio)
        val tvCuenta: TextView = itemView.findViewById(R.id.tv_cuenta)
        val ivTipo: ImageView = itemView.findViewById(R.id.iv_tipo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.tvConcepto.text = transaction.concepto
        holder.tvCategoria.text = transaction.categoria
        holder.tvSubcategoria.text = transaction.subcategoria ?: ""
        holder.tvMonto.text = "$${String.format("%.2f", transaction.monto)}"
        holder.tvFecha.text = transaction.fecha
        holder.tvMedio.text = transaction.medioPago ?: ""
        holder.tvCuenta.text = transaction.cuenta ?: ""

        // Color del CardView según el monto
        when {
            transaction.monto > 100 -> holder.cardView.setCardBackgroundColor(0xFFFFCDD2.toInt()) // rojo claro
            transaction.monto > 50 -> holder.cardView.setCardBackgroundColor(0xFFFFF9C4.toInt()) // amarillo claro
            else -> holder.cardView.setCardBackgroundColor(0xFFFFFFFF.toInt()) // blanco
        }
        val indicator = holder.itemView.findViewById<View>(R.id.indicator_color)
        when {
            transaction.monto > 100 -> indicator.setBackgroundResource(R.drawable.circle_red)
            transaction.monto > 50 -> indicator.setBackgroundResource(R.drawable.circle_yellow)
            else -> indicator.setBackgroundResource(R.drawable.circle_green)
        }
        // Icono según el tipo (usa los drawables que tengas, o cámbialos después)
        when (transaction.tipo) {
            "Ingreso" -> holder.ivTipo.setImageResource(android.R.drawable.ic_menu_add) // temporal
            "Egreso" -> holder.ivTipo.setImageResource(android.R.drawable.ic_menu_delete) // temporal
            "Traspaso" -> holder.ivTipo.setImageResource(android.R.drawable.ic_menu_revert) // temporal
            else -> holder.ivTipo.setImageResource(android.R.drawable.ic_menu_info_details)
        }
    }

    override fun getItemCount(): Int = transactions.size

    fun updateData(newList: List<Transaction>) {
        transactions = newList
        notifyDataSetChanged()
    }
}