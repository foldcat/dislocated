import mill._, scalalib._, publish._

import $ivy.`com.goyeau::mill-scalafix::0.4.0`
import com.goyeau.mill.scalafix.ScalafixModule
import mill.scalalib._

object dislocated extends PublishModule with ScalaModule with ScalafixModule {
  def scalaVersion   = "3.3.3"
  def publishVersion = "0.0.1"

  def ivyDeps = Agg(
    ivy"org.apache.pekko::pekko-actor-typed::1.0.2",
    ivy"org.apache.pekko::pekko-stream::1.0.2",
    ivy"org.apache.pekko::pekko-http::1.0.1",
    ivy"org.slf4j:slf4j-api:2.0.13",
    ivy"com.lihaoyi::upickle::3.3.0",
    ivy"org.json4s::json4s-native::4.0.7"
  )

  def scalacOptions =
    Seq("-Wunused:all", "-feature", "-deprecation")

  def pomSettings = PomSettings(
    description = "pekko based Discord library for scala3",
    organization = "com.github.foldcat",
    url = "https://github.com/foldcat/dislocated",
    licenses = Seq(License.`Apache-2.0`),
    versionControl = VersionControl.github("foldcat", "dislocated"),
    developers = Seq(
      Developer("foldcat", "fold", "https://github.com/foldcat"),
      Developer("SpicyKitten", "SpicyKitten", "https://github.com/SpicyKitten")
    )
  )

}
