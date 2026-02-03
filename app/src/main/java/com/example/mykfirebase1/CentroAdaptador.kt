package com.example.mykfirebase1

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CentroAdaptador(
    private var centers: List<CentroMedico>,
    private val onItemClick: (CentroMedico) -> Unit
) : RecyclerView.Adapter<CentroAdaptador.CenterViewHolder>() {

    inner class CenterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvCenterName)
        val tvHorario: TextView = itemView.findViewById(R.id.tvCenterHorario)

        fun bind(center: CentroMedico) {
            tvName.text = center.nombre
            tvHorario.text = center.horario
            itemView.setOnClickListener { onItemClick(center) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CenterViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_centro, parent, false)
        return CenterViewHolder(view)
    }

    override fun onBindViewHolder(holder: CenterViewHolder, position: Int) {
        holder.bind(centers[position])
    }

    override fun getItemCount(): Int = centers.size

    fun updateList(newList: List<CentroMedico>) {
        centers = newList
        notifyDataSetChanged()
    }
}
