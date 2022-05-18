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

package org.springframework.cloud.openfeign.support;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.ServiceInstanceListSuppliers;
import org.springframework.cloud.openfeign.CollectionFormat;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.test.NoSecurityConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * @author Olga Maciaszek-Sharma
 */
@SpringBootTest(classes = PageableSupportTest.Config.class, webEnvironment = RANDOM_PORT)
public class PageableSupportTest {

	@Autowired
	private PageableFeignClient feignClient;

	@Test
	void shouldProperlyFormatPageable() {
		String direction = feignClient.performRequest(PageRequest.of(1, 10, Sort.by(Sort.Order.desc("property"))));

		assertThat(direction).isEqualTo("DESC");
	}

	@FeignClient("pageable")
	protected interface PageableFeignClient {

		@CollectionFormat(feign.CollectionFormat.CSV)
		@GetMapping(path = "/page")
		String performRequest(Pageable page);

	}

	@SuppressWarnings("ConstantConditions")
	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	@RestController
	@EnableFeignClients(clients = PageableFeignClient.class)
	@Import(NoSecurityConfiguration.class)
	@LoadBalancerClient(name = "pageable", configuration = LocalClientConfiguration.class)
	protected static class Config {

		@GetMapping(path = "/page")
		String performRequest(Pageable page) {
			return page.getSort().getOrderFor("property").getDirection().toString();
		}

	}

	// Load balancer with fixed server list for "local" pointing to localhost
	@Configuration(proxyBeanMethods = false)
	static class LocalClientConfiguration {

		@LocalServerPort
		private int port = 0;

		@Bean
		public ServiceInstanceListSupplier staticServiceInstanceListSupplier() {
			return ServiceInstanceListSuppliers.from("pageable",
					new DefaultServiceInstance("pageable-1", "pageable", "localhost", port, false));
		}

	}

}
