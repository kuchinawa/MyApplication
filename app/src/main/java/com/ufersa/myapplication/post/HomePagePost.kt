package com.ufersa.myapplication.post

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ListenerRegistration
import com.ufersa.myapplication.R
import com.ufersa.myapplication.adapter.PostAdapter
import com.ufersa.myapplication.databinding.ActivityHomePagePostBinding
import com.ufersa.myapplication.model.Post
import com.google.firebase.auth.FirebaseAuth
import com.ufersa.myapplication.LoginActivity

class HomePagePost : AppCompatActivity() {

    private lateinit var binding: ActivityHomePagePostBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var postAdapter: PostAdapter
    private val posts = mutableListOf<Post>()
    private var postsListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomePagePostBinding.inflate(layoutInflater)
        setContentView(binding.root)


        firestore = FirebaseFirestore.getInstance()
 
        val toolbar: Toolbar = binding.toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Meu Aplicativo"

        setupRecyclerView()

        setupPostsListener()

        binding.fabCreatePost.setOnClickListener {
            val intent = Intent(this, CreatePostActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        postsListener?.remove()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_home_page, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                showLogoutConfirmationDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Confirmação")
            .setMessage("Tem certeza que deseja sair?")
            .setPositiveButton("Sim") { _, _ ->
                FirebaseAuth.getInstance().signOut()

                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

                startActivity(intent)

                finish()
            }
            .setNegativeButton("Não") { dialog, _ ->

                dialog.dismiss()
            }
            .show()
    }

    private fun setupRecyclerView() {

        postAdapter = PostAdapter(posts)


        binding.recyclerViewPosts.apply {
            adapter = postAdapter
            layoutManager = LinearLayoutManager(this@HomePagePost)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setupPostsListener() {
        postsListener = firestore.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("HomePagePost", "Listen failed.", e)
                    return@addSnapshotListener
                }

                posts.clear()

                for (doc in snapshots!!) {
                    val post = doc.toObject(Post::class.java)
                    posts.add(post)
                }

                postAdapter.notifyDataSetChanged()
            }
    }
}