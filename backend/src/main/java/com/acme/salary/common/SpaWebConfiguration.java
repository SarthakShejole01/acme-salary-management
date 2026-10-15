package com.acme.salary.common;

import java.io.IOException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Serves the Angular build (copied into {@code static/} when the image is built) from the same origin as the
 * API. Files that exist are served as they are; any other path without a file extension gets {@code index.html},
 * so client-side routes such as {@code /employees} survive a refresh. API and actuator paths and missing files
 * keep their normal 404.
 */
@Configuration(proxyBeanMethods = false)
class SpaWebConfiguration implements WebMvcConfigurer {

	private static final String INDEX = "static/index.html";

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/**")
			.addResourceLocations("classpath:/static/")
			// Always revalidate: a deploy replaces the hashed bundle names that index.html points to.
			.setCacheControl(CacheControl.noCache())
			.resourceChain(true)
			.addResolver(new SpaResourceResolver());
	}

	private static final class SpaResourceResolver extends PathResourceResolver {

		@Override
		protected Resource getResource(String resourcePath, Resource location) throws IOException {
			Resource requested = location.createRelative(resourcePath);
			if (requested.isReadable()) {
				return requested;
			}
			return isClientRoute(resourcePath) ? indexOrNull() : null;
		}

		private static boolean isClientRoute(String path) {
			boolean backendPath = path.equals("api") || path.startsWith("api/") || path.startsWith("actuator");
			String lastSegment = path.substring(path.lastIndexOf('/') + 1);
			return !backendPath && !lastSegment.contains(".");
		}

		/** No app built into this run (for example the backend started on its own): plain 404. */
		private static Resource indexOrNull() {
			Resource index = new ClassPathResource(INDEX);
			return index.isReadable() ? index : null;
		}

	}

}
