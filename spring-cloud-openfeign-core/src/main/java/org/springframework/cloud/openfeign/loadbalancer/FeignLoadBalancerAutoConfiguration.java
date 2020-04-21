/*
 * Copyright 2013-2020 the original author or authors.
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

import feign.Client;
import feign.Feign;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.cloud.openfeign.support.FeignHttpClientProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * An autoconfiguration that instantiates {@link LoadBalancerClient}-based implementations
 * of {@link Client}.
 *
 * @author Olga Maciaszek-Sharma
 * @since 2.2.0
 */
@ConditionalOnClass(Feign.class)
@ConditionalOnBean(LoadBalancerClient.class)
@AutoConfigureBefore(FeignAutoConfiguration.class)
@EnableConfigurationProperties(FeignHttpClientProperties.class)
@Configuration(proxyBeanMethods = false)
// Order is important here, last should be the default, first should be optional
// see
// https://github.com/spring-cloud/spring-cloud-netflix/issues/2086#issuecomment-316281653
@Import({ HttpClientFeignLoadBalancerConfiguration.class,
		OkHttpFeignLoadBalancerConfiguration.class,
		DefaultFeignLoadBalancerConfiguration.class })
public class FeignLoadBalancerAutoConfiguration {

}
