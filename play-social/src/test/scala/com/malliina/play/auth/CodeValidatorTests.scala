package com.malliina.play.auth

import org.scalatest.FunSuite

class CodeValidatorTests extends FunSuite {
  test("rng") {
    val first = CodeValidator.randomState()
    val second = CodeValidator.randomState()
    assert(first != second)
  }
}
