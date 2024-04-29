package org.maidagency

import org.apache.pekko
import org.maidagency.handler.EventHandler
import pekko.actor.typed.*
import pekko.actor.typed.scaladsl.*
import scribe.*

object Main:
  def main(args: Array[String]): Unit =
    ActorSystem[String](EventHandler("123"), "system")
