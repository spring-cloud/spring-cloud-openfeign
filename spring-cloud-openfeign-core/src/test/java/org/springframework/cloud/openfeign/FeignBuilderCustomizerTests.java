/*
 * Copyright 2013-2019 the original author or authors.
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

import feign.Feign;
import feign.Logger;
import org.junit.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Matt King
 * @author Sam Kruglov
 */
public class FeignBuilderCustomizerTests {

	@Test
	public void testBuilderCustomizer() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				FeignBuilderCustomizerTests.SampleConfiguration2.class);

		FeignClientFactoryBean clientFactoryBean = context
				.getBean(FeignClientFactoryBean.class);
		FeignContext feignContext = context.getBean(FeignContext.class);

		Feign.Builder builder = clientFactoryBean.feign(feignContext);
		assertFeignBuilderField(builder, "logLevel", Logger.Level.HEADERS);
		assertFeignBuilderField(builder, "decode404", true);

		context.close();
	}

	private void assertFeignBuilderField(Feign.Builder builder, String fieldName,
			Object expectedValue) {
		Field builderField = ReflectionUtils.findField(Feign.Builder.class, fieldName);
		ReflectionUtils.makeAccessible(builderField);

		Object value = ReflectionUtils.getField(builderField, builder);
		assertThat(value).as("Expected value for the field '" + fieldName + "':")
				.isEqualTo(expectedValue);
	}

	@Test
	public void testBuildCustomizerOrdered() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				FeignBuilderCustomizerTests.SampleConfiguration3.class);

		FeignClientFactoryBean clientFactoryBean = context
				.getBean(FeignClientFactoryBean.class);
		FeignContext feignContext = context.getBean(FeignContext.class);

		Feign.Builder builder = clientFactoryBean.feign(feignContext);
		assertFeignBuilderField(builder, "logLevel", Logger.Level.FULL);
		assertFeignBuilderField(builder, "decode404", true);

		context.close();
	}

	@Test
	public void testBuildCustomizerOrderedWithAdditional() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				FeignBuilderCustomizerTests.SampleConfiguration3.class);

		FeignClientFactoryBean clientFactoryBean = context
				.getBean(FeignClientFactoryBean.class);
		clientFactoryBean.addCustomizer(builder -> builder.logLevel(Logger.Level.BASIC));
		clientFactoryBean.addCustomizer(Feign.Builder::doNotCloseAfterDecode);
		FeignContext feignContext = context.getBean(FeignContext.class);

		Feign.Builder builder = clientFactoryBean.feign(feignContext);
		assertFeignBuilderField(builder, "logLevel", Logger.Level.BASIC);
		assertFeignBuilderField(builder, "decode404", true);
		assertFeignBuilderField(builder, "closeAfterDecode", false);

		context.close();
	}

	private static FeignClientFactoryBean defaultFeignClientFactoryBean() {
		FeignClientFactoryBean feignClientFactoryBean = new FeignClientFactoryBean();
		feignClientFactoryBean.setContextId("test");
		feignClientFactoryBean.setName("test");
		feignClientFactoryBean.setType(FeignClientFactoryTests.TestType.class);
		feignClientFactoryBean.setPath("");
		feignClientFactoryBean.setUrl("http://some.absolute.url");
		return feignClientFactoryBean;
	}

	@Configuration(proxyBeanMethods = false)
	@Import(FeignClientsConfiguration.class)
	protected static class SampleConfiguration2 {

		@Bean
		FeignContext feignContext() {
			return new FeignContext();
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
			return Feign.Builder::decode404;
		}

		@Bean
		FeignClientFactoryBean feignClientFactoryBean() {
			return defaultFeignClientFactoryBean();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(FeignClientsConfiguration.class)
	protected static class SampleConfiguration3 {

		@Bean
		FeignContext feignContext() {
			return new FeignContext();
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
			return Feign.Builder::decode404;
		}

		@Bean
		FeignClientFactoryBean feignClientFactoryBean() {
			return defaultFeignClientFactoryBean();
		}

	}

}
