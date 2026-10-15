package com.acme.salary.seed;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.io.ClassPathResource;

/** First and last names the generator combines; one name per line in {@code seed/*.txt}. */
public record NameLists(List<String> firstNames, List<String> lastNames) {

	public NameLists {
		firstNames = List.copyOf(firstNames);
		lastNames = List.copyOf(lastNames);
	}

	public static NameLists fromClasspath() {
		return new NameLists(read("seed/first_names.txt"), read("seed/last_names.txt"));
	}

	private static List<String> read(String path) {
		try (InputStream in = new ClassPathResource(path).getInputStream()) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines()
				.map(String::strip)
				.filter((line) -> !line.isEmpty())
				.toList();
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Cannot read " + path, ex);
		}
	}

}
