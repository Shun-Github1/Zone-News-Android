package com.searcher.zonenews.ui.mainfrag

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.recyclerview.widget.RecyclerView
import com.searcher.zonenews.R

class FooterAdapter : RecyclerView.Adapter<FooterAdapter.FooterViewHolder>() {

    private var isLoading = false

    fun setLoading(loading: Boolean) {
        if (isLoading != loading) {
            isLoading = loading
            notifyItemChanged(0)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FooterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_footer_loader, parent, false)
        return FooterViewHolder(view)
    }

    override fun onBindViewHolder(holder: FooterViewHolder, position: Int) {
        holder.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        // When not loading, collapse the entire item to minimize space
        holder.itemView.visibility = if (isLoading) View.VISIBLE else View.GONE
        val layoutParams = holder.itemView.layoutParams
        if (isLoading) {
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        } else {
            layoutParams.height = 0
        }
        holder.itemView.layoutParams = layoutParams
    }

    override fun getItemCount(): Int = 1

    class FooterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val progressBar: ProgressBar = itemView.findViewById(R.id.load_more_progress)
    }
}
