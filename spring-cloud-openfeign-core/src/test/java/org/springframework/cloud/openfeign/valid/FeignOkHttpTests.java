/*
 * Copyright 2013-2019 the original author or authors.
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

import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import feign.Client;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.netflix.ribbon.RibbonClients;
import org.springframework.cloud.netflix.ribbon.StaticServerList;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.ribbon.LoadBalancerFeignClient;
import org.springframework.cloud.openfeign.test.NoSecurityConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = FeignOkHttpTests.Application.class,
		webEnvironment = WebEnvironment.RANDOM_PORT,
		value = { "spring.application.name=feignclienttest",
				"feign.hystrix.enabled=false", "feign.httpclient.enabled=false",
				"feign.okhttp.enabled=true",
				"spring.cloud.httpclientfactories.ok.enabled=true" })
@DirtiesContext
public class FeignOkHttpTests {

	@Value("${local.server.port}")
	private int port = 0;

	@Autowired
	private TestClient testClient;

	@Autowired
	private Client feignClient;

	@Autowired
	private UserClient userClient;

	@Test
	public void testSimpleType() {
		Hello hello = this.testClient.getHello();
		assertThat(hello).as("hello was null").isNotNull();
		assertThat(hello).as("first hello didn't match")
				.isEqualTo(new Hello("hello world 1"));
	}

	@Test
	public void testPatch() {
		ResponseEntity<Void> response = this.testClient.patchHello(new Hello("foo"));
		assertThat(response).isNotNull();
		String header = response.getHeaders().getFirst("X-Hello");
		assertThat(header).isEqualTo("hello world patch");
	}

	@Test
	public void testFeignClientType() throws IllegalAccessException {
		assertThat(this.feignClient).isInstanceOf(LoadBalancerFeignClient.class);
		LoadBalancerFeignClient client = (LoadBalancerFeignClient) this.feignClient;
		Client delegate = client.getDelegate();
		assertThat(delegate).isInstanceOf(feign.okhttp.OkHttpClient.class);
	}

	@Test
	public void testFeignInheritanceSupport() {
		assertThat(this.userClient).as("UserClient was null").isNotNull();
		final User user = this.userClient.getUser(1);
		assertThat(user).as("Returned user was null").isNotNull();
		assertThat(new User("John Smith")).as("Users were different").isEqualTo(user);
	}

	@FeignClient("localapp")
	protected interface TestClient extends BaseTestClient {

	}

	protected interface BaseTestClient {

		@RequestMapping(method = RequestMethod.GET, value = "/hello")
		Hello getHello();

		@RequestMapping(method = RequestMethod.PATCH, value = "/hellop",
				consumes = "application/json")
		ResponseEntity<Void> patchHello(Hello hello);

	}

	protected interface UserService {

		@RequestMapping(method = RequestMethod.GET, value = "/users/{id}")
		User getUser(@PathVariable("id") long id);

	}

	@FeignClient("localapp1")
	protected interface UserClient extends UserService {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	@RestController
	@EnableFeignClients(clients = { TestClient.class, UserClient.class })
	@RibbonClients({
			@RibbonClient(name = "localapp",
					configuration = LocalRibbonClientConfiguration.class),
			@RibbonClient(name = "localapp1",
					configuration = LocalRibbonClientConfiguration.class) })
	@Import(NoSecurityConfiguration.class)
	protected static class Application implements UserService {

		@RequestMapping(method = RequestMethod.GET, value = "/hello")
		public Hello getHello() {
			return new Hello("hello world 1");
		}

		@RequestMapping(method = RequestMethod.PATCH, value = "/hellop")
		public ResponseEntity<Void> patchHello(@RequestBody Hello hello,
				@RequestHeader("Content-Length") int contentLength) {
			if (contentLength <= 0) {
				throw new IllegalArgumentException(
						"Invalid Content-Length " + contentLength);
			}
			if (!hello.getMessage().equals("foo")) {
				throw new IllegalArgumentException(
						"Invalid Hello: " + hello.getMessage());
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

	public static class User {

		private String name;

		public User() {
		}

		public User(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
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
			return Objects.equals(this.name, that.name);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.name);
		}

	}

	// Load balancer with fixed server list for "local" pointing to localhost
	@Configuration(proxyBeanMethods = false)
	static class LocalRibbonClientConfiguration {

		@Value("${local.server.port}")
		private int port = 0;

		@Bean
		public ServerList<Server> ribbonServerList() {
			return new StaticServerList<>(new Server("localhost", this.port));
		}

	}

}
