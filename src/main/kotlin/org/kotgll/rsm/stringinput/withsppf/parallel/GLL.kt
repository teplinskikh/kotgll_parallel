package org.kotgll.rsm.stringinput.withsppf.parallel

import org.kotgll.rsm.grammar.RSMState
import org.kotgll.rsm.grammar.symbol.Nonterminal
import org.kotgll.rsm.grammar.symbol.Terminal
import org.kotgll.rsm.stringinput.withsppf.parallel.sppf.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

class GLL(val startState: RSMState, val input: String) {
    val queue: DescriptorsQueue = DescriptorsQueue(input.length + 1)
    val poppedGSSNodes: ConcurrentHashMap<GSSNode, MutableSet<SPPFNode?>> = ConcurrentHashMap()
    val createdGSSNodes: ConcurrentHashMap<GSSNode, GSSNode> = ConcurrentHashMap()
    val createdSPPFNodes: ConcurrentHashMap<SPPFNode, SPPFNode> = ConcurrentHashMap()
    @Volatile var parseResult: SPPFNode? = null

    fun getOrCreateGSSNode(nonterminal: Nonterminal, pos: Int): GSSNode {
        return createdGSSNodes.computeIfAbsent(GSSNode(nonterminal, pos)) { it }
    }

    fun parse(): SPPFNode? {
        queue.add(startState, getOrCreateGSSNode(startState.nonterminal, 0), null, 0)

        while (!queue.isEmpty()) {
            val descriptor: DescriptorsQueue.Descriptor = queue.next()
            parse(descriptor.rsmState, descriptor.gssNode, descriptor.sppfNode, descriptor.pos)
        }

        return parseResult
    }

    fun parse(state: RSMState, gssNode: GSSNode, sppfNode: SPPFNode?, pos: Int) {
        var curSPPFNode: SPPFNode? = sppfNode

        if (state.isStart && state.isFinal)
            curSPPFNode = getNodeP(state, curSPPFNode, getOrCreateItemSPPFNode(state, pos, pos))

        state.outgoingTerminalEdges.parallelStream().forEach { rsmEdge ->
            if (pos < input.length && rsmEdge.terminal.match(pos, input)) {
                val nextSPPFNode: SPPFNode =
                    getOrCreateTerminalSPPFNode(rsmEdge.terminal, pos, rsmEdge.terminal.size)
                queue.add(
                    rsmEdge.head,
                    gssNode,
                    getNodeP(rsmEdge.head, curSPPFNode, nextSPPFNode),
                    pos + rsmEdge.terminal.size)
            }
        }

        state.outgoingNonterminalEdges.parallelStream().forEach { rsmEdge ->
            queue.add(
                rsmEdge.nonterminal.startState,
                createGSSNode(rsmEdge.nonterminal, rsmEdge.head, gssNode, curSPPFNode, pos),
                null,
                pos)
        }

        if (state.isFinal) pop(gssNode, curSPPFNode, pos)
    }

    fun pop(gssNode: GSSNode, sppfNode: SPPFNode?, pos: Int) {
        poppedGSSNodes.computeIfAbsent(gssNode) { ConcurrentHashMap.newKeySet() }.add(sppfNode)
        gssNode.edges.forEach { (key, value) ->
            value.parallelStream().forEach { u ->
                queue.add(key.first, u, getNodeP(key.first, key.second, sppfNode!!), pos)
            }
        }
    }

    fun createGSSNode(
        nonterminal: Nonterminal,
        state: RSMState,
        gssNode: GSSNode,
        sppfNode: SPPFNode?,
        pos: Int
    ): GSSNode {
        val v: GSSNode = getOrCreateGSSNode(nonterminal, pos)

        if (v.addEdge(state, sppfNode, gssNode)) {
            poppedGSSNodes[v]?.forEach { z ->
                queue.add(state, gssNode, getNodeP(state, sppfNode, z!!), z.rightExtent)
            }
        }

        return v
    }

    fun getNodeP(state: RSMState, sppfNode: SPPFNode?, nextSPPFNode: SPPFNode): SPPFNode {
        val leftExtent = sppfNode?.leftExtent ?: nextSPPFNode.leftExtent
        val rightExtent = nextSPPFNode.rightExtent

        val y =
            if (state.isFinal) getOrCreateSymbolSPPFNode(state.nonterminal, leftExtent, rightExtent)
            else getOrCreateItemSPPFNode(state, leftExtent, rightExtent)

        y.kids.add(PackedSPPFNode(nextSPPFNode.leftExtent, state, sppfNode, nextSPPFNode))

        return y
    }

    fun getOrCreateTerminalSPPFNode(terminal: Terminal, leftExtent: Int, rightExtent: Int): SPPFNode {
        return createdSPPFNodes.computeIfAbsent(
            TerminalSPPFNode(leftExtent, leftExtent + rightExtent, terminal)) { it }
    }

    fun getOrCreateItemSPPFNode(state: RSMState, leftExtent: Int, rightExtent: Int): ItemSPPFNode {
        return createdSPPFNodes.computeIfAbsent(
            ItemSPPFNode(leftExtent, rightExtent, state)) { it } as ItemSPPFNode
    }

    fun getOrCreateSymbolSPPFNode(
        nonterminal: Nonterminal,
        leftExtent: Int,
        rightExtent: Int
    ): SymbolSPPFNode {
        val result = createdSPPFNodes.computeIfAbsent(
            SymbolSPPFNode(leftExtent, rightExtent, nonterminal)) { it } as SymbolSPPFNode
        if (parseResult == null &&
            nonterminal == startState.nonterminal &&
            leftExtent == 0 &&
            rightExtent == input.length)
            parseResult = result
        return result
    }
}
