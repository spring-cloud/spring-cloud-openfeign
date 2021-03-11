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

package org.springframework.cloud.openfeign.ribbon;

import feign.Client;
import feign.hc5.ApacheHttp5Client;
import org.apache.hc.client5.http.classic.HttpClient;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.openfeign.clientconfig.HttpClient5FeignConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configuration instantiating a {@link LoadBalancerFeignClient}-based {@link Client}
 * object that uses {@link ApacheHttp5Client} under the hood.
 *
 * @author Nguyen Ky Thanh
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(ApacheHttp5Client.class)
@ConditionalOnProperty(value = "feign.httpclient.hc5.enabled", havingValue = "true")
@Import(HttpClient5FeignConfiguration.class)
class HttpClient5FeignLoadBalancedConfiguration {

	@Bean
	@ConditionalOnMissingBean(Client.class)
	public Client feignClient(CachingSpringLoadBalancerFactory cachingFactory,
			SpringClientFactory clientFactory, HttpClient httpClient5) {
		Client delegate = new ApacheHttp5Client(httpClient5);
		return new LoadBalancerFeignClient(delegate, cachingFactory, clientFactory);
	}

}
