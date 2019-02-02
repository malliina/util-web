package com.malliina.html

import org.scalatest.FunSuite

class BootstrapTests extends FunSuite {
  test("bootstrap helpers") {
    object bs extends BootstrapParts
    val col = bs.col
    assert(col.six === "col-6")
    assert(col.md.six === "col-md-6")
    assert(col.md.offset.four === "offset-md-4")
    assert(col.lg.width("1") === "col-lg-1")
  }
}
