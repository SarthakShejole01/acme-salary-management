package com.acme.salary.employee;

import com.acme.salary.common.PageResponse;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/employees")
class EmployeeController {

	private final EmployeeService service;

	EmployeeController(EmployeeService service) {
		this.service = service;
	}

	/** Query parameters: search, country, department, jobTitle, page, size (max 100), sort. */
	@GetMapping
	PageResponse<EmployeeResponse> search(EmployeeSearchCriteria criteria, @PageableDefault(size = 20) Pageable pageable) {
		return this.service.search(criteria, pageable);
	}

	@GetMapping("/{id}")
	EmployeeResponse get(@PathVariable Long id) {
		return this.service.get(id);
	}

	@PostMapping
	ResponseEntity<EmployeeResponse> create(@Valid @RequestBody EmployeeRequest request) {
		EmployeeResponse created = this.service.create(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(created.id())
			.toUri();
		return ResponseEntity.created(location).body(created);
	}

	@PutMapping("/{id}")
	EmployeeResponse update(@PathVariable Long id, @Valid @RequestBody EmployeeRequest request) {
		return this.service.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@PathVariable Long id) {
		this.service.delete(id);
	}

}
