package com.ufersa.myapplication.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.text
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ufersa.myapplication.R
import com.ufersa.myapplication.model.Post

class PostAdapter(private val posts: List<Post>) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userNameTextView: TextView = itemView.findViewById(R.id.textViewUserName)
        val postImageView: ImageView = itemView.findViewById(R.id.imageViewPost)
        val descriptionTextView: TextView = itemView.findViewById(R.id.textViewDescription)
        val profileImageView: ImageView = itemView.findViewById(R.id.imageViewProfile) // Adicionado aqui
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
        return PostViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val currentPost = posts[position]
        holder.userNameTextView.text = "@" + currentPost.userName // Adicionado o @
        holder.descriptionTextView.text = currentPost.description

        Glide.with(holder.itemView.context)
            .load(currentPost.imageUrl)
            .into(holder.postImageView)

        // Carrega a imagem de perfil
        Glide.with(holder.itemView.context)
            .load(currentPost.profileImageUrl)
            .placeholder(R.drawable.ic_profile) // Imagem padrão enquanto carrega
            .error(R.drawable.ic_profile) // Imagem padrão se der erro
            .circleCrop() // Deixa a imagem circular
            .into(holder.profileImageView)
    }

    override fun getItemCount(): Int {
        return posts.size
    }
}