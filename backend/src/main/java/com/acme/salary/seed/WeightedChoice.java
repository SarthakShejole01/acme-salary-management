package com.acme.salary.seed;

import java.util.List;
import java.util.Random;
import java.util.function.ToIntFunction;

/** Picks items with probability proportional to an integer weight, using the caller's seeded random. */
final class WeightedChoice<T> {

	private final List<T> items;

	private final int[] cumulativeWeights;

	private WeightedChoice(List<T> items, ToIntFunction<T> weight) {
		this.items = List.copyOf(items);
		this.cumulativeWeights = new int[this.items.size()];
		int total = 0;
		for (int i = 0; i < this.items.size(); i++) {
			total += weight.applyAsInt(this.items.get(i));
			this.cumulativeWeights[i] = total;
		}
	}

	static <T> WeightedChoice<T> of(List<T> items, ToIntFunction<T> weight) {
		return new WeightedChoice<>(items, weight);
	}

	T pick(Random random) {
		int target = random.nextInt(this.cumulativeWeights[this.cumulativeWeights.length - 1]);
		for (int i = 0; i < this.cumulativeWeights.length; i++) {
			if (target < this.cumulativeWeights[i]) {
				return this.items.get(i);
			}
		}
		throw new IllegalStateException("unreachable");
	}

}
