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

package org.springframework.cloud.openfeign.circuitbreaker;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.ConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.test.NoSecurityConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.SocketUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = CirciutBreakerTests.Application.class, webEnvironment = WebEnvironment.DEFINED_PORT, value = {
		"spring.application.name=springcircuittest", "spring.jmx.enabled=false", "feign.circuitbreaker.enabled=true" })
@DirtiesContext
public class CirciutBreakerTests {

	@Autowired
	MyCircuitBreaker myCircuitBreaker;

	@Autowired
	TestClient testClient;

	@Autowired
	TestClientWithFactory testClientWithFactory;

	@LocalServerPort
	private int port = 0;

	@BeforeAll
	public static void beforeClass() {
		System.setProperty("server.port", String.valueOf(SocketUtils.findAvailableTcpPort()));
	}

	@AfterAll
	public static void afterClass() {
		System.clearProperty("server.port");
	}

	@Before
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
	public void test404WithFallback() {
		assertThat(testClient.getException()).isEqualTo("Fixed response");
	}

	@Test
	public void testSimpleTypeWithFallbackFactory() {
		Hello hello = testClientWithFactory.getHello();

		assertThat(hello).as("hello was null").isNotNull();
		assertThat(hello).as("first hello didn't match").isEqualTo(new Hello("hello world 1"));
		assertThat(myCircuitBreaker.runWasCalled).as("Circuit Breaker was called").isTrue();
	}

	@Test
	public void test404WithFallbackFactory() {
		assertThat(testClientWithFactory.getException()).isEqualTo("Fixed response");
	}

	// tag::client_with_fallback[]
	@FeignClient(name = "test", url = "http://localhost:${server.port}/", fallback = Fallback.class)
	protected interface TestClient {

		@RequestMapping(method = RequestMethod.GET, value = "/hello")
		Hello getHello();

		@RequestMapping(method = RequestMethod.GET, value = "/hellonotfound")
		String getException();

	}

	@Component
	static class Fallback implements TestClient {

		@Override
		public Hello getHello() {
			throw new NoFallbackAvailableException("Boom!", new RuntimeException());
		}

		@Override
		public String getException() {
			return "Fixed response";
		}

	}
	// end::client_with_fallback[]

	// tag::client_with_fallback_factory[]
	@FeignClient(name = "testClientWithFactory", url = "http://localhost:${server.port}/",
			fallbackFactory = TestFallbackFactory.class)
	protected interface TestClientWithFactory {

		@RequestMapping(method = RequestMethod.GET, value = "/hello")
		Hello getHello();

		@RequestMapping(method = RequestMethod.GET, value = "/hellonotfound")
		String getException();

	}

	@Component
	static class TestFallbackFactory implements FallbackFactory<FallbackWithFactory> {

		@Override
		public FallbackWithFactory create(Throwable cause) {
			return new FallbackWithFactory();
		}

	}

	static class FallbackWithFactory implements TestClientWithFactory {

		@Override
		public Hello getHello() {
			throw new NoFallbackAvailableException("Boom!", new RuntimeException());
		}

		@Override
		public String getException() {
			return "Fixed response";
		}

	}
	// end::client_with_fallback_factory[]

	public static class Hello {

		private String message;

		public Hello() {
		}

		public Hello(String message) {
			this.message = message;
		}

		public String getMessage() {
			return this.message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			Hello that = (Hello) o;
			return Objects.equals(this.message, that.message);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.message);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	@RestController
	@EnableFeignClients(clients = { TestClient.class, TestClientWithFactory.class })
	@Import(NoSecurityConfiguration.class)
	protected static class Application implements TestClient {

		static final Log log = LogFactory.getLog(Application.class);

		@Bean
		MyCircuitBreaker myCircuitBreaker() {
			return new MyCircuitBreaker();
		}

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

		@Bean
		Fallback fallback() {
			return new Fallback();
		}

		@Bean
		TestFallbackFactory testFallbackFactory() {
			return new TestFallbackFactory();
		}

	}

	static class MyCircuitBreaker implements CircuitBreaker {

		AtomicBoolean runWasCalled = new AtomicBoolean();

		@Override
		public <T> T run(Supplier<T> toRun) {
			this.runWasCalled.set(true);
			return toRun.get();
		}

		@Override
		public <T> T run(Supplier<T> toRun, Function<Throwable, T> fallback) {
			try {
				return run(toRun);
			}
			catch (Throwable throwable) {
				return fallback.apply(throwable);
			}
		}

		public void clear() {
			this.runWasCalled.set(false);
		}

	}

}
