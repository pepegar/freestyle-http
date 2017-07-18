import sbtorgpolicies.runnable.syntax._

lazy val fsVersion = Option(sys.props("frees.version")).getOrElse("0.3.1")

lazy val fCoreDeps = freestyleCoreDeps(Some(fsVersion))

lazy val root = (project in file("."))
  .settings(moduleName := "root")
  .settings(name := "freestyle-integrations")
  .settings(noPublishSettings: _*)
  .aggregate(allModules: _*)


lazy val httpHttp4s = (project in file("server/http4s"))
  .settings(name := "freestyle-http-http4s")
  .settings(
    libraryDependencies ++= Seq(
      %%("http4s-core"),
      %%("http4s-dsl") % "test"
    ) ++ commonDeps ++ fCoreDeps
  )

lazy val httpFinch = (project in file("server/finch"))
  .settings(name := "freestyle-http-finch")
  .settings(
    libraryDependencies ++= Seq(%%("finch-core", "0.14.1")) ++ commonDeps ++ fCoreDeps
  )

lazy val httpAkka = (project in file("server/akka"))
  .settings(name := "freestyle-http-akka")
  .settings(
    libraryDependencies ++= Seq(
      %%("akka-http"),
      %%("akka-http-testkit") % "test"
    ) ++ commonDeps ++ fCoreDeps
  )

lazy val httpPlay = (project in file("server/play"))
  .disablePlugins(CoursierPlugin)
  .settings(name := "freestyle-http-play")
  .settings(
    concurrentRestrictions in Global := Seq(Tags.limitAll(1)),
    libraryDependencies ++= Seq(
      %%("play")      % "test",
      %%("play-test") % "test"
    ) ++ commonDeps ++ fCoreDeps
  )

pgpPassphrase := Some(getEnvVar("PGP_PASSPHRASE").getOrElse("").toCharArray)
pgpPublicRing := file(s"$gpgFolder/pubring.gpg")
pgpSecretRing := file(s"$gpgFolder/secring.gpg")

lazy val jvmModules: Seq[ProjectReference] = Seq(
  httpHttp4s,
  httpFinch,
  httpAkka,
  httpPlay
)

lazy val allModules: Seq[ProjectReference] = jvmModules

addCommandAlias("validateJVM", (toCompileTestList(jvmModules) ++ List("project root")).asCmd)
addCommandAlias(
  "validate",
  ";clean;compile;coverage;validateJVM;coverageReport;coverageAggregate;coverageOff")
