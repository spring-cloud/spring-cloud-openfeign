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

package org.springframework.cloud.openfeign.loadbalancer;

import java.util.Map;

import feign.Client;
import feign.hc5.ApacheHttp5Client;
import feign.okhttp.OkHttpClient;
import org.junit.jupiter.api.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.loadbalancer.blocking.client.BlockingLoadBalancerClient;
import org.springframework.cloud.loadbalancer.config.BlockingLoadBalancerClientAutoConfiguration;
import org.springframework.cloud.loadbalancer.config.LoadBalancerAutoConfiguration;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.util.ReflectionTestUtils.getField;

/**
 * @author Olga Maciaszek-Sharma
 * @author Nguyen Ky Thanh
 */
class FeignLoadBalancerAutoConfigurationTests {

	@Test
	void shouldInstantiateDefaultFeignBlockingLoadBalancerClientWhenHttpClientDisabled() {
		ConfigurableApplicationContext context = initContext("spring.cloud.openfeign.httpclient.hc5.enabled=false",
				"spring.cloud.loadbalancer.retry.enabled=false");
		assertThatOneBeanPresent(context, BlockingLoadBalancerClient.class);
		assertLoadBalanced(context, Client.Default.class);
	}

	@Test
	void shouldInstantiateOkHttpFeignClientWhenEnabled() {
		ConfigurableApplicationContext context = initContext("spring.cloud.openfeign.httpclient.hc5.enabled=false",
				"spring.cloud.openfeign.okhttp.enabled=true", "spring.cloud.loadbalancer.retry.enabled=false",
				"spring.cloud.openfeign.httpclient.okhttp.read-timeout=9s");
		assertThatOneBeanPresent(context, BlockingLoadBalancerClient.class);
		Map<String, FeignBlockingLoadBalancerClient> beans = context
				.getBeansOfType(FeignBlockingLoadBalancerClient.class);
		assertThat(beans).as("Missing bean of type %s", OkHttpClient.class).hasSize(1);
		Client client = beans.get("feignClient").getDelegate();
		assertThat(client).isInstanceOf(OkHttpClient.class);
		OkHttpClient okHttpClient = (OkHttpClient) client;
		okhttp3.OkHttpClient httpClient = (okhttp3.OkHttpClient) getField(okHttpClient, "delegate");
		assertThat(httpClient.readTimeoutMillis()).isEqualTo(9000);

	}

	@Test
	void shouldInstantiateHttpFeignClient5WhenAvailableAndOkHttpDisabled() {
		ConfigurableApplicationContext context = initContext("spring.cloud.openfeign.okhttp.enabled=false",
				"spring.cloud.loadbalancer.retry.enabled=false");
		assertThatOneBeanPresent(context, BlockingLoadBalancerClient.class);
		assertLoadBalanced(context, ApacheHttp5Client.class);
	}

	@Test
	void shouldInstantiateRetryableDefaultFeignBlockingLoadBalancerClientWhenHttpClientDisabled() {
		ConfigurableApplicationContext context = initContext("spring.cloud.openfeign.httpclient.hc5.enabled=false");
		assertThatOneBeanPresent(context, BlockingLoadBalancerClient.class);
		assertLoadBalancedWithRetries(context, Client.Default.class);
	}

	@Test
	void shouldInstantiateRetryableOkHttpFeignClientWhenEnabled() {
		ConfigurableApplicationContext context = initContext("spring.cloud.openfeign.httpclient.hc5.enabled=false",
				"spring.cloud.openfeign.okhttp.enabled=true");
		assertThatOneBeanPresent(context, BlockingLoadBalancerClient.class);
		assertLoadBalancedWithRetries(context, OkHttpClient.class);
	}

	@Test
	void shouldInstantiateRetryableHttpFeignClient5WhenEnabled() {
		ConfigurableApplicationContext context = initContext();
		assertThatOneBeanPresent(context, BlockingLoadBalancerClient.class);
		assertLoadBalancedWithRetries(context, ApacheHttp5Client.class);
	}

	private ConfigurableApplicationContext initContext(String... properties) {
		return new SpringApplicationBuilder().web(WebApplicationType.NONE).properties(properties)
				.sources(LoadBalancerAutoConfiguration.class, BlockingLoadBalancerClientAutoConfiguration.class,
						FeignLoadBalancerAutoConfiguration.class, FeignAutoConfiguration.class)
				.run();
	}

	private void assertThatOneBeanPresent(ConfigurableApplicationContext context, Class<?> beanClass) {
		Map<String, ?> beans = context.getBeansOfType(beanClass);
		assertThat(beans).as("Missing bean of type %s", beanClass).hasSize(1);
	}

	private void assertLoadBalanced(ConfigurableApplicationContext context, Class delegateClass) {
		Map<String, FeignBlockingLoadBalancerClient> beans = context
				.getBeansOfType(FeignBlockingLoadBalancerClient.class);
		assertThat(beans).as("Missing bean of type %s", delegateClass).hasSize(1);
		assertThat(beans.get("feignClient").getDelegate()).isInstanceOf(delegateClass);
	}

	private void assertLoadBalancedWithRetries(ConfigurableApplicationContext context, Class delegateClass) {
		Map<String, RetryableFeignBlockingLoadBalancerClient> retryableBeans = context
				.getBeansOfType(RetryableFeignBlockingLoadBalancerClient.class);
		assertThat(retryableBeans).hasSize(1);
		Map<String, FeignBlockingLoadBalancerClient> beans = context
				.getBeansOfType(FeignBlockingLoadBalancerClient.class);
		assertThat(beans).isEmpty();
		assertThat(retryableBeans.get("feignRetryClient").getDelegate()).isInstanceOf(delegateClass);
	}

}
