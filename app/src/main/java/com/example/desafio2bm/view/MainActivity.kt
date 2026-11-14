package com.example.desafio2bm.view

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.desafio2bm.R
import com.example.desafio2bm.dao.DeliveryDao
import com.example.desafio2bm.model.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private val dao = DeliveryDao()
    private var usuario: Usuario? = null
    private var ocupado = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (dao.auth.currentUser == null) mostrarLogin() else carregarUsuario()
    }

    private fun mostrarTela(layout: Int) {
        setContentView(layout)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.sv_principal)) { v, insets ->
            val barras = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }
    }

    private fun lerCampo(id: Int) =
        findViewById<EditText>(id).text.toString().trim()

    private fun mostrarMensagem(texto: String) {
        findViewById<TextView>(R.id.tv_mensagem).text = texto
    }

    private fun mostrarErro(e: Exception) {
        ocupado = false
        mostrarMensagem(e.localizedMessage ?: "Não foi possível concluir.")
    }

    private fun formatarMoeda(valor: Double) =
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(valor)

    private fun mostrarLogin() {
        mostrarTela(R.layout.activity_main)
        findViewById<Button>(R.id.btn_entrar).setOnClickListener { entrar() }
        findViewById<Button>(R.id.btn_cadastrar).setOnClickListener {
            if (ocupado == false) mostrarCadastro()
        }
    }

    private fun entrar() {
        if (ocupado) return
        val email = lerCampo(R.id.et_email)
        val senha = lerCampo(R.id.et_senha)

        if (email.isBlank() || senha.isBlank()) {
            mostrarMensagem("Preencha e-mail e senha.")
            return
        }

        ocupado = true
        mostrarMensagem("Entrando...")
        dao.entrar(email, senha).addOnSuccessListener {
            ocupado = false
            carregarUsuario()
        }.addOnFailureListener { mostrarErro(it) }
    }

    private fun mostrarCadastro() {
        mostrarTela(R.layout.activity_cadastro)
        val perfil = findViewById<Spinner>(R.id.sp_perfil)
        perfil.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Cliente", "Restaurante")
        )
        findViewById<Button>(R.id.btn_voltar).setOnClickListener {
            if (ocupado == false) mostrarLogin()
        }
        findViewById<Button>(R.id.btn_salvar).setOnClickListener { cadastrarConta() }
    }

    private fun cadastrarConta() {
        if (ocupado) return
        val nome = lerCampo(R.id.et_nome)
        val email = lerCampo(R.id.et_email)
        val senha = lerCampo(R.id.et_senha)
        val perfil = findViewById<Spinner>(R.id.sp_perfil).selectedItem.toString()

        if (nome.isBlank() || email.isBlank() || senha.length < 6) {
            mostrarMensagem("Preencha nome, e-mail e senha com pelo menos 6 caracteres.")
            return
        }

        ocupado = true
        mostrarMensagem("Cadastrando...")
        dao.cadastrar(email, senha).addOnSuccessListener { resultado ->
            val pessoa = Usuario(resultado.user!!.uid, nome, email, perfil)
            salvarPerfil(pessoa)
        }.addOnFailureListener { mostrarErro(it) }
    }

    private fun salvarPerfil(pessoa: Usuario) {
        dao.salvarUsuario(pessoa).addOnSuccessListener {
            ocupado = false
            usuario = pessoa
            mostrarInicio()
        }.addOnFailureListener { falha ->
            dao.auth.currentUser?.delete()?.addOnCompleteListener {
                dao.auth.signOut()
            }
            mostrarErro(falha)
        }
    }

    private fun carregarUsuario() {
        mostrarTela(R.layout.activity_main)
        mostrarMensagem("Carregando perfil...")

        dao.usuario()
            .addOnSuccessListener {
                usuario = it.toObject(Usuario::class.java)

                if (usuario == null) {
                    dao.auth.signOut()
                    mostrarLogin()
                    mostrarMensagem("Perfil não encontrado. Crie uma conta novamente.")
                } else {
                    mostrarInicio()
                }
            }
            .addOnFailureListener {
                dao.auth.signOut()
                mostrarLogin()
                mostrarErro(it)
            }
    }

    private fun mostrarInicio() {
        mostrarTela(R.layout.activity_pedidos)
        val u = usuario ?: return

        findViewById<TextView>(R.id.tv_titulo).text = "${u.nome} - ${u.perfil}"
        findViewById<Button>(R.id.btn_novo_item).visibility =
            if (u.perfil == "Restaurante") View.VISIBLE else View.GONE

        findViewById<Button>(R.id.btn_novo_item).setOnClickListener { cadastrarItem() }
        findViewById<Button>(R.id.btn_sair).setOnClickListener {
            dao.auth.signOut()
            usuario = null
            mostrarLogin()
        }
        findViewById<Button>(R.id.btn_cardapio).setOnClickListener { carregarCardapio() }
        findViewById<Button>(R.id.btn_pedidos).setOnClickListener { carregarPedidos() }

        carregarCardapio()
    }

    private fun limparLista() {
        findViewById<ConstraintLayout>(R.id.cl_lista).removeAllViews()
    }

    private fun adicionarBotao(texto: String): Button {
        val lista = findViewById<ConstraintLayout>(R.id.cl_lista)
        val anterior = lista.getChildAt(lista.childCount - 1)
        val botao = Button(this)
        botao.id = View.generateViewId()
        botao.text = texto
        botao.isAllCaps = false

        val posicao = ConstraintLayout.LayoutParams(
            0, ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        posicao.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
        posicao.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
        posicao.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID

        if (anterior == null) {
            posicao.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
        } else {
            posicao.topToBottom = anterior.id
            val posicaoAnterior = anterior.layoutParams as ConstraintLayout.LayoutParams
            posicaoAnterior.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
            posicaoAnterior.bottomToTop = botao.id
            anterior.layoutParams = posicaoAnterior
        }

        lista.addView(botao, posicao)
        return botao
    }

    private fun carregarCardapio() {
        val pessoa = usuario ?: return
        val restauranteId = if (pessoa.perfil == "Restaurante") pessoa.id else null
        mostrarMensagem("Carregando cardápio...")
        limparLista()

        dao.itens(restauranteId).addOnSuccessListener { resultado ->
            limparLista()
            mostrarMensagem(if (resultado.isEmpty) "Nenhum item cadastrado." else "Cardápio")

            for (documento in resultado.documents) {
                val item = documento.toObject(Item::class.java)!!.copy(id = documento.id)
                val botao = adicionarBotao(
                    "${item.restauranteNome}\n${item.nome} - ${formatarMoeda(item.preco)}"
                )
                if (pessoa.perfil == "Cliente") {
                    botao.setOnClickListener { fazerPedido(item) }
                }
            }
        }.addOnFailureListener { mostrarErro(it) }
    }

    private fun cadastrarItem() {
        val u = usuario ?: return
        val campos = layoutInflater.inflate(R.layout.dialog_item, null)
        val nome = campos.findViewById<EditText>(R.id.et_nome_item)
        val preco = campos.findViewById<EditText>(R.id.et_preco_item)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Cadastrar item")
            .setView(campos)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar", null)
            .create()

        dialog.setOnShowListener {
            val salvar = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            salvar.setOnClickListener {
                val descricao = nome.text.toString().trim()
                val valor = try {
                    preco.text.toString().replace(",", ".").toBigDecimal()
                        .setScale(2, RoundingMode.UNNECESSARY).toDouble()
                } catch (_: Exception) {
                    0.0
                }
                if (descricao.isBlank()) {
                    nome.error = "Informe o nome."
                } else if (!valor.isFinite() || valor <= 0) {
                    preco.error = "Informe um preço válido."
                } else {
                    salvar.isEnabled = false
                    val item = Item(
                        nome = descricao,
                        preco = valor,
                        restauranteId = u.id,
                        restauranteNome = u.nome
                    )
                    dao.salvarItem(item)
                        .addOnSuccessListener {
                            dialog.dismiss()
                            carregarCardapio()
                        }
                        .addOnFailureListener {
                            salvar.isEnabled = true
                            mostrarErro(it)
                        }
                }
            }
        }
        dialog.show()
    }

    private fun fazerPedido(item: Item) {
        val quantidade = EditText(this).apply {
            hint = "Quantidade"
            inputType = InputType.TYPE_CLASS_NUMBER
            setText("1")
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(item.nome)
            .setView(quantidade)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Confirmar pedido", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val q = quantidade.text.toString().toLongOrNull() ?: 0

                if (q !in 1..99) {
                    quantidade.error = "Use uma quantidade de 1 a 99."
                } else {
                    val u = usuario ?: return@setOnClickListener
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false

                    dao.salvarPedido(
                        Pedido(
                            clienteId = u.id,
                            clienteNome = u.nome,
                            restauranteId = item.restauranteId,
                            itemId = item.id,
                            itemNome = item.nome,
                            quantidade = q,
                            total = BigDecimal.valueOf(item.preco)
                                .multiply(BigDecimal.valueOf(q))
                                .setScale(2, RoundingMode.HALF_UP)
                                .toDouble()
                        )
                    )
                        .addOnSuccessListener {
                            dialog.dismiss()
                            carregarPedidos()
                        }
                        .addOnFailureListener {
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                            mostrarErro(it)
                        }
                }
            }
        }

        dialog.show()
    }

    private fun carregarPedidos() {
        val pessoa = usuario ?: return
        mostrarMensagem("Carregando pedidos...")
        limparLista()

        dao.pedidos(pessoa).addOnSuccessListener { resultado ->
            limparLista()
            mostrarMensagem(if (resultado.isEmpty) "Nenhum pedido." else "Pedidos")

            for (documento in resultado.documents) {
                val pedido = documento.toObject(Pedido::class.java)!!.copy(id = documento.id)
                adicionarBotao(
                    "${pedido.clienteNome}\n${pedido.quantidade} x ${pedido.itemNome}\n" +
                        formatarMoeda(pedido.total)
                )
            }
        }.addOnFailureListener { mostrarErro(it) }
    }

}
