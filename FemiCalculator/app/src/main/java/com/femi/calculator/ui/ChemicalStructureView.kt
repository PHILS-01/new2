package com.femi.calculator.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.femi.calculator.engine.StructureEngine
import kotlin.math.max

class ChemicalStructureView @JvmOverloads constructor(context:Context,attrs:AttributeSet?=null):View(context,attrs){
    private var structure:StructureEngine.Structure?=null
    private val bond=Paint().apply{color=Color.LTGRAY;strokeWidth=6f;isAntiAlias=true}
    private val atom=Paint().apply{color=Color.parseColor("#202020");style=Paint.Style.FILL;isAntiAlias=true}
    private val outline=Paint().apply{color=Color.WHITE;style=Paint.Style.STROKE;strokeWidth=3f;isAntiAlias=true}
    private val text=Paint().apply{color=Color.WHITE;textSize=34f;textAlign=Paint.Align.CENTER;isAntiAlias=true}
    private val title=Paint().apply{color=Color.LTGRAY;textSize=22f;textAlign=Paint.Align.CENTER;isAntiAlias=true}
    fun setStructure(s:StructureEngine.Structure){structure=s;invalidate()}
    override fun onDraw(c:Canvas){super.onDraw(c);c.drawColor(Color.parseColor("#121212"));val s=structure?:return;val scale=minOf(width,height)*0.22f;val cx=width/2f;val cy=height/2f+15f
        fun pt(x:Float,y:Float)=cx+x*scale to cy+y*scale
        s.bonds.forEach{b->val(aX,aY)=pt(s.atoms[b.a].x,s.atoms[b.a].y);val(bX,bY)=pt(s.atoms[b.b].x,s.atoms[b.b].y);val dx=bX-aX;val dy=bY-aY;val len=max(1f,kotlin.math.sqrt(dx*dx+dy*dy));val nx=-dy/len*8f;val ny=dx/len*8f;for(k in 0 until b.order){val off=(k-(b.order-1)/2f);c.drawLine(aX+nx*off,aY+ny*off,bX+nx*off,bY+ny*off,bond)}}
        s.atoms.forEach{val(x,y)=pt(it.x,it.y);c.drawCircle(x,y,42f,atom);c.drawCircle(x,y,42f,outline);c.drawText(it.element,x,y+12f,text)};c.drawText(s.name,cx,32f,title)
    }
}
