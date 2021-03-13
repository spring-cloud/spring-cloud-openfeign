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

import feign.AsyncClient;
import feign.AsyncFeign;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * An autoconfiguration that instantiates implementations of {@link AsyncClient} and
 * implementations of {@link AsyncTargeter}.
 *
 * @author Nguyen Ky Thanh
 */
@ConditionalOnClass(AsyncFeign.class)
@Configuration(proxyBeanMethods = false)
public class AsyncFeignAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	protected static class DefaultAsyncFeignTargeterConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public AsyncTargeter asyncTargeter() {
			return new DefaultAsyncTargeter();
		}

	}

}
