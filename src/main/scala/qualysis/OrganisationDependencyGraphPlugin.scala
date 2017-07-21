package qualysis

import java.io._
import java.net.URLEncoder
import java.nio.file.{Files, Paths}
import sbt._
import sbt.complete.DefaultParsers._
import scala.collection.mutable.ListBuffer
import scala.io.Source
import utils.FileUtils._

object OrganisationDependencyGraphPlugin extends AutoPlugin {

  val defaultTimeout   = 30000
  val currentDirectory = new java.io.File(".").getCanonicalPath
  val fileLocation     = s"$currentDirectory\\target\\"
  val compileDotFile   = "dependencies-compile.dot"
  val inputFile        = "dependencies.dot"
  val jsOutputFile     = "dependencies.dot.js"
  val orgLines         = new ListBuffer[String]()
  val graphHtmlFile    = new File(s"$currentDirectory\\src\\main\\resources\\graph.html")

  object autoImport {
    val organisationDSLGraph = inputKey[Unit]("Creates DSL graph for the organisationName passed as parameter.")
  }

  import autoImport._

  override lazy val projectSettings = Seq(
    organisationDSLGraph := {
      val organisationName = spaceDelimited("<arg>").parsed.head
      generateDSLDependencyGraph(organisationName)
    }
  )

  def generateDSLDependencyGraph(organisationName: String): Unit = {
    copyGraphHtml(graphHtmlFile,new File(s"${fileLocation}graph.html"))
    generateDotFile
    generateJSFile(organisationName)
  }

  def copyGraphHtml(source: File, destination: File) = {
    new FileOutputStream(destination) getChannel() transferFrom(new FileInputStream(source) getChannel(), 0, Long.MaxValue )
  }

  def generateDotFile = {
    waitWhile(() => Files.exists(Paths.get(fileLocation + compileDotFile)), defaultTimeout)
    val inputDotFile  = Source.fromFile(fileLocation + compileDotFile)
    val tempFile      = new File(fileLocation+"temp.dot")
    val dotOutFile    = new sbt.File(s"$fileLocation\\$inputFile")
    val printWriter   = new PrintWriter(tempFile)

    inputDotFile.getLines
      .map { x => if(x.contains("\"[label=<")) x.replace("\"[label=<","\"[labelType=\"html\" label=\"") else x }
      .map { x => if(x.contains("> style=\"\"]")) x.replace("> style=\"\"]","\" style=\"\"]") else x }
      .foreach(x => printWriter.println(x))
    printWriter.close()
    tempFile.renameTo(dotOutFile)
  }

  def generateJSFile(organisationName: String) = {
    waitWhile(() => Files.exists(Paths.get(fileLocation + inputFile)), defaultTimeout)
    val dotFile         = Source.fromFile(fileLocation + inputFile)
    val outFile         = new sbt.File(s"$fileLocation\\$jsOutputFile")
    val bufferedWriter  = new BufferedWriter(new FileWriter(outFile))

    dotFile.getLines.foreach { line =>
      if (line.trim.startsWith("\"")) {
        line match {
          case x if x.trim.startsWith(s"""\"$organisationName""") && x.trim.contains("labeltype=\"html\"") =>
            orgLines.append(x)
          case x if x.trim.startsWith(s"""\"$organisationName""") && x.split("->").last.trim.startsWith(s"""\"$organisationName""") =>
            orgLines.append(x)
          case x if  x.trim.startsWith(s"""\"$organisationName""") && !x.split("->").last.trim.startsWith(s"""\"$organisationName""") =>
          case x if !x.trim.startsWith(s"""\"$organisationName""") && x.trim.contains("labeltype=\"html\"") =>
          case _ =>
        }
      } else {
        orgLines.append(line)
      }
    }

    bufferedWriter.write("data = \"" + URLEncoder.encode(orgLines.mkString, "UTF-8").replace("+", "%20") + "\";")
    bufferedWriter.flush()
    bufferedWriter.close()
  }
}

