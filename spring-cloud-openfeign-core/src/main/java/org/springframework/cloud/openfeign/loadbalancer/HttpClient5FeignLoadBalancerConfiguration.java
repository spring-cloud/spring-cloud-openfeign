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

import java.util.List;

import feign.Client;
import feign.hc5.ApacheHttp5Client;
import org.apache.hc.client5.http.classic.HttpClient;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryFactory;
import org.springframework.cloud.loadbalancer.blocking.client.BlockingLoadBalancerClient;
import org.springframework.cloud.openfeign.clientconfig.HttpClient5FeignConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

/**
 * Configuration instantiating a {@link BlockingLoadBalancerClient}-based {@link Client}
 * object that uses {@link ApacheHttp5Client} under the hood.
 *
 * @author Nguyen Ky Thanh
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(ApacheHttp5Client.class)
@ConditionalOnBean(BlockingLoadBalancerClient.class)
@ConditionalOnProperty(value = "feign.httpclient.hc5.enabled", havingValue = "true")
@Import(HttpClient5FeignConfiguration.class)
class HttpClient5FeignLoadBalancerConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@Conditional(OnRetryNotEnabledCondition.class)
	public Client feignClient(BlockingLoadBalancerClient loadBalancerClient,
			HttpClient httpClient5) {
		Client delegate = new ApacheHttp5Client(httpClient5);
		return new FeignBlockingLoadBalancerClient(delegate, loadBalancerClient);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnClass(name = "org.springframework.retry.support.RetryTemplate")
	@ConditionalOnBean(LoadBalancedRetryFactory.class)
	@ConditionalOnProperty(value = "spring.cloud.loadbalancer.retry.enabled",
			havingValue = "true", matchIfMissing = true)
	public Client feignRetryClient(BlockingLoadBalancerClient loadBalancerClient,
			HttpClient httpClient5,
			List<LoadBalancedRetryFactory> loadBalancedRetryFactories) {
		AnnotationAwareOrderComparator.sort(loadBalancedRetryFactories);
		Client delegate = new ApacheHttp5Client(httpClient5);
		return new RetryableFeignBlockingLoadBalancerClient(delegate, loadBalancerClient,
				loadBalancedRetryFactories.get(0));
	}

}
