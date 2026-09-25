package com.revisionassistant.algorithm;

/**
 * A generic weighted, valued item for the study-planning algorithms
 * ({@link Knapsack}, {@link SumOfSubsets}). Deliberately independent
 * of the {@code Task} model, so the algorithm package has no
 * dependency on the rest of the application - {@code StudyPlannerService}
 * is responsible for turning tasks into {@code PlanningItem}s (weight
 * = estimated minutes, value = a computed priority/urgency/exam-relevance
 * score) and back again.
 */
public class PlanningItem {

    private final int id;
    private final int weight;
    private final int value;

    public PlanningItem(int id, int weight, int value) {
        this.id = id;
        this.weight = weight;
        this.value = value;
    }

    public int getId() {
        return id;
    }

    public int getWeight() {
        return weight;
    }

    public int getValue() {
        return value;
    }
}
