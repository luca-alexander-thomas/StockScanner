package com.lucathomas.stockscanner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.lucathomas.stockscanner.databinding.FragmentSecondBinding

class SecondFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: DataViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonSecond.setOnClickListener {
            findNavController().navigateUp()
        }

        // Nutze den professionellen HistoryAdapter für die Liste
        viewModel.allData.observe(viewLifecycleOwner) { dataList ->
            val adapter = HistoryAdapter(requireContext(), dataList)
            binding.lvItems.adapter = adapter

            // Beim Klick auf ein Item werden die Details geladen
            binding.lvItems.setOnItemClickListener { _, _, position, _ ->
                val selectedData = dataList[position]
                viewModel.selectFromHistory(selectedData)
                // Navigiere zurück zur Detailansicht im FirstFragment
                findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
