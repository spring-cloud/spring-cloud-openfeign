/*
 * Copyright 2021-2021 the original author or authors.
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

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Roman Kvasnytskyi
 */
@SpringBootTest(
		properties = { "feign.eager-load.enabled=true",
				"feign.eager-load.clients=eagerLoadedService1,eagerLoadedService2" },
		classes = { FeignClientEagerInitializationTests.TestConfig.class })
@DirtiesContext
public class FeignClientEagerInitializationTests {

	@Autowired
	ApplicationContext context;

	@Test
	public void shouldInitializeEagerChildContexts() {
		final FeignContext ctx = context.getBean(FeignContext.class);

		// Eagerly loaded contexts should be initialized before explicit call to bean
		// factory
		assertThat(InstanceCounter.getInstanceCount()).isEqualTo(2);

		// Making explicit call to bean factory to show that configuration is not called
		// second time, therefore bean is not instantiating second time
		ctx.getContext("eagerLoadedService1");
		ctx.getContext("eagerLoadedService2");
		assertThat(InstanceCounter.getInstanceCount()).isEqualTo(2);

		// Explicitly calling bean factory to create lazy service to show that instance
		// counting is working fine
		ctx.getContext("lazyLoadedService");
		assertThat(InstanceCounter.getInstanceCount()).isEqualTo(3);
	}

	@FeignClient(value = "eagerLoadedService1", configuration = InstanceCountingConfiguration.class)
	protected interface EagerFeignClient1 {

		@RequestMapping(method = RequestMethod.GET, value = "/hello")
		Hello getHello();

	}

	@FeignClient(value = "eagerLoadedService2", configuration = InstanceCountingConfiguration.class)
	protected interface EagerFeignClient2 {

		@RequestMapping(method = RequestMethod.GET, value = "/hello")
		Hello getHello();

	}

	@FeignClient(value = "lazyLoadedService", configuration = InstanceCountingConfiguration.class)
	protected interface LazyFeignClient {

		@RequestMapping(method = RequestMethod.GET, value = "/hello")
		Hello getHello();

	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	@EnableFeignClients(clients = { FeignClientEagerInitializationTests.EagerFeignClient1.class,
			FeignClientEagerInitializationTests.EagerFeignClient2.class,
			FeignClientEagerInitializationTests.LazyFeignClient.class })
	protected static class TestConfig {

	}

	static class InstanceCountingConfiguration {

		@Bean
		public InstanceCounter foo() {
			return new InstanceCounter();
		}

	}

	static class InstanceCounter {

		private static final AtomicInteger INSTANCE_COUNT = new AtomicInteger();

		InstanceCounter() {
			INSTANCE_COUNT.incrementAndGet();
		}

		public static int getInstanceCount() {
			return INSTANCE_COUNT.get();
		}

	}

	static class Hello {

		private String message;

		Hello() {
		}

		Hello(String message) {
			this.message = message;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			Hello that = (Hello) o;
			return Objects.equals(message, that.message);
		}

		@Override
		public int hashCode() {
			return Objects.hash(message);
		}

	}

}
