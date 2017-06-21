package qualysis

import java.io.{BufferedWriter, FileWriter}
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
  val fileLocation     = s"$currentDirectory\\target\\browse-dependency-graph\\"
  val inputFile        = "dependencies.dot"
  val outputFile       = "dependencies.dot.js"
  val orgLines         = new ListBuffer[String]()

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
    waitWhile(() => Files.exists(Paths.get(fileLocation + inputFile)), defaultTimeout)

    val dotFile = Source.fromFile(fileLocation + inputFile)
    val outFile = new sbt.File(s"$fileLocation\\$outputFile")
    val writer  = new BufferedWriter(new FileWriter(outFile))

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

    writer.write("data = \"" + URLEncoder.encode(orgLines.mkString, "UTF-8").replace("+", "%20") + "\";")
    writer.flush()
    writer.close()
  }
}

