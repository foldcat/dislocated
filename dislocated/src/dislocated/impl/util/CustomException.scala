package com.github.foldcat.dislocated.impl.util.customexception

final case class WebsocketFailure(
    private val message: String = "",
    private val cause: Throwable = None.orNull
) extends Exception(message, cause)
