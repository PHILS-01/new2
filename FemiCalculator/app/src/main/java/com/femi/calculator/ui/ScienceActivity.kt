package com.femi.calculator.ui

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.femi.calculator.databinding.ActivityScienceBinding
import com.femi.calculator.engine.*
import java.util.Locale
import kotlin.math.sqrt

class ScienceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScienceBinding

    private enum class Mode { FORMULA, CHART }

    private data class Tool(
        val label: String,
        val mode: Mode = Mode.FORMULA,
        val formula: String = "",
        val fieldLabels: List<String> = emptyList()
    )

    private data class Topic(val label: String, val tools: List<Tool>)

    private val topics = listOf(
        Topic("Electrolysis / Electrochemistry", listOf(
            Tool("Mole from Q", Mode.FORMULA, "n = Q / (F × e)", listOf("Q", "F (default 96500)", "e")),
            Tool("Mole from I, t", Mode.FORMULA, "n = I × t / (F × e)", listOf("I", "t", "F (default 96500)", "e")),
            Tool("Mass from Q", Mode.FORMULA, "m = mo × Q / (F × e)", listOf("mo (molar mass)", "Q", "F (default 96500)", "e")),
            Tool("Mass from I, t", Mode.FORMULA, "m = I × t × mo / (F × e)", listOf("mo (molar mass)", "I", "t", "F (default 96500)", "e")),
            Tool("Volume from Q", Mode.FORMULA, "V = 22.4 × Q / (F × e)", listOf("F (default 96500)", "e", "Q")),
            Tool("Volume from I, t", Mode.FORMULA, "V = 22.4 × I × t / (F × e)", listOf("I", "t", "F (default 96500)", "e")),
            Tool("Q from mole", Mode.FORMULA, "Q = n × F × e", listOf("n", "F (default 96500)", "e")),
            Tool("Q from mass", Mode.FORMULA, "Q = m × F × e / mo", listOf("m", "mo (molar mass)", "F (default 96500)", "e")),
            Tool("Q from volume", Mode.FORMULA, "Q = V × F × e / 22.4", listOf("V", "F (default 96500)", "e")),
            Tool("Solve e from mass", Mode.FORMULA, "e = I × t × mo / (F × m)", listOf("I", "t", "mo (molar mass)", "F (default 96500)", "m")),
            Tool("Electrochemical cell E.m.f.", Mode.FORMULA, "Emf = Ecathode − Eanode", listOf("Ecathode (right)", "Eanode (left)")),
            Tool("Farad to Coulomb", Mode.FORMULA, "C = 96500 × F", listOf("F (farads)"))
        )),
        Topic("Half-life", listOf(
            Tool("Solve t½", Mode.FORMULA, "t½ = −t × ln(2) / ln(N/No)", listOf("t (elapsed time)", "N/No (fraction)")),
            Tool("Solve N/No", Mode.FORMULA, "N/No = 2^(−t / t½)", listOf("t (elapsed time)", "t½ (half-life)")),
            Tool("Solve elapsed time", Mode.FORMULA, "t = −t½ × ln(N/No) / ln(2)", listOf("t½ (half-life)", "N/No (fraction)"))
        )),
        Topic("Mechanics", listOf(
            Tool("Final velocity", Mode.FORMULA, "v = u + at", listOf("u (initial velocity)", "a (acceleration)", "t (time)")),
            Tool("Displacement", Mode.FORMULA, "s = ut + ½at²", listOf("u (initial velocity)", "a (acceleration)", "t (time)")),
            Tool("Velocity from v² = u² + 2as", Mode.FORMULA, "v = √(u² + 2as)", listOf("u (initial velocity)", "a (acceleration)", "s (displacement)")),
            Tool("Force", Mode.FORMULA, "F = ma", listOf("m (mass)", "a (acceleration)")),
            Tool("Momentum", Mode.FORMULA, "p = mv", listOf("m (mass)", "v (velocity)")),
            Tool("Kinetic Energy", Mode.FORMULA, "KE = ½mv²", listOf("m (mass)", "v (velocity)")),
            Tool("Potential Energy", Mode.FORMULA, "PE = mgh", listOf("m (mass)", "g (gravity)", "h (height)")),
            Tool("Power", Mode.FORMULA, "P = W / t", listOf("W (work)", "t (time)")),
            Tool("Velocity-Time Graph", Mode.CHART)
        )),
        Topic("Magnetism", listOf(
            Tool("Force on conductor", Mode.FORMULA, "F = BIL sin(θ)", listOf("B (field)", "I (current)", "L (length)", "θ (degrees)")),
            Tool("Force on moving charge", Mode.FORMULA, "F = qvB sin(θ)", listOf("q (charge)", "v (velocity)", "B (field)", "θ (degrees)")),
            Tool("Field around a wire", Mode.FORMULA, "B = μ0 I / (2π r)", listOf("I (current)", "r (distance)")),
            Tool("Torque on a coil", Mode.FORMULA, "τ = N I A B sin(θ)", listOf("N (turns)", "I (current)", "A (area)", "B (field)", "θ (degrees)"))
        )),
        Topic("Electromagnetism", listOf(
            Tool("Induced EMF (Faraday's law)", Mode.FORMULA, "ε = N × ΔΦ / Δt", listOf("N (turns)", "ΔΦ (flux change)", "Δt (time change)")),
            Tool("Motional EMF", Mode.FORMULA, "ε = BLv", listOf("B (field)", "L (length)", "v (velocity)")),
            Tool("Inductor energy", Mode.FORMULA, "E = ½LI²", listOf("L (inductance)", "I (current)")),
            Tool("Transformer secondary voltage", Mode.FORMULA, "Vs = Vp × (Ns / Np)", listOf("Vp (primary V)", "Np (primary turns)", "Ns (secondary turns)"))
        )),
        Topic("Quantum Physics", listOf(
            Tool("Photon energy from frequency", Mode.FORMULA, "E = hf", listOf("f (frequency, Hz)")),
            Tool("Photon energy from wavelength", Mode.FORMULA, "E = hc / λ", listOf("λ (wavelength, m)")),
            Tool("de Broglie wavelength", Mode.FORMULA, "λ = h / (mv)", listOf("m (mass, kg)", "v (velocity, m/s)")),
            Tool("Photoelectric max KE", Mode.FORMULA, "KEmax = hf − φ", listOf("f (frequency, Hz)", "φ (work function, J)")),
            Tool("Hydrogen energy level", Mode.FORMULA, "E_n = −13.6 / n² (eV)", listOf("n (level)"))
        )),
        Topic("Spectroscopy", listOf(
            Tool("Rydberg wavelength", Mode.FORMULA, "1/λ = R(1/n1² − 1/n2²)", listOf("n1", "n2")),
            Tool("Transition energy", Mode.FORMULA, "ΔE = hc / λ", listOf("λ (wavelength, m)")),
            Tool("Wavenumber", Mode.FORMULA, "v̄ = 1 / λ", listOf("λ (wavelength, m)")),
            Tool("Doppler shift fraction", Mode.FORMULA, "Δλ/λ = v / c", listOf("v (source velocity, m/s)"))
        )),
        Topic("Waves", listOf(
            Tool("Wave speed", Mode.FORMULA, "v = fλ", listOf("f (frequency)", "λ (wavelength)")),
            Tool("Period", Mode.FORMULA, "T = 1 / f", listOf("f (frequency)")),
            Tool("String harmonic frequency", Mode.FORMULA, "f_n = nv / (2L)", listOf("n (harmonic)", "v (wave speed)", "L (string length)")),
            Tool("Intensity", Mode.FORMULA, "I = P / A", listOf("P (power)", "A (area)"))
        )),
        Topic("Fluid Mechanics", listOf(
            Tool("Pressure from force", Mode.FORMULA, "P = F / A", listOf("F (force)", "A (area)")),
            Tool("Hydrostatic pressure", Mode.FORMULA, "P = ρgh", listOf("ρ (density)", "g (gravity)", "h (depth)")),
            Tool("Buoyant force (Archimedes)", Mode.FORMULA, "F = ρ_fluid × g × V", listOf("ρ_fluid (density)", "g (gravity)", "V (volume displaced)")),
            Tool("Continuity equation → v2", Mode.FORMULA, "A1v1 = A2v2", listOf("A1", "v1", "A2")),
            Tool("Bernoulli's equation → P2", Mode.FORMULA, "P1 + ½ρv1² = P2 + ½ρv2²", listOf("P1", "ρ (density)", "v1", "v2"))
        ))
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScienceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val topicAdapter = ArrayAdapter(this, com.femi.calculator.R.layout.spinner_item_white, topics.map { it.label })
        topicAdapter.setDropDownViewResource(com.femi.calculator.R.layout.spinner_dropdown_item_white)
        binding.spinnerTopic.adapter = topicAdapter
        binding.spinnerTopic.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                populateTools(topics[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.spinnerTool.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val topic = topics[binding.spinnerTopic.selectedItemPosition]
                applyTool(topic.tools[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        populateTools(topics[0])
        binding.btnCalculate.setOnClickListener { calculate() }
        binding.btnDrawStructure.setOnClickListener {
            try {
                val view = ChemicalStructureView(this)
                view.setStructure(StructureEngine.fromName(binding.editStructureName.text.toString()))
                binding.structureContainer.removeAllViews()
                binding.structureContainer.addView(view, android.widget.FrameLayout.LayoutParams(-1, -1))
            } catch (e: Exception) {
                android.widget.Toast.makeText(this, e.message ?: "Structure not found", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun populateTools(topic: Topic) {
        val adapter = ArrayAdapter(this, com.femi.calculator.R.layout.spinner_item_white, topic.tools.map { it.label })
        adapter.setDropDownViewResource(com.femi.calculator.R.layout.spinner_dropdown_item_white)
        binding.spinnerTool.adapter = adapter
        applyTool(topic.tools[0])
    }

    private fun fieldViews(): List<EditText> =
        listOf(binding.inputA, binding.inputB, binding.inputC, binding.inputD, binding.inputE)

    private fun applyTool(tool: Tool) {
        binding.chartContainer.removeAllViews()
        val isChart = tool.mode == Mode.CHART
        binding.txtScienceResult.visibility = if (isChart) View.GONE else View.VISIBLE
        binding.chartContainer.visibility = if (isChart) View.VISIBLE else View.GONE

        if (isChart) {
            binding.txtFormula.text = "Enter t,v pairs separated by semicolons, e.g. 0,0; 1,4; 2,8; 3,6; 4,0"
            val fields = fieldViews()
            for (f in fields) f.visibility = View.GONE
            binding.inputA.visibility = View.VISIBLE
            binding.inputA.inputType = InputType.TYPE_CLASS_TEXT
            binding.inputA.hint = "0,0; 1,4; 2,8; 3,6; 4,0"
            binding.inputA.setText("")
            binding.btnCalculate.text = "Plot"
        } else {
            binding.txtFormula.text = tool.formula
            val fields = fieldViews()
            binding.inputA.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
            for (i in fields.indices) {
                val label = tool.fieldLabels.getOrNull(i)
                fields[i].hint = label ?: ""
                fields[i].visibility = if (label != null) View.VISIBLE else View.GONE
                fields[i].setText(if (label != null && label.startsWith("F (default")) "96500" else "")
            }
            binding.btnCalculate.text = "Calculate"
        }
        binding.txtScienceResult.text = ""
    }

    private fun num(v: EditText): Double = v.text.toString().trim().toDouble()

    private fun calculate() {
        val topic = topics[binding.spinnerTopic.selectedItemPosition]
        val tool = topic.tools[binding.spinnerTool.selectedItemPosition]
        try {
            if (tool.mode == Mode.CHART) {
                plotVelocityTime()
                return
            }
            val result: String = when (tool.label) {
                "Mole from Q" -> "n = ${fmt(ElectrochemistryEngine.moleFromQ(num(binding.inputA), num(binding.inputB), num(binding.inputC)))} mol"
                "Mole from I, t" -> "n = ${fmt(ElectrochemistryEngine.moleFromCurrentTime(num(binding.inputA), num(binding.inputB), num(binding.inputC), num(binding.inputD)))} mol"
                "Mass from Q" -> "m = ${fmt(ElectrochemistryEngine.massFromQ(num(binding.inputA), num(binding.inputB), num(binding.inputC), num(binding.inputD)))} g"
                "Mass from I, t" -> "m = ${fmt(ElectrochemistryEngine.massFromCurrentTime(num(binding.inputA), num(binding.inputB), num(binding.inputC), num(binding.inputD), num(binding.inputE)))} g"
                "Volume from Q" -> "V = ${fmt(ElectrochemistryEngine.volumeFromQ(num(binding.inputA), num(binding.inputB), num(binding.inputC)))} dm³"
                "Volume from I, t" -> "V = ${fmt(ElectrochemistryEngine.volumeFromCurrentTime(num(binding.inputA), num(binding.inputB), num(binding.inputC), num(binding.inputD)))} dm³"
                "Q from mole" -> "Q = ${fmt(ElectrochemistryEngine.chargeFromMole(num(binding.inputA), num(binding.inputB), num(binding.inputC)))} C"
                "Q from mass" -> "Q = ${fmt(ElectrochemistryEngine.chargeFromMass(num(binding.inputA), num(binding.inputB), num(binding.inputC), num(binding.inputD)))} C"
                "Q from volume" -> "Q = ${fmt(ElectrochemistryEngine.chargeFromVolume(num(binding.inputA), num(binding.inputB), num(binding.inputC)))} C"
                "Solve e from mass" -> "e = ${fmt(ElectrochemistryEngine.electronMoleRatioFromMass(num(binding.inputA), num(binding.inputB), num(binding.inputC), num(binding.inputD), num(binding.inputE)))}"
                "Electrochemical cell E.m.f." -> "E.m.f. = ${fmt(ElectrochemistryEngine.emfOfCell(num(binding.inputA), num(binding.inputB)))} V"
                "Farad to Coulomb" -> "C = ${fmt(ElectrochemistryEngine.faradToCoulomb(num(binding.inputA)))} C"

                "Solve t½" -> "t½ = ${fmt(HalfLifeEngine.halfLifeFromTimeAndFraction(num(binding.inputA), num(binding.inputB)))}"
                "Solve N/No" -> "N/No = ${fmt(HalfLifeEngine.fractionFromTimeAndHalfLife(num(binding.inputA), num(binding.inputB)))}"
                "Solve elapsed time" -> "t = ${fmt(HalfLifeEngine.timeFromHalfLifeAndFraction(num(binding.inputA), num(binding.inputB)))}"

                "Final velocity" -> "v = ${fmt(MechanicsEngine.finalVelocity(num(binding.inputA), num(binding.inputB), num(binding.inputC)))} m/s"
                "Displacement" -> "s = ${fmt(MechanicsEngine.displacement(num(binding.inputA), num(binding.inputB), num(binding.inputC)))} m"
                "Velocity from v² = u² + 2as" -> {
                    val vSq = MechanicsEngine.velocitySquared(num(binding.inputA), num(binding.inputB), num(binding.inputC))
                    if (vSq < 0) "No real solution (v² is negative)" else "v = ${fmt(sqrt(vSq))} m/s"
                }
                "Force" -> "F = ${fmt(MechanicsEngine.force(num(binding.inputA), num(binding.inputB)))} N"
                "Momentum" -> "p = ${fmt(MechanicsEngine.momentum(num(binding.inputA), num(binding.inputB)))} kg·m/s"
                "Kinetic Energy" -> "KE = ${fmt(MechanicsEngine.kineticEnergy(num(binding.inputA), num(binding.inputB)))} J"
                "Potential Energy" -> "PE = ${fmt(MechanicsEngine.potentialEnergy(num(binding.inputA), num(binding.inputB), num(binding.inputC)))} J"
                "Power" -> "P = ${fmt(MechanicsEngine.power(num(binding.inputA), num(binding.inputB)))} W"

                "Force on conductor" -> "F = ${fmt(MagnetismEngine.forceOnConductor(num(binding.inputA), num(binding.inputB), num(binding.inputC), num(binding.inputD)))} N"
                "Force on moving charge" -> "F = ${fmt(MagnetismEngine.forceOnMovingCharge(num(binding.inputA), num(binding.inputB), num(binding.inputC), num(binding.inputD)))} N"
                "Field around a wire" -> "B = ${fmt(MagnetismEngine.fieldAroundWire(num(binding.inputA), num(binding.inputB)))} T"
                "Torque on a coil" -> "τ = ${fmt(MagnetismEngine.torqueOnCoil(num(binding.inputA), num(binding.inputB), num(binding.inputC), num(binding.inputD), num(binding.inputE)))} N·m"

                "Induced EMF (Faraday's law)" -> "ε = ${fmt(ElectromagnetismEngine.inducedEmf(num(binding.inputA), num(binding.inputB), num(binding.inputC)))} V"
                "Motional EMF" -> "ε = ${fmt(ElectromagnetismEngine.motionalEmf(num(binding.inputA), num(binding.inputB), num(binding.inputC)))} V"
                "Inductor energy" -> "E = ${fmt(ElectromagnetismEngine.inductorEnergy(num(binding.inputA), num(binding.inputB)))} J"
                "Transformer secondary voltage" -> "Vs = ${fmt(ElectromagnetismEngine.transformerSecondaryVoltage(num(binding.inputA), num(binding.inputB), num(binding.inputC)))} V"

                "Photon energy from frequency" -> "E = ${fmt(QuantumEngine.photonEnergyFromFrequency(num(binding.inputA)))} J"
                "Photon energy from wavelength" -> "E = ${fmt(QuantumEngine.photonEnergyFromWavelength(num(binding.inputA)))} J"
                "de Broglie wavelength" -> "λ = ${fmt(QuantumEngine.deBroglieWavelength(num(binding.inputA), num(binding.inputB)))} m"
                "Photoelectric max KE" -> "KEmax = ${fmt(QuantumEngine.photoelectricKineticEnergy(num(binding.inputA), num(binding.inputB)))} J"
                "Hydrogen energy level" -> "E_n = ${fmt(QuantumEngine.hydrogenEnergyLevel(num(binding.inputA)))} eV"

                "Rydberg wavelength" -> "λ = ${fmt(SpectroscopyEngine.rydbergWavelength(num(binding.inputA), num(binding.inputB)))} m"
                "Transition energy" -> "ΔE = ${fmt(SpectroscopyEngine.transitionEnergy(num(binding.inputA)))} J"
                "Wavenumber" -> "v̄ = ${fmt(SpectroscopyEngine.wavenumber(num(binding.inputA)))} m⁻¹"
                "Doppler shift fraction" -> "Δλ/λ = ${fmt(SpectroscopyEngine.dopplerShiftFraction(num(binding.inputA)))}"

                "Wave speed" -> "v = ${fmt(WavesEngine.waveSpeed(num(binding.inputA), num(binding.inputB)))} m/s"
                "Period" -> "T = ${fmt(WavesEngine.period(num(binding.inputA)))} s"
                "String harmonic frequency" -> "f_n = ${fmt(WavesEngine.stringHarmonicFrequency(num(binding.inputA), num(binding.inputB), num(binding.inputC)))} Hz"
                "Intensity" -> "I = ${fmt(WavesEngine.intensity(num(binding.inputA), num(binding.inputB)))} W/m²"

                "Pressure from force" -> "P = ${fmt(FluidMechanicsEngine.pressureFromForce(num(binding.inputA), num(binding.inputB)))} Pa"
                "Hydrostatic pressure" -> "P = ${fmt(FluidMechanicsEngine.hydrostaticPressure(num(binding.inputA), num(binding.inputB), num(binding.inputC)))} Pa"
                "Buoyant force (Archimedes)" -> "F = ${fmt(FluidMechanicsEngine.buoyantForce(num(binding.inputA), num(binding.inputB), num(binding.inputC)))} N"
                "Continuity equation → v2" -> "v2 = ${fmt(FluidMechanicsEngine.continuityV2(num(binding.inputA), num(binding.inputB), num(binding.inputC)))} m/s"
                "Bernoulli's equation → P2" -> "P2 = ${fmt(FluidMechanicsEngine.bernoulliP2(num(binding.inputA), num(binding.inputB), num(binding.inputC), num(binding.inputD)))} Pa"

                else -> "Unsupported"
            }
            binding.txtScienceResult.text = result
        } catch (e: Exception) {
            binding.txtScienceResult.text = "Please check your input values."
        }
    }

    private fun plotVelocityTime() {
        try {
            val text = binding.inputA.text.toString()
            val points = text.split(";").map { it.trim() }.filter { it.isNotEmpty() }.map { pair ->
                val parts = pair.split(",").map { it.trim() }
                if (parts.size != 2) throw IllegalArgumentException("Use t,v format")
                parts[0].toDouble() to parts[1].toDouble()
            }
            if (points.size < 2) {
                android.widget.Toast.makeText(this, "Enter at least 2 t,v points", android.widget.Toast.LENGTH_SHORT).show()
                binding.chartContainer.removeAllViews()
                return
            }
            val chart = VelocityTimeGraphView(this)
            chart.setData(points)
            binding.chartContainer.removeAllViews()
            binding.chartContainer.addView(chart)
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "Please check the t,v point format.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun fmt(v: Double): String {
        if (v.isNaN() || v.isInfinite()) return "undefined"
        return if (kotlin.math.abs(v) != 0.0 && (kotlin.math.abs(v) < 0.0001 || kotlin.math.abs(v) >= 1.0e9)) {
            String.format(Locale.US, "%.6e", v)
        } else {
            val s = String.format(Locale.US, "%.6f", v)
            s.trimEnd('0').trimEnd('.')
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
