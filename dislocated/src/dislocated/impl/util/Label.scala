package com.github.foldcat.dislocated.impl.util.label

import java.time.LocalDateTime

object Label:
  def genLabel(nm: String): String =
    nm + "-" + LocalDateTime.now().getNano()
