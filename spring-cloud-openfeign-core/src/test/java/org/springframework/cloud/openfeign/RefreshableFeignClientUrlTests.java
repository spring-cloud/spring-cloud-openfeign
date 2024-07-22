/*
 * Copyright 2013-2023 the original author or authors.
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.bind.annotation.GetMapping;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Jasbir Singh
 * @author Olga Maciaszek-Sharma
 */
@SpringBootTest
@TestPropertySource("classpath:feign-refreshable-properties.properties")
@DirtiesContext
class RefreshableFeignClientUrlTests {

	@Autowired
	private RefreshScope refreshScope;

	@Autowired
	private RefreshableFeignClientUrlTests.Application.RefreshableClientWithFixedUrl refreshableClientWithFixedUrl;

	@Autowired
	private RefreshableFeignClientUrlTests.Application.RefreshableUrlClient refreshableUrlClient;

	@Autowired
	private Application.RefreshableUrlClientForContextRefreshCase refreshableClientForContextRefreshCase;

	@Autowired
	private Application.NameBasedUrlClient nameBasedUrlClient;

	@Autowired
	private FeignClientProperties clientProperties;

	@Test
	void shouldInstantiateFeignClientWhenUrlFromAnnotation() {
		UrlTestClient.UrlResponseForTests response = refreshableClientWithFixedUrl.fixedPath();
		assertThat(response.getUrl()).isEqualTo("http://localhost:8081/fixedPath");
		assertThat(response.getTargetType()).isEqualTo(Target.HardCodedTarget.class);
	}

	@Test
	void shouldInstantiateFeignClientWhenUrlAndPathFromAnnotation(
			@Autowired Application.WithPathAndFixedUrlClient client) {
		UrlTestClient.UrlResponseForTests response = client.test();
		assertThat(response.getUrl()).isEqualTo("http://localhost:7777/common/test");
		assertThat(response.getTargetType()).isEqualTo(Target.HardCodedTarget.class);
	}

	@Test
	public void shouldInstantiateFeignClientWhenUrlFromProperties() {
		UrlTestClient.UrlResponseForTests response = refreshableUrlClient.refreshable();
		assertThat(response.getUrl()).isEqualTo("http://localhost:8082/refreshable");
		assertThat(response.getTargetType()).isEqualTo(RefreshableHardCodedTarget.class);
	}

	@Test
	void shouldInstantiateFeignClientWhenUrlFromPropertiesAndThenUpdateUrlWhenContextRefresh() {
		UrlTestClient.UrlResponseForTests response = refreshableClientForContextRefreshCase.refreshable();
		assertThat(response.getUrl()).isEqualTo("http://localhost:8080/refreshable");

		clientProperties.getConfig().get("refreshableClientForContextRefreshCase").setUrl("http://localhost:8888");
		refreshScope.refreshAll();
		response = refreshableClientForContextRefreshCase.refreshable();
		assertThat(response.getUrl()).isEqualTo("http://localhost:8888/refreshable");
	}

	@Test
	void shouldInstantiateFeignClientWhenUrlFromPropertiesAndPathFromAnnotationThenUpdateUrlWhenContextRefresh(
			@Autowired Application.RefreshableUrlClientForContextRefreshCaseWithPath client) {
		UrlTestClient.UrlResponseForTests response = client.refreshable();
		assertThat(response.getUrl()).isEqualTo("http://localhost:8080/common/refreshable");

		clientProperties.getConfig()
			.get("refreshableClientForContextRefreshCaseWithPath")
			.setUrl("http://localhost:8888");
		refreshScope.refreshAll();
		response = client.refreshable();
		assertThat(response.getUrl()).isEqualTo("http://localhost:8888/common/refreshable");
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
			Application.RefreshableClientWithFixedUrl.class,
			Application.RefreshableUrlClientForContextRefreshCase.class, Application.WithPathAndFixedUrlClient.class,
			Application.RefreshableUrlClientForContextRefreshCaseWithPath.class })
	protected static class Application {

		@Bean
		UrlTestClient client() {
			return new UrlTestClient();
		}

		@FeignClient(name = "refreshableClientWithFixedUrl", url = "http://localhost:8081")
		protected interface RefreshableClientWithFixedUrl {

			@GetMapping("/fixedPath")
			UrlTestClient.UrlResponseForTests fixedPath();

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

		@FeignClient(name = "refreshableClientForContextRefreshCaseWithPath", path = "/common")
		protected interface RefreshableUrlClientForContextRefreshCaseWithPath {

			@GetMapping("/refreshable")
			UrlTestClient.UrlResponseForTests refreshable();

		}

		@FeignClient(name = "nameBasedClient")
		protected interface NameBasedUrlClient {

			@GetMapping("/nonRefreshable")
			UrlTestClient.UrlResponseForTests nonRefreshable();

		}

		@FeignClient(name = "withPathAndFixUrlClient", path = "/common", url = "http://localhost:7777")
		protected interface WithPathAndFixedUrlClient {

			@GetMapping("/test")
			UrlTestClient.UrlResponseForTests test();

		}

	}

}
