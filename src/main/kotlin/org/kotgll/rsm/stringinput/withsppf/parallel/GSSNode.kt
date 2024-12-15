package org.kotgll.rsm.stringinput.withsppf.parallel

import org.kotgll.rsm.grammar.RSMState
import org.kotgll.rsm.grammar.symbol.Nonterminal
import org.kotgll.rsm.stringinput.withsppf.parallel.sppf.SPPFNode
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

class GSSNode(val nonterminal: Nonterminal, val pos: Int) {
  val edges: ConcurrentHashMap<Pair<RSMState, SPPFNode?>, CopyOnWriteArraySet<GSSNode>> = ConcurrentHashMap()

  fun addEdge(rsmState: RSMState, sppfNode: SPPFNode?, gssNode: GSSNode): Boolean {
    val label = Pair(rsmState, sppfNode)
    edges.computeIfAbsent(label) { CopyOnWriteArraySet() }
    return edges[label]!!.add(gssNode)
  }

  override fun toString() = "GSSNode(nonterminal=$nonterminal, pos=$pos)"

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is GSSNode) return false

    if (nonterminal != other.nonterminal) return false
    if (pos != other.pos) return false

    return true
  }

  val hashCode: Int = Objects.hash(nonterminal, pos)
  override fun hashCode() = hashCode
}
