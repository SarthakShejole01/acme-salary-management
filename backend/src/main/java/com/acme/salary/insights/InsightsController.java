package com.acme.salary.insights;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/insights")
class InsightsController {

	private final InsightsService service;

	InsightsController(InsightsService service) {
		this.service = service;
	}

	@GetMapping("/countries")
	List<CountryInsight> countries() {
		return this.service.countryInsights();
	}

}
