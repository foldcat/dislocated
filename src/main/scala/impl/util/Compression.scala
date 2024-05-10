package org.maidagency.maidlib.impl.util.compression

import java.io.ByteArrayOutputStream
import java.util.zip.Inflater

object ZLibDecoder:

  def decode(data: Array[Byte]): String =
    val inflator         = new Inflater()
    val decompressedData = new ByteArrayOutputStream()
    inflator.setInput(data)
    val buffer = new Array[Byte](data.size * 2)
    while !inflator.finished() do
      val count = inflator.inflate(buffer)
      decompressedData.write(buffer, 0, count)
    val da = decompressedData.toByteArray
    new String(decompressedData.toByteArray, "UTF-8")
