/*
 * Copyright 2021-2022 the original author or authors.
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

package org.springframework.cloud.openfeign;

import java.util.HashMap;

import org.assertj.core.util.Maps;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Jonatan Ivanov
 */
@ExtendWith({ MockitoExtension.class })
class FeignClientMicrometerEnabledConditionTests {

	@Mock
	private ConditionContext context;

	@Mock
	private AnnotatedTypeMetadata metadata;

	@Mock
	private ConfigurableListableBeanFactory beanFactory;

	@Mock
	private ObjectProvider<FeignClientProperties> beanProvider;

	@Mock
	private Environment environment;

	private final FeignClientMicrometerEnabledCondition condition = new FeignClientMicrometerEnabledCondition();

	@BeforeEach
	void setUp() {
		when(context.getBeanFactory()).thenReturn(beanFactory);
		when(beanFactory.getBeanProvider(FeignClientProperties.class)).thenReturn(beanProvider);
	}

	@AfterEach
	void tearDown() {
		verify(context).getBeanFactory();
		verify(beanFactory).getBeanProvider(FeignClientProperties.class);
		verify(beanProvider).getIfAvailable();
	}

	@Test
	void shouldMatchWhenFeignClientPropertiesBeanIsMissing() {
		when(beanProvider.getIfAvailable()).thenReturn(null);

		assertThat(condition.matches(context, metadata)).isTrue();
		verify(environment, never()).getProperty("spring.cloud.openfeign.client.name");
	}

	@Test
	void shouldMatchWhenConfigMapIsMissing() {
		FeignClientProperties feignClientProperties = mock(FeignClientProperties.class);
		when(beanProvider.getIfAvailable()).thenReturn(feignClientProperties);
		when(feignClientProperties.getConfig()).thenReturn(null);

		assertThat(condition.matches(context, metadata)).isTrue();
		verify(environment, never()).getProperty("spring.cloud.openfeign.client.name");
	}

	@Test
	void shouldMatchWhenConfigMapDoesNotContainTheConfig() {
		FeignClientProperties feignClientProperties = mock(FeignClientProperties.class);
		when(beanProvider.getIfAvailable()).thenReturn(feignClientProperties);
		when(context.getEnvironment()).thenReturn(environment);
		when(environment.getProperty("spring.cloud.openfeign.client.name")).thenReturn("foo");
		when(feignClientProperties.getConfig()).thenReturn(new HashMap<>());

		assertThat(condition.matches(context, metadata)).isTrue();
		verify(environment).getProperty("spring.cloud.openfeign.client.name");
	}

	@Test
	void shouldMatchWhenClientNameIsNull() {
		FeignClientProperties feignClientProperties = mock(FeignClientProperties.class);
		when(beanProvider.getIfAvailable()).thenReturn(feignClientProperties);
		when(context.getEnvironment()).thenReturn(environment);
		when(environment.getProperty("spring.cloud.openfeign.client.name")).thenReturn(null);
		when(feignClientProperties.getConfig()).thenReturn(new HashMap<>());

		assertThat(condition.matches(context, metadata)).isTrue();
		verify(environment).getProperty("spring.cloud.openfeign.client.name");
	}

	@Test
	void shouldMatchWhenClientNameIsEmpty() {
		FeignClientProperties feignClientProperties = mock(FeignClientProperties.class);
		when(beanProvider.getIfAvailable()).thenReturn(feignClientProperties);
		when(context.getEnvironment()).thenReturn(environment);
		when(environment.getProperty("spring.cloud.openfeign.client.name")).thenReturn("");
		when(feignClientProperties.getConfig()).thenReturn(new HashMap<>());

		assertThat(condition.matches(context, metadata)).isTrue();

		verify(environment).getProperty("spring.cloud.openfeign.client.name");
	}

	@Test
	void shouldMatchWhenConfigMapContainsNullConfig() {
		FeignClientProperties feignClientProperties = mock(FeignClientProperties.class);
		when(beanProvider.getIfAvailable()).thenReturn(feignClientProperties);
		when(context.getEnvironment()).thenReturn(environment);
		when(environment.getProperty("spring.cloud.openfeign.client.name")).thenReturn("foo");
		when(feignClientProperties.getConfig()).thenReturn(Maps.newHashMap("foo", null));

		assertThat(condition.matches(context, metadata)).isTrue();
		verify(environment).getProperty("spring.cloud.openfeign.client.name");
	}

	@Test
	void shouldMatchWhenMicrometerConfigurationIsMissing() {
		FeignClientProperties feignClientProperties = mock(FeignClientProperties.class);
		FeignClientProperties.FeignClientConfiguration feignClientConfig = mock(
				FeignClientProperties.FeignClientConfiguration.class);
		when(beanProvider.getIfAvailable()).thenReturn(feignClientProperties);
		when(context.getEnvironment()).thenReturn(environment);
		when(environment.getProperty("spring.cloud.openfeign.client.name")).thenReturn("foo");
		when(feignClientProperties.getConfig()).thenReturn(Maps.newHashMap("foo", feignClientConfig));
		when(feignClientConfig.getMicrometer()).thenReturn(null);

		assertThat(condition.matches(context, metadata)).isTrue();
		verify(environment).getProperty("spring.cloud.openfeign.client.name");
	}

	@Test
	void shouldMatchWhenEnabledFlagIsNotSet() {
		FeignClientProperties feignClientProperties = mock(FeignClientProperties.class);
		FeignClientProperties.FeignClientConfiguration feignClientConfig = mock(
				FeignClientProperties.FeignClientConfiguration.class);
		when(beanProvider.getIfAvailable()).thenReturn(feignClientProperties);
		when(context.getEnvironment()).thenReturn(environment);
		when(environment.getProperty("spring.cloud.openfeign.client.name")).thenReturn("foo");
		when(feignClientProperties.getConfig()).thenReturn(Maps.newHashMap("foo", feignClientConfig));
		when(feignClientConfig.getMicrometer()).thenReturn(new FeignClientProperties.MicrometerProperties());

		assertThat(condition.matches(context, metadata)).isTrue();
		verify(environment).getProperty("spring.cloud.openfeign.client.name");
	}

	@Test
	void shouldMatchWhenEnabledFlagIsNull() {
		FeignClientProperties feignClientProperties = mock(FeignClientProperties.class);
		FeignClientProperties.FeignClientConfiguration feignClientConfig = mock(
				FeignClientProperties.FeignClientConfiguration.class);
		when(beanProvider.getIfAvailable()).thenReturn(feignClientProperties);
		when(context.getEnvironment()).thenReturn(environment);
		when(environment.getProperty("spring.cloud.openfeign.client.name")).thenReturn("foo");
		when(feignClientProperties.getConfig()).thenReturn(Maps.newHashMap("foo", feignClientConfig));
		FeignClientProperties.MicrometerProperties micrometer = new FeignClientProperties.MicrometerProperties();
		micrometer.setEnabled(null);
		when(feignClientConfig.getMicrometer()).thenReturn(micrometer);

		assertThat(condition.matches(context, metadata)).isTrue();
		verify(environment).getProperty("spring.cloud.openfeign.client.name");
	}

	@Test
	void shouldMatchWhenMicrometerConfigurationIsEnabled() {
		FeignClientProperties feignClientProperties = mock(FeignClientProperties.class);
		FeignClientProperties.FeignClientConfiguration feignClientConfig = mock(
				FeignClientProperties.FeignClientConfiguration.class);
		when(beanProvider.getIfAvailable()).thenReturn(feignClientProperties);
		when(context.getEnvironment()).thenReturn(environment);
		when(environment.getProperty("spring.cloud.openfeign.client.name")).thenReturn("foo");
		when(feignClientProperties.getConfig()).thenReturn(Maps.newHashMap("foo", feignClientConfig));
		FeignClientProperties.MicrometerProperties micrometer = new FeignClientProperties.MicrometerProperties();
		micrometer.setEnabled(true);
		when(feignClientConfig.getMicrometer()).thenReturn(micrometer);

		assertThat(condition.matches(context, metadata)).isTrue();
		verify(environment).getProperty("spring.cloud.openfeign.client.name");
	}

	@Test
	void shouldNotMatchWhenMicrometerConfigurationIsEnabled() {
		FeignClientProperties feignClientProperties = mock(FeignClientProperties.class);
		FeignClientProperties.FeignClientConfiguration feignClientConfig = mock(
				FeignClientProperties.FeignClientConfiguration.class);
		when(beanProvider.getIfAvailable()).thenReturn(feignClientProperties);
		when(context.getEnvironment()).thenReturn(environment);
		when(environment.getProperty("spring.cloud.openfeign.client.name")).thenReturn("foo");
		when(feignClientProperties.getConfig()).thenReturn(Maps.newHashMap("foo", feignClientConfig));
		FeignClientProperties.MicrometerProperties micrometer = new FeignClientProperties.MicrometerProperties();
		micrometer.setEnabled(false);
		when(feignClientConfig.getMicrometer()).thenReturn(micrometer);

		assertThat(condition.matches(context, metadata)).isFalse();
		verify(environment).getProperty("spring.cloud.openfeign.client.name");
	}

}
