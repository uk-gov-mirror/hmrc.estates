import play.core.PlayVersion.current
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  val compile: Seq[ModuleID] = Seq(

    "org.reactivemongo"       %% "play2-reactivemongo"      % "0.18.8-play26",
    "uk.gov.hmrc"             %% "logback-json-logger"            % "4.8.0",
    "uk.gov.hmrc"             %% "bootstrap-play-26"        % "1.8.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-play-26"        % "1.8.0" % Test classifier "tests",
    "org.scalatest"           %% "scalatest"                % "3.0.8"                 % "test",
    "com.typesafe.play"       %% "play-test"                % current                 % "test",
    "org.mockito"             % "mockito-core"              % "1.10.19"               % "test, it",
    "org.pegdown"             %  "pegdown"                  % "1.6.0"                 % "test, it",
    "org.scalatestplus.play"  %% "scalatestplus-play"       % "3.1.2"                 % "test, it"
  )

}
