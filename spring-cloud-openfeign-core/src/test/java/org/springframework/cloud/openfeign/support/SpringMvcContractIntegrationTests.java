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

import feign.codec.Decoder;
import feign.codec.Encoder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.test.NoSecurityConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.util.SocketUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Integration tests for {@link SpringMvcContract}
 *
 * @author Olga Maciaszek-Sharma
 */
@SpringBootTest(classes = SpringMvcContractIntegrationTests.Config.class,
		webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class SpringMvcContractIntegrationTests {

	@Autowired
	private TestClient client;

	@BeforeAll
	public static void beforeClass() {
		System.setProperty("server.port",
				String.valueOf(SocketUtils.findAvailableTcpPort()));
	}

	@AfterAll
	public static void afterClass() {
		System.clearProperty("server.port");
	}

	@Test
	public void shouldNotThrowInvalidMediaTypeExceptionWhenContentTypeTemplateUsed() {
		assertThatCode(() -> client.sendMessage("test", "text/markdown"))
				.doesNotThrowAnyException();
	}

	@FeignClient(name = "test", url = "http://localhost:${server.port}/",
			configuration = NoCodecsFeignConfiguration.class)
	interface TestClient {

		@PostMapping("/test")
		Object sendMessage(@RequestBody String message,
				@RequestHeader(HttpHeaders.CONTENT_TYPE) String acceptHeader);

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

	}

	// avoid feign.codec.EncodeException - this feature works for users that override
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
