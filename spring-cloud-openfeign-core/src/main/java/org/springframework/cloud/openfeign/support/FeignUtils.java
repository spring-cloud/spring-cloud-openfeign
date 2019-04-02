/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.openfeign.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;

import static java.util.Optional.ofNullable;

/**
 * @author Spencer Gibb
 * @author Olga Maciaszek-Sharma
 */
public final class FeignUtils {

	private FeignUtils() {
		throw new IllegalStateException("Can't instantiate a utility class");
	}

	static HttpHeaders getHttpHeaders(Map<String, Collection<String>> headers) {
		HttpHeaders httpHeaders = new HttpHeaders();
		for (Map.Entry<String, Collection<String>> entry : headers.entrySet()) {
			httpHeaders.put(entry.getKey(), new ArrayList<>(entry.getValue()));
		}
		return httpHeaders;
	}

	static Map<String, Collection<String>> getHeaders(HttpHeaders httpHeaders) {
		LinkedHashMap<String, Collection<String>> headers = new LinkedHashMap<>();

		for (Map.Entry<String, List<String>> entry : httpHeaders.entrySet()) {
			headers.put(entry.getKey(), entry.getValue());
		}

		return headers;
	}

	static Collection<String> addTemplateParameter(Collection<String> possiblyNull,
			String paramName) {
		Collection<String> params = ofNullable(possiblyNull).map(ArrayList::new)
				.orElse(new ArrayList<>());
		params.add(String.format("{%s}", paramName));
		return params;
	}

}
