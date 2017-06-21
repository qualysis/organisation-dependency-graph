lazy val root = (project in file(".")).
  settings(
    name         := "organisation-dependency-graph",
    version      := "1.0.0",
    organization := "com.github.qualysis",
    scalaVersion := "2.10.6",
    sbtPlugin    := true
  )