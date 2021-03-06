package ch.epfl.data

import java.io.{File, PrintWriter}

import ch.epfl.data.sc.pardis.utils.document._

object Common {
  def output(text: String, folder: File, fname: String) = {
    val outputFile = new PrintWriter(new File(folder, fname))
    outputFile.println(text)
    outputFile.close()
  }
  def printToFile(f: java.io.File)(op: java.io.PrintWriter => Unit) {
    val p = new java.io.PrintWriter(f)
    try { op(p) } finally { p.close() }
  }
}
import Common._

object SimpleTest extends App {
//  output(Code("SimpleTest", 4, 5, 3, implicits = Some(false), absClass = true, oneCake = false).toString, new File("generated"), "SimpleTest.scala")
//  output(Code("SimpleTest", 2, 2, 2, implicits = Some(true), absClass = false, oneCake = false).toString, new File("generated"), "SimpleTest.scala")
  output(Code("SimpleTest", 80, 30, 30, implicits = Some(false), absClass = true, oneCake = false).toString, new File("../bench"), "SimpleTest.scala")
}

object TestLinear extends App {
  import sys.process._
  
  val folder = new File("../generated")
  
//  val numDefs = 5
  for (i <- 5 to 100 by 5) {
    val fname = s"LinTest_$i.scala"
    output(Code("New", i, 50, 10, implicits = None).toString, folder, fname)
    s"mkdir ${folder}/$fname-bin".!!
    val res = s"time scalac ${folder}/$fname -d ${folder}/$fname-bin".!!
    println(res)
  }
  
}

object CakeGen extends App {
//  val folder = new File("../generated")
  val folder = new File("generated")
  
  val (numOps, numDefs, numCakes) = (3, 2, 4)
//  val (numOps, numDefs, numCakes) = (30, 20, 40)

  val oldCode = Code("Old", numOps, numDefs, numCakes, implicits = Some(true))
  val oldCodeSelfless = Code("OldSelfless", numOps, numDefs, numCakes, implicits = Some(true), selfTypes = false)
  val newCode = Code("New", numOps, numDefs, numCakes, implicits = None)
  val oneCode = Code("One", numOps, numDefs, numCakes, implicits = None, oneCake = true)

  println(oldCode)
  
  output(oldCode.toString, folder, "CakeOld.scala")
  output(oldCodeSelfless.toString, folder, "CakeOldSelfless.scala")
  output(newCode.toString, folder, "CakeNew.scala")
  output(oneCode.toString, folder, "CakeOne.scala")
  
  println("Done.")
}

object Code {
  def apply(name: String,
            numOps: Int,
            numDefs: Int,
            numCakes: Int,
            implicits: Option[Boolean], // None: no implicits; Some(true): yes; Some(false): yes, but with explicit application
            oneCake: Boolean = false,
            absClass: Boolean = false,
            selfTypes: Boolean = true,
            defApps: Int = 1) = {
    val db = new DocBuilder()
    
    val cake =
      if (numOps == 0) "AnyRef"
      else 1 to numOps map {"Ops"+_} mkString " with "
    
    db.bracesAfter(doc"object $name") {
      
      db.bracesAfter(doc"trait Base") {
        db +=\\ doc"class Rep[+T]"
        db +=\\ doc"def lift[T](t: T): Rep[T] = new Rep[T]"
      }
      
      for (i <- 1 to numOps) {
        val tname = s"T$i"
        db.bracesAfter(doc"trait Ops$i extends Base") {
          if (selfTypes) db +=\\ doc"this: $cake => "
          db +=\\ doc"class $tname"
          db.bracesAfter(doc"implicit class Rep$i(self: Rep[$tname])") {
            for (j <- 1 to numDefs)
//              db +=\\ doc"def m$j: Rep[Int] = ${tname}d$j(self)"
              db +=\\ doc"def m$j: Rep[$tname] = ${tname}d$j(self)"
          }
          for (j <- 1 to numDefs)
//            db +=\\ doc"def ${tname}d$j(x: Rep[$tname]): Rep[Int] = lift(42)"
            db +=\\ doc"def ${tname}d$j(x: Rep[$tname]): Rep[$tname] = x"
        }
      }
      
      if (oneCake)
        db +=\\ doc"object Cake extends $cake"
      else
        db +=\\ {(if (absClass) "abstract class " else "trait ") + "DSL" + " extends " + cake}
      
//      def mkCalls = for (i <- 1 to numOps; j <- 1 to numDefs) {
//        def TRep = doc"lift(new T$i)"
//        if (implicits) db +=\\ doc"$TRep.m$j"
//        else db +=\\ doc"T${i}d$j($TRep)"
//      }
      
      def mkCalls() = for (i <- 1 to numOps; _ <- 1 to defApps) {
        def TRep = doc"lift(new T$i)"
        implicits match {
          case Some(true) =>
            db += TRep
            for (j <- 1 to numDefs) db += doc".m$j"
            db.newLine
          case Some(false) =>
            for (j <- 1 to numDefs) db += doc"Rep$i("
            db += TRep
            for (j <- 1 to numDefs) db += doc").m$j"
            db.newLine
          case None =>
            for (j <- 1 to numDefs) db += doc"T${i}d$j("
            db += TRep
            for (j <- 1 to numDefs) db += doc")"
            db.newLine
        }
      }
  
      for (c <- 1 to numCakes)
        if (oneCake) db.bracesAfter(doc"object Use$c") {
          db +=\\ doc"import Cake._"
          mkCalls()
        } else db.bracesAfter(doc"class Cake$c extends DSL") {
          mkCalls()
        }
  
    }
    
    db.toDoc
  }
}













