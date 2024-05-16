package org.maidagency.maidlib.impl.util.json

import ujson.*
import upickle.default.*

object CustomPickle extends upickle.AttributeTagged:
  def camelToSnake(s: String) =
    s.replaceAll("([A-Z])", "#$1").split('#').map(_.toLowerCase).mkString("_")
  def snakeToCamel(s: String) =
    val res = s.split("_", -1).map(x => s"${x(0).toUpper}${x.drop(1)}").mkString
    s"${s(0).toLower}${res.drop(1)}"

  override def objectAttributeKeyReadMap(s: CharSequence) =
    snakeToCamel(s.toString)
  override def objectAttributeKeyWriteMap(s: CharSequence) =
    camelToSnake(s.toString)

  override def objectTypeKeyReadMap(s: CharSequence) =
    snakeToCamel(s.toString)
  override def objectTypeKeyWriteMap(s: CharSequence) =
    camelToSnake(s.toString)

  implicit override def OptionWriter[T: Writer]: Writer[Option[T]] =
    implicitly[Writer[T]].comap[Option[T]]:
        case None    => null.asInstanceOf[T]
        case Some(x) => x

  implicit override def OptionReader[T: Reader]: Reader[Option[T]] =
    new Reader.Delegate[Any, Option[T]](implicitly[Reader[T]].map(Some(_))):
      override def visitNull(index: Int) = None

end CustomPickle

// HACK: due to upickle limitation, we need to manually insert
// $type field for sealed trait case objects
// long term solution would be to write custom pickler
object Bypasser:
  def decode(value: Value, cs: String) =
    val subs =
      value.toString
        .substring(1)
    "{\"$type\": \"org.maidagency.maidlib.objects."
      +
        cs
        +
        "\", "
        +
        subs
