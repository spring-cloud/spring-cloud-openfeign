/*
 * Copyright 2013-2020 the original author or authors.
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

import java.nio.charset.Charset;

import feign.Response;
import feign.codec.Decoder;
import feign.codec.Encoder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.test.NoSecurityConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.util.SocketUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Abstract class for the integration tests for {@link SpringMvcContract}.
 *
 * @author Ram Anaswara
 */
public class AbstractSpringMvcContractIntegrationTests {

	@BeforeAll
	public static void beforeClass() {
		System.setProperty("server.port",
				String.valueOf(SocketUtils.findAvailableTcpPort()));
	}

	@AfterAll
	public static void afterClass() {
		System.clearProperty("server.port");
	}

	protected String getUrlQueryParam(Response response) {
		return response.request().requestTemplate().queries().get("url").stream()
				.findFirst().orElseThrow(IllegalStateException::new);
	}

	@FeignClient(name = "test", url = "http://localhost:${server.port}/",
			configuration = NoCodecsFeignConfiguration.class)
	interface TestClient {

		@PostMapping("/test")
		Object sendMessage(@RequestBody String message,
				@RequestHeader(HttpHeaders.CONTENT_TYPE) String acceptHeader);

		@GetMapping("/get")
		Object getMessage(@RequestParam String url);

	}

	@Configuration(proxyBeanMethods = false)
	@EnableFeignClients(clients = TestClient.class)
	@EnableAutoConfiguration
	@RestController
	@Import(NoSecurityConfiguration.class)
	protected static class Config {

		@PostMapping("/test")
		Object sendMessage(@RequestBody String message,
				@RequestHeader(HttpHeaders.CONTENT_TYPE) String acceptHeader) {
			return message;
		}

		@GetMapping("/get")
		Object getMessage(@RequestParam String url) {
			return url;
		}

	}

	// Avoid feign.codec.EncodeException - this feature works for users that override
	// Encoder
	protected static class NoCodecsFeignConfiguration {

		@Bean
		public Decoder decoder() {
			return (response, type) -> response;
		}

		@Bean
		public Encoder encoder() {
			return (object, bodyType, request) -> request
					.body(object.toString().getBytes(), Charset.defaultCharset());
		}

	}

}
