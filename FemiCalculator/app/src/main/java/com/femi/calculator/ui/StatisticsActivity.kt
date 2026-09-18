package com.femi.calculator.ui

import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.femi.calculator.databinding.ActivityStatisticsBinding
import com.femi.calculator.engine.*
import java.util.Locale

class StatisticsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStatisticsBinding
    private enum class Mode { STATS, ANOVA, ANOVA2, SPEARMAN, REGRESSION, GROUPED_RAW, GROUPED_TABLE, OGIVE, BAR, PIE, FUNNEL, SCATTER }
    private data class Tool(val label:String,val mode:Mode,val instructions:String,val placeholder:String)
    private val tools = listOf(
        Tool("Descriptive Statistics",Mode.STATS,"Enter comma-separated numbers.","4,8,15,16,23,42"),
        Tool("One-Way ANOVA",Mode.ANOVA,"Grid: first row contains group titles; remaining rows contain observations. Empty cells are ignored.","Group A | Group B | Group C"),
        Tool("Two-Way ANOVA (with interaction)",Mode.ANOVA2,"Grid: first row contains Factor-B titles and first column contains Factor-A titles. Each data cell may contain comma-separated replications.","Factor A / Factor B"),
        Tool("Spearman Rank Correlation",Mode.SPEARMAN,"2 × 6 grid: first column contains the row titles; the remaining five columns contain paired observations.","Rank 1 / Rank 2"),
        Tool("Linear Regression (X, Y)",Mode.REGRESSION,"6 × 2 grid: first row contains X and Y titles; five rows contain data pairs.","X | Y"),
        Tool("Grouped Data: From Raw List",Mode.GROUPED_RAW,"Enter raw values. Optionally set the first class interval, e.g. 1 to 5. The output is Class Interval, Midpoint, f and cf.","12,15,18,20,22,25,28,30,31,33,35,38,40,42,45,48,50,52,55,60"),
        Tool("Grouped Data: From Frequency Table",Mode.GROUPED_TABLE,"Enter lower-upper:frequency, one class per line.","0-10:5\n10-20:8\n20-30:15"),
        Tool("Ogive Curve",Mode.OGIVE,"Enter raw values. Quartile positions Q1, Median and Q3 are traced on the ogive.","12,15,18,20,22,25,28,30,31,33,35,38,40,42,45,48,50,52,55,60"),
        Tool("Bar Chart",Mode.BAR,"Enter label:value pairs separated by commas.","Jan:120, Feb:150, Mar:90"),
        Tool("Pie Chart",Mode.PIE,"Enter label:value pairs separated by commas.","A:40, B:25, C:20"),
        Tool("Funnel Chart",Mode.FUNNEL,"Enter stage:value pairs separated by commas.","Visitors:1000, Signups:400, Purchases:120"),
        Tool("Scatter Plot",Mode.SCATTER,"Enter x,y pairs separated by semicolons.","1,2; 2,4; 3,5; 4,8")
    )
    private var rows = 6; private var cols = 6
    private var cells = mutableListOf<MutableList<EditText>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); binding=ActivityStatisticsBinding.inflate(layoutInflater); setContentView(binding.root)
        setSupportActionBar(binding.toolbar); supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val adapter=ArrayAdapter(this,R.layout.spinner_item_white,tools.map{it.label}); adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_white); binding.spinnerTool.adapter=adapter
        binding.spinnerTool.onItemSelectedListener=object:AdapterView.OnItemSelectedListener{override fun onItemSelected(p:AdapterView<*>?,v:View?,pos:Int,id:Long){applyTool(tools[pos])};override fun onNothingSelected(p:AdapterView<*>?) {}}
        binding.btnRun.setOnClickListener { runTool() }
        binding.btnRowsMinus.setOnClickListener { resizeTable(rows-1,cols) }; binding.btnRowsPlus.setOnClickListener { resizeTable(rows+1,cols) }
        binding.btnColsMinus.setOnClickListener { resizeTable(rows,cols-1) }; binding.btnColsPlus.setOnClickListener { resizeTable(rows,cols+1) }
        applyTool(tools[0])
    }

    private fun applyTool(tool:Tool){
        binding.txtInstructions.text=tool.instructions; binding.txtStatsResult.text=""; binding.chartContainer.removeAllViews(); binding.tableContainer.removeAllViews(); cells.clear();
        val gridMode=tool.mode in setOf(Mode.ANOVA,Mode.ANOVA2,Mode.SPEARMAN,Mode.REGRESSION)
        binding.tableControls.visibility=if(gridMode)View.VISIBLE else View.GONE; binding.inputData.visibility=if(gridMode)View.GONE else View.VISIBLE
        binding.rawGroupingControls.visibility=if(tool.mode==Mode.GROUPED_RAW)View.VISIBLE else View.GONE
        val chart=tool.mode in setOf(Mode.BAR,Mode.PIE,Mode.FUNNEL,Mode.SCATTER,Mode.OGIVE)
        binding.chartContainer.visibility=if(chart)View.VISIBLE else View.GONE; binding.checkboxShowLabels.visibility=if(tool.mode==Mode.BAR)View.VISIBLE else View.GONE
        when(tool.mode){Mode.ANOVA->{rows=6;cols=6};Mode.ANOVA2->{rows=6;cols=6};Mode.SPEARMAN->{rows=2;cols=6};Mode.REGRESSION->{rows=6;cols=2};else->{rows=0;cols=0}}
        if(gridMode) buildGrid(tool.mode)
        binding.inputData.hint=tool.placeholder; binding.inputData.setText("")
        if(tool.mode==Mode.GROUPED_RAW){binding.editClassStart.setText("");binding.editClassEnd.setText("")}
    }

    private fun resizeTable(r:Int,c:Int){ if(r<2||c<2||r>12||c>12)return; val old=Array(cells.size){rr->Array(if(cells.isNotEmpty())cells[0].size else 0){cc->cells[rr][cc].text.toString()}}; rows=r;cols=c; buildGrid(binding.spinnerTool.selectedItemPosition.let{tools[it].mode},old) }
    private fun buildGrid(mode:Mode, old:Array<Array<String>>?=null){
        binding.tableContainer.removeAllViews();cells.clear();binding.txtTableSize.text="$rows × $cols"
        for(r in 0 until rows){
            val tr=TableRow(this);tr.layoutParams=TableLayout.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,dp(48)); val row=mutableListOf<EditText>()
            for(c in 0 until cols){
                val e=EditText(this);e.setBackgroundResource(R.drawable.calc_edit);e.setTextColor(Color.WHITE);e.setHintTextColor(Color.GRAY);e.gravity=Gravity.CENTER;e.textSize=13f;e.singleLine=true;e.inputType=InputType.TYPE_CLASS_TEXT;e.layoutParams=TableRow.LayoutParams(dp(92),dp(46));
                val prior=old?.getOrNull(r)?.getOrNull(c); if(prior!=null)e.setText(prior)
                if(r==0 && mode in setOf(Mode.ANOVA,Mode.ANOVA2,Mode.REGRESSION))e.hint=if(mode==Mode.ANOVA2 && c==0)"A / B" else "Title"
                if(mode==Mode.ANOVA2 && c==0 && r>0)e.hint="A title"
                if(mode==Mode.SPEARMAN && c==0)e.hint=if(r==0)"Rank 1" else "Rank 2"
                tr.addView(e);row.add(e)
            }
            binding.tableContainer.addView(tr);cells.add(row)
        }
    }

    private fun valuesGrid():Array<Array<String>>=Array(rows){r->Array(cols){c->cells[r][c].text.toString().trim()}}
    private fun runTool(){
        val mode=tools[binding.spinnerTool.selectedItemPosition].mode
        try{when(mode){Mode.STATS->runStats();Mode.ANOVA->runAnova();Mode.ANOVA2->runAnova2();Mode.SPEARMAN->runSpearman();Mode.REGRESSION->runRegression();Mode.GROUPED_RAW->runGroupedRaw();Mode.GROUPED_TABLE->runGroupedTable();Mode.OGIVE->runOgive();Mode.BAR->runBar();Mode.PIE->runPie();Mode.FUNNEL->runFunnel();Mode.SCATTER->runScatter()}}
        catch(e:Exception){binding.txtStatsResult.text="Error: ${e.message ?: "Please check the input"}"}
    }
    private fun runStats(){val v=parseNums(binding.inputData.text.toString());val r=StatisticsEngine.analyze(v);binding.txtStatsResult.text="Count=${r.count}\nSum=${fmt(r.sum)}\nMean=${fmt(r.mean)}\nMedian=${fmt(r.median)}\nMode=${r.mode.joinToString(","){fmt(it)}}\nVariance=${fmt(r.variance)}\nSD=${fmt(r.stdDev)}\nMD=${fmt(r.meanDeviation)}\nMin=${fmt(r.min)}  Max=${fmt(r.max)}  Range=${fmt(r.range)}"}
    private fun runAnova(){val g=valuesGrid();val groups=(0 until cols).map{c->(1 until rows).mapNotNull{r->g[r][c].toDoubleOrNull()}}.filter{it.isNotEmpty()};val r=AnovaEngine.oneWayAnova(groups);binding.txtStatsResult.text="Groups=${r.groupCount}\nN=${r.totalCount}\nSS between=${fmt(r.ssBetween)}\nSS within=${fmt(r.ssWithin)}\ndf=${r.dfBetween}, ${r.dfWithin}\nMS=${fmt(r.msBetween)}, ${fmt(r.msWithin)}\nF=${fmt(r.fStatistic)}\np=${fmt(r.pValue)}"}
    private fun runAnova2(){val g=valuesGrid();val aOrder=(1 until rows).mapNotNull{g[it][0].takeIf{it.isNotEmpty()}}.distinct();val bOrder=(1 until cols).mapNotNull{g[0][it].takeIf{it.isNotEmpty()}}.distinct();val cellsMap=LinkedHashMap<Pair<String,String>,List<Double>>();for((ri,a)in aOrder.withIndex())for((ci,b)in bOrder.withIndex()){val raw=g[ri+1][ci+1];val vals=raw.split(",").filter{it.isNotBlank()}.map{it.trim().toDouble()};cellsMap[a to b]=vals};val r=AnovaEngine.twoWayAnova(aOrder,bOrder,cellsMap);binding.txtStatsResult.text="Factor A: SS=${fmt(r.ssA)} df=${r.dfA} MS=${fmt(r.msA)} F=${fmt(r.fA)} p=${fmt(r.pA)}\nFactor B: SS=${fmt(r.ssB)} df=${r.dfB} MS=${fmt(r.msB)} F=${fmt(r.fB)} p=${fmt(r.pB)}\nInteraction: SS=${fmt(r.ssAB)} df=${r.dfAB} MS=${fmt(r.msAB)} F=${fmt(r.fAB)} p=${fmt(r.pAB)}\nWithin: SS=${fmt(r.ssWithin)} df=${r.dfWithin} MS=${fmt(r.msWithin)}"}
    private fun runSpearman(){val g=valuesGrid();val labels=mutableListOf<String>();val a=mutableListOf<Double>();val b=mutableListOf<Double>();for(c in 1 until cols){val av=g[0][c].toDoubleOrNull();val bv=g[1][c].toDoubleOrNull();if(av!=null&&bv!=null){labels.add("${c}");a.add(av);b.add(bv)}};val r=CorrelationEngine.spearman(labels,a,b);val out=mutableListOf(listOf("Item","A","Rank A","B","Rank B","d","d²"));r.items.forEach{out.add(listOf(it.label,fmt(it.a),fmt(it.rankA),fmt(it.b),fmt(it.rankB),fmt(it.d),fmt(it.dSquared)))};showOutputTable(out);binding.txtStatsResult.append("\nrs = ${fmt(r.rs)}\nΣd² = ${fmt(r.sumDSquared)}")}
    private fun runRegression(){val g=valuesGrid();val x=mutableListOf<Double>();val y=mutableListOf<Double>();for(r in 1 until rows){val a=g[r][0].toDoubleOrNull();val b=g[r][1].toDoubleOrNull();if(a!=null&&b!=null){x.add(a);y.add(b)}};val r=RegressionEngine.linearRegression(x,y);binding.txtStatsResult.text="n=${r.n}\nΣx=${fmt(r.sumX)}  Σy=${fmt(r.sumY)}  Σxy=${fmt(r.sumXY)}\nΣx²=${fmt(r.sumX2)}  Σy²=${fmt(r.sumY2)}\nRegression: y=${fmt(r.intercept)} ${if(r.slope>=0)"+" else "−"} ${fmt(kotlin.math.abs(r.slope))}x\nr=${fmt(r.r)}\nr²=${fmt(r.rSquared)}"}
    private fun runGroupedRaw(){val v=parseNums(binding.inputData.text.toString());val s=binding.editClassStart.text.toString().trim();val e=binding.editClassEnd.text.toString().trim();val table=if(s.isNotEmpty()&&e.isNotEmpty())GroupedStatsEngine.buildFromRawData(v,s.toDouble(),e.toDouble())else GroupedStatsEngine.buildFromRawData(v);displayGrouped(table)}
    private fun runGroupedTable(){val entries=binding.inputData.text.toString().lines().filter{it.isNotBlank()}.map{line->val ci=line.indexOf(':');val range=line.substring(0,ci);val f=line.substring(ci+1).trim().toInt();val di=range.lastIndexOf('-');Triple(range.substring(0,di).trim().toDouble(),range.substring(di+1).trim().toDouble(),f)};displayGrouped(GroupedStatsEngine.buildFromDirectClasses(entries))}
    private fun displayGrouped(table:GroupedStatsEngine.FrequencyTable){val r=GroupedStatsEngine.computeGroupedStats(table);val out=mutableListOf(listOf("Class interval","Midpoint","f","cf"));table.classes.forEach{out.add(listOf("${fmt(it.lower)}–${fmt(it.upper)}",fmt(it.midpoint),it.frequency.toString(),it.cumulativeFrequency.toString()))};showOutputTable(out);binding.txtStatsResult.append("\nN=${table.n}\nMean=${fmt(r.mean)}\nMedian=${fmt(r.median)}\nMode=${fmt(r.mode)}\nQ1=${fmt(r.q1)}\nQ2=${fmt(r.q2)}\nQ3=${fmt(r.q3)}\nIQR=${fmt(r.iqr)}")}
    private fun runOgive(){val v=parseNums(binding.inputData.text.toString());val table=GroupedStatsEngine.buildFromRawData(v);val pts=mutableListOf(table.classes.first().lower to 0.0);table.classes.forEach{pts.add(it.upper to it.cumulativeFrequency.toDouble())};val r=GroupedStatsEngine.computeGroupedStats(table);val q1F=table.n/4.0;val medF=table.n/2.0;val q3F=3.0*table.n/4.0;val chart=OgiveView(this);chart.setData(pts,q1F,medF,q3F);binding.chartContainer.removeAllViews();binding.chartContainer.addView(chart);binding.txtStatsResult.text="Q1=${fmt(r.q1)}   Median=${fmt(r.q2)}   Q3=${fmt(r.q3)}"}
    private fun runBar(){val e=parsePairs(binding.inputData.text.toString()).map{BarChartView.Entry(it.first,it.second)};val c=BarChartView(this);c.setData(e,binding.checkboxShowLabels.isChecked);binding.chartContainer.removeAllViews();binding.chartContainer.addView(c)}
    private fun runPie(){val e=parsePairs(binding.inputData.text.toString()).map{PieChartView.Entry(it.first,it.second)};val c=PieChartView(this);c.setData(e);binding.chartContainer.removeAllViews();binding.chartContainer.addView(c)}
    private fun runFunnel(){val e=parsePairs(binding.inputData.text.toString()).map{FunnelChartView.Entry(it.first,it.second)};val c=FunnelChartView(this);c.setData(e);binding.chartContainer.removeAllViews();binding.chartContainer.addView(c)}
    private fun runScatter(){val pts=binding.inputData.text.toString().split(";").filter{it.isNotBlank()}.map{val p=it.split(",");p[0].trim().toDouble() to p[1].trim().toDouble()};val c=ScatterPlotView(this);c.setData(pts);binding.chartContainer.removeAllViews();binding.chartContainer.addView(c)}
    private fun parseNums(t:String)=t.split(",").filter{it.isNotBlank()}.map{it.trim().toDouble()}
    private fun parsePairs(t:String)=t.split(",").filter{it.isNotBlank()}.map{val i=it.lastIndexOf(':');it.substring(0,i).trim() to it.substring(i+1).trim().toDouble()}
    private fun showOutputTable(data:List<List<String>>){val table=TableLayout(this);table.setPadding(dp(2),dp(4),dp(2),dp(4));data.forEachIndexed{ri,row->val tr=TableRow(this);row.forEach{value->val tv=TextView(this);tv.text=value;tv.setTextColor(Color.WHITE);tv.gravity=Gravity.CENTER;tv.setPadding(dp(10),dp(8),dp(10),dp(8));tv.setBackgroundResource(if(ri==0)R.drawable.calc_edit else R.drawable.calc_button_dark);tr.addView(tv)};table.addView(tr)};binding.tableContainer.removeAllViews();binding.tableContainer.addView(table);binding.tableControls.visibility=View.GONE;binding.inputData.visibility=View.GONE}
    private fun fmt(v:Double)=if(v.isNaN()||v.isInfinite())"undefined" else String.format(Locale.US,"%.4f",v).trimEnd('0').trimEnd('.')
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    override fun onSupportNavigateUp():Boolean{onBackPressedDispatcher.onBackPressed();return true}
}
