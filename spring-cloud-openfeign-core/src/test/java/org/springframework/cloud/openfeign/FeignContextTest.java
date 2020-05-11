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

import java.util.Collection;

import feign.Logger;
import feign.RequestInterceptor;
import org.assertj.core.util.Lists;
import org.junit.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

public class FeignContextTest {

	@Test
	public void getInstanceWithoutAncestors_verifyNullForMissing() {
		AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext();
		parent.refresh();

		FeignContext feignContext = new FeignContext();
		feignContext.setApplicationContext(parent);
		feignContext.setConfigurations(
				Lists.newArrayList(getSpec("empty", EmptyConfiguration.class)));

		Logger.Level level = feignContext.getInstanceWithoutAncestors("empty",
				Logger.Level.class);

		assertThat(level).as("Logger was not null").isNull();
	}

	private FeignClientSpecification getSpec(String name, Class<?> configClass) {
		return new FeignClientSpecification(name, new Class[] { configClass });
	}

	@Test
	public void getInstancesWithoutAncestors_verifyEmptyForMissing() {
		AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext();
		parent.refresh();

		FeignContext feignContext = new FeignContext();
		feignContext.setApplicationContext(parent);
		feignContext.setConfigurations(
				Lists.newArrayList(getSpec("empty", EmptyConfiguration.class)));

		Collection<RequestInterceptor> interceptors = feignContext
				.getInstancesWithoutAncestors("empty", RequestInterceptor.class).values();

		assertThat(interceptors).as("Interceptors is not empty").isEmpty();
	}

	@Test
	public void getInstanceWithoutAncestors() {
		AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext();
		parent.refresh();

		FeignContext feignContext = new FeignContext();
		feignContext.setApplicationContext(parent);
		feignContext.setConfigurations(
				Lists.newArrayList(getSpec("demo", DemoConfiguration.class)));

		Logger.Level level = feignContext.getInstanceWithoutAncestors("demo",
				Logger.Level.class);

		assertThat(level).isEqualTo(Logger.Level.FULL);
	}

	@Test
	public void getInstancesWithoutAncestors() {
		AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext();
		parent.refresh();

		FeignContext feignContext = new FeignContext();
		feignContext.setApplicationContext(parent);
		feignContext.setConfigurations(
				Lists.newArrayList(getSpec("demo", DemoConfiguration.class)));

		Collection<RequestInterceptor> interceptors = feignContext
				.getInstancesWithoutAncestors("demo", RequestInterceptor.class).values();

		assertThat(interceptors.size()).isEqualTo(1);
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
		public RequestInterceptor requestInterceptor() {
			return (requestTemplate) -> {
			};
		}

	}

}
