import mill._, scalalib._, publish._

object maidlib extends ScalaModule with PublishModule {
  def scalaVersion = "3.3.3"
  def publishVersion = "0.0.1"

  def ivyDeps = Agg(
    ivy"org.apache.pekko::pekko-actor-typed::1.0.2",
    ivy"org.apache.pekko::pekko-stream::1.0.2",
    ivy"org.apache.pekko::pekko-http::1.0.1",
    ivy"org.slf4j:slf4j-api:2.0.13",
    ivy"com.lihaoyi::upickle::3.3.0"
  )

  def pomSettings = PomSettings(
    description = "pekko based Discord library for scala3",
    organization = "org.maidagency",
    url = "https://github.com/magency-prod/maidlib/",
    licenses = Seq(License.`Apache-2.0`),
    versionControl = VersionControl.github("magency-prod", "maidlib"),
    developers = Seq(
      Developer("magnecy-prod", "akane","https://github.com/magnecy-prod")
    )
  )   
}
