package com.malliina.play.auth

import org.scalatest.FunSuite

class CodeValidatorTests extends FunSuite {
  test("rng") {
    val first = CodeValidator.randomString()
    val second = CodeValidator.randomString()
    assert(first != second)
  }
}
