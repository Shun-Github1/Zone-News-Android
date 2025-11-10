package com.anssy.znewspro.ui.mainfrag

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.anssy.znewspro.databinding.FragRecapBinding
import com.anssy.znewspro.selfview.RecapView

/**
 * Fragment displaying the "Recap" page with daily/weekly/monthly summaries
 */
class RecapFragment : Fragment() {
    
    private var _binding: FragRecapBinding? = null
    private val binding get() = _binding!!
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragRecapBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecapView()
    }
    
    private fun setupRecapView() {
        // Always replace existing content to avoid stale/old layout remaining
        binding.recapContainer.removeAllViews()
        
        // Create and add the new unified RecapView
        val recapView = RecapView(requireContext())
        binding.recapContainer.addView(recapView)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


