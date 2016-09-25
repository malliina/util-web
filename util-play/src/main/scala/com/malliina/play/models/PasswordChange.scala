package com.malliina.play.models

case class PasswordChange(oldPass: Password,
                          newPass: Password,
                          newPassAgain: Password)
