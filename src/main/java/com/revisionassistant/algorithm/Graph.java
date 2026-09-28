package com.revisionassistant.algorithm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A simple directed graph over integer node ids (topic ids, in this
 * application). Deliberately independent of any model class so the
 * algorithm package can be reasoned about - and unit tested - on its
 * own, without a database.
 * <p>
 * An edge {@code addEdge(a, b)} means "a comes before b" - in the
 * topic dependency graph this is read as "a is a prerequisite of b".
 * Both the forward adjacency (successors) and reverse adjacency
 * (predecessors) are maintained, since the application needs to walk
 * the graph in both directions (e.g. "what does this topic require?"
 * vs. "what requires this topic?").
 * <p>
 * <b>Complexity.</b> {@code addNode}/{@code addEdge} are O(1) amortised.
 * {@code getSuccessors}/{@code getPredecessors} are O(1) lookups that
 * return a view in O(out-degree)/O(in-degree). Space is O(V + E).
 */
public class Graph {

    private final Set<Integer> nodes = new LinkedHashSet<>();
    private final Map<Integer, List<Integer>> outgoing = new HashMap<>();
    private final Map<Integer, List<Integer>> incoming = new HashMap<>();

    public void addNode(int id) {
        if (nodes.add(id)) {
            outgoing.put(id, new ArrayList<>());
            incoming.put(id, new ArrayList<>());
        }
    }

    public void addEdge(int from, int to) {
        addNode(from);
        addNode(to);
        outgoing.get(from).add(to);
        incoming.get(to).add(from);
    }

    public Set<Integer> getNodes() {
        return nodes;
    }

    /** Nodes with a direct edge FROM {@code id} (e.g. topics that directly require it). */
    public List<Integer> getSuccessors(int id) {
        return outgoing.getOrDefault(id, List.of());
    }

    /** Nodes with a direct edge TO {@code id} (e.g. its direct prerequisites). */
    public List<Integer> getPredecessors(int id) {
        return incoming.getOrDefault(id, List.of());
    }

    public boolean containsNode(int id) {
        return nodes.contains(id);
    }

    public int size() {
        return nodes.size();
    }
}
