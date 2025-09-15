/*
 * Copyright 2013-present the original author or authors.
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

package org.springframework.cloud.openfeign;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import feign.Client;
import feign.Request;
import feign.Response;
import tools.jackson.databind.ObjectMapper;

/**
 * @author Jasbir Singh
 */
public class OptionsTestClient implements Client {

	private final static ObjectMapper mapper;

	static {
		mapper = new ObjectMapper();
	}

	@Override
	public Response execute(Request request, Request.Options options) {
		return Response.builder()
			.status(200)
			.request(request)
			.headers(headers())
			.body(prepareResponse(options))
			.build();
	}

	private Map<String, Collection<String>> headers() {
		Map<String, Collection<String>> headers = new LinkedHashMap<>();
		headers.put("Content-Type", Collections.singletonList("application/json"));
		return headers;
	}

	private byte[] prepareResponse(Request.Options options) {
		OptionsResponseForTests response = new OptionsResponseForTests(options.connectTimeoutMillis(),
				TimeUnit.MILLISECONDS, options.readTimeoutMillis(), TimeUnit.MILLISECONDS);
		return mapper.writeValueAsString(response).getBytes();
	}

	record OptionsResponseForTests(long connectTimeout, TimeUnit connectTimeoutUnit, long readTimeout,
			TimeUnit readTimeoutUnit) {

		@Override
		public String toString() {
			return "OptionsResponseForTests{" + "connectTimeout=" + connectTimeout + ", connectTimeoutUnit="
					+ connectTimeoutUnit + ", readTimeout=" + readTimeout + ", readTimeoutUnit=" + readTimeoutUnit
					+ '}';
		}

	}

}
