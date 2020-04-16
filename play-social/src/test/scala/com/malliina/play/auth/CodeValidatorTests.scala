package com.malliina.play.auth

class CodeValidatorTests extends munit.FunSuite {
  test("rng") {
    val first = CodeValidator.randomString()
    val second = CodeValidator.randomString()
    assert(first != second)
  }
}
