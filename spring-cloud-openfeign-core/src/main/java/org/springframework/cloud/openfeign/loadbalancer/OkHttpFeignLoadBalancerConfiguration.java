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

import java.util.List;

import feign.Client;
import feign.okhttp.OkHttpClient;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClientsProperties;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration instantiating a {@link LoadBalancerClient}-based {@link Client} object
 * that uses {@link OkHttpClient} under the hood.
 *
 * @author Olga Maciaszek-Sharma
 * @author changjin wei(魏昌进)
 * @since 2.2.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(OkHttpClient.class)
@ConditionalOnProperty("spring.cloud.openfeign.okhttp.enabled")
@ConditionalOnBean({ LoadBalancerClient.class, LoadBalancerClientFactory.class })
@EnableConfigurationProperties(LoadBalancerClientsProperties.class)
class OkHttpFeignLoadBalancerConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@Conditional(OnRetryNotEnabledCondition.class)
	public Client feignClient(okhttp3.OkHttpClient okHttpClient, LoadBalancerClient loadBalancerClient,
			LoadBalancerClientFactory loadBalancerClientFactory,
			List<LoadBalancerFeignRequestTransformer> transformers) {
		OkHttpClient delegate = new OkHttpClient(okHttpClient);
		return new FeignBlockingLoadBalancerClient(delegate, loadBalancerClient, loadBalancerClientFactory,
				transformers);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnClass(name = "org.springframework.retry.support.RetryTemplate")
	@ConditionalOnBean(LoadBalancedRetryFactory.class)
	@ConditionalOnProperty(value = "spring.cloud.loadbalancer.retry.enabled", havingValue = "true",
			matchIfMissing = true)
	public Client feignRetryClient(LoadBalancerClient loadBalancerClient, okhttp3.OkHttpClient okHttpClient,
			LoadBalancedRetryFactory loadBalancedRetryFactory, LoadBalancerClientFactory loadBalancerClientFactory,
			List<LoadBalancerFeignRequestTransformer> transformers) {
		OkHttpClient delegate = new OkHttpClient(okHttpClient);
		return new RetryableFeignBlockingLoadBalancerClient(delegate, loadBalancerClient, loadBalancedRetryFactory,
				loadBalancerClientFactory, transformers);
	}

}
