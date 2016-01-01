package com.malliina.maps

import scala.concurrent.stm.TMap

/**
 * @author mle
 */
class StmItemMap[K, V] extends ItemMap[K, V] {
  val inner = TMap.empty[K, V]

  override def put(key: K, value: V): Option[V] = inner.single.put(key, value)

  override def items: Map[K, V] = inner.snapshot

  override def get(key: K): Option[V] = inner.single.get(key)

  override def size: Int = inner.single.size

  override def remove(key: K): Option[V] = inner.single.remove(key)

  override def keys: Seq[K] = inner.single.keys.toSeq
}

object StmItemMap {
  def empty[K, V] = new StmItemMap[K, V]
}
