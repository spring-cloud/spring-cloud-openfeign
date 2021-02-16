/*
 * Copyright 2013-2020 the original author or authors.
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

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.netflix.hystrix.HystrixCircuitBreakerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Tim Peeters
 */
class FeignAutoConfigurationTests {

	@Test
	void shouldInstantiateFeignHystrixTargeter() {
		ConfigurableApplicationContext context = contextBuilder()
				.sources(FeignAutoConfiguration.class).run();
		assertThatOneBeanPresent(context, HystrixTargeter.class);
	}

	@Test
	void shouldInstantiateDefaultFeignTargeterWhenHystrixDisabled() {
		ConfigurableApplicationContext context = contextBuilder()
				.properties("feign.hystrix.enabled=false")
				.sources(FeignAutoConfiguration.class).run();
		assertThatOneBeanPresent(context, DefaultTargeter.class);
	}

	@Test
	void shouldInstantiateFeignCircuitBreakerTargeterWhenCircuitBreakerEnabled() {
		ConfigurableApplicationContext context = contextBuilder()
				.properties("feign.circuitbreaker.enabled=true")
				.sources(CircuitBreakerFactoryConfig.class, FeignAutoConfiguration.class)
				.run();
		assertThatOneBeanPresent(context, FeignCircuitBreakerTargeter.class);
	}

	private SpringApplicationBuilder contextBuilder() {
		return new SpringApplicationBuilder().web(WebApplicationType.NONE);
	}

	private void assertThatOneBeanPresent(ConfigurableApplicationContext context,
			Class<?> beanClass) {
		Map<String, ?> beans = context.getBeansOfType(beanClass);
		assertThat(beans).hasSize(1);
	}

	@Configuration
	static class CircuitBreakerFactoryConfig {

		@Bean
		HystrixCircuitBreakerFactory circuitBreakerFactory() {
			return new HystrixCircuitBreakerFactory();
		}

	}

}
