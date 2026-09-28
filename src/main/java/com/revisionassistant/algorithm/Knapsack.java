package com.revisionassistant.algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Classic 0/1 knapsack, solved with bottom-up dynamic programming.
 * <p>
 * <b>Purpose.</b> Powers the priority-based Study Planner: out of all
 * pending tasks, choose the subset that fits within the student's
 * available time while maximising total "value" (a score combining
 * priority, deadline urgency and exam relevance - computed by
 * {@code StudyPlannerService}, not by this class). This is a genuine
 * 0/1 knapsack - each task is either fully included in the plan or
 * left out; it cannot be partially done.
 * <p>
 * <b>Input.</b> A list of {@link PlanningItem}s (weight = estimated
 * minutes, value = planning score) and a capacity (available minutes).
 * <b>Output.</b> A {@link Result} holding the selected items, their
 * total weight and their total value.
 * <p>
 * <b>Complexity.</b> Time O(n &times; capacity) where n is the number
 * of tasks. Space O(n &times; capacity) for the DP table - kept as a
 * full 2D table (rather than the usual 1D rolling-array optimisation)
 * specifically so the selected items can be recovered by backtracking
 * through it; the application needs to display which tasks were
 * chosen, not just the best possible score.
 */
public final class Knapsack {

    private Knapsack() {
        // Utility class - no instances.
    }

    public static Result solve(List<PlanningItem> items, int capacity) {
        int n = items.size();
        int[][] dp = new int[n + 1][capacity + 1];

        for (int i = 1; i <= n; i++) {
            PlanningItem item = items.get(i - 1);
            for (int w = 0; w <= capacity; w++) {
                dp[i][w] = dp[i - 1][w];
                if (item.getWeight() <= w) {
                    dp[i][w] = Math.max(dp[i][w], dp[i - 1][w - item.getWeight()] + item.getValue());
                }
            }
        }

        List<PlanningItem> selected = new ArrayList<>();
        int w = capacity;
        for (int i = n; i >= 1; i--) {
            if (dp[i][w] != dp[i - 1][w]) {
                PlanningItem item = items.get(i - 1);
                selected.add(item);
                w -= item.getWeight();
            }
        }
        Collections.reverse(selected);

        int totalWeight = selected.stream().mapToInt(PlanningItem::getWeight).sum();
        return new Result(selected, totalWeight, dp[n][capacity]);
    }

    /** The chosen items plus their combined weight and value. */
    public static class Result {
        private final List<PlanningItem> selectedItems;
        private final int totalWeight;
        private final int totalValue;

        public Result(List<PlanningItem> selectedItems, int totalWeight, int totalValue) {
            this.selectedItems = selectedItems;
            this.totalWeight = totalWeight;
            this.totalValue = totalValue;
        }

        public List<PlanningItem> getSelectedItems() {
            return selectedItems;
        }

        public int getTotalWeight() {
            return totalWeight;
        }

        public int getTotalValue() {
            return totalValue;
        }
    }
}
