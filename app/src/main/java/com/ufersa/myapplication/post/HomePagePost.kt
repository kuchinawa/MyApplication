package com.ufersa.myapplication.post

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ufersa.myapplication.adapter.PostAdapter
import com.ufersa.myapplication.databinding.ActivityHomePagePostBinding
import com.ufersa.myapplication.model.Post

class HomePagePost : AppCompatActivity() {

    private lateinit var binding: ActivityHomePagePostBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var postAdapter: PostAdapter
    private val posts = mutableListOf<Post>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomePagePostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializa o Firestore
        firestore = FirebaseFirestore.getInstance()

        // Configura o RecyclerView
        setupRecyclerView()

        // Busca as publicações do Firestore
        fetchPosts()

        // Define o listener de clique para o FAB
        binding.fabCreatePost.setOnClickListener {
            val intent = Intent(this, CreatePostActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        // Inicializa o adaptador com uma lista vazia
        postAdapter = PostAdapter(posts)

        // Configura o RecyclerView
        binding.recyclerViewPosts.apply {
            adapter = postAdapter
            layoutManager = LinearLayoutManager(this@HomePagePost) // Usa LinearLayoutManager
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun fetchPosts() {
        // Obtém uma referência para a coleção 'posts' no Firestore
        firestore.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING) // Ordena por timestamp em ordem decrescente
            .get() // Obtém os documentos
            .addOnSuccessListener { documents ->
                // Limpa a lista atual de publicações
                posts.clear()

                // Itera sobre os documentos e os converte em objetos Post
                for (document in documents) {
                    val post = document.toObject(Post::class.java)
                    posts.add(post)
                }

                // Notifica o adaptador que os dados foram alterados
                postAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                // Lida com quaisquer erros
                Log.w("HomePagePost", "Erro ao obter documentos: ", exception)
            }
    }
}