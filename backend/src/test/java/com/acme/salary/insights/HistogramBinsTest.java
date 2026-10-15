package com.acme.salary.insights;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class HistogramBinsTest {

	@Test
	void rounds_bucket_width_up_to_a_nice_number() {
		HistogramBins bins = HistogramBins.covering(amount("16000"), amount("109500"), 10);

		assertThat(bins.width()).isEqualByComparingTo("10000");
		assertThat(bins.start()).isEqualByComparingTo("10000");
		assertThat(bins.count()).isEqualTo(10);
	}

	@Test
	void uses_lakh_sized_buckets_for_indian_salaries() {
		HistogramBins bins = HistogramBins.covering(amount("508000"), amount("3621000"), 10);

		assertThat(bins.width()).isEqualByComparingTo("500000");
		assertThat(bins.start()).isEqualByComparingTo("500000");
		assertThat(bins.end()).isEqualByComparingTo("4000000");
	}

	@ParameterizedTest(name = "{0} .. {1}")
	@CsvSource({
			"16000, 109500",
			"508000, 3621000",
			"28600, 200800",
			"30000, 40000",
			"10.01, 10.02",
			"99999.99, 100000.00",
	})
	void covers_the_lowest_and_highest_salary(String min, String max) {
		HistogramBins bins = HistogramBins.covering(amount(min), amount(max), 10);

		assertThat(bins.start()).isLessThanOrEqualTo(amount(min));
		assertThat(bins.end()).isGreaterThan(amount(max));
		assertThat(bins.count()).isBetween(1, 11);
	}

	@ParameterizedTest(name = "range {0} gives width {1}")
	@CsvSource({
			"9350, 1000",
			"10000, 1000",
			"15000, 2000",
			"22000, 2500",
			"40000, 5000",
			"51000, 10000",
	})
	void picks_widths_from_one_two_two_and_a_half_or_five_times_a_power_of_ten(String range, String width) {
		HistogramBins bins = HistogramBins.covering(BigDecimal.ZERO, amount(range), 10);

		assertThat(bins.width()).isEqualByComparingTo(width);
	}

	@Test
	void never_uses_buckets_narrower_than_one_cent() {
		HistogramBins bins = HistogramBins.covering(amount("10.01"), amount("10.02"), 10);

		assertThat(bins.width()).isEqualByComparingTo("0.01");
	}

	@Test
	void gives_each_bucket_a_lower_and_upper_edge() {
		HistogramBins bins = HistogramBins.covering(amount("16000"), amount("109500"), 10);

		assertThat(bins.lowerEdge(1)).isEqualByComparingTo("10000");
		assertThat(bins.upperEdge(1)).isEqualByComparingTo("20000");
		assertThat(bins.upperEdge(10)).isEqualByComparingTo("110000");
	}

	private static BigDecimal amount(String value) {
		return new BigDecimal(value);
	}

}
