package com.revisionassistant.service;

import com.revisionassistant.algorithm.BFS;
import com.revisionassistant.algorithm.CycleDetection;
import com.revisionassistant.algorithm.DFS;
import com.revisionassistant.algorithm.Graph;
import com.revisionassistant.algorithm.ShortestPath;
import com.revisionassistant.algorithm.TopologicalSort;
import com.revisionassistant.dao.TopicDAO;
import com.revisionassistant.dao.TopicDependencyDAO;
import com.revisionassistant.model.Topic;
import com.revisionassistant.model.TopicDependency;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validation and business rules for the topic dependency graph.
 * Controllers talk to this class instead of the DAOs or the
 * algorithm package directly: this class is the one place that turns
 * database rows into a {@link Graph}, runs the algorithms on it, and
 * turns the results back into {@link Topic} objects.
 */
public class TopicDependencyService {

    private final TopicDependencyDAO dependencyDAO;
    private final TopicDAO topicDAO;

    public TopicDependencyService() {
        this.dependencyDAO = new TopicDependencyDAO();
        this.topicDAO = new TopicDAO();
    }

    /**
     * Marks {@code prerequisiteId} as required before {@code topicId}.
     * Rejects a topic depending on itself, a relationship that
     * already exists, and - using {@link DFS#hasPath} - any
     * relationship that would create a circular dependency.
     */
    public void addDependency(int topicId, int prerequisiteId) throws SQLException {
        if (topicId == prerequisiteId) {
            throw new IllegalArgumentException("A topic cannot be a prerequisite of itself.");
        }
        if (topicDAO.findById(topicId) == null || topicDAO.findById(prerequisiteId) == null) {
            throw new IllegalArgumentException("Please choose two valid topics.");
        }
        if (dependencyDAO.findPrerequisitesOf(topicId).contains(prerequisiteId)) {
            throw new IllegalStateException("That prerequisite relationship already exists.");
        }

        Graph graph = buildGraph();
        // Adding edge (prerequisiteId -> topicId) would close a cycle exactly when a path
        // already exists the other way round, from topicId back to prerequisiteId.
        if (DFS.hasPath(graph, topicId, prerequisiteId)) {
            throw new IllegalStateException(
                    "That would create a circular dependency (these topics already depend on each other).");
        }

        dependencyDAO.insert(topicId, prerequisiteId);
    }

    public void removeDependency(int topicId, int prerequisiteId) throws SQLException {
        dependencyDAO.delete(topicId, prerequisiteId);
    }

    /** The topics listed directly as prerequisites of {@code topicId}. */
    public List<Topic> getDirectPrerequisites(int topicId) throws SQLException {
        return toTopics(dependencyDAO.findPrerequisitesOf(topicId));
    }

    /** The topics that directly require {@code topicId}. */
    public List<Topic> getDirectDependents(int topicId) throws SQLException {
        return toTopics(dependencyDAO.findDependentsOf(topicId));
    }

    /** Every topic that {@code topicId} requires, directly or transitively (all of its ancestors). */
    public List<Topic> getAllPrerequisites(int topicId) throws SQLException {
        Graph graph = buildGraph();
        Set<Integer> ancestorIds = BFS.reachableBackward(graph, topicId);
        return toTopics(new ArrayList<>(ancestorIds));
    }

    /** Every topic that requires {@code topicId}, directly or transitively (all of its descendants). */
    public List<Topic> getAllDependents(int topicId) throws SQLException {
        Graph graph = buildGraph();
        Set<Integer> descendantIds = BFS.reachableForward(graph, topicId);
        return toTopics(new ArrayList<>(descendantIds));
    }

    /**
     * A valid study order for the topics in one subject, respecting
     * every prerequisite - including prerequisites that belong to a
     * different subject. Built by topologically sorting the whole
     * dependency graph and then keeping only this subject's topics,
     * which preserves their correct relative order.
     */
    public List<Topic> getStudyOrder(int subjectId) throws SQLException {
        Graph graph = buildGraph();
        Optional<List<Integer>> sorted = TopologicalSort.sort(graph);
        if (sorted.isEmpty()) {
            throw new IllegalStateException(
                    "The dependency graph currently contains a cycle, so no valid study order exists. "
                            + "Use \"Check Dependencies\" to find it.");
        }

        Set<Integer> subjectTopicIds = topicDAO.findBySubjectId(subjectId).stream()
                .map(Topic::getId).collect(Collectors.toSet());

        List<Topic> order = new ArrayList<>();
        for (int topicId : sorted.get()) {
            if (subjectTopicIds.contains(topicId)) {
                order.add(topicDAO.findById(topicId));
            }
        }
        return order;
    }

    /** True if the dependency graph (across every subject) currently contains a cycle. */
    public boolean hasCycle() throws SQLException {
        return CycleDetection.hasCycle(buildGraph());
    }

    /** The shortest chain of prerequisites connecting two topics, or an empty list if none exists. */
    public List<Topic> getDependencyPath(int fromTopicId, int toTopicId) throws SQLException {
        Graph graph = buildGraph();
        List<Integer> pathIds = ShortestPath.pathBetween(graph, fromTopicId, toTopicId);
        return toTopics(pathIds);
    }

    /**
     * How many prerequisite-steps away each other topic is from
     * {@code topicId}, following the direction "requires" (i.e. only
     * topics that depend on it, directly or indirectly, are included).
     */
    public Map<Topic, Integer> getDependentDistances(int topicId) throws SQLException {
        Graph graph = buildGraph();
        Map<Integer, Integer> distances = ShortestPath.distancesFrom(graph, topicId);

        Map<Topic, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<Integer, Integer> entry : distances.entrySet()) {
            Topic topic = topicDAO.findById(entry.getKey());
            if (topic != null) {
                result.put(topic, entry.getValue());
            }
        }
        return result;
    }

    /** Builds the full dependency graph: one node per topic, one edge per stored prerequisite relationship. */
    private Graph buildGraph() throws SQLException {
        Graph graph = new Graph();
        for (Topic topic : topicDAO.findAll()) {
            graph.addNode(topic.getId());
        }
        for (TopicDependency dependency : dependencyDAO.findAll()) {
            // Edge points from the prerequisite to the topic that requires it, matching
            // Graph's "a before b" / TopologicalSort's "prerequisites first" convention.
            graph.addEdge(dependency.getPrerequisiteId(), dependency.getTopicId());
        }
        return graph;
    }

    private List<Topic> toTopics(List<Integer> topicIds) throws SQLException {
        List<Topic> topics = new ArrayList<>();
        for (int id : topicIds) {
            Topic topic = topicDAO.findById(id);
            if (topic != null) {
                topics.add(topic);
            }
        }
        return topics;
    }
}
