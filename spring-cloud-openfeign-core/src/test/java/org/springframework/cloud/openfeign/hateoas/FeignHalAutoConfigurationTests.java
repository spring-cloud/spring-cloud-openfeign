/*
 * Copyright 2016-2020 the original author or authors.
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.hateoas.mediatype.MessageResolver;
import org.springframework.hateoas.mediatype.hal.CurieProvider;
import org.springframework.hateoas.mediatype.hal.HalConfiguration;
import org.springframework.hateoas.mediatype.hal.HalMediaTypeConfiguration;
import org.springframework.hateoas.mediatype.hal.Jackson2HalModule;
import org.springframework.hateoas.server.LinkRelationProvider;
import org.springframework.hateoas.server.mvc.TypeConstrainedMappingJackson2HttpMessageConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.hateoas.MediaTypes.HAL_JSON;

/**
 * @author Hector Espert
 */
@RunWith(MockitoJUnitRunner.class)
public class FeignHalAutoConfigurationTests {

	@Mock
	private ObjectProvider<HalConfiguration> halConfiguration;

	@Mock
	private ObjectProvider<ObjectMapper> objectMapper;

	@Mock
	private LinkRelationProvider relProvider;

	@Mock
	private ObjectProvider<CurieProvider> curieProvider;

	@Mock
	private MessageResolver messageResolver;

	@InjectMocks
	private FeignHalAutoConfiguration feignHalAutoConfiguration;

	@Test
	public void halJacksonHttpMessageConverter() {
		ObjectMapper mapper = new ObjectMapper();
		when(objectMapper.getIfAvailable(any())).thenReturn(mapper);

		when(halConfiguration.getIfAvailable(any()))
				.thenReturn(mock(HalConfiguration.class));
		when(curieProvider.getIfAvailable(any())).thenReturn(mock(CurieProvider.class));

		HalMediaTypeConfiguration halMediaTypeConfiguration = new HalMediaTypeConfiguration(
				relProvider, curieProvider, halConfiguration, messageResolver,
				new DefaultListableBeanFactory());

		TypeConstrainedMappingJackson2HttpMessageConverter converter = feignHalAutoConfiguration
				.halJacksonHttpMessageConverter(objectMapper, halMediaTypeConfiguration);

		assertThat(converter).isNotNull();
		assertThat(converter.getObjectMapper()).isNotNull();
		assertThat(converter.getSupportedMediaTypes())
				.isEqualTo(Collections.singletonList(HAL_JSON));

		assertThat(Jackson2HalModule.isAlreadyRegisteredIn(converter.getObjectMapper()))
				.isTrue();
	}

}
