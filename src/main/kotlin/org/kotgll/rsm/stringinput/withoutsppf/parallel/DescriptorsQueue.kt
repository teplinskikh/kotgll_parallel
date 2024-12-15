package org.kotgll.rsm.stringinput.withoutsppf.parallel

import org.kotgll.rsm.grammar.RSMState
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentHashMap

class DescriptorsQueue(size: Int) {
  private val todo: ConcurrentLinkedQueue<Descriptor> = ConcurrentLinkedQueue()
  private val created: Array<ConcurrentHashMap<Descriptor, Boolean>> = Array(size) { ConcurrentHashMap() }

  fun add(rsmState: RSMState, gssNode: GSSNode, pos: Int) {
    val descriptor = Descriptor(rsmState, gssNode, pos)
    if (created[pos].putIfAbsent(descriptor, true) == null) {
      todo.add(descriptor)
    }
  }

  fun next(): Descriptor? = todo.poll()

  fun isEmpty(): Boolean = todo.isEmpty()

  class Descriptor(val rsmState: RSMState, val gssNode: GSSNode, val pos: Int) {
    override fun toString() = "Descriptor(rsmState=$rsmState, gssNode=$gssNode, pos=$pos)"

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other !is Descriptor) return false
      if (rsmState != other.rsmState) return false
      if (gssNode != other.gssNode) return false
      return true
    }

    override fun hashCode(): Int = Objects.hash(rsmState, gssNode)
  }
}