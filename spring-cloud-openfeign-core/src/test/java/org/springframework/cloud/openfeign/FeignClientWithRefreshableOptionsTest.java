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

import java.util.concurrent.TimeUnit;

import feign.Request;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
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
import org.springframework.web.context.support.GenericWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Jasbir Singh
 */
@SpringBootTest
@TestPropertySource("classpath:feign-refreshable-properties.properties")
@DirtiesContext
public class FeignClientWithRefreshableOptionsTest {

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private RefreshScope refreshScope;

	@Autowired
	private Application.RefreshableClient refreshableClient;

	@Autowired
	private Application.ReadTimeoutClient readTimeoutClient;

	@Autowired
	private Application.ConnectTimeoutClient connectTimeoutClient;

	@Autowired
	private Application.OverrideOptionsClient overrideOptionsClient;

	@Autowired
	private FeignClientProperties clientProperties;

	@Test
	public void overriddenOptionsBeanShouldBePresentInsteadOfRefreshable() {
		OptionsTestClient.OptionsResponseForTests options = overrideOptionsClient.override();
		assertConnectionAndReadTimeout(options, 1, 1);
	}

	@Test
	public void refreshScopeBeanDefinitionShouldBePresent() {
		BeanDefinition beanDefinition = ((GenericWebApplicationContext) applicationContext)
				.getBeanDefinition(Request.Options.class.getCanonicalName() + "-" + "refreshableClient");
		BeanDefinition originBeanDefinition = beanDefinition.getOriginatingBeanDefinition();
		assertThat(originBeanDefinition.getBeanClassName()).isEqualTo(OptionsFactoryBean.class.getCanonicalName());
		assertThat(originBeanDefinition.getScope()).isEqualTo("refresh");
	}

	@Test
	public void withConfigDefaultConnectTimeoutAndReadTimeout() {
		OptionsTestClient.OptionsResponseForTests options = refreshableClient.refreshable();
		assertConnectionAndReadTimeout(options, 5000, 5000);
	}

	@Test
	public void readTimeoutShouldWorkWhenConnectTimeoutNotSet() {
		OptionsTestClient.OptionsResponseForTests options = readTimeoutClient.readTimeout();
		assertConnectionAndReadTimeout(options, 5000, 2000);
	}

	@Test
	public void connectTimeoutShouldWorkWhenReadTimeoutNotSet() {
		OptionsTestClient.OptionsResponseForTests options = connectTimeoutClient.connectTimeout();
		assertConnectionAndReadTimeout(options, 2000, 5000);
	}

	@Test
	public void connectTimeoutShouldNotChangeWithoutContextRefresh() {
		OptionsTestClient.OptionsResponseForTests options = connectTimeoutClient.connectTimeout();
		assertConnectionAndReadTimeout(options, 2000, 5000);

		clientProperties.getConfig().get("connectTimeout").setConnectTimeout(5000);
		options = connectTimeoutClient.connectTimeout();
		assertConnectionAndReadTimeout(options, 2000, 5000);
	}

	@Test
	public void connectTimeoutShouldChangeAfterContextRefresh() {
		OptionsTestClient.OptionsResponseForTests options = connectTimeoutClient.connectTimeout();
		assertConnectionAndReadTimeout(options, 2000, 5000);

		clientProperties.getConfig().get("connectTimeout").setConnectTimeout(5000);
		refreshScope.refreshAll();
		options = connectTimeoutClient.connectTimeout();
		assertConnectionAndReadTimeout(options, 5000, 5000);
	}

	private void assertConnectionAndReadTimeout(OptionsTestClient.OptionsResponseForTests options,
			int expectedConnectTimeoutInMillis, int expectedReadTimeoutInMillis) {
		assertThat(options.connectTimeout()).isEqualTo(expectedConnectTimeoutInMillis);
		assertThat(options.readTimeout()).isEqualTo(expectedReadTimeoutInMillis);
	}

	@Configuration
	@EnableAutoConfiguration
	@EnableConfigurationProperties(FeignClientProperties.class)
	@EnableFeignClients(clients = { Application.OverrideOptionsClient.class, Application.RefreshableClient.class,
			Application.ReadTimeoutClient.class, Application.ConnectTimeoutClient.class })
	protected static class Application {

		@Bean
		OptionsTestClient client() {
			return new OptionsTestClient();
		}

		@FeignClient(name = "overrideOptionsClient", configuration = OverrideConfig.class)
		protected interface OverrideOptionsClient {

			@GetMapping("/override")
			OptionsTestClient.OptionsResponseForTests override();

		}

		@FeignClient(name = "refreshableClient")
		protected interface RefreshableClient {

			@GetMapping("/refreshable")
			OptionsTestClient.OptionsResponseForTests refreshable();

		}

		@FeignClient(name = "readTimeout")
		protected interface ReadTimeoutClient {

			@GetMapping("/readTimeout")
			OptionsTestClient.OptionsResponseForTests readTimeout();

		}

		@FeignClient(name = "connectTimeout")
		protected interface ConnectTimeoutClient {

			@GetMapping("/connectTimeout")
			OptionsTestClient.OptionsResponseForTests connectTimeout();

		}

		@Configuration
		protected static class OverrideConfig {

			@Bean
			public Request.Options options() {
				return new Request.Options(1, TimeUnit.MILLISECONDS, 1, TimeUnit.MILLISECONDS, true);
			}

		}

	}

}
