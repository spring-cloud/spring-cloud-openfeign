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

package org.springframework.cloud.openfeign.hystrix.security;

import java.util.Base64;

import com.netflix.hystrix.strategy.HystrixPlugins;
import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.netflix.hystrix.security.SecurityContextConcurrencyStrategy;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.netflix.ribbon.StaticServerList;
import org.springframework.cloud.openfeign.hystrix.security.app.CustomConcurrenyStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that a secured web service returning values using a feign client properly access
 * the security context from a hystrix command.
 *
 * @author Daniel Lavoie
 */
@RunWith(SpringRunner.class)
@DirtiesContext
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = { "feign.hystrix.enabled=true" })
@ActiveProfiles("proxysecurity")
public class HystrixSecurityTests {

	@Autowired
	private CustomConcurrenyStrategy customConcurrenyStrategy;

	@LocalServerPort
	private String serverPort;

	// TODO: move to constants in TestAutoConfiguration
	private String username = "user";

	private String password = "password";

	public static HttpHeaders createBasicAuthHeader(final String username,
			final String password) {
		return new HttpHeaders() {
			private static final long serialVersionUID = 1766341693637204893L;

			{
				String auth = username + ":" + password;
				byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
				String authHeader = "Basic " + new String(encodedAuth);
				this.set("Authorization", authHeader);
			}
		};
	}

	@Test
	public void testSecurityConcurrencyStrategyInstalled() {
		HystrixConcurrencyStrategy concurrencyStrategy = HystrixPlugins.getInstance()
				.getConcurrencyStrategy();
		assertThat(concurrencyStrategy)
				.isInstanceOf(SecurityContextConcurrencyStrategy.class);
	}

	@Test
	public void testFeignHystrixSecurity() {
		HttpHeaders headers = createBasicAuthHeader(this.username, this.password);

		ResponseEntity<String> entity = new RestTemplate().exchange(
				"http://localhost:" + this.serverPort + "/proxy-username", HttpMethod.GET,
				new HttpEntity<Void>(headers), String.class);

		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);

		assertThat(entity.getBody())
				.as("Username should have been intercepted by feign interceptor.")
				.isEqualTo(this.username);

		assertThat(this.customConcurrenyStrategy.isHookCalled())
				.as("Custom hook should have been called.").isTrue();
	}

	@SpringBootConfiguration
	@Import(HystrixSecurityApplication.class)
	@RibbonClient(name = "username", configuration = LocalRibbonClientConfiguration.class)
	protected static class TestConfig {

	}

	protected static class LocalRibbonClientConfiguration {

		@LocalServerPort
		private int port = 0;

		@Bean
		public ServerList<Server> ribbonServerList() {
			return new StaticServerList<>(new Server("localhost", this.port));
		}

	}

}
