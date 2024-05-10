package org.maidagency.maidlib.events

sealed trait Events

case class messageCreate(argName: Int) extends Events
