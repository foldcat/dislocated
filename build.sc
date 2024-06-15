import mill._, scalalib._, publish._

import $ivy.`com.goyeau::mill-scalafix::0.4.0`
import com.goyeau.mill.scalafix.ScalafixModule
import mill.scalalib._

object dislocated
    extends PublishModule
    with ScalaModule
    with ScalafixModule {
  def scalaVersion   = "3.3.3"
  def publishVersion = "0.0.1"

  def ivyDeps = Agg(
    ivy"org.apache.pekko::pekko-actor-typed::1.0.2",
    ivy"org.apache.pekko::pekko-stream::1.0.2",
    ivy"org.apache.pekko::pekko-http::1.0.1",
    ivy"org.slf4j:slf4j-api:2.0.13",
    ivy"org.typelevel::fabric-core::1.15.1",
    ivy"org.typelevel::fabric-io::1.15.1"
  )

  // inlines 1000 due to humongous amounts of RW deriving
  // if only the scala compiler gives me less troubles
  def scalacOptions =
    Seq("-Wunused:all", "-feature", "-deprecation", "-Xmax-inlines", "1000")

  def pomSettings = PomSettings(
    description = "pekko based Discord library for scala3",
    organization = "com.github.foldcat",
    url = "https://github.com/foldcat/dislocated",
    licenses = Seq(License.`Apache-2.0`),
    versionControl = VersionControl.github("foldcat", "dislocated"),
    developers = Seq(
      Developer("foldcat", "fold", "https://github.com/foldcat"),
      Developer(
        "SpicyKitten",
        "SpicyKitten",
        "https://github.com/SpicyKitten"
      )
    )
  )

}
