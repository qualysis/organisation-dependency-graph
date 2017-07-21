lazy val commonSettings = Seq(
    version      := "1.0.1",
    organization := "com.github.qualysis"
)

lazy val root = (project in file("."))
  .settings(
      commonSettings,
      sbtPlugin := true,
      scalaVersion := "2.10.6",
      name := "organisation-dependency-graph",
      description := "Creates dependencies graph for given input organisation name.",
      licenses in GlobalScope += "Apache License 2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.html"),
      publishMavenStyle := false,
      bintrayRepository := "sbt-plugins",
      bintrayOrganization in bintray := Some("qcl")
  )
