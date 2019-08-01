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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.hateoas.mediatype.MessageResolver;
import org.springframework.hateoas.mediatype.hal.CurieProvider;
import org.springframework.hateoas.mediatype.hal.HalConfiguration;
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
	private ObjectProvider<ObjectMapper> objectMapper;

	@Mock
	private ObjectProvider<HalConfiguration> halConfiguration;

	@Mock
	private ObjectProvider<LinkRelationProvider> relProvider;

	@Mock
	private ObjectProvider<CurieProvider> curieProvider;

	@Mock
	private ObjectProvider<MessageResolver> messageResolver;

	@InjectMocks
	private FeignHalAutoConfiguration feignHalAutoConfiguration;

	@Test
	public void halJacksonHttpMessageConverter() {
		ObjectMapper mapper = new ObjectMapper();
		when(objectMapper.getIfAvailable(any())).thenReturn(mapper);

		when(halConfiguration.getIfAvailable(any()))
				.thenReturn(mock(HalConfiguration.class));
		when(relProvider.getIfAvailable()).thenReturn(mock(LinkRelationProvider.class));
		when(curieProvider.getIfAvailable(any())).thenReturn(mock(CurieProvider.class));
		when(messageResolver.getIfAvailable()).thenReturn(mock(MessageResolver.class));

		TypeConstrainedMappingJackson2HttpMessageConverter converter = feignHalAutoConfiguration
				.halJacksonHttpMessageConverter(objectMapper, halConfiguration,
						messageResolver, curieProvider, relProvider);

		assertThat(converter).isNotNull();
		assertThat(converter.getObjectMapper()).isNotNull();
		assertThat(converter.getSupportedMediaTypes()).isEqualTo(Arrays.asList(HAL_JSON));

		assertThat(Jackson2HalModule.isAlreadyRegisteredIn(converter.getObjectMapper()))
				.isTrue();
	}

}
