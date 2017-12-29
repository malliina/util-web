package com.malliina.maps

trait ItemMap[K, V] {
  def put(key: K, value: V): Option[V]

  def remove(key: K): Option[V]

  def get(key: K): Option[V]

  def items: Map[K, V]

  def keys: Seq[K]

  def size: Int
}
