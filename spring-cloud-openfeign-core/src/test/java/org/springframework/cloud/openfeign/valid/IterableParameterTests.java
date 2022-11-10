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

import io.vavr.collection.HashSet;
import io.vavr.collection.Set;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.ServiceInstanceListSuppliers;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.test.NoSecurityConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Spencer Gibb
 */
@SpringBootTest(classes = IterableParameterTests.Application.class, webEnvironment = WebEnvironment.RANDOM_PORT,
		value = { "spring.application.name=iterableparametertest",
				"spring.cloud.openfeign.httpclient.hc5.enabled=false", "spring.cloud.openfeign.okhttp.enabled=false",
				"spring.cloud.openfeign.circuitbreaker.enabled=false" })
@DirtiesContext
class IterableParameterTests {

	@Autowired
	private TestClient testClient;

	@Test
	void testClient() {
		assertThat(this.testClient).as("testClient was null").isNotNull();
		String results = this.testClient.echo(HashSet.of("a", "b"));
		assertThat(results).isEqualTo("a,b");
	}

	@FeignClient(name = "localapp")
	protected interface TestClient {

		@GetMapping("/echo")
		String echo(@RequestParam Set<String> set);

	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	@RestController
	@EnableFeignClients(clients = TestClient.class)
	@LoadBalancerClient(name = "localapp", configuration = LocalLoadBalancerConfiguration.class)
	@Import(NoSecurityConfiguration.class)
	protected static class Application {

		@GetMapping("/echo")
		public String echo(@RequestParam String set) {
			return set;
		}

	}

	// Load balancer with fixed server list for "local" pointing to localhost
	public static class LocalLoadBalancerConfiguration {

		@LocalServerPort
		private int port = 0;

		@Bean
		public ServiceInstanceListSupplier staticServiceInstanceListSupplier() {
			return ServiceInstanceListSuppliers.from("local",
					new DefaultServiceInstance("local-1", "local", "localhost", port, false));
		}

	}

}
