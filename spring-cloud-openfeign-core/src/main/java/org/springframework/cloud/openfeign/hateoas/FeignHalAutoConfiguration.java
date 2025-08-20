/*
 * Copyright 2016-present the original author or authors.
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

package org.springframework.cloud.openfeign.hateoas;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.cloud.openfeign.support.HttpMessageConverterCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.config.WebConverters;

/**
 * @author Hector Espert
 * @author Olga Maciaszek-Sharma
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication
@ConditionalOnClass(WebConverters.class)
@AutoConfigureAfter(name = { "org.springframework.boot.data.rest.autoconfigure.RepositoryRestMvcAutoConfiguration",
		"org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration",
		"org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration" })
public class FeignHalAutoConfiguration {

	@Bean
	@ConditionalOnBean(WebConverters.class)
	HttpMessageConverterCustomizer webConvertersCustomizer(WebConverters webConverters) {
		return new WebConvertersCustomizer(webConverters);
	}

}
