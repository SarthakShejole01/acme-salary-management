package com.acme.salary.employee;

import com.acme.salary.currency.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
public class Employee {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String employeeCode;

	private String fullName;

	private String email;

	private String jobTitle;

	private String department;

	@Enumerated(EnumType.STRING)
	@Column(name = "country_code")
	private Country country;

	private Currency currency;

	private BigDecimal salary;

	@Enumerated(EnumType.STRING)
	private EmploymentType employmentType;

	private LocalDate hireDate;

	@CreatedDate
	@Column(updatable = false)
	private Instant createdAt;

	@LastModifiedDate
	private Instant updatedAt;

	protected Employee() {
	}

	public Employee(String employeeCode, String fullName, String email, String jobTitle, String department,
			Country country, BigDecimal salaryAmount, EmploymentType employmentType, LocalDate hireDate) {
		changeDetails(employeeCode, fullName, email, jobTitle, department, country, salaryAmount, employmentType,
				hireDate);
	}

	/** Replaces every editable field; business rules are checked by the service beforehand. */
	public void changeDetails(String employeeCode, String fullName, String email, String jobTitle,
			String department, Country country, BigDecimal salaryAmount, EmploymentType employmentType,
			LocalDate hireDate) {
		this.employeeCode = employeeCode;
		this.fullName = fullName;
		this.email = email;
		this.jobTitle = jobTitle;
		this.department = department;
		this.employmentType = employmentType;
		this.hireDate = hireDate;
		setCountryAndSalary(country, salaryAmount);
	}

	/** Currency always follows the country, so the two are set together. */
	private void setCountryAndSalary(Country country, BigDecimal salaryAmount) {
		Money money = new Money(salaryAmount, country.currency());
		this.country = country;
		this.currency = money.currency();
		this.salary = money.amount();
	}

	public Long getId() {
		return this.id;
	}

	public String getEmployeeCode() {
		return this.employeeCode;
	}

	public String getFullName() {
		return this.fullName;
	}

	public String getEmail() {
		return this.email;
	}

	public String getJobTitle() {
		return this.jobTitle;
	}

	public String getDepartment() {
		return this.department;
	}

	public Country getCountry() {
		return this.country;
	}

	public Money getSalary() {
		return new Money(this.salary, this.currency);
	}

	public EmploymentType getEmploymentType() {
		return this.employmentType;
	}

	public LocalDate getHireDate() {
		return this.hireDate;
	}

	public Instant getCreatedAt() {
		return this.createdAt;
	}

	public Instant getUpdatedAt() {
		return this.updatedAt;
	}

}
