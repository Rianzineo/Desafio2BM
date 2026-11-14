package com.example.desafio2bm.dao

import com.example.desafio2bm.model.Item
import com.example.desafio2bm.model.Pedido
import com.example.desafio2bm.model.Usuario
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DeliveryDao {
    val auth = FirebaseAuth.getInstance()
    private val banco = FirebaseFirestore.getInstance()

    fun entrar(email: String, senha: String) =
        auth.signInWithEmailAndPassword(email, senha)

    fun cadastrar(email: String, senha: String) =
        auth.createUserWithEmailAndPassword(email, senha)

    fun salvarUsuario(usuario: Usuario) =
        banco.collection("usuarios")
            .document(usuario.id)
            .set(usuario)

    fun usuario() =
        banco.collection("usuarios")
            .document(auth.currentUser!!.uid)
            .get()

    fun itens(restauranteId: String? = null) =
        if (restauranteId == null) {
            banco.collection("itens").get()
        } else {
            banco.collection("itens")
                .whereEqualTo("restauranteId", restauranteId)
                .get()
        }

    fun salvarItem(item: Item): Task<Void> {
        val documento = banco.collection("itens").document()
        return documento.set(item.copy(id = documento.id))
    }

    fun salvarPedido(pedido: Pedido): Task<Void> {
        val documento = banco.collection("pedidos").document()
        return documento.set(pedido.copy(id = documento.id))
    }

    fun pedidos(usuario: Usuario) =
        banco.collection("pedidos")
            .whereEqualTo(
                if (usuario.perfil == "Cliente") "clienteId" else "restauranteId",
                usuario.id
            )
            .get()
}
