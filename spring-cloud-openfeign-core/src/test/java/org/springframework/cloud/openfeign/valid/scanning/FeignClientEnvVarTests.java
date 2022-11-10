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

package org.springframework.cloud.openfeign.valid.scanning;

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
import org.springframework.cloud.openfeign.test.NoSecurityConfiguration;
import org.springframework.cloud.openfeign.testclients.TestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * @author Ryan Baxter
 */
@SpringBootTest(classes = FeignClientEnvVarTests.Application.class, webEnvironment = RANDOM_PORT,
		value = { "spring.application.name=feignclienttest", "spring.cloud.openfeign.httpclient.hc5.enabled=false",
				"basepackage=org.springframework.cloud.openfeign.testclients" })
@DirtiesContext
class FeignClientEnvVarTests {

	@Autowired
	private TestClient testClient;

	@Test
	void testSimpleType() {
		String hello = this.testClient.getHello();
		assertThat(hello).as("hello was null").isNotNull();
		assertThat(hello).as("first hello didn't match").isEqualTo("hello world 1");
	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	@RestController
	@EnableFeignClients(basePackages = { "${basepackage}" })
	@LoadBalancerClient(name = "localapp", configuration = LocalClientConfiguration.class)
	@Import(NoSecurityConfiguration.class)
	protected static class Application {

		@GetMapping("/hello")
		public String getHello() {
			return "hello world 1";
		}

	}

	// Load balancer with fixed server list for "local" pointing to localhost
	@Configuration(proxyBeanMethods = false)
	public static class LocalClientConfiguration {

		@LocalServerPort
		private int port = 0;

		@Bean
		public ServiceInstanceListSupplier staticServiceInstanceListSupplier() {
			return ServiceInstanceListSuppliers.from("localapp",
					new DefaultServiceInstance("localapp-1", "localapp", "localhost", port, false));
		}

	}

}
