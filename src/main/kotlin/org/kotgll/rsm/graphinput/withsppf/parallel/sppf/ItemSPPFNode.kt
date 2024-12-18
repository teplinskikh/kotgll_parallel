package org.kotgll.rsm.graphinput.withsppf.parallel.sppf

import org.kotgll.graph.GraphNode
import org.kotgll.rsm.grammar.RSMState
import java.util.Objects

class ItemSPPFNode(leftExtent: GraphNode, rightExtent: GraphNode, val rsmState: RSMState) :
  ParentSPPFNode(leftExtent, rightExtent) {
  override fun toString() =
    "ItemSPPFNode(leftExtent=$leftExtent, rightExtent=$rightExtent, rsmState=$rsmState)"

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ItemSPPFNode) return false
    if (!super.equals(other)) return false

    if (rsmState != other.rsmState) return false

    return true
  }

  override val hashCode: Int = Objects.hash(leftExtent, rightExtent, rsmState)
  override fun hashCode() = hashCode
}
