package com.acme.salary.common;

import java.util.List;
import org.springframework.data.domain.Page;

/** A stable JSON shape for one page of results (Spring's {@link Page} is not meant to be serialised). */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

	public static <T> PageResponse<T> from(Page<T> page) {
		return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
				page.getTotalPages());
	}

}
