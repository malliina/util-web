package com.mle.play.io

import com.mle.util.Log

/**
 * @author Michael
 */
trait LoggingList[T] extends PersistentList[T] with Log {
  abstract override def add(item: T): Boolean = {
    val added = super.add(item)
    if (added) {
      log.info(s"Added item: $item")
    }
    added
  }

  abstract override def remove(item: T): Boolean = {
    val removed = super.remove(item)
    if (removed) {
      log.info(s"Removed item: $item")
    }
    removed
  }
}
