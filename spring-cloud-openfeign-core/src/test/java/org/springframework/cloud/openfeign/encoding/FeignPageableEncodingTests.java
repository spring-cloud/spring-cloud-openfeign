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

package org.springframework.cloud.openfeign.encoding;

import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.cloud.openfeign.encoding.app.client.InvoiceClient;
import org.springframework.cloud.openfeign.encoding.app.domain.Invoice;
import org.springframework.cloud.openfeign.test.NoSecurityConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Tests the pagination encoding.
 *
 * @author Charlie Mordant.
 */
@SpringBootTest(classes = FeignPageableEncodingTests.Application.class,
		webEnvironment = RANDOM_PORT,
		value = { "feign.compression.request.enabled=true",
				"hystrix.command.default.execution.isolation.strategy=SEMAPHORE",
				"ribbon.OkToRetryOnAllOperations=false" })
@RunWith(SpringJUnit4ClassRunner.class)
public class FeignPageableEncodingTests {

	@Autowired
	private InvoiceClient invoiceClient;

	@Test
	public void testPageable() {

		// given
		Pageable pageable = PageRequest.of(0, 10, Sort.Direction.ASC, "sortProperty");

		// when
		final ResponseEntity<Page<Invoice>> response = this.invoiceClient
				.getInvoicesPaged(pageable);

		// then
		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(pageable.getPageSize()).isEqualTo(response.getBody().getSize());
		assertThat(response.getBody().getPageable().getSort()).hasSize(1);
		Optional<Sort.Order> optionalOrder = response.getBody().getPageable().getSort()
				.get().findFirst();
		if (optionalOrder.isPresent()) {
			Sort.Order order = optionalOrder.get();
			assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
			assertThat(order.getProperty()).isEqualTo("sortProperty");
		}

	}

	@EnableFeignClients(clients = InvoiceClient.class)
	@LoadBalancerClient(name = "local", configuration = LocalClientConfiguration.class)
	@SpringBootApplication(
			scanBasePackages = "org.springframework.cloud.openfeign.encoding.app",
			exclude = { RepositoryRestMvcAutoConfiguration.class })
	@EnableSpringDataWebSupport
	@Import({ NoSecurityConfiguration.class, FeignClientsConfiguration.class })
	public static class Application {

	}

	@Configuration(proxyBeanMethods = false)
	static class LocalClientConfiguration {

		@LocalServerPort
		private int port = 0;

		@Bean
		public ServiceInstanceListSupplier staticServiceInstanceListSupplier(
				Environment env) {
			return ServiceInstanceListSupplier.fixed(env).instance(port, "local").build();
		}

	}

}
