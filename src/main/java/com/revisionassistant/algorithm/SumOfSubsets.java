package com.revisionassistant.algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Sum of Subsets, solved with bottom-up dynamic programming over
 * reachable totals.
 * <p>
 * <b>Purpose.</b> Powers the "maximise time used" Study Planner mode:
 * unlike {@link Knapsack}, this mode ignores each task's
 * priority/urgency score entirely and instead asks a purely
 * combinatorial question - "which combination of tasks' estimated
 * durations comes closest to (without exceeding) the available time,
 * so as little of it goes unused as possible?" This is the classic
 * Sum of Subsets problem: decide, for each achievable total from 0 up
 * to the capacity, whether some subset of the items sums to exactly
 * that total, then take the largest achievable total.
 * <p>
 * <b>Input.</b> A list of {@link PlanningItem}s (only their weight -
 * estimated minutes - is used; value is ignored) and a capacity
 * (available minutes).
 * <b>Output.</b> A {@link Result} holding the selected items and
 * their total weight (the largest achievable total &le; capacity).
 * <p>
 * <b>Complexity.</b> Time O(n &times; capacity). Space O(n &times;
 * capacity) for the boolean reachability table, again kept 2D so the
 * chosen subset can be recovered by backtracking rather than only the
 * best achievable total.
 */
public final class SumOfSubsets {

    private SumOfSubsets() {
        // Utility class - no instances.
    }

    public static Result solve(List<PlanningItem> items, int capacity) {
        int n = items.size();
        boolean[][] reachable = new boolean[n + 1][capacity + 1];
        for (int i = 0; i <= n; i++) {
            reachable[i][0] = true; // a total of 0 is always achievable (the empty subset)
        }

        for (int i = 1; i <= n; i++) {
            PlanningItem item = items.get(i - 1);
            for (int w = 0; w <= capacity; w++) {
                reachable[i][w] = reachable[i - 1][w];
                if (!reachable[i][w] && item.getWeight() <= w && reachable[i - 1][w - item.getWeight()]) {
                    reachable[i][w] = true;
                }
            }
        }

        int bestTotal = capacity;
        while (bestTotal > 0 && !reachable[n][bestTotal]) {
            bestTotal--;
        }

        List<PlanningItem> selected = new ArrayList<>();
        int w = bestTotal;
        for (int i = n; i >= 1 && w > 0; i--) {
            if (!reachable[i - 1][w]) {
                PlanningItem item = items.get(i - 1);
                selected.add(item);
                w -= item.getWeight();
            }
        }
        Collections.reverse(selected);

        return new Result(selected, bestTotal);
    }

    /** The chosen items plus their combined weight (the closest achievable fit to the capacity). */
    public static class Result {
        private final List<PlanningItem> selectedItems;
        private final int totalWeight;

        public Result(List<PlanningItem> selectedItems, int totalWeight) {
            this.selectedItems = selectedItems;
            this.totalWeight = totalWeight;
        }

        public List<PlanningItem> getSelectedItems() {
            return selectedItems;
        }

        public int getTotalWeight() {
            return totalWeight;
        }
    }
}
