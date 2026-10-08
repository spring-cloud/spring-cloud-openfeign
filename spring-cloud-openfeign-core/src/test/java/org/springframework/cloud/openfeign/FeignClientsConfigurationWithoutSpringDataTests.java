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

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import feign.Contract;
import feign.Feign;
import feign.QueryMapEncoder;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link FeignClientsConfiguration} when Spring Data is not on the classpath.
 *
 */
class FeignClientsConfigurationWithoutSpringDataTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withClassLoader(new FilteredClassLoader(Pageable.class))
		.withConfiguration(AutoConfigurations.of(FeignClientsConfiguration.class));

	@Test
	void shouldSendHttpHeadersHeaderMapValues() {
		contextRunner.run(context -> {
			AtomicReference<Request> sent = new AtomicReference<>();
			Feign.Builder builder = Feign.builder()
				.contract(context.getBean(Contract.class))
				.client((request, options) -> {
					sent.set(request);
					return Response.builder().status(200).request(request).headers(Collections.emptyMap()).build();
				});
			context.getBeanProvider(QueryMapEncoder.class).ifAvailable(builder::queryMapEncoder);
			HeadersClient client = builder.target(HeadersClient.class, "http://localhost");

			HttpHeaders headers = new HttpHeaders();
			headers.add("X-Custom", "value1");
			headers.add("X-Custom", "value2");
			headers.add(HttpHeaders.AUTHORIZATION, "Bearer token");
			client.headers(headers);

			assertThat(sent.get().headers()).containsOnlyKeys("X-Custom", HttpHeaders.AUTHORIZATION);
			assertThat(sent.get().headers().get("X-Custom")).containsExactly("value1", "value2");
			assertThat(sent.get().headers().get(HttpHeaders.AUTHORIZATION)).containsExactly("Bearer token");
		});
	}

	@Test
	void shouldEncodeOtherQueryMapObjectsByFields() {
		contextRunner.run(context -> {
			Map<String, Object> queryMap = context.getBean(QueryMapEncoder.class).encode(new Filter("feign"));

			assertThat(queryMap).containsOnly(entry("name", "feign"));
		});
	}

	record Filter(String name) {
	}

	interface HeadersClient {

		@GetMapping("/headers")
		void headers(@RequestHeader HttpHeaders headers);

	}

}
