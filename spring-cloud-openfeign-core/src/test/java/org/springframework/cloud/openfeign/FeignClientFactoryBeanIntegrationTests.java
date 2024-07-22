/*
 * Copyright 2013-2024 the original author or authors.
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

package org.springframework.cloud.openfeign;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import feign.FeignException;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.ServiceInstanceListSuppliers;
import org.springframework.cloud.openfeign.test.NoSecurityConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Integration tests for {@link FeignClientFactoryBean}.
 *
 * @author Olga Maciaszek-Sharma
 */
@SpringBootTest(classes = FeignClientFactoryBeanIntegrationTests.Application.class,
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("defaultstest")
class FeignClientFactoryBeanIntegrationTests {

	@Autowired
	TestClientA testClientA;

	@Autowired
	TestClientB testClientB;

	@Test
	void shouldProcessDefaultRequestHeadersPerClient() {
		assertThat(testClientA.headers()).isNotNull()
			.contains(entry("x-custom-header-2", List.of("2 from default")),
					entry("x-custom-header", List.of("from client A")));
		assertThat(testClientB.headers()).isNotNull()
			.contains(entry("x-custom-header-2", List.of("2 from default")),
					entry("x-custom-header", List.of("from client B")));
	}

	@Test
	void shouldProcessDefaultQueryParamsPerClient() {
		assertThat(testClientA.params()).isNotNull()
			.contains(entry("customParam2", "2 from default"), entry("customParam1", "from client A"));
		assertThat(testClientB.params()).isNotNull()
			.contains(entry("customParam2", "2 from default"), entry("customParam1", "from client B"));
	}

	@Test
	void shouldProcessDismiss404PerClient() {
		assertThatExceptionOfType(FeignException.FeignClientException.class).isThrownBy(() -> testClientA.test404());
		assertThatCode(() -> {
			ResponseEntity<String> response404 = testClientB.test404();
			assertThat(response404.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
			assertThat(response404.getBody()).isNull();
		}).doesNotThrowAnyException();
	}

	@FeignClient("testClientA")
	public interface TestClientA extends TestClient {

	}

	@FeignClient("testClientB")
	public interface TestClientB extends TestClient {

	}

	public interface TestClient {

		@GetMapping("/headers")
		Map<String, List<String>> headers();

		@GetMapping("/params")
		Map<String, String> params();

		@GetMapping
		ResponseEntity<String> test404();

	}

	@EnableAutoConfiguration
	@EnableFeignClients(clients = { TestClientA.class, TestClientB.class })
	@RestController
	@LoadBalancerClients({ @LoadBalancerClient(name = "testClientA", configuration = TestClientAConfiguration.class),
			@LoadBalancerClient(name = "testClientB", configuration = TestClientBConfiguration.class) })
	@RequestMapping
	@Import(NoSecurityConfiguration.class)
	protected static class Application {

		@GetMapping(value = "/headers", produces = APPLICATION_JSON_VALUE)
		public Map<String, List<String>> headers(@RequestHeader HttpHeaders headers) {
			return new HashMap<>(headers);
		}

		@GetMapping(value = "/params", produces = APPLICATION_JSON_VALUE)
		public Map<String, String> headersB(@RequestParam Map<String, String> params) {
			return params;
		}

		@GetMapping
		ResponseEntity<String> test404() {
			return ResponseEntity.notFound().build();
		}

	}

	// LoadBalancer with fixed server list for "testClientA" pointing to localhost
	static class TestClientAConfiguration {

		@LocalServerPort
		private int port = 0;

		@Bean
		public ServiceInstanceListSupplier testClientAServiceInstanceListSupplier() {
			return ServiceInstanceListSuppliers.from("testClientA",
					new DefaultServiceInstance("local-1", "testClientA", "localhost", port, false));
		}

	}

	// LoadBalancer with fixed server list for "testClientB" pointing to localhost
	static class TestClientBConfiguration {

		@LocalServerPort
		private int port = 0;

		@Bean
		public ServiceInstanceListSupplier testClientBServiceInstanceListSupplier() {
			return ServiceInstanceListSuppliers.from("testClientB",
					new DefaultServiceInstance("local-1", "testClientB", "localhost", port, false));
		}

	}

}
