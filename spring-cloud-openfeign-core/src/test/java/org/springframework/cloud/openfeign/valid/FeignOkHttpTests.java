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

package org.springframework.cloud.openfeign.valid;

import java.util.Objects;

import feign.Client;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.ServiceInstanceListSuppliers;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.loadbalancer.FeignBlockingLoadBalancerClient;
import org.springframework.cloud.openfeign.test.NoSecurityConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Spencer Gibb
 * @author Olga Maciaszek-Sharma
 */
@SpringBootTest(classes = FeignOkHttpTests.Application.class, webEnvironment = WebEnvironment.RANDOM_PORT,
		value = { "spring.application.name=feignclienttest", "spring.cloud.openfeign.circuitbreaker.enabled=false",
				"spring.cloud.openfeign.httpclient.hc5.enabled=false", "spring.cloud.openfeign.okhttp.enabled=true",
				"spring.cloud.httpclientfactories.ok.enabled=true", "spring.cloud.loadbalancer.retry.enabled=false" })
@DirtiesContext
class FeignOkHttpTests {

	@Autowired
	private TestClient testClient;

	@Autowired
	private Client feignClient;

	@Autowired
	private UserClient userClient;

	@Test
	void testSimpleType() {
		Hello hello = testClient.getHello();
		assertThat(hello).as("hello was null").isNotNull();
		assertThat(hello).as("first hello didn't match").isEqualTo(new Hello("hello world 1"));
	}

	@Test
	void testPatch() {
		ResponseEntity<Void> response = testClient.patchHello(new Hello("foo"));
		assertThat(response).isNotNull();
		String header = response.getHeaders().getFirst("x-hello");
		assertThat(header).isEqualTo("hello world patch");
	}

	@Test
	void testFeignClientType() {
		assertThat(feignClient).isInstanceOf(FeignBlockingLoadBalancerClient.class);
		FeignBlockingLoadBalancerClient client = (FeignBlockingLoadBalancerClient) feignClient;
		Client delegate = client.getDelegate();
		assertThat(delegate).isInstanceOf(feign.okhttp.OkHttpClient.class);
	}

	@Test
	void testFeignInheritanceSupport() {
		assertThat(userClient).as("UserClient was null").isNotNull();
		final User user = userClient.getUser(1);
		assertThat(user).as("Returned user was null").isNotNull();
		assertThat(new User("John Smith")).as("Users were different").isEqualTo(user);
	}

	@FeignClient("localapp")
	protected interface TestClient extends BaseTestClient {

	}

	protected interface BaseTestClient {

		@GetMapping("/hello")
		Hello getHello();

		@PatchMapping(value = "/hellop", consumes = "application/json")
		ResponseEntity<Void> patchHello(Hello hello);

	}

	protected interface UserService {

		@GetMapping("/users/{id}")
		User getUser(@PathVariable("id") long id);

	}

	@FeignClient("localapp1")
	protected interface UserClient extends UserService {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	@RestController
	@EnableFeignClients(clients = { TestClient.class, UserClient.class })
	@LoadBalancerClients({
			@LoadBalancerClient(name = "localapp", configuration = FeignHttpClientTests.LocalClientConfiguration.class),
			@LoadBalancerClient(name = "localapp1",
					configuration = FeignHttpClientTests.LocalClientConfiguration.class) })
	@Import(NoSecurityConfiguration.class)
	protected static class Application implements UserService {

		@GetMapping("/hello")
		public Hello getHello() {
			return new Hello("hello world 1");
		}

		@PatchMapping("/hellop")
		public ResponseEntity<Void> patchHello(@RequestBody Hello hello,
				@RequestHeader("Content-Length") int contentLength) {
			if (contentLength <= 0) {
				throw new IllegalArgumentException("Invalid Content-Length " + contentLength);
			}
			if (!hello.getMessage().equals("foo")) {
				throw new IllegalArgumentException("Invalid Hello: " + hello.getMessage());
			}
			return ResponseEntity.ok().header("X-Hello", "hello world patch").build();
		}

		@Override
		public User getUser(@PathVariable("id") long id) {
			return new User("John Smith");
		}

	}

	public static class Hello {

		private String message;

		Hello() {
		}

		Hello(String message) {
			this.message = message;
		}

		public String getMessage() {
			return message;
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
			return Objects.equals(message, that.message);
		}

		@Override
		public int hashCode() {
			return Objects.hash(message);
		}

	}

	public static class User {

		private String name;

		User() {
		}

		User(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			User that = (User) o;
			return Objects.equals(name, that.name);
		}

		@Override
		public int hashCode() {
			return Objects.hash(name);
		}

	}

	// Load balancer with fixed server list for "local" pointing to localhost
	@Configuration(proxyBeanMethods = false)
	static class LocalClientConfiguration {

		@LocalServerPort
		private int port = 0;

		@Bean
		public ServiceInstanceListSupplier staticServiceInstanceListSupplier() {
			return ServiceInstanceListSuppliers.from("local",
					new DefaultServiceInstance("local-1", "local", "localhost", port, false));
		}

	}

}
