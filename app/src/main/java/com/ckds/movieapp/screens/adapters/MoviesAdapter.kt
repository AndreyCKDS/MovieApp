package com.ckds.movieapp.screens.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ckds.movieapp.R
import com.ckds.movieapp.data.model.movie.Movie
import com.ckds.movieapp.utils.Constants.Companion.POSTER_BASE_URL
import kotlinx.android.synthetic.main.item_poster.view.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

open class MoviesAdapter(open val viewModel: AdapterViewModel): RecyclerView.Adapter<MoviesAdapter.ViewHolder>() {

    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view)

    private val callback = object : DiffUtil.ItemCallback<Movie>() {
        override fun areItemsTheSame(oldItem: Movie, newItem: Movie): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Movie, newItem: Movie): Boolean {
            return oldItem == newItem
        }

    }

    val differ = AsyncListDiffer(this, callback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_poster,parent,false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val movie = differ.currentList[position]

        holder.itemView.apply {
            Glide.with(this).load("$POSTER_BASE_URL${movie.poster_path}")
                .error(R.drawable.no_image_sample).into(img_poster)
            img_poster.clipToOutline = true
            tv_name.text = movie.title

            setOnClickListener {
                onItemClickListener?.let { it(movie) }
            }

            MainScope().launch {
                val favorite = MainScope().async {viewModel.checkIfMovieIsFavorite(movie.id!!)}
                checkbox_favorite.isChecked = favorite.await()
            }
            checkbox_favorite.setOnCheckedChangeListener{ _, isChecked ->
                if (isChecked) {
                    viewModel.addMovieToFavourite(movie = movie)
                } else {
                    viewModel.deleteMovieFromFavourite(movieId = movie.id!!)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        val limit = 9
        differ.currentList.let { list ->
            return if (list.size > limit){
                limit
            } else {
                list.size
            }
        }
    }

    internal var onItemClickListener: ((Movie) -> Unit)? = null

    fun setOnItemClickListener(listener: (Movie) -> Unit) {
        onItemClickListener = listener
    }

}