package org.kotgll.rsm.graphinput.withsppf.parallel

import org.kotgll.graph.GraphNode
import org.kotgll.rsm.grammar.RSMState
import org.kotgll.rsm.graphinput.withsppf.parallel.sppf.SPPFNode
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.Objects

class DescriptorsQueue {
  private val todo: ConcurrentLinkedQueue<Descriptor> = ConcurrentLinkedQueue()
  private val created: ConcurrentHashMap<GraphNode, MutableSet<Descriptor>> = ConcurrentHashMap()

  fun add(rsmState: RSMState, gssNode: GSSNode, sppfNode: SPPFNode?, pos: GraphNode) {
    val descriptor = Descriptor(rsmState, gssNode, sppfNode, pos)
    created.computeIfAbsent(pos) { ConcurrentHashMap.newKeySet() }.let {
      if (it.add(descriptor)) todo.add(descriptor)
    }
  }

  fun next(): Descriptor? = todo.poll()

  fun isEmpty(): Boolean = todo.isEmpty()

  class Descriptor(
    val rsmState: RSMState,
    val gssNode: GSSNode,
    val sppfNode: SPPFNode?,
    val pos: GraphNode,
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
