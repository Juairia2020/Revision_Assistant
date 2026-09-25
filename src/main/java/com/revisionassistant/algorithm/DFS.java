package com.revisionassistant.algorithm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Depth-first search over a {@link Graph}.
 * <p>
 * <b>Purpose.</b> Two uses in this application: producing a
 * depth-first visit order ({@link #traverse}), and - its main job -
 * answering "is there already a path from A to B?" via
 * {@link #hasPath}. That question is exactly what
 * {@code TopicDependencyService} asks before adding a new prerequisite
 * edge: adding edge (prerequisite &rarr; topic) would close a cycle
 * exactly when a path already exists from {@code topic} back to
 * {@code prerequisite}.
 * <p>
 * <b>Input.</b> A {@link Graph} and start/target node ids.
 * <b>Output.</b> A visit order, or a boolean reachability answer.
 * <p>
 * <b>Complexity.</b> Time O(V + E) per call - each node is visited at
 * most once and each edge inspected at most once. Space O(V) for the
 * visited set and the recursion stack.
 */
public final class DFS {

    private DFS() {
        // Utility class - no instances.
    }

    /** Depth-first preorder visit order starting from {@code start}, following outgoing edges. */
    public static List<Integer> traverse(Graph graph, int start) {
        List<Integer> order = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        visit(graph, start, visited, order);
        return order;
    }

    private static void visit(Graph graph, int node, Set<Integer> visited, List<Integer> order) {
        if (!visited.add(node)) {
            return;
        }
        order.add(node);
        for (int next : graph.getSuccessors(node)) {
            visit(graph, next, visited, order);
        }
    }

    /** True if there is a directed path from {@code from} to {@code to} (following outgoing edges). */
    public static boolean hasPath(Graph graph, int from, int to) {
        if (from == to) {
            return true;
        }
        if (!graph.containsNode(from)) {
            return false;
        }
        return traverse(graph, from).contains(to);
    }
}
