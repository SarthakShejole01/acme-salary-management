package com.acme.salary.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.acme.salary.TestcontainersConfiguration;
import com.acme.salary.common.ClockConfiguration;
import com.acme.salary.common.JpaAuditingConfiguration;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import({ TestcontainersConfiguration.class, ClockConfiguration.class, JpaAuditingConfiguration.class })
class EmployeeCsvExporterTest {

	private static final EmployeeSearchCriteria EVERYONE = new EmployeeSearchCriteria(null, null, null, null);

	@Autowired
	private EmployeeRepository repository;

	private EmployeeCsvExporter exporter;

	@BeforeEach
	void setUp() {
		this.exporter = new EmployeeCsvExporter(this.repository, 2);
		this.repository.saveAllAndFlush(List.of(employee("EMP-00001", "Priya Sharma", Country.IN),
				employee("EMP-00002", "Arjun Mehta", Country.IN), employee("EMP-00003", "Emily Clark", Country.US),
				employee("EMP-00004", "Lukas Weber", Country.DE), employee("EMP-00005", "Sophie Martin", Country.FR)));
	}

	@Test
	void writes_the_header_then_every_matching_employee() throws IOException {
		List<String> lines = lines(export(new EmployeeSearchCriteria(null, Country.IN, null, null), Sort.unsorted()));

		assertThat(lines).hasSize(3);
		assertThat(lines.getFirst()).startsWith("Employee code,Full name");
		assertThat(lines.subList(1, 3)).allMatch((line) -> line.contains(",IN,INR,"));
	}

	@Test
	void writes_every_row_once_in_sort_order_across_pages() throws IOException {
		List<String> lines = lines(export(EVERYONE, Sort.unsorted()));

		assertThat(lines.subList(1, lines.size())).extracting((line) -> line.split(",")[1])
			.containsExactly("Arjun Mehta", "Emily Clark", "Lukas Weber", "Priya Sharma", "Sophie Martin");
	}

	@Test
	void honours_the_requested_sort() throws IOException {
		List<String> lines = lines(export(EVERYONE, Sort.by(Sort.Direction.DESC, "employeeCode")));

		assertThat(lines.get(1)).startsWith("EMP-00005,");
		assertThat(lines.getLast()).startsWith("EMP-00001,");
	}

	@Test
	void starts_with_a_utf8_byte_order_mark_so_excel_reads_accents() throws IOException {
		byte[] csv = export(EVERYONE, Sort.unsorted());

		assertThat(Arrays.copyOf(csv, 3)).containsExactly(0xEF, 0xBB, 0xBF);
	}

	@Test
	void rejects_an_unsupported_sort_before_writing_anything() {
		assertThatThrownBy(() -> this.exporter.export(EVERYONE, Sort.by("createdAt")))
			.isInstanceOf(UnsupportedSortException.class);
	}

	private byte[] export(EmployeeSearchCriteria criteria, Sort sort) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		this.exporter.export(criteria, sort).writeTo(out);
		return out.toByteArray();
	}

	private static List<String> lines(byte[] csv) {
		String text = new String(csv, StandardCharsets.UTF_8).replace("﻿", "");
		return List.of(text.split("\r\n"));
	}

	private static Employee employee(String code, String name, Country country) {
		return new Employee(code, name, code.toLowerCase() + "@acme.com", "Software Engineer", "Engineering", country,
				new BigDecimal("50000"), EmploymentType.FULL_TIME, LocalDate.of(2022, 4, 1));
	}

}
