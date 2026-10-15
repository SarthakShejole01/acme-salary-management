package com.acme.salary.employee;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ReferenceDataController {

	private final ReferenceDataService service;

	ReferenceDataController(ReferenceDataService service) {
		this.service = service;
	}

	@GetMapping("/api/reference-data")
	ReferenceDataResponse referenceData() {
		return this.service.referenceData();
	}

}
