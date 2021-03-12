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

package org.springframework.cloud.openfeign.async;

import feign.AsyncFeign;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.FeignClientProperties;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.cloud.openfeign.support.FeignEncoderProperties;
import org.springframework.cloud.openfeign.support.FeignHttpClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;

@ConditionalOnClass(AsyncFeign.class)
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ FeignClientProperties.class,
		FeignHttpClientProperties.class, FeignEncoderProperties.class })
@Import(FeignClientsConfiguration.class)
public class AsyncFeignAutoConfiguration {

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	@ConditionalOnMissingBean
	public AsyncFeign.AsyncBuilder asyncFeignBuilder() {
		return AsyncFeign.asyncBuilder();
	}

	@Configuration(proxyBeanMethods = false)
	protected static class DefaultAsyncFeignTargeterConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public AsyncTargeter asyncTargeter() {
			return new DefaultAsyncTargeter();
		}

	}

}
