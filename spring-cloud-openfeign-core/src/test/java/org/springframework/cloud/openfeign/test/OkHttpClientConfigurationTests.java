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

package org.springframework.cloud.openfeign.test;

import java.util.concurrent.TimeUnit;

import feign.Client;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;
import org.mockito.MockingDetails;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.commons.httpclient.DefaultOkHttpClientConnectionPoolFactory;
import org.springframework.cloud.commons.httpclient.DefaultOkHttpClientFactory;
import org.springframework.cloud.commons.httpclient.OkHttpClientConnectionPoolFactory;
import org.springframework.cloud.commons.httpclient.OkHttpClientFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.loadbalancer.FeignBlockingLoadBalancerClient;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;

/**
 * @author Ryan Baxter
 * @author Olga Maciaszek-Sharma
 */
@SpringBootTest(properties = { "spring.cloud.openfeign.okhttp.enabled: true",
		"spring.cloud.httpclientfactories.ok.enabled: true", "spring.cloud.openfeign.okhttp.enabled: true",
		"spring.cloud.openfeign.httpclient.enabled: false", "spring.cloud.loadbalancer.retry.enabled=false" })
@DirtiesContext
class OkHttpClientConfigurationTests {

	@Autowired
	OkHttpClientFactory okHttpClientFactory;

	@Autowired
	OkHttpClientConnectionPoolFactory connectionPoolFactory;

	@Autowired
	FeignBlockingLoadBalancerClient feignClient;

	@Test
	void testFactories() {
		assertThat(connectionPoolFactory).isInstanceOf(OkHttpClientConnectionPoolFactory.class);
		assertThat(connectionPoolFactory).isInstanceOf(TestConfig.MyOkHttpClientConnectionPoolFactory.class);
		assertThat(okHttpClientFactory).isInstanceOf(OkHttpClientFactory.class);
		assertThat(okHttpClientFactory).isInstanceOf(TestConfig.MyOkHttpClientFactory.class);
	}

	@Test
	void testHttpClientWithFeign() {
		Client delegate = feignClient.getDelegate();
		assertThat(feign.okhttp.OkHttpClient.class.isInstance(delegate)).isTrue();
		feign.okhttp.OkHttpClient okHttpClient = (feign.okhttp.OkHttpClient) delegate;
		OkHttpClient httpClient = getField(okHttpClient, "delegate");
		MockingDetails httpClientDetails = mockingDetails(httpClient);
		assertThat(httpClientDetails.isMock()).isTrue();
	}

	protected <T> T getField(Object target, String name) {
		Object value = ReflectionTestUtils.getField(target, target.getClass(), name);
		return (T) value;
	}

	@FeignClient(name = "foo")
	interface FooClient {

	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	static class TestConfig {

		@Bean
		public OkHttpClientConnectionPoolFactory connectionPoolFactory() {
			return new MyOkHttpClientConnectionPoolFactory();
		}

		@Bean
		public OkHttpClientFactory clientFactory(OkHttpClient.Builder builder) {
			return new MyOkHttpClientFactory(builder);
		}

		@Bean
		public OkHttpClient client() {
			return mock(OkHttpClient.class);
		}

		static class MyOkHttpClientConnectionPoolFactory extends DefaultOkHttpClientConnectionPoolFactory {

			@Override
			public ConnectionPool create(int maxIdleConnections, long keepAliveDuration, TimeUnit timeUnit) {
				return new ConnectionPool();
			}

		}

		static class MyOkHttpClientFactory extends DefaultOkHttpClientFactory {

			MyOkHttpClientFactory(OkHttpClient.Builder builder) {
				super(builder);
			}

		}

	}

}
