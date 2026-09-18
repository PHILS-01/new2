package com.femi.calculator.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import java.util.Locale

/** Ogive with a connected trace and Q1/Median/Q3 construction lines. */
class OgiveView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private var points: List<Pair<Double,Double>> = emptyList()
    private var q1=Double.NaN; private var median=Double.NaN; private var q3=Double.NaN
    private val axis=Paint().apply{color=Color.GRAY;strokeWidth=2f;style=Paint.Style.STROKE}
    private val grid=Paint().apply{color=Color.DKGRAY;strokeWidth=1f}
    private val line=Paint().apply{color=Color.parseColor("#FF9800");strokeWidth=5f;style=Paint.Style.STROKE;isAntiAlias=true}
    private val marker=Paint().apply{color=Color.WHITE;strokeWidth=2f;style=Paint.Style.STROKE}
    private val text=Paint().apply{color=Color.LTGRAY;textSize=20f;isAntiAlias=true}
    private val markerText=Paint().apply{color=Color.WHITE;textSize=18f;isAntiAlias=true}

    fun setData(data:List<Pair<Double,Double>>, q1Value:Double=Double.NaN, medianValue:Double=Double.NaN, q3Value:Double=Double.NaN){points=data.sortedBy{it.first};q1=q1Value;median=medianValue;q3=q3Value;invalidate()}
    override fun onDraw(canvas:Canvas){super.onDraw(canvas);canvas.drawColor(Color.parseColor("#121212"));if(points.size<2)return
        val p=70f;val minX=points.minOf{it.first};val maxX=points.maxOf{it.first};val maxY=maxOf(1.0,points.maxOf{it.second});val rx=(maxX-minX).let{if(it==0.0)1.0 else it};val ry=maxY
        val l=p;val r=width-p;val t=p;val b=height-p-40f
        for(i in 0..5){val x=l+(r-l)*i/5f;val y=b-(b-t)*i/5f;canvas.drawLine(x,t,x,b,grid);canvas.drawLine(l,y,r,y,grid)}
        canvas.drawLine(l,b,r,b,axis);canvas.drawLine(l,t,l,b,axis)
        fun sx(x:Double)=(l+(x-minX)/rx*(r-l)).toFloat();fun sy(y:Double)=(b-y/ry*(b-t)).toFloat()
        val path=Path();path.moveTo(sx(points.first().first),sy(points.first().second));for(pt in points.drop(1))path.lineTo(sx(pt.first),sy(pt.second));canvas.drawPath(path,line)
        points.forEach{canvas.drawCircle(sx(it.first),sy(it.second),6f,line)}
        val qs=listOf("Q1" to q1,"Median" to median,"Q3" to q3)
        for((label,q) in qs) if(q.isFinite()){
            val idx=points.indexOfFirst{it.second>=q};val x=if(idx<0)maxX else points[idx].first;val y=q
            canvas.drawLine(sx(x),sy(0.0),sx(x),sy(y),marker);canvas.drawLine(l,sy(y),sx(x),sy(y),marker)
            canvas.drawCircle(sx(x),sy(y),8f,marker);canvas.drawText("$label = ${String.format(Locale.US,"%.2f",x)}",sx(x)+8f,sy(y)-8f,markerText)
        }
        canvas.drawText("Upper class boundary",l,b+32f,text);canvas.drawText("Cumulative frequency",l,t-22f,text)
        canvas.drawText(String.format(Locale.US,"%.1f",minX),l,b+58f,text);canvas.drawText(String.format(Locale.US,"%.1f",maxX),r-55f,b+58f,text);canvas.drawText("0",l-28f,b+5f,text);canvas.drawText(String.format(Locale.US,"%.0f",maxY),l-50f,t+8f,text)
    }
}
