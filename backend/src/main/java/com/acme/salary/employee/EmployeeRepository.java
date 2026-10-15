package com.acme.salary.employee;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

	boolean existsByEmployeeCode(String employeeCode);

	boolean existsByEmailIgnoreCase(String email);

	boolean existsByEmployeeCodeAndIdNot(String employeeCode, Long id);

	boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

	@Query("select distinct e.department from Employee e order by e.department")
	List<String> findDistinctDepartments();

	@Query("select distinct e.jobTitle from Employee e order by e.jobTitle")
	List<String> findDistinctJobTitles();

}
