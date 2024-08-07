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

import feign.Client;
import feign.hc5.ApacheHttp5Client;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.openfeign.clientconfig.HttpClient5FeignConfiguration.HttpClientBuilderCustomizer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

/**
 * @author Nguyen Ky Thanh
 * @author Olga Maciaszek-Sharma
 * @author Kwangyong Kim
 */
class FeignHttpClient5ConfigurationTests {

	private static void verifyHc5BeansAvailable(ConfigurableApplicationContext context) {
		CloseableHttpClient httpClient = context.getBean(CloseableHttpClient.class);
		assertThat(httpClient).isNotNull();
		HttpClientConnectionManager connectionManager = context.getBean(HttpClientConnectionManager.class);
		assertThat(connectionManager).isInstanceOf(PoolingHttpClientConnectionManager.class);
		Client client = context.getBean(Client.class);
		assertThat(client).isInstanceOf(ApacheHttp5Client.class);
	}

	@Test
	void shouldInstantiateHttpClient5ByDefaultWhenDependenciesPresent() {
		ConfigurableApplicationContext context = new SpringApplicationBuilder().web(WebApplicationType.NONE)
			.sources(FeignAutoConfiguration.class)
			.run();

		verifyHc5BeansAvailable(context);

		if (context != null) {
			context.close();
		}
	}

	@Test
	void shouldNotInstantiateHttpClient5ByWhenDependenciesPresentButPropertyDisabled() {
		ConfigurableApplicationContext context = new SpringApplicationBuilder()
			.properties("spring.cloud.openfeign.httpclient.hc5.enabled=false")
			.web(WebApplicationType.NONE)
			.sources(FeignAutoConfiguration.class)
			.run();

		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
			.isThrownBy(() -> context.getBean(CloseableHttpClient.class));

		if (context != null) {
			context.close();
		}
	}

	@Test
	void shouldInstantiateHttpClient5ByUsingHttpClientBuilderCustomizer() {
		ConfigurableApplicationContext context = new SpringApplicationBuilder().web(WebApplicationType.NONE)
			.sources(FeignAutoConfiguration.class, Config.class)
			.run();

		CloseableHttpClient httpClient = context.getBean(CloseableHttpClient.class);
		assertThat(httpClient).isNotNull();
		HttpClientBuilderCustomizer customizer = context.getBean(HttpClientBuilderCustomizer.class);
		verify(customizer).customize(any(HttpClientBuilder.class));

		if (context != null) {
			context.close();
		}
	}

	@Configuration
	static class Config {

		@Bean
		HttpClientBuilderCustomizer customizer() {
			return Mockito.mock(HttpClientBuilderCustomizer.class);
		}

	}

}
