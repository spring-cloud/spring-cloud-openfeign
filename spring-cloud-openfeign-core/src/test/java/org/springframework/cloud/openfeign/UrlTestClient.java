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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

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
public class UrlTestClient implements Client {

	protected static ObjectMapper mapper;

	static {
		mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule()).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	@Override
	public Response execute(Request request, Request.Options options) {
		return Response.builder().status(200).request(request).headers(headers()).body(prepareResponse(request))
				.build();
	}

	private Map<String, Collection<String>> headers() {
		Map<String, Collection<String>> headers = new LinkedHashMap<>();
		headers.put("Content-Type", Collections.singletonList("application/json"));
		return headers;
	}

	private byte[] prepareResponse(Request request) {
		try {
			UrlResponseForTests response = new UrlResponseForTests(request.url(),
					request.requestTemplate().feignTarget().getClass());
			return mapper.writeValueAsString(response).getBytes();
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	static class UrlResponseForTests {

		private String url;

		private Class<?> targetType;

		UrlResponseForTests() {
		}

		UrlResponseForTests(String url, Class<?> targetType) {
			this.url = url;
			this.targetType = targetType;
		}

		void setUrl(String url) {
			this.url = url;
		}

		public String getUrl() {
			return url;
		}

		public Class<?> getTargetType() {
			return targetType;
		}

		public void setTargetType(Class<?> targetType) {
			this.targetType = targetType;
		}

		@Override
		public String toString() {
			return "UrlResponseForTests{" + "url='" + url + '\'' + ", targetType=" + targetType + '}';
		}

	}

}
