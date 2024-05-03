/*
 * Copyright 2013-2024 the original author or authors.
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

import java.util.Collection;
import java.util.Map;

import feign.Logger;
import feign.QueryMapEncoder;
import feign.RequestInterceptor;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;

import org.springframework.cloud.openfeign.support.BaseQueryMapEncoder;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author changjin wei(魏昌进)
 */
class FeignClientFactoryTest {

	@Test
	void getInstanceWithoutAncestors_verifyNullForMissing() {
		AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext();
		parent.refresh();

		FeignClientFactory feignClientFactory = new FeignClientFactory();
		feignClientFactory.setApplicationContext(parent);
		feignClientFactory.setConfigurations(Lists.newArrayList(getSpec("empty", null, EmptyConfiguration.class)));

		Logger.Level level = feignClientFactory.getInstanceWithoutAncestors("empty", Logger.Level.class);

		assertThat(level).as("Logger was not null").isNull();
	}

	private FeignClientSpecification getSpec(String name, String className, Class<?> configClass) {
		return new FeignClientSpecification(name, className, new Class[] { configClass });
	}

	@Test
	void getInstancesWithoutAncestors_verifyEmptyForMissing() {
		AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext();
		parent.refresh();

		FeignClientFactory feignClientFactory = new FeignClientFactory();
		feignClientFactory.setApplicationContext(parent);
		feignClientFactory.setConfigurations(Lists.newArrayList(getSpec("empty", null, EmptyConfiguration.class)));

		Collection<RequestInterceptor> interceptors = feignClientFactory
				.getInstancesWithoutAncestors("empty", RequestInterceptor.class).values();

		assertThat(interceptors).as("Interceptors is not empty").isEmpty();
	}

	@Test
	void getInstanceWithoutAncestors() {
		AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext();
		parent.refresh();

		FeignClientFactory feignClientFactory = new FeignClientFactory();
		feignClientFactory.setApplicationContext(parent);
		feignClientFactory.setConfigurations(Lists.newArrayList(getSpec("demo", null, DemoConfiguration.class)));

		Logger.Level level = feignClientFactory.getInstanceWithoutAncestors("demo", Logger.Level.class);

		assertThat(level).isEqualTo(Logger.Level.FULL);
	}

	@Test
	void getInstancesWithoutAncestors() {
		AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext();
		parent.refresh();

		FeignClientFactory feignClientFactory = new FeignClientFactory();
		feignClientFactory.setApplicationContext(parent);
		feignClientFactory.setConfigurations(Lists.newArrayList(getSpec("demo", null, DemoConfiguration.class)));

		Collection<RequestInterceptor> interceptors = feignClientFactory
				.getInstancesWithoutAncestors("demo", RequestInterceptor.class).values();

		assertThat(interceptors.size()).isEqualTo(1);
	}

	@Test
	void getInstanceWithoutAncestorsForAnnotation() {
		AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext();
		parent.refresh();

		FeignClientFactory feignClientFactory = new FeignClientFactory();
		feignClientFactory.setApplicationContext(parent);
		feignClientFactory.setConfigurations(Lists.newArrayList(getSpec("demo", null, DemoConfiguration.class)));

		QueryMapEncoder queryMapEncoder = feignClientFactory.getInstanceWithoutAncestorsForAnnotation("demo",
				QueryMapEncoder.class, BaseQueryMapEncoder.class);
		assertThat(queryMapEncoder.getClass()).isEqualTo(DemoConfiguration.TestBaseQueryMapEncoder.class);
	}

	@Configuration(proxyBeanMethods = false)
	@Import(FeignClientsConfiguration.class)
	protected static class EmptyConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@Import(FeignClientsConfiguration.class)
	protected static class DemoConfiguration {

		@Bean
		public Logger.Level loggerLevel() {
			return Logger.Level.FULL;
		}

		@Bean
		@BaseQueryMapEncoder
		public QueryMapEncoder testBaseQueryMapEncoder() {
			return new TestBaseQueryMapEncoder();
		}

		@Bean
		public QueryMapEncoder queryMapEncoder() {
			return new TestQueryMapEncoder();
		}

		@Bean
		public RequestInterceptor requestInterceptor() {
			return (requestTemplate) -> {
			};
		}

		public static class TestBaseQueryMapEncoder implements QueryMapEncoder {

			@Override
			public Map<String, Object> encode(Object object) {
				return null;
			}

		}

		public static class TestQueryMapEncoder implements QueryMapEncoder {

			@Override
			public Map<String, Object> encode(Object object) {
				return null;
			}

		}

	}

}
