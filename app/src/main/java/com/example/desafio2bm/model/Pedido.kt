package com.example.desafio2bm.model

data class Pedido(
    val id: String = "",
    val clienteId: String = "",
    val clienteNome: String = "",
    val restauranteId: String = "",
    val itemId: String = "",
    val itemNome: String = "",
    val quantidade: Long = 1,
    val total: Double = 0.0
)
