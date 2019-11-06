/*
 * Copyright 2013-2019 the original author or authors.
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
import feign.httpclient.ApacheHttpClient;
import feign.okhttp.OkHttpClient;
import org.junit.jupiter.api.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.commons.httpclient.HttpClientConfiguration;
import org.springframework.cloud.loadbalancer.blocking.client.BlockingLoadBalancerClient;
import org.springframework.cloud.loadbalancer.config.BlockingLoadBalancerClientAutoConfiguration;
import org.springframework.cloud.loadbalancer.config.LoadBalancerAutoConfiguration;
import org.springframework.cloud.netflix.ribbon.RibbonAutoConfiguration;
import org.springframework.cloud.openfeign.ribbon.FeignRibbonClientAutoConfiguration;
import org.springframework.cloud.openfeign.ribbon.LoadBalancerFeignClient;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Olga Maciaszek-Sharma
 */
class FeignLoadBalancerAutoConfigurationTests {

	@Test
	void shouldInstantiateDefaultFeignBlockingLoadBalancerClientWhenHttpClientDisabled() {
		ConfigurableApplicationContext context = initContext(
				"spring.cloud.loadbalancer.ribbon.enabled=false",
				"feign.httpclient.enabled=false");
		assertThatOneBeanPresent(context, BlockingLoadBalancerClient.class);
		assertLoadBalanced(context, Client.Default.class);
		assertThatBeanNotPresent(context, LoadBalancerFeignClient.class);
	}

	@Test
	void shouldInstantiateHttpFeignClientWhenEnabled() {
		ConfigurableApplicationContext context = initContext(
				"spring.cloud.loadbalancer.ribbon.enabled=false");
		assertThatOneBeanPresent(context, BlockingLoadBalancerClient.class);
		assertLoadBalanced(context, ApacheHttpClient.class);
		assertThatBeanNotPresent(context, LoadBalancerFeignClient.class);
	}

	@Test
	void shouldInstantiateOkHttpFeignClientWhenEnabled() {
		ConfigurableApplicationContext context = initContext(
				"spring.cloud.loadbalancer.ribbon.enabled=false",
				"feign.httpclient.enabled=false", "feign.okhttp.enabled=true");
		assertThatOneBeanPresent(context, BlockingLoadBalancerClient.class);
		assertLoadBalanced(context, OkHttpClient.class);
		assertThatBeanNotPresent(context, LoadBalancerFeignClient.class);
	}

	@Test
	void shouldNotProcessLoadBalancerConfigurationWhenRibbonEnabled() {
		ConfigurableApplicationContext context = initContext(
				"spring.cloud.loadbalancer.ribbon.enabled=true");
		assertThatOneBeanPresent(context, LoadBalancerFeignClient.class);
		assertThatBeanNotPresent(context, BlockingLoadBalancerClient.class);
		assertThatBeanNotPresent(context, FeignBlockingLoadBalancerClient.class);
	}

	private ConfigurableApplicationContext initContext(String... properties) {
		return new SpringApplicationBuilder().web(WebApplicationType.NONE)
				.properties(properties)
				.sources(HttpClientConfiguration.class, RibbonAutoConfiguration.class,
						LoadBalancerAutoConfiguration.class,
						BlockingLoadBalancerClientAutoConfiguration.class,
						FeignRibbonClientAutoConfiguration.class,
						FeignLoadBalancerAutoConfiguration.class)
				.run();
	}

	private void assertThatOneBeanPresent(ConfigurableApplicationContext context,
			Class<?> beanClass) {
		Map<String, ?> beans = context.getBeansOfType(beanClass);
		assertThat(beans).hasSize(1);
	}

	private void assertLoadBalanced(ConfigurableApplicationContext context,
			Class delegateClass) {
		Map<String, FeignBlockingLoadBalancerClient> beans = context
				.getBeansOfType(FeignBlockingLoadBalancerClient.class);
		assertThat(beans).hasSize(1);
		assertThat(beans.get("feignClient").getDelegate()).isInstanceOf(delegateClass);
	}

	private void assertThatBeanNotPresent(ConfigurableApplicationContext context,
			Class<?> beanClass) {
		Map<String, ?> beans = context.getBeansOfType(beanClass);
		assertThat(beans).isEmpty();
	}

}
