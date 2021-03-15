/*
 * Copyright 2013-2021 the original author or authors.
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

package org.springframework.cloud.openfeign.async;

import java.util.Map;

import feign.AsyncClient;
import feign.hc5.AsyncApacheHttp5Client;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.nio.AsyncClientConnectionManager;
import org.junit.jupiter.api.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Nguyen Ky Thanh
 */
public class AsyncFeignAutoConfigurationTests {

	@Test
	void shouldInstantiateAsyncHc5FeignWhenAsyncHc5Enabled() {
		ConfigurableApplicationContext context = initContext(
				"feign.httpclient.asyncHc5.enabled=true");
		assertThatOneBeanPresent(context, AsyncApacheHttp5Client.class);
		assertThatOneBeanPresent(context, CloseableHttpAsyncClient.class);
		assertThatOneBeanPresent(context, AsyncClientConnectionManager.class);
	}

	@Test
	void shouldInstantiateDefaultAsyncTargeterAndNoAnyAsyncClient() {
		ConfigurableApplicationContext context = initContext();
		assertThatOneBeanPresent(context, DefaultAsyncTargeter.class);
		assertThatBeanNotPresent(context, AsyncClient.class);
	}

	private ConfigurableApplicationContext initContext(String... properties) {
		return new SpringApplicationBuilder().web(WebApplicationType.NONE)
				.properties(properties).sources(AsyncFeignAutoConfiguration.class).run();
	}

	private void assertThatOneBeanPresent(ConfigurableApplicationContext context,
			Class<?> beanClass) {
		Map<String, ?> beans = context.getBeansOfType(beanClass);
		assertThat(beans).hasSize(1);
	}

	private void assertThatBeanNotPresent(ConfigurableApplicationContext context,
			Class<?> beanClass) {
		Map<String, ?> beans = context.getBeansOfType(beanClass);
		assertThat(beans).isEmpty();
	}

}
