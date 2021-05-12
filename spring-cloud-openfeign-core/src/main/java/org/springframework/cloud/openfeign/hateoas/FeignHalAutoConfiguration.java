/*
 * Copyright 2016-2021 the original author or authors.
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

import java.util.Collections;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.mediatype.hal.HalMediaTypeConfiguration;
import org.springframework.hateoas.server.mvc.TypeConstrainedMappingJackson2HttpMessageConverter;

import static org.springframework.hateoas.MediaTypes.HAL_JSON;

/**
 * @author Hector Espert
 * @author Olga Maciaszek-Sharma
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication
@ConditionalOnClass(RepresentationModel.class)
@AutoConfigureAfter({ JacksonAutoConfiguration.class,
		HttpMessageConvertersAutoConfiguration.class,
		RepositoryRestMvcAutoConfiguration.class })
public class FeignHalAutoConfiguration {

	@Bean
	@ConditionalOnBean(HalMediaTypeConfiguration.class)
	@ConditionalOnMissingBean
	public TypeConstrainedMappingJackson2HttpMessageConverter halJacksonHttpMessageConverter(
			ObjectProvider<ObjectMapper> objectMapper,
			HalMediaTypeConfiguration halConfiguration) {
		ObjectMapper mapper = objectMapper.getIfAvailable(ObjectMapper::new).copy();
		halConfiguration.configureObjectMapper(mapper);
		TypeConstrainedMappingJackson2HttpMessageConverter converter = new TypeConstrainedMappingJackson2HttpMessageConverter(
				RepresentationModel.class);
		converter.setSupportedMediaTypes(Collections.singletonList(HAL_JSON));
		converter.setObjectMapper(mapper);
		return converter;
	}

}
