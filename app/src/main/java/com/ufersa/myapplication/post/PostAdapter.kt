package com.ufersa.myapplication.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ufersa.myapplication.R
import com.ufersa.myapplication.model.Post

class PostAdapter(private val posts: List<Post>) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userNameTextView: TextView = itemView.findViewById(R.id.textViewUserName)
        val postImageView: ImageView = itemView.findViewById(R.id.imageViewPost)
        val descriptionTextView: TextView = itemView.findViewById(R.id.textViewDescription)
        val profileImageView: ImageView = itemView.findViewById(R.id.imageViewProfile)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
        return PostViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val currentPost = posts[position]
        holder.userNameTextView.text = "@" + currentPost.userName
        holder.descriptionTextView.text = currentPost.description

        Glide.with(holder.itemView.context)
            .load(currentPost.imageUrl)
            .into(holder.postImageView)


        Glide.with(holder.itemView.context)
            .load(currentPost.profileImageUrl)
            .placeholder(R.drawable.ic_profile)
            .error(R.drawable.ic_profile)
            .circleCrop()
            .into(holder.profileImageView)
    }

    override fun getItemCount(): Int {
        return posts.size
    }
}