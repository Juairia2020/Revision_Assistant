package com.revisionassistant.algorithm;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Topological sort using Kahn's algorithm (repeatedly removing nodes
 * with no remaining incoming edges).
 * <p>
 * <b>Purpose.</b> Produces a linear order of topics such that every
 * prerequisite appears before the topics that depend on it - directly
 * powering the "Suggested Study Order" feature.
 * <p>
 * <b>Input.</b> A {@link Graph} (edges point from prerequisite to
 * dependent, matching {@link Graph}'s "a before b" convention).
 * <b>Output.</b> {@code Optional.of(order)} with every node exactly
 * once, or {@code Optional.empty()} if the graph contains a cycle (a
 * topological order does not exist in that case).
 * <p>
 * <b>Complexity.</b> Time O(V + E) - each node is enqueued once and
 * each edge decrements exactly one in-degree counter. Space O(V) for
 * the in-degree map and queue.
 */
public final class TopologicalSort {

    private TopologicalSort() {
        // Utility class - no instances.
    }

    public static Optional<List<Integer>> sort(Graph graph) {
        Map<Integer, Integer> inDegree = new HashMap<>();
        for (int node : graph.getNodes()) {
            inDegree.put(node, 0);
        }
        for (int node : graph.getNodes()) {
            for (int next : graph.getSuccessors(node)) {
                inDegree.merge(next, 1, Integer::sum);
            }
        }

        Deque<Integer> queue = new ArrayDeque<>();
        for (Map.Entry<Integer, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        List<Integer> order = new ArrayList<>();
        while (!queue.isEmpty()) {
            int node = queue.poll();
            order.add(node);
            for (int next : graph.getSuccessors(node)) {
                int remaining = inDegree.get(next) - 1;
                inDegree.put(next, remaining);
                if (remaining == 0) {
                    queue.add(next);
                }
            }
        }

        if (order.size() != graph.size()) {
            return Optional.empty(); // a cycle prevented every node from reaching in-degree 0
        }
        return Optional.of(order);
    }
}
