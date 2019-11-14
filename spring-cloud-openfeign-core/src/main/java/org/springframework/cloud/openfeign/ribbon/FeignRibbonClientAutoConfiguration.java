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

package org.springframework.cloud.openfeign.ribbon;

import com.netflix.loadbalancer.ILoadBalancer;
import feign.Feign;
import feign.Request;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryFactory;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.cloud.openfeign.support.FeignHttpClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

/**
 * Autoconfiguration to be activated if Feign is in use and needs to be use Ribbon as a
 * load balancer.
 *
 * @author Dave Syer
 * @author Olga Maciaszek-Sharma
 */
@ConditionalOnClass({ ILoadBalancer.class, Feign.class })
@ConditionalOnProperty(value = "spring.cloud.loadbalancer.ribbon.enabled",
		matchIfMissing = true)
@Configuration(proxyBeanMethods = false)
@AutoConfigureBefore(FeignAutoConfiguration.class)
@EnableConfigurationProperties({ FeignHttpClientProperties.class })
// Order is important here, last should be the default, first should be optional
// see
// https://github.com/spring-cloud/spring-cloud-netflix/issues/2086#issuecomment-316281653
@Import({ HttpClientFeignLoadBalancedConfiguration.class,
		OkHttpFeignLoadBalancedConfiguration.class,
		DefaultFeignLoadBalancedConfiguration.class })
public class FeignRibbonClientAutoConfiguration {

	@Bean
	@Primary
	@ConditionalOnMissingBean
	@ConditionalOnMissingClass("org.springframework.retry.support.RetryTemplate")
	public CachingSpringLoadBalancerFactory cachingLBClientFactory(
			SpringClientFactory factory) {
		return new CachingSpringLoadBalancerFactory(factory);
	}

	@Bean
	@Primary
	@ConditionalOnMissingBean
	@ConditionalOnClass(name = "org.springframework.retry.support.RetryTemplate")
	public CachingSpringLoadBalancerFactory retryabeCachingLBClientFactory(
			SpringClientFactory factory, LoadBalancedRetryFactory retryFactory) {
		return new CachingSpringLoadBalancerFactory(factory, retryFactory);
	}

	@Bean
	@ConditionalOnMissingBean
	public Request.Options feignRequestOptions() {
		return LoadBalancerFeignClient.DEFAULT_OPTIONS;
	}

}
