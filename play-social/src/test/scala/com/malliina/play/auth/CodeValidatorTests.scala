package com.malliina.play.auth

import com.malliina.web.Utils.randomString

class CodeValidatorTests extends munit.FunSuite {
  test("rng") {
    val first = randomString()
    val second = randomString()
    assert(first != second)
  }
}
