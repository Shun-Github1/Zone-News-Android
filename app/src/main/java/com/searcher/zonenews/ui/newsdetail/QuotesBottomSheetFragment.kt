package com.searcher.zonenews.ui.newsdetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.searcher.zonenews.R
import com.searcher.zonenews.entry.QuoteEntry
import java.io.Serializable

class QuotesBottomSheetFragment : BottomSheetDialogFragment() {
    
    private var quotes: List<QuoteEntry> = emptyList()
    private var onQuoteClickListener: ((QuoteEntry) -> Unit)? = null

    companion object {
        private const val ARG_QUOTES = "arg_quotes"

        fun newInstance(quotes: List<QuoteEntry>): QuotesBottomSheetFragment {
            val fragment = QuotesBottomSheetFragment()
            val args = Bundle()
            args.putSerializable(ARG_QUOTES, quotes as Serializable)
            fragment.arguments = args
            return fragment
        }
    }

    fun setOnQuoteClickListener(listener: (QuoteEntry) -> Unit) {
        onQuoteClickListener = listener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_quotes_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        @Suppress("UNCHECKED_CAST")
        quotes = arguments?.getSerializable(ARG_QUOTES) as? List<QuoteEntry> ?: emptyList()

        val closeButton = view.findViewById<View>(R.id.closeButton)
        closeButton.setOnClickListener { dismiss() }

        val recyclerView = view.findViewById<RecyclerView>(R.id.rv_quotes_list)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = QuotesBottomSheetAdapter(quotes) { quote ->
            dismiss()
            onQuoteClickListener?.invoke(quote)
        }
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.let { window ->
            val params = window.attributes
            params.windowAnimations = R.style.BottomSheetAnimation
            window.attributes = params
        }
        return dialog
    }
}
