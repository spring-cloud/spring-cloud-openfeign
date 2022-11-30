/*
 * Copyright 2013-2022 the original author or authors.
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

import java.lang.reflect.Field;
import java.util.stream.Stream;

import feign.Client;
import feign.Feign;
import feign.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * @author Matt King
 * @author Sam Kruglov
 * @author Felix Dittrich
 * @author Olga Maciaszek-Sharma
 */
class FeignBuilderCustomizerTests {

	private static final Targeter targeterSpy = spy(DefaultTargeter.class);

	private static final Client defaultClient = mock(Client.class);

	@Test
	void testBuilderCustomizer() {
		ArgumentCaptor<Feign.Builder> feignBuilderCaptor = ArgumentCaptor.forClass(Feign.Builder.class);
		doCallRealMethod().when(targeterSpy).target(any(), feignBuilderCaptor.capture(), any(), any());

		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				FeignBuilderCustomizerTests.SampleConfiguration2.class);

		FeignClientFactoryBean clientFactoryBean = context.getBean(FeignClientFactoryBean.class);
		clientFactoryBean.getTarget();

		Assertions.assertNotNull(feignBuilderCaptor.getValue());
		Feign.Builder builder = feignBuilderCaptor.getValue();
		assertFeignBuilderField(builder, "logLevel", Logger.Level.HEADERS);
		assertFeignBuilderField(builder, "dismiss404", true);

		context.close();
	}

	private void assertFeignBuilderField(Feign.Builder builder, String fieldName, Object expectedValue) {
		Field builderField = ReflectionUtils.findField(Feign.Builder.class, fieldName);
		ReflectionUtils.makeAccessible(builderField);

		Object value = ReflectionUtils.getField(builderField, builder);
		assertThat(value).as("Expected value for the field '" + fieldName + "':").isEqualTo(expectedValue);
	}

	@Test
	void testBuildCustomizerOrdered() {
		ArgumentCaptor<Feign.Builder> feignBuilderCaptor = ArgumentCaptor.forClass(Feign.Builder.class);
		doCallRealMethod().when(targeterSpy).target(any(), feignBuilderCaptor.capture(), any(), any());

		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				FeignBuilderCustomizerTests.SampleConfiguration3.class);

		FeignClientFactoryBean clientFactoryBean = context.getBean(FeignClientFactoryBean.class);
		clientFactoryBean.getTarget();

		Assertions.assertNotNull(feignBuilderCaptor.getValue());
		Feign.Builder builder = feignBuilderCaptor.getValue();
		assertFeignBuilderField(builder, "logLevel", Logger.Level.FULL);
		assertFeignBuilderField(builder, "dismiss404", true);

		context.close();
	}

	@Test
	void testBuildCustomizerOrderedWithAdditional() {
		ArgumentCaptor<Feign.Builder> feignBuilderCaptor = ArgumentCaptor.forClass(Feign.Builder.class);
		doCallRealMethod().when(targeterSpy).target(any(), feignBuilderCaptor.capture(), any(), any());

		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				FeignBuilderCustomizerTests.SampleConfiguration3.class);

		FeignClientFactoryBean clientFactoryBean = context.getBean(FeignClientFactoryBean.class);
		clientFactoryBean.addCustomizer(builder -> builder.logLevel(Logger.Level.BASIC));
		clientFactoryBean.addCustomizer(Feign.Builder::doNotCloseAfterDecode);
		clientFactoryBean.getTarget();

		Assertions.assertNotNull(feignBuilderCaptor.getValue());
		Feign.Builder builder = feignBuilderCaptor.getValue();
		assertFeignBuilderField(builder, "logLevel", Logger.Level.BASIC);
		assertFeignBuilderField(builder, "dismiss404", true);
		assertFeignBuilderField(builder, "closeAfterDecode", false);

		context.close();
	}

	@ParameterizedTest(name = "should use custom HttpClient with config: {0}")
	@MethodSource("testConfiguration")
	void testBuildCustomizerWithCustomHttpClient(Class configClass) {
		ArgumentCaptor<Feign.Builder> feignBuilderCaptor = ArgumentCaptor.forClass(Feign.Builder.class);
		doCallRealMethod().when(targeterSpy).target(any(), feignBuilderCaptor.capture(), any(), any());
		Client customClientMock = mock(Client.class);

		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(configClass);
		FeignClientFactoryBean clientFactoryBean = context.getBean(FeignClientFactoryBean.class);
		clientFactoryBean.addCustomizer(builder -> builder.client(customClientMock));
		clientFactoryBean.getTarget();

		Assertions.assertNotNull(feignBuilderCaptor.getValue());
		Feign.Builder builder = feignBuilderCaptor.getValue();
		assertFeignBuilderField(builder, "client", customClientMock);

		context.close();
	}

	private static FeignClientFactoryBean defaultFeignClientFactoryBean(String url) {
		FeignClientFactoryBean feignClientFactoryBean = new FeignClientFactoryBean();
		feignClientFactoryBean.setContextId("test");
		feignClientFactoryBean.setName("test");
		feignClientFactoryBean.setType(FeignClientFactoryTests.TestType.class);
		feignClientFactoryBean.setPath("");
		if (url != null) {
			feignClientFactoryBean.setUrl(url);
		}
		return feignClientFactoryBean;
	}

	private static Stream<Class> testConfiguration() {
		return Stream.of(SampleConfiguration3.class, LoadBalancedSampleConfiguration.class);
	}

	@Configuration(proxyBeanMethods = false)
	@Import(FeignClientsConfiguration.class)
	protected static class SampleConfiguration2 {

		@Bean
		FeignClientFactory feignContext() {
			return new FeignClientFactory();
		}

		@Bean
		FeignClientProperties feignClientProperties() {
			return new FeignClientProperties();
		}

		@Bean
		FeignBuilderCustomizer feignBuilderCustomizer() {
			return builder -> builder.logLevel(Logger.Level.HEADERS);
		}

		@Bean
		FeignBuilderCustomizer feignBuilderCustomizer2() {
			return Feign.Builder::dismiss404;
		}

		@Bean
		FeignClientFactoryBean feignClientFactoryBean() {
			return defaultFeignClientFactoryBean("http://some.absolute.url");
		}

		@Bean
		Targeter targeter() {
			return targeterSpy;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(FeignClientsConfiguration.class)
	protected static class SampleConfiguration3 {

		@Bean
		FeignClientFactory feignContext() {
			return new FeignClientFactory();
		}

		@Bean
		FeignClientProperties feignClientProperties() {
			return new FeignClientProperties();
		}

		@Bean
		@Order(1)
		FeignBuilderCustomizer feignBuilderCustomizer() {
			return builder -> builder.logLevel(Logger.Level.HEADERS);
		}

		@Bean
		@Order(2)
		FeignBuilderCustomizer feignBuilderCustomizer1() {
			return builder -> builder.logLevel(Logger.Level.FULL);
		}

		@Bean
		FeignBuilderCustomizer feignBuilderCustomizer2() {
			return Feign.Builder::dismiss404;
		}

		@Bean
		FeignClientFactoryBean feignClientFactoryBean() {
			return defaultFeignClientFactoryBean("http://some.absolute.url");
		}

		@Bean
		Targeter targeter() {
			return targeterSpy;
		}

		@Bean
		Client client() {
			return defaultClient;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(SampleConfiguration3.class)
	protected static class LoadBalancedSampleConfiguration {

		@Primary
		@Bean
		FeignClientFactoryBean feignClientFactoryBean() {
			return defaultFeignClientFactoryBean(null);
		}

	}

}
