package com.mle.play.auth

/**
  * @author Michael
  */
trait TokenStore {
   def persist(token: Token)

   def remove(token: Token)

   def removeAll(user: String)

   def remove(user: String, series: Long)

   def findToken(user: String, series: Long): Option[Token]
 }