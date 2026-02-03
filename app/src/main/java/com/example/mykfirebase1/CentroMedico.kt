package com.example.mykfirebase1

data class CentroMedico(
    val id: String = "",
    val nombre: String = "",
    val latitud: Double = 0.0,
    val longitud: Double = 0.0,
    val especialidades: List<String> = emptyList(),
    val horario: String = "",
    val imagenUrl: String = ""
)
