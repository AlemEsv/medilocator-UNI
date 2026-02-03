package com.example.mykfirebase1

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.database.FirebaseDatabase
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var spinner: Spinner
    private lateinit var btnLocate: Button

    private val firebaseDb = FirebaseDatabase.getInstance().reference
    private val specialties = mutableListOf<String>()
    private val client = OkHttpClient()

    // Ruta actual
    private var currentPolyline: Polyline? = null
    // Marcadores de centros
    private val centerMarkers = mutableListOf<Marker>()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(
                    this,
                    "Sin permiso de ubicación no se puede localizar.",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                enableLocationIfAllowed()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        spinner = findViewById(R.id.spinnerEspecialidad)
        btnLocate = findViewById(R.id.btnLocate)

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            specialties
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        // Cargar especialidades desde Firebase
        firebaseDb.child("especialidades").get()
            .addOnSuccessListener { snap ->
                specialties.clear()
                for (s in snap.children) {
                    val name = s.child("nombre").getValue(String::class.java)
                    if (!name.isNullOrBlank()) specialties.add(name)
                }
                if (specialties.isEmpty()) {
                    specialties.addAll(
                        listOf(
                            "Cardiología",
                            "Gastroenterología",
                            "Pediatría",
                            "Traumatología"
                        )
                    )
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                specialties.addAll(
                    listOf(
                        "Cardiología",
                        "Gastroenterología",
                        "Pediatría",
                        "Traumatología"
                    )
                )
                adapter.notifyDataSetChanged()
            }

        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        btnLocate.setOnClickListener {
            locateAndShowNearest()
        }

        findViewById<Button>(R.id.btnNavCenters).setOnClickListener {
            startActivity(Intent(this, CentroListaActivity::class.java))
        }

        findViewById<Button>(R.id.btnNavSpecialties).setOnClickListener {
            startActivity(Intent(this, ListaEspecialidadesActivity::class.java))
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                // opcional: localizar automáticamente
                // locateAndShowNearest()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isZoomControlsEnabled = true

        // Ajustar padding para que los controles de zoom no queden tapados por la barra inferior
        val bottomBar = findViewById<View>(R.id.llBottomBar)
        bottomBar.post {
            val height = bottomBar.height
            // left, top, right, bottom
            map.setPadding(0, 0, 0, height)
        }

        // Lima por defecto
        map.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(-12.046374, -77.042793),
                12f
            )
        )

        enableLocationIfAllowed()
    }

    private fun enableLocationIfAllowed() {
        if (!::map.isInitialized) return

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = true
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun locateAndShowNearest() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }

        val fused = LocationServices.getFusedLocationProviderClient(this)
        fused.lastLocation.addOnSuccessListener { loc ->
            if (loc == null) {
                Toast.makeText(this, "Ubicación es nula. Revisa el GPS del emulador.", Toast.LENGTH_LONG).show()
                return@addOnSuccessListener
            }

            Toast.makeText(this, "Ubicación OK.", Toast.LENGTH_SHORT).show()

            val userLatLng = LatLng(loc.latitude, loc.longitude)
            val specialty = spinner.selectedItem as? String ?: ""

            firebaseDb.child("centros").get()
                .addOnSuccessListener { snap ->
                    val centros = snap.children.mapNotNull { p ->
                        val id = p.key ?: return@mapNotNull null
                        val nombre = p.child("nombre").getValue(String::class.java) ?: ""

                        // Intentar leer lat/lng como Double o String y convertir
                        val lat = try {
                            p.child("lat").getValue(Double::class.java)
                                ?: p.child("latitud").getValue(Double::class.java)
                                ?: p.child("lat").getValue(String::class.java)?.toDoubleOrNull()
                                ?: p.child("latitud").getValue(String::class.java)?.toDoubleOrNull()
                        } catch (e: Exception) { null }

                        val lng = try {
                            p.child("lng").getValue(Double::class.java)
                                ?: p.child("longitud").getValue(Double::class.java)
                                ?: p.child("lng").getValue(String::class.java)?.toDoubleOrNull()
                                ?: p.child("longitud").getValue(String::class.java)?.toDoubleOrNull()
                        } catch (e: Exception) { null }

                        if (lat == null || lng == null) return@mapNotNull null

                        // Manejo robusto de especialidades
                        val especs = mutableListOf<String>()
                        val espChild = p.child("especialidades")
                        if (espChild.exists()) {
                            if (espChild.value is List<*>) {
                                espChild.children.forEach { 
                                    it.getValue(String::class.java)?.let { s -> especs.add(s) } 
                                }
                            } else {
                                // Si es un mapa o string único
                                espChild.children.forEach { 
                                    it.getValue(String::class.java)?.let { s -> especs.add(s) } 
                                }
                            }
                        }

                        val horario = p.child("horario").getValue(String::class.java) ?: ""
                        val imagen = p.child("imagenUrl").getValue(String::class.java) ?: ""

                        CentroMedico(id, nombre, lat, lng, especs, horario, imagen)
                    }

                    if (centros.isEmpty()) {
                        Toast.makeText(this, "No se pudieron parsear centros válidos", Toast.LENGTH_LONG).show()
                        return@addOnSuccessListener
                    }

                    val filtered = centros.filter { 
                        it.especialidades.any { e -> e.equals(specialty, ignoreCase = true) }
                    }

                    if (filtered.isEmpty()) {
                        Toast.makeText(
                            this,
                            "No se encontraron centros con esa especialidad",
                            Toast.LENGTH_LONG
                        ).show()
                        return@addOnSuccessListener
                    }

                    clearCenterMarkers()

                    filtered.forEach {
                        map.addMarker(
                            MarkerOptions()
                                .position(LatLng(it.latitud, it.longitud))
                                .title(it.nombre)
                        )?.let { m -> centerMarkers.add(m) }
                    }

                    val nearest = filtered.minByOrNull {
                        val r = FloatArray(1)
                        Location.distanceBetween(
                            loc.latitude,
                            loc.longitude,
                            it.latitud,
                            it.longitud,
                            r
                        )
                        r[0]
                    } ?: return@addOnSuccessListener

                    val destLatLng = LatLng(nearest.latitud, nearest.longitud)
                    map.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(destLatLng, 14f)
                    )

                    currentPolyline?.remove()
                    currentPolyline = map.addPolyline(
                        PolylineOptions()
                            .add(userLatLng, destLatLng)
                            .width(6f)
                    )

                    val url = directionsUrl(
                        userLatLng.latitude,
                        userLatLng.longitude,
                        destLatLng.latitude,
                        destLatLng.longitude
                    )

                    fetchDirectionsAndDraw(url)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error leyendo centros: ${e.message}", Toast.LENGTH_LONG).show()
                    android.util.Log.e("FIREBASE_READ", "Error", e)
                }
        }
    }

    private fun clearCenterMarkers() {
        centerMarkers.forEach { it.remove() }
        centerMarkers.clear()
    }

    private fun directionsUrl(
        slat: Double,
        slng: Double,
        dlat: Double,
        dlng: Double
    ): String {
        val key = getString(R.string.google_maps_key)
        return "https://maps.googleapis.com/maps/api/directions/json" +
                "?origin=$slat,$slng" +
                "&destination=$dlat,$dlng" +
                "&mode=driving" +
                "&departure_time=now" +
                "&traffic_model=best_guess" +
                "&key=$key"
    }

    private fun fetchDirectionsAndDraw(url: String) {
        val req = Request.Builder().url(url).build()

        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Error al obtener ruta",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: return

                try {
                    val json = JSONObject(body)
                    
                    // Verificar estado de la respuesta
                    val status = json.optString("status")
                    if (status != "OK") {
                        val errorMsg = json.optString("error_message")
                        runOnUiThread {
                            Toast.makeText(
                                this@MainActivity,
                                "API Error: $status - $errorMsg",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        return
                    }

                    val routes = json.optJSONArray("routes")
                    if (routes == null || routes.length() == 0) {
                        runOnUiThread {
                            Toast.makeText(
                                this@MainActivity,
                                "No se encontró ruta",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        return
                    }

                    val points = routes
                        .getJSONObject(0)
                        .getJSONObject("overview_polyline")
                        .getString("points")

                    val decoded = decodePoly(points)

                    runOnUiThread {
                        currentPolyline?.remove()
                        currentPolyline = map.addPolyline(
                            PolylineOptions()
                                .addAll(decoded)
                                .width(12f)
                        )
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "Error procesando ruta",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })
    }

    private fun decodePoly(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        var lat = 0
        var lng = 0

        while (index < encoded.length) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)

            lat += if (result and 1 != 0) (result shr 1).inv() else result shr 1

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)

            lng += if (result and 1 != 0) (result shr 1).inv() else result shr 1

            poly.add(LatLng(lat / 1E5, lng / 1E5))
        }
        return poly
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_centers -> {
                startActivity(Intent(this, CentroListaActivity::class.java))
                true
            }
            R.id.menu_specialties -> {
                startActivity(Intent(this, ListaEspecialidadesActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
