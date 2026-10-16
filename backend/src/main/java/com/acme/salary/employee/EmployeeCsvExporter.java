package com.acme.salary.employee;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * Streams the filtered employee list as CSV. Rows are read a page at a time (each page its own short read), so
 * memory stays flat however many employees match. Starts with a UTF-8 byte order mark so Excel shows accents.
 */
@Component
public class EmployeeCsvExporter {

	static final int PAGE_SIZE = 1_000;

	private static final char BYTE_ORDER_MARK = '﻿';

	private final EmployeeRepository repository;

	private final int pageSize;

	@Autowired
	EmployeeCsvExporter(EmployeeRepository repository) {
		this(repository, PAGE_SIZE);
	}

	EmployeeCsvExporter(EmployeeRepository repository, int pageSize) {
		this.repository = repository;
		this.pageSize = pageSize;
	}

	/**
	 * Checks the sort straight away, so an invalid request fails with 400 before any of the file is sent, and
	 * returns the body that writes the CSV.
	 */
	public StreamingResponseBody export(EmployeeSearchCriteria criteria, Sort requestedSort) {
		Sort sort = EmployeeSort.supported(requestedSort);
		Specification<Employee> matching = EmployeeSpecifications.matching(criteria);
		return (out) -> {
			Writer writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
			writer.write(BYTE_ORDER_MARK);
			writer.write(EmployeeCsv.header());
			Page<Employee> page;
			int pageNumber = 0;
			do {
				page = this.repository.findAll(matching, PageRequest.of(pageNumber++, this.pageSize, sort));
				for (Employee employee : page) {
					writer.write(EmployeeCsv.row(EmployeeResponse.from(employee)));
				}
				writer.flush();
			}
			while (page.hasNext());
		};
	}

}
