package com.revisionassistant.algorithm;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Breadth-first search over a {@link Graph}.
 * <p>
 * <b>Purpose.</b> Finds every node reachable from a starting node,
 * either following edges forward or backward. In this application
 * that answers two real questions directly: "which topics
 * (transitively) depend on this one?" ({@link #reachableForward}) and
 * "which topics does this one (transitively) require?"
 * ({@link #reachableBackward}).
 * <p>
 * <b>Input.</b> A {@link Graph} and a starting node id.
 * <b>Output.</b> The set of node ids reachable from the start (the
 * start node itself is excluded).
 * <p>
 * <b>Complexity.</b> Time O(V + E) - every node is enqueued at most
 * once and every edge is inspected at most once. Space O(V) for the
 * visited set and queue.
 */
public final class BFS {

    private BFS() {
        // Utility class - no instances.
    }

    /** All nodes reachable from {@code start} by following outgoing edges (its descendants). */
    public static Set<Integer> reachableForward(Graph graph, int start) {
        return reachable(graph, start, true);
    }

    /** All nodes that can reach {@code start} by following edges backward (its ancestors). */
    public static Set<Integer> reachableBackward(Graph graph, int start) {
        return reachable(graph, start, false);
    }

    private static Set<Integer> reachable(Graph graph, int start, boolean forward) {
        Set<Integer> visited = new HashSet<>();
        Deque<Integer> queue = new ArrayDeque<>();
        visited.add(start);
        queue.add(start);

        while (!queue.isEmpty()) {
            int node = queue.poll();
            List<Integer> neighbours = forward ? graph.getSuccessors(node) : graph.getPredecessors(node);
            for (int next : neighbours) {
                if (visited.add(next)) {
                    queue.add(next);
                }
            }
        }

        visited.remove(start);
        return visited;
    }
}
