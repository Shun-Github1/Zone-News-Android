package com.searcher.zonenews.ui.newsdetail

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.searcher.zonenews.R
import com.searcher.zonenews.databinding.FragmentSavedArticlesBottomSheetBinding
import com.searcher.zonenews.entry.ViewHisEntry
import com.searcher.zonenews.model.MyModel
import com.searcher.zonenews.ui.newsdetail.NewsDetailActivity
import com.searcher.zonenews.utils.Constants
import com.searcher.zonenews.utils.SystemDialogUtils
import com.searcher.zonenews.utils.ToastUtils
import com.searcher.zonenews.utils.Utils
import com.searcher.zonenews.utils.glide.GlideApp
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.zhy.adapter.recyclerview.CommonAdapter
import com.zhy.adapter.recyclerview.base.ViewHolder

class SavedArticlesBottomSheetFragment : BottomSheetDialogFragment() {
    
    private var _binding: FragmentSavedArticlesBottomSheetBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var myModel: MyModel
    private var articlesList = ArrayList<ViewHisEntry.DataDTO.ArticlesDTO>()
    private lateinit var articlesAdapter: CommonAdapter<ViewHisEntry.DataDTO.ArticlesDTO>
    private val swipeRevealedPositions = HashSet<Int>()
    private var currentRevealedPosition = -1
    private var recentlyRevealedPosition = -1 // Track if we just revealed to prevent same-swipe confirmation
    private var isClearingAll = false
    private var pendingDeleteCount = 0
    
    companion object {
        fun newInstance(): SavedArticlesBottomSheetFragment {
            return SavedArticlesBottomSheetFragment()
        }
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        // Apply custom animation style to slow down the popup animation
        dialog.window?.let { window ->
            val params = window.attributes
            params.windowAnimations = R.style.BottomSheetAnimation
            window.attributes = params
        }
        return dialog
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSavedArticlesBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViewModel()
        setupViews()
        loadData()
    }
    

    


    private fun setupViewModel() {
        myModel = ViewModelProvider(requireActivity())[MyModel::class.java]
        
        // Observe saved articles data
        myModel.myCollectEntry.observe(viewLifecycleOwner) { response ->
            if (response != null) {
                if (response.code == Constants.SUCCESS_CODE) {
                    articlesList.clear()
                    articlesList.addAll(response.data.articles)
                    swipeRevealedPositions.clear()
                    currentRevealedPosition = -1
                    articlesAdapter.notifyDataSetChanged()
                    updateUI()
                } else {
                    if (response.code == 1000) {
                        ToastUtils.showShortToast(requireContext(), getString(R.string.server_error_message))
                    } else {
                        ToastUtils.showShortToast(requireContext(), response.msg)
                    }
                }
            }
        }
        
        // Observe delete response
        myModel.commonResponseEntry.observe(viewLifecycleOwner) { response ->
            if (response != null) {
                if (response.code == Constants.SUCCESS_CODE) {
                    if (isClearingAll) {
                        // Decrement pending count
                        pendingDeleteCount--
                        if (pendingDeleteCount <= 0) {
                            // All deletions complete
                            isClearingAll = false
                            ToastUtils.showShortToast(requireContext(), getString(R.string.cleared_all_items))
                            loadData()
                        }
                    } else {
                        // Single item delete - reload data after successful delete
                        loadData()
                    }
                } else {
                    if (response.code == 1000) {
                        ToastUtils.showShortToast(requireContext(), getString(R.string.server_error_message))
                    } else {
                        ToastUtils.showShortToast(requireContext(), response.msg)
                    }
                    // If clearing all and we got an error, still continue
                    if (isClearingAll) {
                        pendingDeleteCount--
                        if (pendingDeleteCount <= 0) {
                            isClearingAll = false
                            loadData()
                        }
                    }
                }
            }
        }
    }
    
    private fun setupViews() {
        // Setup close button
        binding.closeButton.setOnClickListener {
            dismiss()
        }
        
        // Setup clear all button
        binding.clearAllButton.setOnClickListener {
            showClearAllConfirmation()
        }
        
        // Setup recycler views
        setupRecyclerViews()
    }
    
    private fun setupRecyclerViews() {
        // Single adapter for articles
        articlesAdapter = object : CommonAdapter<ViewHisEntry.DataDTO.ArticlesDTO>(
            requireContext(),
            R.layout.item_reading_history_news,
            articlesList
        ) {
            override fun convert(holder: ViewHolder, article: ViewHisEntry.DataDTO.ArticlesDTO, position: Int) {
                val dateTimeText = holder.getView<android.widget.TextView>(R.id.dateTimeText)
                val newsTitleText = holder.getView<android.widget.TextView>(R.id.newsTitleText)
                val newsImage = holder.getView<android.widget.ImageView>(R.id.newsImage)
                val cardContent = holder.getView<View>(R.id.cardContent)
                val deleteButtonLayout = holder.getView<View>(R.id.deleteButtonLayout)
                
                // Format date and time with HTML formatting using centralized utility
                dateTimeText.text = Html.fromHtml(Utils.formatBackendDateWithTime(article.date), Html.FROM_HTML_MODE_COMPACT)
                
                // Set title
                newsTitleText.text = article.title
                
                // Load image
                GlideApp.with(requireContext())
                    .load(article.pictureURL)
                    .error(R.drawable.ic_image_not_supported_24)
                    .into(newsImage)
                
                // Set view state based on revealed positions
                // Cancel any ongoing animations first to prevent conflicts
                cardContent.animate().cancel()
                deleteButtonLayout.animate().cancel()
                
                if (swipeRevealedPositions.contains(position)) {
                    // Position should be revealed
                    val deleteButtonWidth = if (deleteButtonLayout.width > 0) {
                        deleteButtonLayout.width.toFloat()
                    } else {
                        (80 * holder.convertView.context.resources.displayMetrics.density)
                    }
                    cardContent.translationX = -deleteButtonWidth
                    deleteButtonLayout.alpha = 1f
                } else {
                    // Position should be reset
                    cardContent.translationX = 0f
                    deleteButtonLayout.alpha = 0f
                }
                
                // Delete button click listener
                deleteButtonLayout.setOnClickListener {
                    confirmDelete(article.articleID, position)
                }
                
                // Set click listeners on card content
                cardContent.setOnClickListener {
                    if (swipeRevealedPositions.contains(position)) {
                        // Hide delete button
                        hideDeleteButton(position)
                    } else {
                        openNewsDetail(article.articleID)
                    }
                }
                
                // Add long press listener for sharing with shrink animation
                cardContent.setOnLongClickListener {
                    if (!swipeRevealedPositions.contains(position)) {
                        // Animate shrink effect
                        cardContent.animate()
                            .scaleX(0.95f)
                            .scaleY(0.95f)
                            .setDuration(150)
                            .withEndAction {
                                // Restore original size after animation
                                cardContent.animate()
                                    .scaleX(1.0f)
                                    .scaleY(1.0f)
                                    .setDuration(300)
                                    .start()
                            }
                            .start()
                        
                        shareArticle(article)
                        true
                    } else {
                        false
                    }
                }
            }
        }
        
        // Set layout manager
        binding.newsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        
        // Setup swipe to delete
        setupSwipeToDelete()
    }
    
    private fun setupSwipeToDelete() {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false
            
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position != RecyclerView.NO_POSITION && position < articlesList.size) {
                    when (direction) {
                        ItemTouchHelper.LEFT -> {
                            if (swipeRevealedPositions.contains(position)) {
                                // Swipe left when revealed - confirm delete
                                // Only allow if this is a new swipe (not the same one that revealed)
                                if (recentlyRevealedPosition != position) {
                                    confirmDelete(articlesList[position].articleID, position)
                                }
                                // Reset the recently revealed flag
                                recentlyRevealedPosition = -1
                            } else {
                                // First swipe left - reveal delete button
                                revealDeleteButton(position)
                                // Mark this position as recently revealed to prevent immediate confirmation
                                recentlyRevealedPosition = position
                            }
                        }
                        ItemTouchHelper.RIGHT -> {
                            // Swipe right - cancel deletion (hide delete button)
                            // Only do this if delete button is revealed
                            if (swipeRevealedPositions.contains(position)) {
                                hideDeleteButton(position)
                            }
                            // Reset the recently revealed flag
                            recentlyRevealedPosition = -1
                        }
                    }
                }
            }
            
            override fun onChildDraw(
                c: android.graphics.Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val itemView = viewHolder.itemView
                    val cardContent = itemView.findViewById<View>(R.id.cardContent)
                    val deleteButtonLayout = itemView.findViewById<View>(R.id.deleteButtonLayout)
                    val position = viewHolder.adapterPosition
                    
                    if (dX < 0) {
                        // Swiping left
                        val deleteButtonWidth = if (deleteButtonLayout.width > 0) {
                            deleteButtonLayout.width.toFloat()
                        } else {
                            (80 * itemView.context.resources.displayMetrics.density)
                        }
                        val translationX = dX.coerceIn(-deleteButtonWidth, 0f)
                        cardContent.translationX = translationX
                        
                        // Fade in delete button based on swipe progress
                        val swipeProgress = (-translationX / deleteButtonWidth).coerceIn(0f, 1f)
                        deleteButtonLayout.alpha = swipeProgress
                        
                        // Draw without moving the item itself
                        super.onChildDraw(c, recyclerView, viewHolder, 0f, dY, actionState, false)
                    } else if (dX > 0 && position != RecyclerView.NO_POSITION && swipeRevealedPositions.contains(position)) {
                        // Swiping right when delete button is revealed - slide back
                        val deleteButtonWidth = if (deleteButtonLayout.width > 0) {
                            deleteButtonLayout.width.toFloat()
                        } else {
                            (80 * itemView.context.resources.displayMetrics.density)
                        }
                        val translationX = dX.coerceIn(0f, deleteButtonWidth)
                        cardContent.translationX = -deleteButtonWidth + translationX
                        
                        // Fade out delete button based on swipe progress
                        val swipeProgress = (translationX / deleteButtonWidth).coerceIn(0f, 1f)
                        deleteButtonLayout.alpha = 1f - swipeProgress
                        
                        // Draw without moving the item itself
                        super.onChildDraw(c, recyclerView, viewHolder, 0f, dY, actionState, false)
                    } else {
                        // Swiping right when delete button is not revealed - block the swipe
                        deleteButtonLayout.alpha = 0f
                        super.onChildDraw(c, recyclerView, viewHolder, 0f, dY, actionState, false)
                    }
                } else {
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                }
            }
            
            override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                val position = viewHolder.adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    // Only allow right swipe if delete button is revealed
                    if (swipeRevealedPositions.contains(position)) {
                        return ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
                    } else {
                        // Only allow LEFT swipe when delete is not revealed
                        return ItemTouchHelper.LEFT
                    }
                }
                return super.getSwipeDirs(recyclerView, viewHolder)
            }
            
            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                val position = viewHolder.adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val itemView = viewHolder.itemView
                    val cardContent = itemView.findViewById<View>(R.id.cardContent)
                    val deleteButtonLayout = itemView.findViewById<View>(R.id.deleteButtonLayout)
                    
                    // If the card has been dragged back close to its original position,
                    // treat it as a cancelled delete and clear the revealed state.
                    val deleteButtonWidth = if (deleteButtonLayout.width > 0) {
                        deleteButtonLayout.width.toFloat()
                    } else {
                        (80 * itemView.context.resources.displayMetrics.density)
                    }
                    val isFullyClosed = cardContent.translationX >= -deleteButtonWidth * 0.25f
                    if (isFullyClosed && swipeRevealedPositions.contains(position)) {
                        swipeRevealedPositions.remove(position)
                        if (currentRevealedPosition == position) {
                            currentRevealedPosition = -1
                        }
                        cardContent.translationX = 0f
                        deleteButtonLayout.alpha = 0f
                    }
                    
                    // Reset recently revealed flag when swipe gesture ends (touch lifts)
                    // This ensures the next swipe will be treated as a new gesture
                    if (recentlyRevealedPosition == position) {
                        recentlyRevealedPosition = -1
                    }
                }
                
                super.clearView(recyclerView, viewHolder)
            }
            
            override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
                val position = viewHolder.adapterPosition
                if (position != RecyclerView.NO_POSITION && swipeRevealedPositions.contains(position)) {
                    // Confirmation swipe (delete button already revealed) - require 80% threshold
                    // This makes accidental deletion much harder
                    return 0.8f
                } else {
                    // Initial swipe to reveal delete button - lower threshold (40%) for easier reveal
                    return 0.4f
                }
            }
            
            override fun getSwipeVelocityThreshold(defaultValue: Float): Float {
                // Require higher velocity (1.5x default) to trigger swipe on quick flicks
                // Applies to both LEFT and RIGHT swipes equally
                return defaultValue * 1.5f
            }
        })
        
        itemTouchHelper.attachToRecyclerView(binding.newsRecyclerView)
    }
    
    private fun revealDeleteButton(position: Int) {
        // Hide previously revealed item
        if (currentRevealedPosition != -1 && currentRevealedPosition != position) {
            hideDeleteButton(currentRevealedPosition)
        }
        
        swipeRevealedPositions.add(position)
        currentRevealedPosition = position
        
        val viewHolder = binding.newsRecyclerView.findViewHolderForAdapterPosition(position)
        viewHolder?.itemView?.let { itemView ->
            val cardContent = itemView.findViewById<View>(R.id.cardContent)
            val deleteButtonLayout = itemView.findViewById<View>(R.id.deleteButtonLayout)
            
            // Use 80dp converted to pixels (delete button width)
            val deleteButtonWidth = (80 * resources.displayMetrics.density).toInt()
            val width = if (deleteButtonLayout.width > 0) deleteButtonLayout.width else deleteButtonWidth
            
            cardContent.animate()
                .translationX(-width.toFloat())
                .setDuration(200)
                .start()
            
            // Fade in delete button
            deleteButtonLayout.animate()
                .alpha(1f)
                .setDuration(200)
                .start()
        }
    }
    
    private fun hideDeleteButton(position: Int) {
        // Remove from set first to prevent clearView from interfering
        swipeRevealedPositions.remove(position)
        if (currentRevealedPosition == position) {
            currentRevealedPosition = -1
        }
        
        val viewHolder = binding.newsRecyclerView.findViewHolderForAdapterPosition(position)
        viewHolder?.itemView?.let { itemView ->
            val cardContent = itemView.findViewById<View>(R.id.cardContent)
            val deleteButtonLayout = itemView.findViewById<View>(R.id.deleteButtonLayout)
            
            // Cancel any existing animations first
            cardContent.animate().cancel()
            deleteButtonLayout.animate().cancel()
            
            // Animate to reset position
            cardContent.animate()
                .translationX(0f)
                .setDuration(200)
                .start()
            
            // Fade out delete button
            deleteButtonLayout.animate()
                .alpha(0f)
                .setDuration(200)
                .start()
        }
    }
    
    private fun confirmDelete(articleId: String, position: Int) {
        val viewHolder = binding.newsRecyclerView.findViewHolderForAdapterPosition(position)
        viewHolder?.itemView?.let { itemView ->
            val cardContent = itemView.findViewById<View>(R.id.cardContent)
            val deleteButtonLayout = itemView.findViewById<View>(R.id.deleteButtonLayout)
            
            // Slide card out to the left
            cardContent.animate()
                .translationX(-itemView.width.toFloat())
                .setDuration(300)
                .start()
            
            // Fade out delete button
            deleteButtonLayout.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction {
                    swipeRevealedPositions.remove(position)
                    if (currentRevealedPosition == position) {
                        currentRevealedPosition = -1
                    }
                    myModel.deleteCollect(articleId)
                }
                .start()
        } ?: run {
            // If view holder is null, just delete directly
            swipeRevealedPositions.remove(position)
            if (currentRevealedPosition == position) {
                currentRevealedPosition = -1
            }
            myModel.deleteCollect(articleId)
        }
    }
    
    private fun loadData() {
        showLoading()
        myModel.queryMyCollect()
    }
    
    private fun updateUI() {
        val hasItems = articlesList.isNotEmpty()
        
        // Update clear all button visibility
        binding.clearAllButton.isVisible = hasItems
        
        // Update content visibility
        if (hasItems) {
            binding.loadingView.stopShimmer()
            binding.loadingView.isVisible = false
            binding.emptyView.isVisible = false
            binding.newsRecyclerView.isVisible = true
            
            // Set adapter
            binding.newsRecyclerView.adapter = articlesAdapter
        } else {
            binding.loadingView.stopShimmer()
            binding.loadingView.isVisible = false
            binding.newsRecyclerView.isVisible = false
            binding.emptyView.isVisible = true
        }
    }
    
    private fun showLoading() {
        binding.loadingView.isVisible = true
        binding.loadingView.startShimmer()
        binding.emptyView.isVisible = false
        binding.newsRecyclerView.isVisible = false
    }
    
    private fun showClearAllConfirmation() {
        SystemDialogUtils.showAlertDialog(
            requireContext(),
            getString(R.string.clear_saved_articles_confirmation_title),
            getString(R.string.clear_saved_articles_confirmation_message),
            getString(R.string.clear_saved_articles_confirmation_confirm),
            getString(R.string.dialog_button_cancel),
            isDestructive = true,
            onPositiveClick = {
                clearAllItems()
            }
        )
    }
    
    private fun clearAllItems() {
        if (articlesList.isEmpty()) {
            return
        }
        
        // Create a copy of the article IDs to delete
        val articleIdsToDelete = articlesList.map { it.articleID }.toList()
        
        // Set tracking variables
        isClearingAll = true
        pendingDeleteCount = articleIdsToDelete.size
        
        // Clear the UI immediately for better UX
        articlesList.clear()
        articlesAdapter.notifyDataSetChanged()
        updateUI()
        
        // Delete all items from backend sequentially
        deleteAllItemsSequentially(articleIdsToDelete, 0)
    }
    
    private fun deleteAllItemsSequentially(articleIds: List<String>, index: Int) {
        if (index >= articleIds.size) {
            // All items deleted, show success message
            ToastUtils.showShortToast(requireContext(), getString(R.string.cleared_all_items))
            // Reload data to ensure UI is in sync
            loadData()
            return
        }
        
        val articleId = articleIds[index]
        myModel.deleteCollect(articleId)
        
        // Continue with next item after a short delay to avoid overwhelming the server
        // The delete response observer will handle reloading, but we'll continue deleting
        // We'll use a simple approach: delete all and let the observer reload once
        if (index < articleIds.size - 1) {
            // Delete next item
            deleteAllItemsSequentially(articleIds, index + 1)
        } else {
            // Last item, the observer will reload
        }
    }
    
    private fun openNewsDetail(articleId: String) {
        val intent = Intent(requireContext(), NewsDetailActivity::class.java)
        intent.putExtra("id", articleId)
        intent.putExtra("source_fragment", "my")
        startActivity(intent)
    }
    
    /**
     * Share article functionality
     */
    private fun shareArticle(article: ViewHisEntry.DataDTO.ArticlesDTO) {
        val shareText = buildString {
            append(article.title)
            if (!article.articleURL.isNullOrEmpty()) {
                append("\n\n")
                append(article.articleURL)
            }
        }
        
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText)
        startActivity(Intent.createChooser(shareIntent, getString(R.string.app_name)))
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

