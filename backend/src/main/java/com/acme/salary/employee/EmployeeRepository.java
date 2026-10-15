package com.acme.salary.employee;

import org.springframework.data.jpa.repository.JpaRepository;

interface EmployeeRepository extends JpaRepository<Employee, Long> {

	boolean existsByEmployeeCode(String employeeCode);

	boolean existsByEmailIgnoreCase(String email);

	boolean existsByEmployeeCodeAndIdNot(String employeeCode, Long id);

	boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

}
