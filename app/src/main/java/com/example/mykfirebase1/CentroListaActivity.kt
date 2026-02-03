package com.example.mykfirebase1

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class CentroListaActivity : AppCompatActivity() {

    private lateinit var adapter: CentroAdaptador
    private val centersList = mutableListOf<CentroMedico>()
    private val firebaseDb = FirebaseDatabase.getInstance().reference.child("centros")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_centro_lista)

        val rv: RecyclerView = findViewById(R.id.rvCenters)
        rv.layoutManager = LinearLayoutManager(this)

        adapter = CentroAdaptador(centersList) { center ->
            // Al hacer clic, vamos a editar
            val intent = Intent(this, AgregarEditarCentroActivity::class.java)
            intent.putExtra("CENTER_ID", center.id)
            startActivity(intent)
        }
        rv.adapter = adapter

        val btnAdd: Button = findViewById(R.id.btnAddCenter)
        btnAdd.setOnClickListener {
            startActivity(Intent(this, AgregarEditarCentroActivity::class.java))
        }

        val btnBack: Button = findViewById(R.id.btnBack)
        btnBack.setOnClickListener {
            finish() // Vuelve a la actividad anterior (MainActivity)
        }

        loadCenters()
    }

    private fun loadCenters() {
        firebaseDb.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                centersList.clear()
                for (child in snapshot.children) {
                    val id = child.key ?: continue
                    val nombre = child.child("nombre").getValue(String::class.java) ?: ""
                    
                    // Lectura robusta de lat/lng
                    val lat = try {
                        child.child("lat").getValue(Double::class.java)
                            ?: child.child("latitud").getValue(Double::class.java)
                            ?: child.child("lat").getValue(String::class.java)?.toDoubleOrNull()
                            ?: 0.0
                    } catch (e: Exception) { 0.0 }

                    val lng = try {
                        child.child("lng").getValue(Double::class.java)
                            ?: child.child("longitud").getValue(Double::class.java)
                            ?: child.child("lng").getValue(String::class.java)?.toDoubleOrNull()
                            ?: 0.0
                    } catch (e: Exception) { 0.0 }

                    val horario = child.child("horario").getValue(String::class.java) ?: ""
                    val imagen = child.child("imagenUrl").getValue(String::class.java) ?: ""
                    
                    val especs = mutableListOf<String>()
                    val espChild = child.child("especialidades")
                    if (espChild.exists()) {
                        if (espChild.value is List<*>) {
                            espChild.children.forEach { 
                                it.getValue(String::class.java)?.let { s -> especs.add(s) } 
                            }
                        } else {
                            espChild.children.forEach { 
                                it.getValue(String::class.java)?.let { s -> especs.add(s) } 
                            }
                        }
                    }

                    centersList.add(CentroMedico(id, nombre, lat, lng, especs, horario, imagen))
                }
                adapter.updateList(centersList)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@CentroListaActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
