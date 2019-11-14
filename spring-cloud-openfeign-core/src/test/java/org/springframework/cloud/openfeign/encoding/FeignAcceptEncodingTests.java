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

package org.springframework.cloud.openfeign.encoding;

import java.util.Collections;
import java.util.List;

import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.encoding.app.client.InvoiceClient;
import org.springframework.cloud.openfeign.encoding.app.domain.Invoice;
import org.springframework.cloud.openfeign.test.NoSecurityConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Tests the response compression.
 *
 * @author Jakub Narloch
 */
@SpringBootTest(classes = FeignAcceptEncodingTests.Application.class,
		webEnvironment = RANDOM_PORT,
		value = { "feign.compression.response.enabled=true" })
@RunWith(SpringRunner.class)
@DirtiesContext
public class FeignAcceptEncodingTests {

	@Autowired
	private InvoiceClient invoiceClient;

	@Test
	public void compressedResponse() {

		// when
		final ResponseEntity<List<Invoice>> invoices = this.invoiceClient.getInvoices();

		// then
		assertThat(invoices).isNotNull();
		assertThat(invoices.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(invoices.getBody()).isNotNull();
		assertThat(invoices.getBody().size()).isEqualTo(100);

	}

	@EnableFeignClients(clients = InvoiceClient.class)
	@RibbonClient(name = "local", configuration = LocalRibbonClientConfiguration.class)
	@SpringBootApplication(
			scanBasePackages = "org.springframework.cloud.openfeign.encoding.app")
	@Import(NoSecurityConfiguration.class)
	public static class Application {

	}

	@Configuration(proxyBeanMethods = false)
	static class LocalRibbonClientConfiguration {

		@Value("${local.server.port}")
		private int port = 0;

		@Bean
		public ILoadBalancer ribbonLoadBalancer() {
			BaseLoadBalancer balancer = new BaseLoadBalancer();
			balancer.setServersList(
					Collections.singletonList(new Server("localhost", this.port)));
			return balancer;
		}

	}

}
