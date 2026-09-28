package com.revisionassistant.algorithm;

import java.util.HashMap;
import java.util.Map;

/**
 * Whole-graph directed-cycle detection using the classic
 * white/gray/black DFS colouring.
 * <p>
 * <b>Purpose.</b> {@code TopicDependencyService} already stops a cycle
 * from ever being created (see {@link DFS#hasPath}), so in normal use
 * this should always report "no cycle". It still earns its place as a
 * genuine feature: the "Check Dependencies" action in the Study Tools
 * screen runs it on demand as an integrity check over the whole graph,
 * which is a useful safety net and a natural precondition to check
 * before running {@link TopologicalSort} (which is only defined for
 * an acyclic graph).
 * <p>
 * <b>Input.</b> A {@link Graph}.
 * <b>Output.</b> {@code true} if the graph contains at least one
 * directed cycle, {@code false} otherwise.
 * <p>
 * <b>Complexity.</b> Time O(V + E) - standard DFS with colouring,
 * every node and edge is examined a constant number of times. Space
 * O(V) for the colour map and recursion stack.
 */
public final class CycleDetection {

    private static final int WHITE = 0; // not yet visited
    private static final int GRAY = 1;  // on the current DFS path
    private static final int BLACK = 2; // fully explored

    private CycleDetection() {
        // Utility class - no instances.
    }

    public static boolean hasCycle(Graph graph) {
        Map<Integer, Integer> color = new HashMap<>();
        for (int node : graph.getNodes()) {
            color.put(node, WHITE);
        }
        for (int node : graph.getNodes()) {
            if (color.get(node) == WHITE && dfsVisit(graph, node, color)) {
                return true;
            }
        }
        return false;
    }

    private static boolean dfsVisit(Graph graph, int node, Map<Integer, Integer> color) {
        color.put(node, GRAY);
        for (int next : graph.getSuccessors(node)) {
            int state = color.get(next);
            if (state == GRAY) {
                return true; // back edge - a cycle
            }
            if (state == WHITE && dfsVisit(graph, next, color)) {
                return true;
            }
        }
        color.put(node, BLACK);
        return false;
    }
}
