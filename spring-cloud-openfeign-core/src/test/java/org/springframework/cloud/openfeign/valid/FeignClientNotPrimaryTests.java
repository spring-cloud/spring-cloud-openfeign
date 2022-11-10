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

import java.util.List;

import feign.Logger;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.ServiceInstanceListSuppliers;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * @author Spencer Gibb
 * @author Jakub Narloch
 */
@SpringBootTest(classes = FeignClientNotPrimaryTests.Application.class, webEnvironment = RANDOM_PORT,
		value = { "spring.application.name=feignclientnotprimarytest",
				"spring.cloud.openfeign.httpclient.hc5.enabled=false", "spring.cloud.openfeign.okhttp.enabled=false" })
@DirtiesContext
class FeignClientNotPrimaryTests {

	public static final String HELLO_WORLD_1 = "hello world 1";

	@Autowired
	private TestClient testClient;

	@Autowired
	private List<TestClient> testClients;

	@Test
	void testClientType() {
		assertThat(this.testClient).as("testClient was of wrong type").isInstanceOf(PrimaryTestClient.class);
	}

	@Test
	void testClientCount() {
		assertThat(this.testClients).as("testClients was wrong").hasSize(2);
	}

	@Test
	void testSimpleType() {
		Hello hello = this.testClient.getHello();
		assertThat(hello).as("hello was null").isNull();
	}

	@FeignClient(name = "localapp", primary = false)
	protected interface TestClient {

		@GetMapping("/hello")
		Hello getHello();

	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	@RestController
	@EnableFeignClients(clients = { TestClient.class }, defaultConfiguration = TestDefaultFeignConfig.class)
	@LoadBalancerClient(name = "localapp", configuration = LocalClientConfiguration.class)
	protected static class Application {

		@Bean
		@Primary
		public PrimaryTestClient primaryTestClient() {
			return new PrimaryTestClient();
		}

		@GetMapping("/hello")
		public Hello getHello() {
			return new Hello(HELLO_WORLD_1);
		}

	}

	protected static class PrimaryTestClient implements TestClient {

		@Override
		public Hello getHello() {
			return null;
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
			return this.message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

	}

	@Configuration(proxyBeanMethods = false)
	public static class TestDefaultFeignConfig {

		@Bean
		Logger.Level feignLoggerLevel() {
			return Logger.Level.FULL;
		}

	}

	// Load balancer with fixed server list for "local" pointing to localhost
	@Configuration(proxyBeanMethods = false)
	public static class LocalClientConfiguration {

		@LocalServerPort
		private int port = 0;

		@Bean
		public ServiceInstanceListSupplier staticServiceInstanceListSupplier() {
			return ServiceInstanceListSuppliers.from("local",
					new DefaultServiceInstance("local-1", "local", "localhost", port, false));
		}

	}

}
