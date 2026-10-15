package com.acme.salary.insights;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

/**
 * Equal-width salary buckets with readable edges: the width is rounded up to 1, 2, 2.5 or 5 times a power of
 * ten, and the first edge down to a multiple of the width. Buckets are numbered from 1, matching PostgreSQL's
 * {@code width_bucket}; each includes its lower edge and excludes its upper edge.
 */
record HistogramBins(BigDecimal start, BigDecimal width, int count) {

	private static final BigDecimal SMALLEST_WIDTH = new BigDecimal("0.01");

	private static final List<BigDecimal> NICE_STEPS = List.of(new BigDecimal("1"), new BigDecimal("2"),
			new BigDecimal("2.5"), new BigDecimal("5"), new BigDecimal("10"));

	/** Bins of a nice width covering {@code min} to {@code max}, aiming for about {@code targetCount} buckets. */
	static HistogramBins covering(BigDecimal min, BigDecimal max, int targetCount) {
		BigDecimal width = niceWidth(max.subtract(min).divide(BigDecimal.valueOf(targetCount), MathContext.DECIMAL64));
		BigDecimal start = min.divide(width, 0, RoundingMode.FLOOR).multiply(width);
		int count = max.subtract(start).divide(width, 0, RoundingMode.FLOOR).intValueExact() + 1;
		return new HistogramBins(start, width, count);
	}

	BigDecimal end() {
		return upperEdge(this.count);
	}

	BigDecimal lowerEdge(int bucket) {
		return this.start.add(this.width.multiply(BigDecimal.valueOf(bucket - 1L)));
	}

	BigDecimal upperEdge(int bucket) {
		return this.start.add(this.width.multiply(BigDecimal.valueOf(bucket)));
	}

	private static BigDecimal niceWidth(BigDecimal rawWidth) {
		if (rawWidth.compareTo(SMALLEST_WIDTH) <= 0) {
			return SMALLEST_WIDTH;
		}
		int exponent = rawWidth.precision() - rawWidth.scale() - 1;
		BigDecimal magnitude = BigDecimal.ONE.scaleByPowerOfTen(exponent);
		BigDecimal fraction = rawWidth.divide(magnitude, MathContext.DECIMAL64);
		BigDecimal step = NICE_STEPS.stream()
			.filter((candidate) -> candidate.compareTo(fraction) >= 0)
			.findFirst()
			.orElseThrow();
		// 2.5 x 10^-2 would be 0.025: round up to whole cents so every edge is a valid Money amount.
		return step.multiply(magnitude).setScale(2, RoundingMode.CEILING).max(SMALLEST_WIDTH);
	}

}
