/*
 * Copyright 2013-present the original author or authors.
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

import java.net.http.HttpClient;
import java.util.Map;

import feign.Client;
import feign.hc5.ApacheHttp5Client;
import feign.http2client.Http2Client;
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
 * @author changjin wei(魏昌进)
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
	void shouldInstantiateHttp2ClientFeignClientWhenEnabled() {
		ConfigurableApplicationContext context = initContext("spring.cloud.openfeign.httpclient.hc5.enabled=false",
				"spring.cloud.openfeign.http2client.enabled=true", "spring.cloud.loadbalancer.retry.enabled=false");
		assertThatOneBeanPresent(context, BlockingLoadBalancerClient.class);
		Map<String, FeignBlockingLoadBalancerClient> beans = context
			.getBeansOfType(FeignBlockingLoadBalancerClient.class);
		assertThat(beans).as("Missing bean of type %s", Http2Client.class).hasSize(1);
		Client client = beans.get("feignClient").getDelegate();
		assertThat(client).isInstanceOf(Http2Client.class);
		Http2Client http2Client = (Http2Client) client;
		HttpClient httpClient = (HttpClient) getField(http2Client, "client");
		assertThat(httpClient).isInstanceOf(HttpClient.class);
	}

	@Test
	void shouldInstantiateHttpFeignClient5WhenAvailableAndOkHttpDisabled() {
		ConfigurableApplicationContext context = initContext("spring.cloud.loadbalancer.retry.enabled=false");
		assertThatOneBeanPresent(context, BlockingLoadBalancerClient.class);
		assertLoadBalanced(context, ApacheHttp5Client.class);
	}

	@Test
	void shouldInstantiateHttpFeignClient5WhenAvailableAndHttp2ClientDisabled() {
		ConfigurableApplicationContext context = initContext("spring.cloud.openfeign.http2client.enabled=false",
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
	void shouldInstantiateRetryableHttp2ClientFeignClientWhenEnabled() {
		ConfigurableApplicationContext context = initContext("spring.cloud.openfeign.httpclient.hc5.enabled=false",
				"spring.cloud.openfeign.http2client.enabled=true");
		assertThatOneBeanPresent(context, BlockingLoadBalancerClient.class);
		assertLoadBalancedWithRetries(context, Http2Client.class);
	}

	@Test
	void shouldInstantiateRetryableHttpFeignClient5WhenEnabled() {
		ConfigurableApplicationContext context = initContext();
		assertThatOneBeanPresent(context, BlockingLoadBalancerClient.class);
		assertLoadBalancedWithRetries(context, ApacheHttp5Client.class);
	}

	private ConfigurableApplicationContext initContext(String... properties) {
		return new SpringApplicationBuilder().web(WebApplicationType.NONE)
			.properties(properties)
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
