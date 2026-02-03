package com.example.mykfirebase1

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EspecialidadAdaptador(
    private var especialidades: List<Especialidad>,
    private val onEditClick: (Especialidad) -> Unit,
    private val onDeleteClick: (Especialidad) -> Unit
) : RecyclerView.Adapter<EspecialidadAdaptador.EspecialidadViewHolder>() {

    inner class EspecialidadViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvEspecialidadName)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteEspecialidad)

        fun bind(especialidad: Especialidad) {
            tvName.text = especialidad.nombre
            btnDelete.setOnClickListener { onDeleteClick(especialidad) }
            itemView.setOnClickListener { onEditClick(especialidad) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EspecialidadViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_especialidad, parent, false)
        return EspecialidadViewHolder(view)
    }

    override fun onBindViewHolder(holder: EspecialidadViewHolder, position: Int) {
        holder.bind(especialidades[position])
    }

    override fun getItemCount(): Int = especialidades.size

    fun updateList(newList: List<Especialidad>) {
        especialidades = newList
        notifyDataSetChanged()
    }
}
