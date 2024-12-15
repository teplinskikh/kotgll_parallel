package rsm.graphinput.withsppf.parallel

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.kotgll.graph.GraphNode
import org.kotgll.graph.readGraphFromString
import org.kotgll.rsm.grammar.RSMNonterminalEdge
import org.kotgll.rsm.grammar.RSMState
import org.kotgll.rsm.grammar.RSMTerminalEdge
import org.kotgll.rsm.grammar.symbol.Nonterminal
import org.kotgll.rsm.grammar.symbol.Terminal
import org.kotgll.rsm.graphinput.withsppf.parallel.GLL
import kotlin.test.assertEquals

class TestRSMGraphInputParallelSuccess {

    @Test
    fun `test 'empty' hand-crafted grammar`() {
        val nonterminalS = Nonterminal("S")
        val rsmState0 =
            RSMState(
                id = 0,
                nonterminal = nonterminalS,
                isStart = true,
                isFinal = true,
            )
        nonterminalS.startState = rsmState0

        val graph = GraphNode(id = 0, isStart = true, isFinal = true)

        val result = GLL(rsmState0, listOf(graph)).parse()
        val pairs: HashMap<Int, HashSet<Int>> = HashMap()
        result.keys.forEach { tail ->
            if (!pairs.containsKey(tail)) pairs[tail] = HashSet()
            result[tail]!!.keys.forEach { head -> pairs[tail]!!.add(head) }
        }

        assertEquals(expected = hashMapOf(0 to hashSetOf(0)), actual = pairs)
    }

    @ParameterizedTest(name = "Should be NotEmpty for {0}")
    @ValueSource(strings = ["", "a"])
    fun `test 'a-star' hand-crafted grammar`(input: String) {
        val nonterminalS = Nonterminal("S")
        val rsmState0 =
            RSMState(
                id = 0,
                nonterminal = nonterminalS,
                isStart = true,
                isFinal = true,
            )
        nonterminalS.startState = rsmState0
        val rsmState1 =
            RSMState(
                id = 1,
                nonterminal = nonterminalS,
                isFinal = true,
            )
        rsmState0.addTerminalEdge(
            RSMTerminalEdge(
                terminal = Terminal("a"),
                head = rsmState1,
            ))
        rsmState1.addTerminalEdge(
            RSMTerminalEdge(
                terminal = Terminal("a"),
                head = rsmState1,
            ))

        val result = GLL(rsmState0, listOf(readGraphFromString(input))).parse()
        val pairs: HashMap<Int, HashSet<Int>> = HashMap()
        result.keys.forEach { tail ->
            if (!pairs.containsKey(tail)) pairs[tail] = HashSet()
            result[tail]!!.keys.forEach { head -> pairs[tail]!!.add(head) }
        }

        assertEquals(expected = hashMapOf(0 to hashSetOf(input.length)), actual = pairs)
    }
}