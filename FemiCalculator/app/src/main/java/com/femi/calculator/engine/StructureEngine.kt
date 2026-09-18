package com.femi.calculator.engine

/** Name/formula lookup for common school-level molecules and ions. */
object StructureEngine {
    data class Atom(val element:String,val x:Float,val y:Float)
    data class Bond(val a:Int,val b:Int,val order:Int=1)
    data class Structure(val name:String,val atoms:List<Atom>,val bonds:List<Bond>)
    fun fromName(input:String):Structure{
        val key=input.trim().lowercase().replace(" ","")
        return when(key){
            "sulphur(iv)oxide","sulfur(iv)oxide","sulphur(4)oxide","sulfur(4)oxide","so2" -> Structure("Sulphur(IV) oxide — SO₂",listOf(Atom("S",0f,0f),Atom("O",-1f,0.75f),Atom("O",1f,0.75f)),listOf(Bond(0,1,2),Bond(0,2,2)))
            "carbon dioxide","co2" -> Structure("Carbon dioxide — CO₂",listOf(Atom("C",0f,0f),Atom("O",-1.5f,0f),Atom("O",1.5f,0f)),listOf(Bond(0,1,2),Bond(0,2,2)))
            "water","h2o" -> Structure("Water — H₂O",listOf(Atom("O",0f,0f),Atom("H",-1f,0.9f),Atom("H",1f,0.9f)),listOf(Bond(0,1),Bond(0,2)))
            "ammonia","nh3" -> Structure("Ammonia — NH₃",listOf(Atom("N",0f,0f),Atom("H",-1f,1f),Atom("H",1f,1f),Atom("H",0f,-1.2f)),listOf(Bond(0,1),Bond(0,2),Bond(0,3)))
            "methane","ch4" -> Structure("Methane — CH₄",listOf(Atom("C",0f,0f),Atom("H",-1f,0f),Atom("H",1f,0f),Atom("H",0f,-1f),Atom("H",0f,1f)),listOf(Bond(0,1),Bond(0,2),Bond(0,3),Bond(0,4)))
            "sulphur trioxide","sulfur trioxide","so3" -> Structure("Sulphur trioxide — SO₃",listOf(Atom("S",0f,0f),Atom("O",-1f,0.9f),Atom("O",1f,0.9f),Atom("O",0f,-1.1f)),listOf(Bond(0,1,2),Bond(0,2,2),Bond(0,3,2)))
            "hydrogen sulfide","hydrogen sulphide","h2s" -> Structure("Hydrogen sulphide — H₂S",listOf(Atom("S",0f,0f),Atom("H",-1f,0.9f),Atom("H",1f,0.9f)),listOf(Bond(0,1),Bond(0,2)))
            "hydrogen chloride","hcl" -> Structure("Hydrogen chloride — HCl",listOf(Atom("H",-1f,0f),Atom("Cl",1f,0f)),listOf(Bond(0,1)))
            "oxygen","o2" -> Structure("Oxygen — O₂",listOf(Atom("O",-1f,0f),Atom("O",1f,0f)),listOf(Bond(0,1,2)))
            "nitrogen","n2" -> Structure("Nitrogen — N₂",listOf(Atom("N",-1f,0f),Atom("N",1f,0f)),listOf(Bond(0,1,3)))
            "hydrogen","h2" -> Structure("Hydrogen — H₂",listOf(Atom("H",-1f,0f),Atom("H",1f,0f)),listOf(Bond(0,1)))
            else -> throw IllegalArgumentException("Structure not in the built-in library. Try SO₂, H₂O, CO₂, NH₃, CH₄, SO₃, H₂S, HCl, O₂ or N₂.")
        }
    }
}
