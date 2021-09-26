package com.helicoptera.cga.saammod

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {

    private var a_random = 0
    private var m_random: Int = 0
    private var r0_random: Int = 0
    private var rn1 = 0.0
    private var rn = 0.0

    private var a = 0.0
    private var b = 0.0
    private var mx = 0.0
    private var sigma = 0.0
    private var lambda = 0.0
    private var nu = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.calculate).setOnClickListener {
            onCalculateClick()
        }
        onCalculateClick()
    }

    private fun onCalculateClick() {
        a_random = findViewById<EditText>(R.id.a_random).text.toString().toInt()
        m_random = findViewById<EditText>(R.id.m_random).text.toString().toInt()
        r0_random = findViewById<EditText>(R.id.r0_random).text.toString().toInt()
        a = findViewById<EditText>(R.id.a).text.toString().toDouble()
        b = findViewById<EditText>(R.id.b).text.toString().toDouble()
        mx = findViewById<EditText>(R.id.mx).text.toString().toDouble()
        lambda = findViewById<EditText>(R.id.lambda).text.toString().toDouble()
        sigma = findViewById<EditText>(R.id.sigma).text.toString().toDouble()
        nu = findViewById<EditText>(R.id.nu).text.toString().toInt()

        rn = r0_random.toDouble()

        val randoms = mutableListOf<Double>()
        for (i in 0 until N) {
            randoms.add(random())
        }
        calculateEstimates(randoms)
        calculateHistogram(randoms)

    }

    private fun random(): Double {
        val group = findViewById<RadioGroup>(R.id.group)
        val id = group.checkedRadioButtonId

        return when (id) {
            R.id.unifogm -> uniformDistribution()
            R.id.gaussian -> gaussianDistribution()
            R.id.exponential -> exponentialDistribution()
            R.id.gamma -> gammalDistribution()
            R.id.triangle -> triangleDistribution()
            R.id.triangle_reverse -> triangleReverseDistribution()
            R.id.simpson -> simpsonDistribution()
            else -> throw Exception()
        }
    }

    private fun uniformDistribution(): Double {
        val r1: Double = lehmerAlgorithm()
        val res = a + (b - a) * r1
        return res
    }

    private fun gaussianDistribution(): Double {
        val r1 = mutableListOf<Double>()
        for (i in 0..11) {
            r1.add(lehmerAlgorithm())
        }
        return mx + sigma * (r1.toDoubleArray().sum() - 6)
    }

    private fun exponentialDistribution(): Double {
        val r1: Double = lehmerAlgorithm()
        val res = -1 * ln(r1) / lambda
        return res
    }

    private fun gammalDistribution(): Double {
        val r1 = mutableListOf<Double>()
        for (i in 0 until nu) {
            r1.add(lehmerAlgorithm())
        }
        val X: Double
        var sum = 0.0
        for (i in 0 until nu) {
            sum += ln(r1[i])
        }
        X = -1 * sum / sigma
        return X
    }

    private fun triangleDistribution(): Double {
        val r1: Double = lehmerAlgorithm()
        val R2: Double = lehmerAlgorithm()
        return a + (b - a) * Math.max(r1, R2)
    }

    private fun triangleReverseDistribution(): Double {
        val r1: Double = lehmerAlgorithm()
        val R2: Double = lehmerAlgorithm()
        return a + (b - a) * r1.coerceAtMost(R2)
    }

    private fun simpsonDistribution(): Double {
        val r1: Double = lehmerAlgorithm()
        val R2: Double = lehmerAlgorithm()
        val X: Double = (Math.max(a, b) - Math.min(a, b)) * (r1 + R2) / 2 + a
        return X
    }

    private fun lehmerAlgorithm(): Double {
        rn1 = rn
        rn = a_random * rn1 % m_random
        return rn / m_random
    }

    private fun calculateHistogram(randoms: List<Double>) {
        val count = DoubleArray(INTERVAL_COUNT) { 0.0 }
        val intervals = DoubleArray(INTERVAL_COUNT) { 0.0 }
        val min = randoms.toDoubleArray().minOrNull()!!
        val max = randoms.toDoubleArray().maxOrNull()!!
        val intervalStep = (max - min) / INTERVAL_COUNT
        var left = min
        var right = left + intervalStep

        for (i in 0 until INTERVAL_COUNT) {
            for (j in randoms.indices) {
                if (randoms[j] >= left && randoms[j] < right) {
                    count[i]++
                }
            }
            val intervalValue = (right - left) / 2
            intervals[i] = intervalValue

            left += intervalStep
            right += intervalStep
        }

        val frequencies = count.map { it / randoms.size }

        val chart = findViewById<BarChart>(R.id.barchart)

        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            labelCount = INTERVAL_COUNT + 1
            mEntryCount = INTERVAL_COUNT + 1

            textSize = 0.5F
            valueFormatter = CustomFormatter(intervalStep.toFloat())
        }

        chart.axisLeft.apply {
            axisMinimum = 0F
            labelCount = 10
        }

        val entries = mutableListOf<BarEntry>()
        for (i in 0 until INTERVAL_COUNT) {
            entries.add(BarEntry(i.toFloat() , frequencies[i].toFloat()))
        }

        val dataSet = BarDataSet(entries, "Частота значений").apply {
            valueTextSize = 5F
            colors = ColorTemplate.COLORFUL_COLORS.toList()
        }

        val data = BarData(dataSet)
        chart.animateY(3000)
        chart.data = data;
        chart.setFitBars(true);
        chart.invalidate()
    }

    private class CustomFormatter(val intervalStep: Float) : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            val value = (intervalStep * value).toString()
            return value.substring(0, min(5, value.length))
        }
    }

    private fun calculateEstimates(randoms: List<Double>) {
        val mx = findViewById<TextView>(R.id.calculated_mx)
        val dx = findViewById<TextView>(R.id.calculated_dx)
        val sx = findViewById<TextView>(R.id.calculated_sx)

        val mxValue = randoms.sum() / N
        var dxValue = 0.0


        for (i in 0 until N) {
            dxValue += (randoms[i] - mxValue).pow(2)
        }

        dxValue /= N - 1
        val sxValue = sqrt(dxValue)

        mx.text = mxValue.toString()
        dx.text = dxValue.toString()
        sx.text = sxValue.toString()
    }

    companion object {
        private const val N = 1_000_000
        private const val INTERVAL_COUNT = 20
    }
}