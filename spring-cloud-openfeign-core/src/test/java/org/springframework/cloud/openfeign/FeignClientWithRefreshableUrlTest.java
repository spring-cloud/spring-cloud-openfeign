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

package org.springframework.cloud.openfeign;

import feign.Target;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.context.scope.refresh.RefreshScope;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.bind.annotation.GetMapping;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Jasbir Singh
 */
@SpringBootTest
@TestPropertySource("classpath:feign-refreshable-properties.properties")
@DirtiesContext
public class FeignClientWithRefreshableUrlTest {

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private RefreshScope refreshScope;

	@Autowired
	private FeignClientWithRefreshableUrlTest.Application.RefreshableUrlClient refreshableUrlClient;

	@Autowired
	private FeignClientWithRefreshableUrlTest.Application.NonRefreshableUrlClient nonRefreshableUrlClient;

	@Autowired
	private FeignClientWithRefreshableUrlTest.Application.MalformedUrlClient malformedUrlClient;

	@Autowired
	private FeignClientProperties clientProperties;

	@Test
	public void withoutConfigUrlFromProperties() {
		UrlTestClient.UrlResponseForTests response = nonRefreshableUrlClient.nonRefreshable();
		assertThat(response.getUrl()).isEqualTo("http://nonRefreshableClient/nonRefreshable");
	}

	@Test
	public void targetShouldBeHardCodedTarget() {
		UrlTestClient.UrlResponseForTests response = nonRefreshableUrlClient.nonRefreshable();
		assertThat(response.getTargetType()).isEqualTo(Target.HardCodedTarget.class);
	}

	@Test
	public void withConfigUrlFromProperties() {
		UrlTestClient.UrlResponseForTests response = refreshableUrlClient.refreshable();
		assertThat(response.getUrl()).isEqualTo("http://localhost:8082/refreshable");
	}

	@Test
	public void targetShouldBeRefreshableHardCodedTarget() {
		UrlTestClient.UrlResponseForTests response = refreshableUrlClient.refreshable();
		assertThat(response.getTargetType()).isEqualTo(RefreshableHardCodedTarget.class);
	}

	@Test
	public void urlShouldChangeAfterContextRefresh() {
		UrlTestClient.UrlResponseForTests response = refreshableUrlClient.refreshable();
		assertThat(response.getUrl()).isEqualTo("http://localhost:8082/refreshable");

		clientProperties.getConfig().get("refreshableClient").setUrl("http://localhost:8888/");
		refreshScope.refreshAll();
		response = refreshableUrlClient.refreshable();
		assertThat(response.getUrl()).isEqualTo("http://localhost:8888/refreshable");
	}

	@FeignClient(name = "refreshableClient")
	protected interface RefreshableClient {

		@GetMapping("/refreshable")
		OptionsTestClient.OptionsResponseForTests refreshable();

	}

	@Configuration
	@EnableAutoConfiguration
	@EnableConfigurationProperties(FeignClientProperties.class)
	@EnableFeignClients(clients = { Application.RefreshableUrlClient.class, Application.NonRefreshableUrlClient.class,
			Application.MalformedUrlClient.class })
	protected static class Application {

		@Bean
		UrlTestClient client() {
			return new UrlTestClient();
		}

		@FeignClient(name = "refreshableClient")
		protected interface RefreshableUrlClient {

			@GetMapping("/refreshable")
			UrlTestClient.UrlResponseForTests refreshable();

		}

		@FeignClient(name = "nonRefreshableClient")
		protected interface NonRefreshableUrlClient {

			@GetMapping("/nonRefreshable")
			UrlTestClient.UrlResponseForTests nonRefreshable();

		}

		@FeignClient(name = "malformedUrlClient")
		protected interface MalformedUrlClient {

			@GetMapping("/nonReachablePath")
			UrlTestClient.UrlResponseForTests nonReachablePath();

		}

	}

}
