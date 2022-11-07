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

import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.ConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.test.NoSecurityConfiguration;
import org.springframework.cloud.test.TestSocketUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for Feign calls with CircuitBreaker, without fallbacks.
 *
 * @author Olga Maciaszek-Sharma
 */
@SpringBootTest(classes = CircuitBreakerWithNoFallbackTests.Application.class,
		webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
		value = { "spring.application.name=springcircuittest", "spring.jmx.enabled=false",
				"spring.cloud.openfeign.circuitbreaker.enabled=true" })
@DirtiesContext
public class CircuitBreakerWithNoFallbackTests {

	@Autowired
	MyCircuitBreaker myCircuitBreaker;

	@Autowired
	CircuitBreakerTestClient testClient;

	@BeforeAll
	public static void beforeClass() {
		System.setProperty("server.port", String.valueOf(TestSocketUtils.findAvailableTcpPort()));
	}

	@AfterAll
	public static void afterClass() {
		System.clearProperty("server.port");
	}

	@BeforeEach
	public void setup() {
		this.myCircuitBreaker.clear();
	}

	@Test
	public void testSimpleTypeWithFallback() {
		Hello hello = testClient.getHello();

		assertThat(hello).as("hello was null").isNotNull();
		assertThat(hello).as("first hello didn't match").isEqualTo(new Hello("hello world 1"));
		assertThat(myCircuitBreaker.runWasCalled).as("Circuit Breaker was called").isTrue();
	}

	@Test
	public void test404WithoutFallback() {
		assertThatThrownBy(() -> testClient.getException()).isInstanceOf(NoFallbackAvailableException.class);
	}

	@FeignClient(name = "test", url = "http://localhost:${server.port}/")
	protected interface CircuitBreakerTestClient {

		@GetMapping("/hello")
		Hello getHello();

		@GetMapping("/hellonotfound")
		String getException();

	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	@RestController
	@EnableFeignClients(clients = { CircuitBreakerTestClient.class })
	@Import(NoSecurityConfiguration.class)
	protected static class Application implements CircuitBreakerTestClient {

		static final Log log = LogFactory.getLog(Application.class);

		@Bean
		MyCircuitBreaker myCircuitBreaker() {
			return new MyCircuitBreaker();
		}

		@SuppressWarnings("rawtypes")
		@Bean
		CircuitBreakerFactory circuitBreakerFactory(MyCircuitBreaker myCircuitBreaker) {
			return new CircuitBreakerFactory() {
				@Override
				public CircuitBreaker create(String id) {
					log.info("Creating a circuit breaker with id [" + id + "]");
					return myCircuitBreaker;
				}

				@Override
				protected ConfigBuilder configBuilder(String id) {
					return Object::new;
				}

				@Override
				public void configureDefault(Function defaultConfiguration) {

				}
			};
		}

		@Override
		public Hello getHello() {
			return new Hello("hello world 1");
		}

		@Override
		public String getException() {
			throw new IllegalStateException("BOOM!");
		}

	}

}
