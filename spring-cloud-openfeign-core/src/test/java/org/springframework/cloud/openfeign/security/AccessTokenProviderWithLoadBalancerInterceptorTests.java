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

package org.springframework.cloud.openfeign.security;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.cloud.client.loadbalancer.RetryLoadBalancerInterceptor;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.FeignContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * @author Wojciech Mąka
 */
@SpringBootTest(classes = AccessTokenProviderWithLoadBalancerInterceptorTests.Application.class,
		webEnvironment = RANDOM_PORT,
		value = { "security.oauth2.client.id=test-service", "security.oauth2.client.client-id=test-service",
				"security.oauth2.client.client-secret=test-service",
				"security.oauth2.client.grant-type=client_credentials", "spring.cloud.openfeign.oauth2.enabled=true",
				"spring.cloud.openfeign.oauth2.load-balanced=true" })
@DirtiesContext
public class AccessTokenProviderWithLoadBalancerInterceptorTests {

	@Autowired
	FeignContext context;

	@Autowired
	private ConfigurableApplicationContext applicationContext;

	@Test
	void testOAuth2RequestInterceptorIsLoadBalanced() {
		AssertableApplicationContext assertableContext = AssertableApplicationContext.get(() -> applicationContext);
		assertThat(assertableContext).hasSingleBean(Application.SampleClient.class);
		assertThat(assertableContext).hasSingleBean(OAuth2FeignRequestInterceptor.class);
		assertThat(assertableContext).getBean(OAuth2FeignRequestInterceptor.class).extracting("accessTokenProvider")
				.extracting("interceptors").asList()
				.filteredOn(obj -> RetryLoadBalancerInterceptor.class.equals(obj.getClass())).hasSize(1);
	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	@RestController
	@EnableFeignClients(
			clients = { AccessTokenProviderWithLoadBalancerInterceptorTests.Application.SampleClient.class })
	protected static class Application {

		@GetMapping("/foo")
		public String foo(HttpServletRequest request) throws IllegalAccessException {
			if ("Foo".equals(request.getHeader("Foo")) && "Bar".equals(request.getHeader("Bar"))) {
				return "OK";
			}
			else {
				throw new IllegalAccessException("It should has Foo and Bar header");
			}
		}

		@FeignClient(name = "sampleClient")
		protected interface SampleClient {

			@GetMapping("/foo")
			String foo();

		}

	}

}
