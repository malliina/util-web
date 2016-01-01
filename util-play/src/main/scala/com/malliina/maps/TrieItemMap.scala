package com.malliina.maps

import scala.collection.concurrent.TrieMap

/**
 * @author mle
 */
class TrieItemMap[K, V] extends ItemMap[K, V] {
  val inner = TrieMap.empty[K, V]

  override def put(key: K, value: V): Option[V] = inner.put(key, value)

  override def items: Map[K, V] = inner.readOnlySnapshot().toMap

  override def get(key: K): Option[V] = inner.get(key)

  override def size: Int = inner.size

  override def remove(key: K): Option[V] = inner.remove(key)

  override def keys: Seq[K] = items.keys.toSeq
}

object TrieItemMap {
  def empty[K, V] = new TrieItemMap[K, V]
}
