package org.kotgll.rsm.stringinput.withsppf.parallel

import org.kotgll.rsm.grammar.RSMState
import org.kotgll.rsm.stringinput.withsppf.parallel.sppf.SPPFNode
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.Objects

class DescriptorsQueue(size: Int) {
  val todo: ConcurrentLinkedQueue<Descriptor> = ConcurrentLinkedQueue()
  val created: Array<ConcurrentHashMap<Descriptor, Boolean>> = Array(size) { ConcurrentHashMap() }

  fun add(rsmState: RSMState, gssNode: GSSNode, sppfNode: SPPFNode?, pos: Int) {
    val descriptor = Descriptor(rsmState, gssNode, sppfNode, pos)
    if (created[pos].putIfAbsent(descriptor, true) == null) {
      todo.add(descriptor)
    }
  }

  fun next() = todo.poll()

  fun isEmpty() = todo.isEmpty()

  class Descriptor(
    val rsmState: RSMState,
    val gssNode: GSSNode,
    val sppfNode: SPPFNode?,
    val pos: Int,
  ) {
    override fun toString() =
      "Descriptor(rsmState=$rsmState, gssNode=$gssNode, sppfNode=$sppfNode, pos=$pos)"

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other !is Descriptor) return false

      if (rsmState != other.rsmState) return false
      if (gssNode != other.gssNode) return false
      if (sppfNode != other.sppfNode) return false

      return true
    }

    val hashCode: Int = Objects.hash(rsmState, gssNode, sppfNode)
    override fun hashCode() = hashCode
  }
}
