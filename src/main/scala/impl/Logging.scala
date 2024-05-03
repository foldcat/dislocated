package org.maidagency.impl.logging

class MaidlibLog:
  import org.slf4j.Logger
  import org.slf4j.LoggerFactory
  val logger = LoggerFactory.getLogger(classOf[MaidlibLog])

object MaidlibLog:
  def info(x: String) =
    val logger = new MaidlibLog()
    logger.logger.info(x)
