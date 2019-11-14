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

package org.springframework.cloud.openfeign.valid;

import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import io.vavr.collection.HashSet;
import io.vavr.collection.Set;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.netflix.ribbon.StaticServerList;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.test.NoSecurityConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = IterableParameterTests.Application.class,
		webEnvironment = WebEnvironment.RANDOM_PORT,
		value = { "spring.application.name=iterableparametertest",
				"logging.level.org.springframework.cloud.openfeign.valid=DEBUG",
				"feign.httpclient.enabled=false", "feign.okhttp.enabled=false",
				"feign.hystrix.enabled=false" })
@DirtiesContext
public class IterableParameterTests {

	@Autowired
	private TestClient testClient;

	@Test
	public void testClient() {
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
	@RibbonClient(name = "localapp", configuration = LocalRibbonClientConfiguration.class)
	@Import(NoSecurityConfiguration.class)
	protected static class Application {

		@GetMapping("/echo")
		public String echo(@RequestParam String set) {
			return set;
		}

	}

	// Load balancer with fixed server list for "local" pointing to localhost
	public static class LocalRibbonClientConfiguration {

		@LocalServerPort
		private int port = 0;

		@Bean
		public ServerList<Server> ribbonServerList() {
			return new StaticServerList<>(new Server("localhost", this.port));
		}

	}

}
