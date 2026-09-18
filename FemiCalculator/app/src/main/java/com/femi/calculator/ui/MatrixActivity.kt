package com.femi.calculator.ui

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.femi.calculator.R
import com.femi.calculator.databinding.ActivityMatrixBinding
import com.femi.calculator.engine.Matrix
import com.femi.calculator.engine.MatrixException
import com.femi.calculator.engine.MatrixExpressionEngine

class MatrixActivity : AppCompatActivity() {
    private lateinit var binding:ActivityMatrixBinding
    private data class Entry(val name:String,val view:BracketedMatrixView)
    private val entries=mutableListOf<Entry>()

    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);binding=ActivityMatrixBinding.inflate(layoutInflater);setContentView(binding.root);setSupportActionBar(binding.toolbar);supportActionBar?.setDisplayHomeAsUpEnabled(true)
        addMatrix();addMatrix()
        binding.btnMatrixAdd.setOnClickListener{if(entries.size<10)addMatrix()else toast("Maximum 10 matrices")}
        binding.btnMatrixRemove.setOnClickListener{if(entries.size>2)removeMatrix()else toast("Keep at least A and B")}
        binding.btnCompute.setOnClickListener{compute()};binding.btnSolve.setOnClickListener{solveSystem()}
    }
    private fun addMatrix(){val name=('A'.code+entries.size).toChar().toString();val v=BracketedMatrixView(this);v.configure(2,2);entries.add(Entry(name,v));rebuildCards()}
    private fun removeMatrix(){entries.removeAt(entries.lastIndex);rebuildCards()}
    private fun rebuildCards(){binding.matricesContainer.removeAllViews();entries.forEach{entry->
        val card=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER_HORIZONTAL;setPadding(dp(4),dp(2),dp(4),dp(2))}
        val title=TextView(this).apply{text="Matrix ${entry.name}";setTextColor(Color.WHITE);textSize=17f;gravity=Gravity.CENTER}
        val controls=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
        val rowMinus=small("R−");val rowPlus=small("R+");val colMinus=small("C−");val colPlus=small("C+")
        val dims=TextView(this).apply{setTextColor(Color.LTGRAY);gravity=Gravity.CENTER;setPadding(dp(4),0,dp(4),0);text="2×2"}
        controls.addView(rowMinus);controls.addView(rowPlus);controls.addView(dims);controls.addView(colMinus);controls.addView(colPlus)
        fun update(){dims.text="${entry.view.matrixRows}×${entry.view.matrixCols}"}
        rowMinus.setOnClickListener{entry.view.configure(entry.view.matrixRows-1,entry.view.matrixCols);update()};rowPlus.setOnClickListener{entry.view.configure(entry.view.matrixRows+1,entry.view.matrixCols);update()};colMinus.setOnClickListener{entry.view.configure(entry.view.matrixRows,entry.view.matrixCols-1);update()};colPlus.setOnClickListener{entry.view.configure(entry.view.matrixRows,entry.view.matrixCols+1);update()}
        card.addView(title);card.addView(controls);card.addView(entry.view);binding.matricesContainer.addView(card)
    }}
    private fun small(s:String)=Button(this).apply{layoutParams=LinearLayout.LayoutParams(dp(42),dp(40));text=s;setPadding(0,0,0,0);textSize=11f}
    private fun compute(){binding.txtScalarResult.text="";binding.matrixResult.visibility=android.view.View.GONE;try{val env=entries.associate{it.name to it.view.toMatrix()};val expr=binding.editMatrixExpression.text.toString().trim();if(expr.isEmpty())throw MatrixException("Enter a matrix expression");when(val result=MatrixExpressionEngine.evaluate(expr,env)){is Matrix->showMatrixResult(result);is Double->binding.txtScalarResult.text="Result = ${fmt(result)}"}}catch(e:Exception){binding.txtScalarResult.text="Error: ${e.message ?: "could not compute"}"}}
    private fun showMatrixResult(m:Matrix){binding.matrixResult.visibility=android.view.View.VISIBLE;binding.matrixResult.showResult(m)}
    private fun solveSystem(){try{val a=entries.first().view.toMatrix();val b=binding.editVectorB.text.toString().split(",").filter{it.isNotBlank()}.map{it.trim().toDouble()}.toDoubleArray();val x=a.solveLinearSystem(b);binding.txtVectorResult.text="x = [${x.joinToString(", "){fmt(it)}}]"}catch(e:Exception){binding.txtVectorResult.text="Error: ${e.message}"}}
    private fun fmt(v:Double)=if(v==v.toLong().toDouble())v.toLong().toString() else String.format("%.4f",v)
    private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show();private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    override fun onSupportNavigateUp():Boolean{onBackPressedDispatcher.onBackPressed();return true}
}
