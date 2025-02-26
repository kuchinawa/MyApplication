package com.ufersa.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.semantics.text
import com.ufersa.myapplication.cadastro.CadastroActivity
import com.ufersa.myapplication.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.ufersa.myapplication.post.HomePagePost

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        binding.buttonLogin.setOnClickListener {
            val email = binding.editTextEmail.text.toString()
            val password = binding.editTextPassword.text.toString()
            login(email, password)
        }

        binding.textViewCadastreSe.setOnClickListener {
            val intent = Intent(this, CadastroActivity::class.java)
            startActivity(intent)
        }
    }

    private fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Login bem-sucedido
                    // Exibe uma mensagem de sucesso
                    Toast.makeText(baseContext, "Login realizado com sucesso.", Toast.LENGTH_SHORT).show()

                    // Cria uma Intent para iniciar a HomePagePost
                    val intent = Intent(this, HomePagePost::class.java)

                    // Inicia a HomePagePost
                    startActivity(intent)

                    // Finaliza a LoginActivity para que o usuário não possa voltar para ela com o botão "voltar"
                    finish()
                } else {
                    // Falha no login
                    Toast.makeText(baseContext, "Falha no login.", Toast.LENGTH_SHORT).show()
                }
            }
    }
}