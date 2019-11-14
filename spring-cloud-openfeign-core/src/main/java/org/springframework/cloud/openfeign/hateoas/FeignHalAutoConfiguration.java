/*
 * Copyright 2016-2019 the original author or authors.
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

import java.util.Arrays;
import java.util.Collections;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.hateoas.HypermediaAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.mediatype.MessageResolver;
import org.springframework.hateoas.mediatype.hal.CurieProvider;
import org.springframework.hateoas.mediatype.hal.DefaultCurieProvider;
import org.springframework.hateoas.mediatype.hal.HalConfiguration;
import org.springframework.hateoas.mediatype.hal.Jackson2HalModule;
import org.springframework.hateoas.server.LinkRelationProvider;
import org.springframework.hateoas.server.mvc.TypeConstrainedMappingJackson2HttpMessageConverter;

import static org.springframework.hateoas.MediaTypes.HAL_JSON;

/**
 * @author Hector Espert
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication
@ConditionalOnClass(RepresentationModel.class)
@AutoConfigureAfter({ JacksonAutoConfiguration.class,
		HttpMessageConvertersAutoConfiguration.class,
		RepositoryRestMvcAutoConfiguration.class })
@AutoConfigureBefore(HypermediaAutoConfiguration.class)
public class FeignHalAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public TypeConstrainedMappingJackson2HttpMessageConverter halJacksonHttpMessageConverter(
			ObjectProvider<ObjectMapper> objectMapper,
			ObjectProvider<HalConfiguration> halConfiguration,
			ObjectProvider<MessageResolver> messageResolver,
			ObjectProvider<CurieProvider> curieProvider,
			ObjectProvider<LinkRelationProvider> linkRelationProvider) {

		ObjectMapper mapper = objectMapper.getIfAvailable(ObjectMapper::new).copy();
		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

		HalConfiguration configuration = halConfiguration
				.getIfAvailable(HalConfiguration::new);

		CurieProvider curieProviderInstance = curieProvider
				.getIfAvailable(() -> new DefaultCurieProvider(Collections.emptyMap()));

		Jackson2HalModule.HalHandlerInstantiator halHandlerInstantiator = new Jackson2HalModule.HalHandlerInstantiator(
				linkRelationProvider.getIfAvailable(), curieProviderInstance,
				messageResolver.getIfAvailable(), configuration);

		mapper.setHandlerInstantiator(halHandlerInstantiator);

		if (!Jackson2HalModule.isAlreadyRegisteredIn(mapper)) {
			Jackson2HalModule halModule = new Jackson2HalModule();
			mapper.registerModule(halModule);
		}

		TypeConstrainedMappingJackson2HttpMessageConverter converter = new TypeConstrainedMappingJackson2HttpMessageConverter(
				RepresentationModel.class);
		converter.setSupportedMediaTypes(Arrays.asList(HAL_JSON));
		converter.setObjectMapper(mapper);
		return converter;
	}

}
