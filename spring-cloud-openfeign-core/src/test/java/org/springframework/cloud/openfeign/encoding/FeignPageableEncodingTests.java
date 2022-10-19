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

package org.springframework.cloud.openfeign.encoding;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.ServiceInstanceListSuppliers;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.encoding.app.client.InvoiceClient;
import org.springframework.cloud.openfeign.encoding.app.domain.Invoice;
import org.springframework.cloud.openfeign.test.NoSecurityConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Tests the pagination encoding.
 *
 * @author Charlie Mordant.
 * @author Hyeonmin Park
 */
@SpringBootTest(classes = FeignPageableEncodingTests.Application.class, webEnvironment = RANDOM_PORT,
		value = { "spring.cloud.openfeign.compression.request.enabled=true" })
class FeignPageableEncodingTests {

	@Autowired
	private InvoiceClient invoiceClient;

	@Test
	void testPageable() {
		// given
		Pageable pageable = PageRequest.of(0, 10, Sort.Direction.ASC, "sortProperty");

		// when
		final ResponseEntity<Page<Invoice>> response = this.invoiceClient.getInvoicesPaged(pageable);

		// then
		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(pageable.getPageSize()).isEqualTo(response.getBody().getSize());
		assertThat(response.getBody().getPageable().getSort()).hasSize(1);
		Optional<Sort.Order> optionalOrder = response.getBody().getPageable().getSort().get().findFirst();
		if (optionalOrder.isPresent()) {
			Sort.Order order = optionalOrder.get();
			assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
			assertThat(order.getProperty()).isEqualTo("sortProperty");
		}
	}

	@Test
	void testPageableWithDescDirection() {
		// given
		Pageable pageable = PageRequest.of(0, 10, Sort.Direction.DESC, "sortProperty");

		// when
		final ResponseEntity<Page<Invoice>> response = this.invoiceClient.getInvoicesPaged(pageable);

		// then
		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(pageable.getPageSize()).isEqualTo(response.getBody().getSize());

		Sort sort = response.getBody().getPageable().getSort();
		assertThat(sort).hasSize(1);
		assertThat(sort.get()).hasSize(1);

		Optional<Sort.Order> optionalOrder = sort.get().findFirst();
		assertThat(optionalOrder.isPresent()).isTrue();

		Sort.Order order = optionalOrder.get();
		assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
		assertThat(order.getProperty()).isEqualTo("sortProperty");
	}

	@Test
	void testPageableWithMultipleSort() {
		// given
		Pageable pageable = PageRequest.of(0, 10,
				Sort.by(Sort.Order.desc("sortProperty1"), Sort.Order.asc("sortProperty2")));

		// when
		final ResponseEntity<Page<Invoice>> response = this.invoiceClient.getInvoicesPaged(pageable);

		// then
		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(pageable.getPageSize()).isEqualTo(response.getBody().getSize());

		Sort sort = response.getBody().getPageable().getSort();
		assertThat(sort).hasSize(2);

		List<Sort.Order> orderList = sort.toList();
		assertThat(orderList).hasSize(2);

		Sort.Order firstOrder = orderList.get(0);
		assertThat(firstOrder.getDirection()).isEqualTo(Sort.Direction.DESC);
		assertThat(firstOrder.getProperty()).isEqualTo("sortProperty1");

		Sort.Order secondOrder = orderList.get(1);
		assertThat(secondOrder.getDirection()).isEqualTo(Sort.Direction.ASC);
		assertThat(secondOrder.getProperty()).isEqualTo("sortProperty2");
	}

	@Test
	void testPageableWithoutSort() {
		// given
		Pageable pageable = PageRequest.of(0, 10);

		// when
		final ResponseEntity<Page<Invoice>> response = this.invoiceClient.getInvoicesPaged(pageable);

		// then
		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(pageable.getPageSize()).isEqualTo(response.getBody().getSize());
		assertThat(response.getBody().getPageable().getSort().isSorted()).isFalse();

		List<Invoice> invoiceList = response.getBody().getContent();
		assertThat(invoiceList).hasSizeGreaterThanOrEqualTo(1);
	}

	@Test
	void testPageableWithoutSortWithBody() {
		// given
		Pageable pageable = PageRequest.of(0, 10);

		// when
		final ResponseEntity<Page<Invoice>> response = this.invoiceClient.getInvoicesPagedWithBody(pageable,
				"InvoiceTitleFromBody");

		// then
		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(pageable.getPageSize()).isEqualTo(response.getBody().getSize());

		List<Invoice> invoiceList = response.getBody().getContent();
		assertThat(invoiceList).hasSizeGreaterThanOrEqualTo(1);

		Invoice firstInvoice = invoiceList.get(0);
		assertThat(firstInvoice.getTitle()).startsWith("InvoiceTitleFromBody");
	}

	@Test
	void testPageableWithBody() {
		// given
		Pageable pageable = PageRequest.of(0, 10,
				Sort.by(Sort.Order.desc("sortProperty1"), Sort.Order.asc("sortProperty2")));

		// when
		final ResponseEntity<Page<Invoice>> response = this.invoiceClient.getInvoicesPagedWithBody(pageable,
				"InvoiceTitleFromBody");

		// then
		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(pageable.getPageSize()).isEqualTo(response.getBody().getSize());

		List<Invoice> invoiceList = response.getBody().getContent();
		assertThat(invoiceList).hasSizeGreaterThanOrEqualTo(1);

		Invoice firstInvoice = invoiceList.get(0);
		assertThat(firstInvoice.getTitle()).startsWith("InvoiceTitleFromBody");

		Sort sort = response.getBody().getPageable().getSort();
		assertThat(sort).hasSize(2);

		List<Sort.Order> orderList = sort.toList();
		assertThat(orderList).hasSize(2);

		Sort.Order firstOrder = orderList.get(0);
		assertThat(firstOrder.getDirection()).isEqualTo(Sort.Direction.DESC);
		assertThat(firstOrder.getProperty()).isEqualTo("sortProperty1");

		Sort.Order secondOrder = orderList.get(1);
		assertThat(secondOrder.getDirection()).isEqualTo(Sort.Direction.ASC);
		assertThat(secondOrder.getProperty()).isEqualTo("sortProperty2");
	}

	@Test
	void testUnpagedWithBody() {
		// given
		Pageable unpaged = Pageable.unpaged();

		// when
		final ResponseEntity<Page<Invoice>> response = this.invoiceClient.getInvoicesPagedWithBody(unpaged,
				"InvoiceTitleFromBody");

		// then
		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();

		List<Invoice> invoiceList = response.getBody().getContent();
		assertThat(invoiceList).hasSizeGreaterThanOrEqualTo(1);

		Invoice firstInvoice = invoiceList.get(0);
		assertThat(firstInvoice.getTitle()).startsWith("InvoiceTitleFromBody");
	}

	@Test
	void testSortWithBody() {
		// given
		Sort sort = Sort.by(Sort.Order.desc("amount"));

		// when
		final ResponseEntity<Page<Invoice>> response = this.invoiceClient.getInvoicesSortedWithBody(sort,
				"InvoiceTitleFromBody");

		// then
		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(sort).isEqualTo(response.getBody().getSort());

		List<Invoice> invoiceList = response.getBody().getContent();
		assertThat(invoiceList).hasSizeGreaterThanOrEqualTo(1);

		Invoice firstInvoice = invoiceList.get(0);
		assertThat(firstInvoice.getTitle()).startsWith("InvoiceTitleFromBody");

		for (int ind = 0; ind < invoiceList.size() - 1; ind++) {
			assertThat(invoiceList.get(ind).getAmount()).isGreaterThanOrEqualTo(invoiceList.get(ind + 1).getAmount());
		}
	}

	@EnableFeignClients(clients = InvoiceClient.class)
	@LoadBalancerClient(name = "local", configuration = LocalClientConfiguration.class)
	@SpringBootApplication(scanBasePackages = "org.springframework.cloud.openfeign.encoding.app",
			exclude = { RepositoryRestMvcAutoConfiguration.class })
	@EnableSpringDataWebSupport
	@Import({ NoSecurityConfiguration.class })
	public static class Application {

	}

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
