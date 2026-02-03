package com.example.mykfirebase1

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ListaEspecialidadesActivity : AppCompatActivity() {

    private lateinit var adapter: EspecialidadAdaptador
    private val especialidadesList = mutableListOf<Especialidad>()
    private val firebaseDb = FirebaseDatabase.getInstance().reference.child("especialidades")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_especialidades)

        val rv: RecyclerView = findViewById(R.id.rvEspecialidades)
        rv.layoutManager = LinearLayoutManager(this)

        adapter = EspecialidadAdaptador(
            especialidadesList,
            onEditClick = { especialidad -> showEditDialog(especialidad) },
            onDeleteClick = { especialidad -> deleteEspecialidad(especialidad) }
        )
        rv.adapter = adapter

        val btnAdd: Button = findViewById(R.id.btnAddEspecialidad)
        btnAdd.setOnClickListener { showAddDialog() }

        val btnBack: Button = findViewById(R.id.btnBack)
        btnBack.setOnClickListener {
            finish() // Vuelve a la actividad anterior (MainActivity)
        }

        loadEspecialidades()
    }

    private fun loadEspecialidades() {
        firebaseDb.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                especialidadesList.clear()
                for (child in snapshot.children) {
                    val id = child.key ?: continue
                    val nombre = child.child("nombre").getValue(String::class.java) ?: ""
                    especialidadesList.add(Especialidad(id, nombre))
                }
                adapter.updateList(especialidadesList)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ListaEspecialidadesActivity, "Error al cargar: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showAddDialog() {
        val input = EditText(this)
        input.hint = "Nombre de la especialidad"

        AlertDialog.Builder(this)
            .setTitle("Nueva Especialidad")
            .setView(input)
            .setPositiveButton("Guardar") { _, _ ->
                val nombre = input.text.toString().trim()
                if (nombre.isNotEmpty()) {
                    saveEspecialidad(nombre)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showEditDialog(especialidad: Especialidad) {
        val input = EditText(this)
        input.setText(especialidad.nombre)
        input.hint = "Nombre de la especialidad"

        AlertDialog.Builder(this)
            .setTitle("Editar Especialidad")
            .setView(input)
            .setPositiveButton("Actualizar") { _, _ ->
                val nuevoNombre = input.text.toString().trim()
                if (nuevoNombre.isNotEmpty()) {
                    updateEspecialidad(especialidad.id, nuevoNombre)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun saveEspecialidad(nombre: String) {
        val key = firebaseDb.push().key ?: return
        val map = mapOf("nombre" to nombre)
        firebaseDb.child(key).setValue(map)
            .addOnSuccessListener { Toast.makeText(this, "Guardado", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show() }
    }

    private fun updateEspecialidad(id: String, nombre: String) {
        firebaseDb.child(id).child("nombre").setValue(nombre)
            .addOnSuccessListener { Toast.makeText(this, "Actualizado", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { Toast.makeText(this, "Error al actualizar", Toast.LENGTH_SHORT).show() }
    }

    private fun deleteEspecialidad(especialidad: Especialidad) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar")
            .setMessage("¿Seguro que deseas eliminar ${especialidad.nombre}?")
            .setPositiveButton("Sí") { _, _ ->
                firebaseDb.child(especialidad.id).removeValue()
                    .addOnSuccessListener { Toast.makeText(this, "Eliminado", Toast.LENGTH_SHORT).show() }
            }
            .setNegativeButton("No", null)
            .show()
    }
}
