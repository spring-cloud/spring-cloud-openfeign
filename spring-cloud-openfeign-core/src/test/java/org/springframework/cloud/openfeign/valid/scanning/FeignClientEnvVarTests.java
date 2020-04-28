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

package org.springframework.cloud.openfeign.valid.scanning;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.test.NoSecurityConfiguration;
import org.springframework.cloud.openfeign.testclients.TestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * @author Ryan Baxter
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = FeignClientEnvVarTests.Application.class,
		webEnvironment = RANDOM_PORT,
		value = { "spring.application.name=feignclienttest",
				"feign.httpclient.enabled=false",
				"basepackage=org.springframework.cloud.openfeign.testclients" })
@DirtiesContext
public class FeignClientEnvVarTests {

	@Autowired
	private TestClient testClient;

	@Test
	public void testSimpleType() {
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

		@RequestMapping(method = RequestMethod.GET, value = "/hello")
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
		public ServiceInstanceListSupplier staticServiceInstanceListSupplier(
				Environment env) {
			return ServiceInstanceListSupplier.fixed(env).instance(port, "localapp")
					.build();
		}

	}

}
