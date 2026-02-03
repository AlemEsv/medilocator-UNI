package com.example.mykfirebase1

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class AgregarEditarCentroActivity : AppCompatActivity() {
    private var selectedImageUri: Uri? = null
    private val firebaseDb = FirebaseDatabase.getInstance().reference
    private val storageRef = FirebaseStorage.getInstance().reference

    private lateinit var etName: EditText
    private lateinit var etLat: EditText
    private lateinit var etLng: EditText
    private lateinit var etHorario: EditText
    private lateinit var tvSelectedSpecialties: TextView
    private lateinit var ivPreview: ImageView
    private lateinit var btnDelete: Button

    private val availableSpecialties = mutableListOf<String>()
    private val selectedSpecialties = mutableListOf<String>()
    private var editingCenterId: String? = null
    private var currentImageUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_editar_centro)

        etName = findViewById(R.id.etCenterName)
        etLat = findViewById(R.id.etLat)
        etLng = findViewById(R.id.etLng)
        etHorario = findViewById(R.id.etHorario)
        tvSelectedSpecialties = findViewById(R.id.tvSelectedSpecialties)
        ivPreview = findViewById(R.id.ivPreview)
        btnDelete = findViewById(R.id.btnDeleteCenter)
        
        val btnPick: Button = findViewById(R.id.btnPickImage)
        val btnSave: Button = findViewById(R.id.btnSaveCenter)
        val btnSelectSpec: Button = findViewById(R.id.btnSelectSpecialties)
        val btnBack: Button = findViewById(R.id.btnBack)

        btnBack.setOnClickListener {
            finish()
        }

        // Cargar especialidades disponibles
        loadAvailableSpecialties()

        // Verificar si estamos editando
        editingCenterId = intent.getStringExtra("CENTER_ID")
        if (editingCenterId != null) {
            loadCenterData(editingCenterId!!)
            btnDelete.visibility = View.VISIBLE
            btnSave.text = "Actualizar"
        }

        val pickLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) { selectedImageUri = uri; ivPreview.setImageURI(uri) }
        }

        btnPick.setOnClickListener { pickLauncher.launch("image/*") }

        btnSelectSpec.setOnClickListener { showSpecialtyDialog() }

        btnSave.setOnClickListener { saveCenter() }

        btnDelete.setOnClickListener { deleteCenter() }
    }

    private fun loadAvailableSpecialties() {
        firebaseDb.child("especialidades").get().addOnSuccessListener { snap ->
            availableSpecialties.clear()
            snap.children.forEach { 
                val name = it.child("nombre").getValue(String::class.java)
                if (name != null) availableSpecialties.add(name)
            }
        }
    }

    private fun loadCenterData(id: String) {
        firebaseDb.child("centros").child(id).get().addOnSuccessListener { snap ->
            if (!snap.exists()) return@addOnSuccessListener
            
            etName.setText(snap.child("nombre").getValue(String::class.java))
            etLat.setText(snap.child("lat").getValue(Any::class.java).toString())
            etLng.setText(snap.child("lng").getValue(Any::class.java).toString())
            etHorario.setText(snap.child("horario").getValue(String::class.java))
            
            currentImageUrl = snap.child("imagenUrl").getValue(String::class.java) ?: ""

            selectedSpecialties.clear()
            val espChild = snap.child("especialidades")
            if (espChild.exists()) {
                 if (espChild.value is List<*>) {
                    espChild.children.forEach { it.getValue(String::class.java)?.let { s -> selectedSpecialties.add(s) } }
                } else {
                    espChild.children.forEach { it.getValue(String::class.java)?.let { s -> selectedSpecialties.add(s) } }
                }
            }
            updateSelectedSpecialtiesText()
        }
    }

    private fun showSpecialtyDialog() {
        val specialtiesArray = availableSpecialties.toTypedArray()
        val checkedItems = BooleanArray(specialtiesArray.size) { index ->
            selectedSpecialties.contains(specialtiesArray[index])
        }

        AlertDialog.Builder(this)
            .setTitle("Seleccionar Especialidades")
            .setMultiChoiceItems(specialtiesArray, checkedItems) { _, which, isChecked ->
                val spec = specialtiesArray[which]
                if (isChecked) {
                    if (!selectedSpecialties.contains(spec)) selectedSpecialties.add(spec)
                } else {
                    selectedSpecialties.remove(spec)
                }
            }
            .setPositiveButton("OK") { _, _ -> updateSelectedSpecialtiesText() }
            .show()
    }

    private fun updateSelectedSpecialtiesText() {
        tvSelectedSpecialties.text = if (selectedSpecialties.isEmpty()) "Ninguna seleccionada" else selectedSpecialties.joinToString(", ")
    }

    private fun saveCenter() {
        val name = etName.text.toString().trim()
        val lat = etLat.text.toString().toDoubleOrNull() ?: 0.0
        val lng = etLng.text.toString().toDoubleOrNull() ?: 0.0
        val horario = etHorario.text.toString().trim()
        
        if (name.isEmpty()) {
            etName.error = "Requerido"; return
        }

        val id = editingCenterId ?: (firebaseDb.child("centros").push().key ?: return)

        if (selectedImageUri != null) {
            val ref = storageRef.child("centros_images/$id.jpg")
            ref.putFile(selectedImageUri!!).addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    saveToDb(id, name, lat, lng, horario, uri.toString())
                }
            }
        } else {
            saveToDb(id, name, lat, lng, horario, currentImageUrl)
        }
    }

    private fun saveToDb(id: String, name: String, lat: Double, lng: Double, horario: String, imgUrl: String) {
        val center = hashMapOf(
            "id" to id,
            "nombre" to name,
            "lat" to lat,
            "lng" to lng,
            "horario" to horario,
            "imagenUrl" to imgUrl,
            "especialidades" to selectedSpecialties
        )
        firebaseDb.child("centros").child(id).setValue(center)
            .addOnSuccessListener { 
                Toast.makeText(this, "Guardado correctamente", Toast.LENGTH_SHORT).show()
                finish() 
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteCenter() {
        if (editingCenterId == null) return
        AlertDialog.Builder(this)
            .setTitle("Eliminar")
            .setMessage("¿Estás seguro?")
            .setPositiveButton("Sí") { _, _ ->
                firebaseDb.child("centros").child(editingCenterId!!).removeValue()
                    .addOnSuccessListener { finish() }
            }
            .setNegativeButton("No", null)
            .show()
    }
}
