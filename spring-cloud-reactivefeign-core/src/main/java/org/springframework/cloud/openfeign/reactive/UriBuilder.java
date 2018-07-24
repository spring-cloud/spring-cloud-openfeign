package org.springframework.cloud.openfeign.reactive;

import java.net.URI;
import java.util.List;
import java.util.Map;

public interface UriBuilder {

	/**
	 * @param uriTemplate the URI template to use
	 */
	UriBuilder uriString(String uriTemplate);

	/**
	 * Add the given query parameters.
	 * @param parameters the params
	 */
	UriBuilder queryParameters(Map<String, List<String>> parameters);

	/**
	 * Build a {@link URI} instance and replaces URI template variables
	 * with the values from a map.
	 * @param uriVariables the map of URI variables
	 * @return the URI
	 */
	URI build(Map<String, ?> uriVariables);
}
