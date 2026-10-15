package com.acme.salary.employee;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

	boolean existsByEmployeeCode(String employeeCode);

	boolean existsByEmailIgnoreCase(String email);

	boolean existsByEmployeeCodeAndIdNot(String employeeCode, Long id);

	boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

}
