/*
 * Copyright 2013-2022 the original author or authors.
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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import feign.Client;
import feign.Request;
import feign.Response;

/**
 * @author Jasbir Singh
 */
public class OptionsTestClient implements Client {

	private static ObjectMapper mapper;

	static {
		mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule()).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	@Override
	public Response execute(Request request, Request.Options options) throws IOException {
		return Response.builder().status(200).request(request).headers(headers()).body(prepareResponse(options))
				.build();
	}

	private Map<String, Collection<String>> headers() {
		Map<String, Collection<String>> headers = new LinkedHashMap<>();
		headers.put("Content-Type", Collections.singletonList("application/json"));
		return headers;
	}

	private byte[] prepareResponse(Request.Options options) {
		try {

			OptionsResponseForTests response = new OptionsResponseForTests(options.connectTimeoutMillis(),
					TimeUnit.MILLISECONDS, options.readTimeoutMillis(), TimeUnit.MILLISECONDS);
			return mapper.writeValueAsString(response).getBytes();
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	static class OptionsResponseForTests {

		private long connectTimeout;

		private TimeUnit connectTimeoutUnit;

		private long readTimeout;

		private TimeUnit readTimeoutUnit;

		OptionsResponseForTests(long connectTimeout, TimeUnit connectTimeoutUnit, long readTimeout,
				TimeUnit readTimeoutUnit) {
			this.connectTimeout = connectTimeout;
			this.connectTimeoutUnit = connectTimeoutUnit;
			this.readTimeout = readTimeout;
			this.readTimeoutUnit = readTimeoutUnit;
		}

		public long getConnectTimeout() {
			return connectTimeout;
		}

		public TimeUnit getConnectTimeoutUnit() {
			return connectTimeoutUnit;
		}

		public long getReadTimeout() {
			return readTimeout;
		}

		public TimeUnit getReadTimeoutUnit() {
			return readTimeoutUnit;
		}

		@Override
		public String toString() {
			return "OptionsResponseForTests{" + "connectTimeout=" + connectTimeout + ", connectTimeoutUnit="
					+ connectTimeoutUnit + ", readTimeout=" + readTimeout + ", readTimeoutUnit=" + readTimeoutUnit
					+ '}';
		}

	}

}
