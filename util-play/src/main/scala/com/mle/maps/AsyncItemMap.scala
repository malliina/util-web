package com.mle.maps

import scala.concurrent.Future

/**
 * @author mle
 */
trait AsyncItemMap[K, V] {
  def putAsync(key: K, value: V): Future[Unit]

  def removeAsync(key: K): Future[Option[V]]

  def getAsync(key: K): Future[Option[V]]

  def itemsAsync: Future[Map[K, V]]

  def keysAsync: Future[Seq[K]]

  def sizeAsync: Future[Int]
}

object AsyncItemMap {
  def fromSync[K, V](itemMap: ItemMap[K, V]): AsyncItemMap[K, V] = new AsyncItemMap[K, V] {
    override def putAsync(key: K, value: V): Future[Unit] = fut(itemMap.put(key, value))

    override def removeAsync(key: K): Future[Option[V]] = fut(itemMap.remove(key))

    override def getAsync(key: K): Future[Option[V]] = fut(itemMap.get(key))

    override def sizeAsync: Future[Int] = fut(itemMap.size)

    override def keysAsync: Future[Seq[K]] = fut(itemMap.keys)

    override def itemsAsync: Future[Map[K, V]] = fut(itemMap.items)

    def fut[T](t: T): Future[T] = Future.successful(t)
  }
}
