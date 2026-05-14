package com.lucathomas.stockscanner

import android.os.Bundle
import android.view.*
import android.widget.TableRow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.lucathomas.stockscanner.databinding.FragmentFirstBinding
import org.json.JSONObject

class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DataViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.currentProduct.observe(viewLifecycleOwner) { product ->
            if (product != null) {
                updateUI(product)
            } else {
                resetUI()
            }
        }
    }

    private fun updateUI(product: OFFProduct) {
        binding.tvName.text = product.name
        binding.tvBrandName.text = product.brand
        binding.tvCategory.text = product.category
        binding.tvIngredients.text = product.ingredients

        updateNutriScoreUI(product.nutriScore)
        updateNovaGroupUI(product.novaGroup)
        updateEcoScoreUI(product.ecoScore)
        updateAdditivesUI(product.additives)

        if (product.allergens.isNotEmpty()) {
            binding.labelAllergens.isVisible = true
            binding.tvAllergens.isVisible = true
            binding.tvAllergens.text = product.allergens.replace("en:", "").replace("de:", "").replace(",", ", ")
        } else {
            binding.labelAllergens.isVisible = false
            binding.tvAllergens.isVisible = false
        }

        Glide.with(this)
            .load(product.imageUrl)
            .placeholder(android.R.drawable.ic_menu_report_image)
            .error(android.R.drawable.ic_menu_report_image)
            .into(binding.ivProductImage)

        populateNutrimentsTable(product.nutriments)
    }

    private fun resetUI() {
        binding.tvName.text = getString(R.string.scan_barcode)
        binding.tvBrandName.text = ""
        binding.tvCategory.text = ""
        binding.ivProductImage.setImageResource(android.R.drawable.ic_menu_report_image)
        binding.tvIngredients.text = ""
        binding.labelAllergens.isVisible = false
        binding.tvAllergens.isVisible = false
        binding.cardNutriScore.isVisible = false
        binding.cardNovaGroup.isVisible = false
        binding.cardEcoScore.isVisible = false
        binding.labelAdditives.isVisible = false
        binding.chipGroupAdditives.isVisible = false
        binding.tableNutriments.removeAllViews()
    }

    private fun updateNutriScoreUI(nutriScore: String) {
        val score = nutriScore.lowercase()
        val hasScore = score.isNotEmpty() && score != "unknown"
        binding.cardNutriScore.isVisible = hasScore
        if (!hasScore) return

        val colors = mapOf(
            "a" to listOf(R.color.white, R.color.nutriYellow, R.color.nutriYellow, R.color.nutriOrange, R.color.nutriRed),
            "b" to listOf(R.color.nutriGreen, R.color.white, R.color.nutriYellow, R.color.nutriOrange, R.color.nutriRed),
            "c" to listOf(R.color.nutriGreen, R.color.nutriLightGreen, R.color.white, R.color.nutriOrange, R.color.nutriRed),
            "d" to listOf(R.color.nutriGreen, R.color.nutriLightGreen, R.color.nutriYellow, R.color.white, R.color.nutriRed),
            "e" to listOf(R.color.nutriGreen, R.color.nutriLightGreen, R.color.nutriYellow, R.color.nutriOrange, R.color.white)
        )
        val colorList = colors[score] ?: listOf(R.color.white, R.color.white, R.color.white, R.color.white, R.color.white)
        val textViews = listOf(binding.tvNutriScoreA, binding.tvNutriScoreB, binding.tvNutriScoreC, binding.tvNutriScoreD, binding.tvNutriScoreE)
        textViews.forEachIndexed { index, textView ->
            textView.setTextColor(ContextCompat.getColor(requireContext(), colorList[index]))
            textView.alpha = if (score == textView.text.toString().lowercase()) 1.0f else 0.3f
        }
    }

    private fun updateNovaGroupUI(novaGroup: String) {
        val hasNova = novaGroup.isNotEmpty() && novaGroup != "unknown"
        binding.cardNovaGroup.isVisible = hasNova
        if (!hasNova) return

        binding.tvNovaValue.text = novaGroup
        val color = when (novaGroup) {
            "1" -> R.color.nutriGreen
            "2" -> R.color.nutriYellow
            "3" -> R.color.nutriOrange
            "4" -> R.color.nutriRed
            else -> android.R.color.darker_gray
        }
        binding.tvNovaValue.setBackgroundColor(ContextCompat.getColor(requireContext(), color))
    }

    private fun updateEcoScoreUI(ecoScore: String) {
        val score = ecoScore.lowercase()
        val hasEco = score.isNotEmpty() && score != "unknown"
        binding.cardEcoScore.isVisible = hasEco
        if (!hasEco) return

        binding.tvEcoScoreValue.text = score.uppercase()
        val color = when (score) {
            "a" -> R.color.nutriGreen
            "b" -> R.color.nutriLightGreen
            "c" -> R.color.nutriYellow
            "d" -> R.color.nutriOrange
            "e" -> R.color.nutriRed
            else -> android.R.color.darker_gray
        }
        binding.tvEcoScoreValue.setBackgroundColor(ContextCompat.getColor(requireContext(), color))
    }

    private fun updateAdditivesUI(additives: String) {
        binding.chipGroupAdditives.removeAllViews()
        val list = additives.split(",").filter { it.isNotBlank() }
        val hasAdditives = list.isNotEmpty()
        binding.labelAdditives.isVisible = hasAdditives
        binding.chipGroupAdditives.isVisible = hasAdditives

        list.forEach { additive ->
            val chip = Chip(requireContext()).apply {
                text = additive.trim().uppercase()
                isClickable = false
                setChipBackgroundColorResource(R.color.colorSurfaceVariant)
            }
            binding.chipGroupAdditives.addView(chip)
        }
    }

    private fun populateNutrimentsTable(nutriments: JSONObject) {
        binding.tableNutriments.removeAllViews()
        val keys = nutriments.keys()
        val sortedKeys = mutableListOf<String>()
        while (keys.hasNext()) sortedKeys.add(keys.next())

        sortedKeys.filter { it.endsWith("_100g") }.sortedBy { it }.forEach { key ->
            val value = nutriments.optString(key, "")
            if (value.isNotEmpty() && value != "null") {
                val row = TableRow(requireContext())
                val keyTextView = TextView(requireContext()).apply {
                    text = translateKey(key)
                    setPadding(16, 12, 16, 12)
                    layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1.5f)
                }
                val valueTextView = TextView(requireContext()).apply {
                    val unit = nutriments.optString("${key.removeSuffix("_100g")}_unit", "g")
                    text = "$value $unit"
                    setPadding(16, 12, 16, 12)
                    layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
                    gravity = Gravity.END
                }
                row.addView(keyTextView)
                row.addView(valueTextView)
                binding.tableNutriments.addView(row)
            }
        }
    }

    private fun translateKey(key: String): String {
        return when (key.lowercase()) {
            "energy_100g", "energy-kj_100g" -> "Energie (kJ)"
            "energy-kcal_100g" -> "Energie (kcal)"
            "fat_100g" -> "Fett"
            "saturated-fat_100g" -> "- gesättigte Fettsäuren"
            "carbohydrates_100g" -> "Kohlenhydrate"
            "sugars_100g" -> "- Zucker"
            "fiber_100g" -> "Ballaststoffe"
            "proteins_100g" -> "Eiweiß"
            "salt_100g" -> "Salz"
            "sodium_100g" -> "Natrium"
            else -> key.replace("_100g", "").replace("-", " ").replaceFirstChar { it.uppercase() }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
