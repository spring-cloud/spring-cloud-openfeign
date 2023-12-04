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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.bind.annotation.GetMapping;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Jasbir Singh
 * @author Olga Maciaszek-Sharma
 * @author Alex Elin
 */
@SpringBootTest
@TestPropertySource("classpath:feign-properties.properties")
@DirtiesContext
class NonRefreshableFeignClientUrlTests {

	@Autowired
	private Application.FeignClientWithFixUrl feignClientWithFixUrl;

	@Autowired
	private Application.ConfigBasedClient configBasedClient;

	@Autowired
	private Application.NameBasedUrlClient nameBasedUrlClient;

	@Test
	void shouldInstantiateFeignClientWhenUrlFromFeignClientUrl() {
		UrlTestClient.UrlResponseForTests response = feignClientWithFixUrl.fixPath();
		assertThat(response.getUrl()).isEqualTo("http://localhost:8081/fixPath");
		assertThat(response.getTargetType()).isEqualTo(Target.HardCodedTarget.class);
	}

	@Test
	public void shouldInstantiateFeignClientWhenUrlFromProperties() {
		UrlTestClient.UrlResponseForTests response = configBasedClient.test();
		assertThat(response.getUrl()).isEqualTo("http://localhost:9999/test");
		assertThat(response.getTargetType()).isEqualTo(PropertyBasedTarget.class);
	}

	@Test
	void shouldInstantiateFeignClientWhenUrlFromFeignClientName() {
		UrlTestClient.UrlResponseForTests response = nameBasedUrlClient.test();
		assertThat(response.getUrl()).isEqualTo("http://nameBasedClient/test");
		assertThat(response.getTargetType()).isEqualTo(Target.HardCodedTarget.class);
	}

	@Test
	void shouldInstantiateFeignClientWhenUrlAndPathAreInTheFeignClientAnnotation(
			@Autowired Application.WithPathAndFixedUrlClient client) {
		UrlTestClient.UrlResponseForTests response = client.test();
		assertThat(response.getUrl()).isEqualTo("http://localhost:7777/common/test");
		assertThat(response.getTargetType()).isEqualTo(Target.HardCodedTarget.class);
	}

	@Test
	void shouldInstantiateFeignClientWhenUrlFromPropertiesAndPathInTheFeignClientAnnotation(
			@Autowired Application.WithPathAndUrlFromConfigClient client) {
		UrlTestClient.UrlResponseForTests response = client.test();
		assertThat(response.getUrl()).isEqualTo("http://localhost:7777/common/test");
		assertThat(response.getTargetType()).isEqualTo(PropertyBasedTarget.class);
	}

	@Configuration
	@EnableAutoConfiguration
	@EnableConfigurationProperties(FeignClientProperties.class)
	@EnableFeignClients(clients = { Application.FeignClientWithFixUrl.class, Application.ConfigBasedClient.class,
			Application.NameBasedUrlClient.class, Application.WithPathAndUrlFromConfigClient.class,
			Application.WithPathAndFixedUrlClient.class })
	protected static class Application {

		@Bean
		UrlTestClient client() {
			return new UrlTestClient();
		}

		@FeignClient(name = "feignClientWithFixUrl", url = "http://localhost:8081")
		protected interface FeignClientWithFixUrl {

			@GetMapping("/fixPath")
			UrlTestClient.UrlResponseForTests fixPath();

		}

		@FeignClient(name = "configBasedClient")
		protected interface ConfigBasedClient {

			@GetMapping("/test")
			UrlTestClient.UrlResponseForTests test();

		}

		@FeignClient(name = "nameBasedClient")
		protected interface NameBasedUrlClient {

			@GetMapping("/test")
			UrlTestClient.UrlResponseForTests test();

		}

		@FeignClient(name = "withPathAndFixUrlClient", path = "/common", url = "http://localhost:7777")
		protected interface WithPathAndFixedUrlClient {

			@GetMapping("/test")
			UrlTestClient.UrlResponseForTests test();

		}

		@FeignClient(name = "withPathAndUrlFromConfigClient", path = "/common")
		protected interface WithPathAndUrlFromConfigClient {

			@GetMapping("/test")
			UrlTestClient.UrlResponseForTests test();

		}

	}

}
