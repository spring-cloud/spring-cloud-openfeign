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

package org.springframework.cloud.openfeign.loadbalancer;

import java.util.Map;

import feign.Client;
import feign.hc5.ApacheHttp5Client;
import feign.httpclient.ApacheHttpClient;
import feign.okhttp.OkHttpClient;
import org.junit.jupiter.api.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.commons.httpclient.HttpClientConfiguration;
import org.springframework.cloud.loadbalancer.blocking.client.BlockingLoadBalancerClient;
import org.springframework.cloud.loadbalancer.config.BlockingLoadBalancerClientAutoConfiguration;
import org.springframework.cloud.loadbalancer.config.LoadBalancerAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Olga Maciaszek-Sharma
 * @author Nguyen Ky Thanh
 */
class FeignLoadBalancerAutoConfigurationTests {

	@Test
	void shouldInstantiateDefaultFeignBlockingLoadBalancerClientWhenHttpClientDisabled() {
		ConfigurableApplicationContext context = initContext("feign.httpclient.enabled=false",
				"spring.cloud.loadbalancer.retry.enabled=false");
		assertThatOneBeanPresent(context, BlockingLoadBalancerClient.class);
		assertLoadBalanced(context, Client.Default.class);
	}

	@Test
	void shouldInstantiateHttpFeignClientWhenEnabled() {
		ConfigurableApplicationContext context = initContext("spring.cloud.loadbalancer.retry.enabled=false");
		assertThatOneBeanPresent(context, BlockingLoadBalancerClient.class);
		assertLoadBalanced(context, ApacheHttpClient.class);
	}

	@Test
	void shouldInstantiateOkHttpFeignClientWhenEnabled() {
		ConfigurableApplicationContext context = initContext("feign.httpclient.enabled=false",
				"feign.okhttp.enabled=true", "spring.cloud.loadbalancer.retry.enabled=false");
		assertThatOneBeanPresent(context, BlockingLoadBalancerClient.class);
		assertLoadBalanced(context, OkHttpClient.class);
	}

	@Test
	void shouldInstantiateHttpFeignClient5WhenEnabled() {
		ConfigurableApplicationContext context = initContext("feign.httpclient.enabled=false",
				"feign.okhttp.enabled=false", "feign.httpclient.hc5.enabled=true",
				"spring.cloud.loadbalancer.retry.enabled=false");
		assertThatOneBeanPresent(context, BlockingLoadBalancerClient.class);
		assertLoadBalanced(context, ApacheHttp5Client.class);
	}

	@Test
	void shouldInstantiateHttpFeignClient5WhenBothHttpClientAndHttpClient5Enabled() {
		ConfigurableApplicationContext context = initContext("feign.httpclient.enabled=true",
				"feign.okhttp.enabled=false", "feign.httpclient.hc5.enabled=true",
				"spring.cloud.loadbalancer.retry.enabled=false");
		assertThatOneBeanPresent(context, BlockingLoadBalancerClient.class);
		assertLoadBalanced(context, ApacheHttp5Client.class);
	}

	@Test
	void shouldInstantiateRetryableDefaultFeignBlockingLoadBalancerClientWhenHttpClientDisabled() {
		ConfigurableApplicationContext context = initContext("feign.httpclient.enabled=false");
		assertThatOneBeanPresent(context, BlockingLoadBalancerClient.class);
		assertLoadBalancedWithRetries(context, Client.Default.class);
	}

	@Test
	void shouldInstantiateRetryableHttpFeignClientWhenEnabled() {
		ConfigurableApplicationContext context = initContext();
		assertThatOneBeanPresent(context, BlockingLoadBalancerClient.class);
		assertLoadBalancedWithRetries(context, ApacheHttpClient.class);
	}

	@Test
	void shouldInstantiateRetryableOkHttpFeignClientWhenEnabled() {
		ConfigurableApplicationContext context = initContext("feign.httpclient.enabled=false",
				"feign.okhttp.enabled=true");
		assertThatOneBeanPresent(context, BlockingLoadBalancerClient.class);
		assertLoadBalancedWithRetries(context, OkHttpClient.class);
	}

	@Test
	void shouldInstantiateRetryableHttpFeignClient5WhenEnabled() {
		ConfigurableApplicationContext context = initContext("feign.httpclient.enabled=false",
				"feign.okhttp.enabled=false", "feign.httpclient.hc5.enabled=true");
		assertThatOneBeanPresent(context, BlockingLoadBalancerClient.class);
		assertLoadBalancedWithRetries(context, ApacheHttp5Client.class);
	}

	@Test
	void shouldInstantiateRetryableHttpFeignClient5WhenBothHttpClientAndHttpClient5Enabled() {
		ConfigurableApplicationContext context = initContext("feign.httpclient.enabled=true",
				"feign.okhttp.enabled=false", "feign.httpclient.hc5.enabled=true");
		assertThatOneBeanPresent(context, BlockingLoadBalancerClient.class);
		assertLoadBalancedWithRetries(context, ApacheHttp5Client.class);
	}

	private ConfigurableApplicationContext initContext(String... properties) {
		return new SpringApplicationBuilder().web(WebApplicationType.NONE).properties(properties)
				.sources(HttpClientConfiguration.class, LoadBalancerAutoConfiguration.class,
						BlockingLoadBalancerClientAutoConfiguration.class, FeignLoadBalancerAutoConfiguration.class)
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
