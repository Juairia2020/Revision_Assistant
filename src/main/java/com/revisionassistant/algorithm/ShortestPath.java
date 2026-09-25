package com.revisionassistant.algorithm;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Shortest paths over the (unweighted) dependency graph, via BFS.
 * Every edge has an implicit weight of 1 (each edge is "one topic
 * closer"), so BFS already gives the shortest path in terms of number
 * of edges - no need for Dijkstra's algorithm here.
 * <p>
 * <b>Purpose.</b>
 * <ul>
 *     <li>{@link #distancesFrom} is a <b>single-source shortest path</b>
 *     computation: from one chosen topic, how many prerequisite steps
 *     away is every other reachable topic? Used to show how "deep"
 *     each dependent topic is relative to a foundational one.</li>
 *     <li>{@link #pathBetween} reconstructs the actual shortest chain
 *     of topics between two chosen topics - the "Identifying
 *     dependency paths" feature.</li>
 * </ul>
 * <b>Note on all-pairs shortest paths:</b> the assignment brief also
 * mentions all-source shortest paths. Only single-source is
 * implemented here, deliberately: an all-pairs table (e.g.
 * Floyd-Warshall, O(V&sup3;)) would need to be computed and stored up
 * front for every topic pair, but nothing in the UI ever needs more
 * than "distances from the one topic currently selected" - so an
 * all-pairs table would sit unused and only add complexity. Running
 * {@link #distancesFrom} again whenever the user picks a different
 * topic is simpler and just as fast in practice for the topic counts
 * a student would realistically enter.
 * <p>
 * <b>Input.</b> A {@link Graph} and one or two node ids.
 * <b>Output.</b> A distance map, or an ordered list of node ids
 * forming the shortest path (empty if unreachable).
 * <p>
 * <b>Complexity.</b> Time O(V + E) per call - standard BFS. Space O(V)
 * for the distance/parent maps and the queue.
 */
public final class ShortestPath {

    private ShortestPath() {
        // Utility class - no instances.
    }

    /** Single-source shortest distances (in number of edges) from {@code source} to every reachable node. */
    public static Map<Integer, Integer> distancesFrom(Graph graph, int source) {
        Map<Integer, Integer> distances = new HashMap<>();
        Deque<Integer> queue = new ArrayDeque<>();
        distances.put(source, 0);
        queue.add(source);

        while (!queue.isEmpty()) {
            int node = queue.poll();
            for (int next : graph.getSuccessors(node)) {
                if (!distances.containsKey(next)) {
                    distances.put(next, distances.get(node) + 1);
                    queue.add(next);
                }
            }
        }

        distances.remove(source);
        return distances;
    }

    /** The shortest path from {@code source} to {@code target} as a list of node ids (inclusive of both), or empty if unreachable. */
    public static List<Integer> pathBetween(Graph graph, int source, int target) {
        if (source == target) {
            return List.of(source);
        }

        Map<Integer, Integer> parent = new HashMap<>();
        Set<Integer> visited = new HashSet<>();
        Deque<Integer> queue = new ArrayDeque<>();
        visited.add(source);
        queue.add(source);

        while (!queue.isEmpty()) {
            int node = queue.poll();
            if (node == target) {
                break;
            }
            for (int next : graph.getSuccessors(node)) {
                if (visited.add(next)) {
                    parent.put(next, node);
                    queue.add(next);
                }
            }
        }

        if (!visited.contains(target)) {
            return List.of();
        }

        LinkedList<Integer> path = new LinkedList<>();
        int current = target;
        path.addFirst(current);
        while (current != source) {
            current = parent.get(current);
            path.addFirst(current);
        }
        return path;
    }
}
