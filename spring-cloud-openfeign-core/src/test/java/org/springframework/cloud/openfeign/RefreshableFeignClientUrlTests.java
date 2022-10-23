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
class RefreshableFeignClientUrlTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private RefreshScope refreshScope;

	@Autowired
	private RefreshableFeignClientUrlTests.Application.RefreshableClientWithFixUrl refreshableClientWithFixUrl;

	@Autowired
	private RefreshableFeignClientUrlTests.Application.RefreshableUrlClient refreshableUrlClient;

	@Autowired
	private Application.RefreshableUrlClientForContextRefreshCase refreshableUrlClientForContextRefreshCase;

	@Autowired
	private Application.NameBasedUrlClient nameBasedUrlClient;

	@Autowired
	private FeignClientProperties clientProperties;

	@Test
	void shouldInstantiateFeignClientWhenUrlFromFeignClientUrl() {
		UrlTestClient.UrlResponseForTests response = refreshableClientWithFixUrl.fixPath();
		assertThat(response.getUrl()).isEqualTo("http://localhost:8081/fixPath");
		assertThat(response.getTargetType()).isEqualTo(Target.HardCodedTarget.class);
	}

	@Test
	void shouldInstantiateFeignClientWhenUrlFromFeignClientUrlGivenPreferenceOverProperties() {
		UrlTestClient.UrlResponseForTests response = refreshableClientWithFixUrl.fixPath();
		assertThat(response.getUrl()).isEqualTo("http://localhost:8081/fixPath");
	}

	@Test
	public void shouldInstantiateFeignClientWhenUrlFromProperties() {
		UrlTestClient.UrlResponseForTests response = refreshableUrlClient.refreshable();
		assertThat(response.getUrl()).isEqualTo("http://localhost:8082/refreshable");
		assertThat(response.getTargetType()).isEqualTo(RefreshableHardCodedTarget.class);
	}

	@Test
	void shouldInstantiateFeignClientWhenUrlFromPropertiesAndThenUpdateUrlWhenContextRefresh() {
		UrlTestClient.UrlResponseForTests response = refreshableUrlClientForContextRefreshCase.refreshable();
		assertThat(response.getUrl()).isEqualTo("http://localhost:8080/refreshable");

		clientProperties.getConfig().get("refreshableClient").setUrl("http://localhost:8888/");
		refreshScope.refreshAll();
		response = refreshableUrlClient.refreshable();
		assertThat(response.getUrl()).isEqualTo("http://localhost:8888/refreshable");
	}

	@Test
	void shouldInstantiateFeignClientWhenUrlFromFeignClientName() {
		UrlTestClient.UrlResponseForTests response = nameBasedUrlClient.nonRefreshable();
		assertThat(response.getUrl()).isEqualTo("http://nameBasedClient/nonRefreshable");
		assertThat(response.getTargetType()).isEqualTo(Target.HardCodedTarget.class);
	}

	@Configuration
	@EnableAutoConfiguration
	@EnableConfigurationProperties(FeignClientProperties.class)
	@EnableFeignClients(clients = { Application.RefreshableUrlClient.class, Application.NameBasedUrlClient.class,
			Application.RefreshableClientWithFixUrl.class,
			Application.RefreshableUrlClientForContextRefreshCase.class })
	protected static class Application {

		@Bean
		UrlTestClient client() {
			return new UrlTestClient();
		}

		@FeignClient(name = "refreshableClientWithFixUrl", url = "http://localhost:8081")
		protected interface RefreshableClientWithFixUrl {

			@GetMapping("/fixPath")
			UrlTestClient.UrlResponseForTests fixPath();

		}

		@FeignClient(name = "refreshableClient")
		protected interface RefreshableUrlClient {

			@GetMapping("/refreshable")
			UrlTestClient.UrlResponseForTests refreshable();

		}

		@FeignClient(name = "refreshableClientForContextRefreshCase")
		protected interface RefreshableUrlClientForContextRefreshCase {

			@GetMapping("/refreshable")
			UrlTestClient.UrlResponseForTests refreshable();

		}

		@FeignClient(name = "nameBasedClient")
		protected interface NameBasedUrlClient {

			@GetMapping("/nonRefreshable")
			UrlTestClient.UrlResponseForTests nonRefreshable();

		}

	}

}
