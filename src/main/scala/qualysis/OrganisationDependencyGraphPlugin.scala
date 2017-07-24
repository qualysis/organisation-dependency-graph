package qualysis

import java.io._
import java.net.URLEncoder
import java.nio.file.{Files, Paths}
import scala.annotation.tailrec
import sbt._
import sbt.complete.DefaultParsers._
import scala.collection.mutable.ListBuffer
import scala.io.Source
import utils.FileUtils._

object OrganisationDependencyGraphPlugin extends AutoPlugin {

  val defaultTimeout   = 30000
  val currentDirectory = new java.io.File(".").getCanonicalPath
  val targetLocation   = s"$currentDirectory\\target\\"
  val compileDotFile   = "dependencies-compile.dot"
  val inputFile        = "dependencies.dot"
  val jsOutputFile     = "dependencies.dot.js"
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
    copyFileFromJar(targetLocation, "graph.html")
    generateDotFile
    generateJSFile(organisationName)
  }

  def copyFileFromJar(targetPath: String, fileToCopy: String) = {
    val targetFolder = new File(targetPath)
    targetFolder.mkdirs()
    val graphHTML        = new File(targetFolder, fileToCopy)
    val inputStream      = getClass.getClassLoader.getResourceAsStream(fileToCopy)
    val fileOutputStream = new FileOutputStream(graphHTML)

    copy(inputStream, fileOutputStream)
    inputStream.close()
    fileOutputStream.close()
  }

  def copy(inputStream: InputStream, outputStream: OutputStream) = {
    val buffer = new Array[Byte](65536)

    @tailrec def writeRecursive(): Unit = {
      val data = inputStream.read(buffer)
      if (data > 0) {
        outputStream.write(buffer, 0, data)
        writeRecursive()
      } else if (data == 0)
        throw new IllegalStateException("InputStream returned 0")
    }
    writeRecursive()
  }

  def generateDotFile = {
    waitWhile(() => Files.exists(Paths.get(targetLocation + compileDotFile)), defaultTimeout)
    val inputDotFile = Source.fromFile(targetLocation + compileDotFile)
    val tempFile = new File(targetLocation + "temp.dot")
    val dotOutFile = new sbt.File(s"$targetLocation\\$inputFile")
    val printWriter = new PrintWriter(tempFile)

    inputDotFile.getLines
      .map { x => if (x.contains("\"[label=<")) x.replace("\"[label=<", "\"[labelType=\"html\" label=\"") else x }
      .map { x => if (x.contains("> style=\"\"]")) x.replace("> style=\"\"]", "\" style=\"\"]") else x }
      .foreach(x => printWriter.println(x))
    printWriter.flush()
    printWriter.close()
    tempFile.renameTo(dotOutFile)
  }

  def generateJSFile(organisationName: String) = {
    waitWhile(() => Files.exists(Paths.get(targetLocation + inputFile)), defaultTimeout)
    val dotFile = Source.fromFile(targetLocation + inputFile)
    val outFile = new sbt.File(s"$targetLocation\\$jsOutputFile")
    val bufferedWriter = new BufferedWriter(new FileWriter(outFile))

    dotFile.getLines.foreach { line =>
      if (line.trim.startsWith("\"")) {
        line match {
          case x if x.trim.startsWith(s"""\"$organisationName""") && x.trim.contains("labeltype=\"html\"") =>
            orgLines.append(x)
          case x if x.trim.startsWith(s"""\"$organisationName""") && x.split("->").last.trim.startsWith(s"""\"$organisationName""") =>
            orgLines.append(x)
          case x if x.trim.startsWith(s"""\"$organisationName""") && !x.split("->").last.trim.startsWith(s"""\"$organisationName""") =>
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

