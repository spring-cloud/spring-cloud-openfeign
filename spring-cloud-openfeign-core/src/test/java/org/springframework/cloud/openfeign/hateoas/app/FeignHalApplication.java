/*
 * Copyright 2016-2025 the original author or authors.
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

package org.springframework.cloud.openfeign.hateoas.app;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.data.rest.autoconfigure.RepositoryRestMvcAutoConfiguration;
import org.springframework.boot.web.server.test.LocalServerPort;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.ServiceInstanceListSuppliers;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.test.NoSecurityConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Test HATEOAS application.
 *
 * @author Hector Espert
 */
@EnableFeignClients(clients = FeignHalClient.class)
@SpringBootApplication(scanBasePackages = "org.springframework.cloud.openfeign.hateoas.app",
		exclude = RepositoryRestMvcAutoConfiguration.class)
@LoadBalancerClient(name = "local", configuration = LocalHalClientConfiguration.class)
@Import(NoSecurityConfiguration.class)
public class FeignHalApplication {

	// Load balancer with fixed server list for "local" pointing to localhost

}

class LocalHalClientConfiguration {

	@LocalServerPort
	private int port = 0;

	@Bean
	public ServiceInstanceListSupplier staticServiceInstanceListSupplier() {
		return ServiceInstanceListSuppliers.from("local",
				new DefaultServiceInstance("local-1", "local", "localhost", port, false));
	}

}
