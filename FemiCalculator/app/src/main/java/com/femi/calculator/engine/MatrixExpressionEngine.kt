package com.femi.calculator.engine

import kotlin.math.abs
import kotlin.math.pow

/** Evaluates expressions containing named matrices and scalar coefficients, e.g. 0.2A^2-3B+4AB. */
object MatrixExpressionEngine {
    private sealed class Value { data class S(val v:Double):Value(); data class M(val m:Matrix):Value() }
    private sealed class Tok { data class Num(val v:Double):Tok(); data class Id(val s:String):Tok(); data class Op(val s:String):Tok(); object L:Tok(); object R:Tok() }
    private class Parser(val tokens:List<Tok>, val env:Map<String,Matrix>){var p=0
        fun parse():Value{val v=expr();if(p!=tokens.size)throw MatrixException("Unexpected token in matrix expression");return v}
        private fun expr():Value{var v=term();while(p<tokens.size){val op=(tokens[p] as? Tok.Op)?.s?:break;if(op!="+"&&op!="-")break;p++;val b=term();v=if(op=="+")add(v,b)else sub(v,b)};return v}
        private fun term():Value{var v=power();while(p<tokens.size){val explicit=(tokens[p]as?Tok.Op)?.s;if(explicit=="*"||explicit=="/"){p++;val b=power();v=if(explicit=="*")mul(v,b)else div(v,b)}else if(canStartFactor(tokens[p])){val b=power();v=mul(v,b)}else break};return v}
        private fun power():Value{var v=factor();if(p<tokens.size&&(tokens[p]as?Tok.Op)?.s=="^"){p++;val n=(tokens.getOrNull(p)as?Tok.Num)?.v?:throw MatrixException("Matrix power must use an integer");p++;if(abs(n-n.toInt())>1e-9)throw MatrixException("Matrix power must be an integer");v=pow(v,n.toInt())};return v}
        private fun factor():Value{if(p>=tokens.size)throw MatrixException("Incomplete expression");when(val t=tokens[p]){is Tok.Op->if(t.s=="-"){p++;return neg(factor())} else throw MatrixException("Unexpected operator ${t.s}");Tok.L->{p++;val v=expr();if(p>=tokens.size||tokens[p]!==Tok.R)throw MatrixException("Mismatched parentheses");p++;return v};is Tok.Num->{p++;return Value.S(t.v)};is Tok.Id->{p++;val m=env[t.s.uppercase()]?:throw MatrixException("Matrix ${t.s} is not defined");return Value.M(m)};Tok.R->throw MatrixException("Unexpected )")}}
        private fun canStartFactor(t:Tok)=t is Tok.Num||t is Tok.Id||t===Tok.L||(t is Tok.Op&&t.s=="-")
        private fun add(a:Value,b:Value)=when{a is Value.M&&b is Value.M->Value.M(a.m+b.m);a is Value.S&&b is Value.S->Value.S(a.v+b.v);else->throw MatrixException("Addition requires two matrices or two scalars")}
        private fun sub(a:Value,b:Value)=when{a is Value.M&&b is Value.M->Value.M(a.m-b.m);a is Value.S&&b is Value.S->Value.S(a.v-b.v);else->throw MatrixException("Subtraction requires matching types")}
        private fun mul(a:Value,b:Value)=when{a is Value.M&&b is Value.M->Value.M(a.m*b.m);a is Value.M&&b is Value.S->Value.M(scale(a.m,b.v));a is Value.S&&b is Value.M->Value.M(scale(b.m,a.v));a is Value.S&&b is Value.S->Value.S(a.v*b.v)}
        private fun div(a:Value,b:Value)=when{a is Value.S&&b is Value.S->{if(b.v==0.0)throw MatrixException("Division by zero");Value.S(a.v/b.v)};a is Value.M&&b is Value.S->{if(b.v==0.0)throw MatrixException("Division by zero");Value.M(scale(a.m,1.0/b.v))};else->throw MatrixException("A matrix cannot be divided by another matrix")}
        private fun neg(a:Value)=when(a){is Value.S->Value.S(-a.v);is Value.M->Value.M(scale(a.m,-1.0))}
        private fun pow(a:Value,n:Int)=when(a){is Value.S->Value.S(a.v.pow(n));is Value.M->{if(n<0)Value.M(a.m.inverse().power(-n))else Value.M(a.m.power(n))}}
    }
    fun evaluate(expression:String, matrices:Map<String,Matrix>):Any{val v=Parser(tokenize(expression),matrices).parse();return when(v){is Value.S->v.v;is Value.M->v.m}}
    private fun tokenize(s:String):List<Tok>{val out=mutableListOf<Tok>();var i=0;while(i<s.length){val c=s[i];when{c.isWhitespace()->i++;c.isDigit()||c=='.'||(c=='-'&&i+1<s.length&&s[i+1].isDigit()&&(i==0||s[i-1]in "(+-*/^"))-> {val st=i;i++;while(i<s.length&&(s[i].isDigit()||s[i]=='.'))i++;out.add(Tok.Num(s.substring(st,i).toDouble()))};c.isLetter()->{out.add(Tok.Id(c.toString()));i++};c in "+-*/^"->{out.add(Tok.Op(c.toString()));i++};c=='('->{out.add(Tok.L);i++};c==')'->{out.add(Tok.R);i++};else->throw MatrixException("Unexpected character: $c")}};return out}
    private fun scale(m:Matrix,s:Double):Matrix{val r=Matrix(m.rows,m.cols);for(i in 0 until m.rows)for(j in 0 until m.cols)r.data[i][j]=m.data[i][j]*s;return r}
}

private fun Matrix.power(n:Int):Matrix{if(rows!=cols)throw MatrixException("Matrix powers require a square matrix");if(n==0)return Matrix.identity(rows);var result=Matrix.identity(rows);var base=this;var k=n;while(k>0){if((k and 1) == 1)result=result*base;k=k shr 1;if(k>0)base=base*base};return result}
