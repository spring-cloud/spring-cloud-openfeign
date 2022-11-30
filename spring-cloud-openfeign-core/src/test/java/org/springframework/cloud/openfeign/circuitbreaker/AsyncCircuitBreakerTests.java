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

package org.springframework.cloud.openfeign.circuitbreaker;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.ConfigBuilder;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.test.NoSecurityConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for asynchronous circuit breaker.
 *
 * @author John Niang
 */
@SpringBootTest(classes = AsyncCircuitBreakerTests.Application.class, webEnvironment = RANDOM_PORT,
		properties = { "spring.cloud.openfeign.circuitbreaker.enabled=true",
				"spring.cloud.openfeign.lazy-attributes-resolution=true" })
@AutoConfigureMockMvc
class AsyncCircuitBreakerTests {

	@Autowired
	MockMvc mvc;

	@Autowired
	@Qualifier("asyncWorker")
	ExecutorService asyncCircuitBreakerExecutor;

	@Test
	void shouldWorkNormally() throws Exception {
		mvc.perform(get("/hello/proxy")).andDo(print()).andExpect(status().isOk())
				.andExpect(content().string("openfeign"));
	}

	@Test
	void shouldNotProxyAnyHeadersWithoutHeaderSet() throws Exception {
		mvc.perform(get("/headers/" + HttpHeaders.AUTHORIZATION + "/proxy")).andDo(print()).andExpect(status().isOk())
				.andExpect(content().string(""));
	}

	@Test
	void shouldProxyHeaderWhenHeaderSet() throws Exception {
		String authorization = UUID.randomUUID().toString();
		mvc.perform(get("/headers/" + HttpHeaders.AUTHORIZATION + "/proxy").header(HttpHeaders.AUTHORIZATION,
				authorization)).andDo(print()).andExpect(status().isOk()).andExpect(content().string(authorization));
	}

	@Test
	void shouldProxyHeaderWhenHeaderSetAndCleanRequestAttributesAfterReturn() throws Exception {
		shouldNotProxyAnyHeadersWithoutHeaderSet();
		Future<ServletRequestAttributes> future = asyncCircuitBreakerExecutor
				.submit(() -> (ServletRequestAttributes) RequestContextHolder.getRequestAttributes());
		assertThat(future.get()).as("the RequestAttributes has been cleared").isNull();
	}

	@EnableAutoConfiguration
	@Configuration(proxyBeanMethods = false)
	@EnableFeignClients(clients = { TestClient.class })
	@Import({ NoSecurityConfiguration.class, TestController.class })
	static class Application {

		@Bean(name = "asyncWorker", destroyMethod = "shutdown")
		ExecutorService asyncCircuitBreakerExecutor() {
			return Executors.newSingleThreadExecutor(new CustomizableThreadFactory("async"));
		}

		@Bean
		CircuitBreakerFactory<Duration, ConfigBuilder<Duration>> circuitBreakerFactory(
				@Qualifier("asyncWorker") ExecutorService asyncCircuitBreakerExecutor) {
			return new CircuitBreakerFactory<>() {

				Function<String, Duration> defaultConfiguration = id -> Duration.ofMillis(1000);

				@Override
				public CircuitBreaker create(String id) {
					Duration timeout = super.getConfigurations().computeIfAbsent(id, defaultConfiguration);
					return new AsyncCircuitBreaker(timeout, asyncCircuitBreakerExecutor);
				}

				@Override
				protected ConfigBuilder<Duration> configBuilder(String id) {
					return () -> Duration.ofMillis(100);
				}

				@Override
				public void configureDefault(Function<String, Duration> defaultConfiguration) {
					this.defaultConfiguration = defaultConfiguration;
				}
			};
		}

		@Bean
		RequestInterceptor proxyHeaderRequestInterceptor() {
			return template -> {
				ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder
						.getRequestAttributes();
				String authorization = Objects.requireNonNull(requestAttributes).getRequest()
						.getHeader(HttpHeaders.AUTHORIZATION);
				if (authorization != null) {
					// proxy authorization header
					template.header(HttpHeaders.AUTHORIZATION, authorization);
				}
			};
		}

	}

	@RestController
	static class TestController {

		final ObjectProvider<TestClient> testClient;

		TestController(ObjectProvider<TestClient> testClient) {
			this.testClient = testClient;
		}

		@GetMapping("/hello")
		String hello() {
			return "openfeign";
		}

		@GetMapping("/hello/proxy")
		String helloProxy() {
			return testClient.getObject().hello();
		}

		@GetMapping("/headers/{headerName}")
		String header(HttpServletRequest request, @PathVariable String headerName) {
			return request.getHeader(headerName);
		}

		@GetMapping("/headers/{headerName}/proxy")
		String headerProxy(@PathVariable String headerName) {
			return testClient.getObject().header(headerName);
		}

	}

	@FeignClient(name = "async-circuit-breaker-test", url = "http://localhost:${local.server.port}")
	interface TestClient {

		@GetMapping("/hello")
		String hello();

		@GetMapping("/headers/{headerName}")
		String header(@PathVariable String headerName);

	}

}
