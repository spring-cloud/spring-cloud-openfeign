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

import feign.hystrix.HystrixFeign;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Tim Peeters
 */
class FeignAutoConfigurationTests {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(FeignAutoConfiguration.class));

	@Test
	void shouldInstantiateHystrixTargeterToMaintainBackwardsCompatibility() {
		runner.run(ctx -> assertOnlyOneTargeterPresent(ctx, HystrixTargeter.class));
	}

	@Test
	void shouldInstantiateHystrixTargeterWhenExplicitlyEnabled() {
		runner.withPropertyValues("feign.hystrix.enabled=true")
				.run(ctx -> assertOnlyOneTargeterPresent(ctx, HystrixTargeter.class));
	}

	@Test
	void shouldInstantiateDefaultTargeterWhenHystrixIsDisabled() {
		runner.withPropertyValues("feign.hystrix.enabled=false")
				.run(ctx -> assertOnlyOneTargeterPresent(ctx, DefaultTargeter.class));
	}

	@Test
	void shouldInstantiateDefaultTargeterWhenHystrixFeignClassIsMissing() {
		runner.withPropertyValues("feign.hystrix.enabled=true")
				.withClassLoader(new FilteredClassLoader(HystrixFeign.class))
				.run(ctx -> assertOnlyOneTargeterPresent(ctx, DefaultTargeter.class));
	}

	@Test
	void shouldInstantiateDefaultTargeterWhenHystrixFeignAndCircuitBreakerClassesAreMissing() {
		runner.withPropertyValues("feign.hystrix.enabled=true",
				"feign.circuitbreaker.enabled=true")
				.withClassLoader(
						new FilteredClassLoader(HystrixFeign.class, CircuitBreaker.class))
				.run(ctx -> assertOnlyOneTargeterPresent(ctx, DefaultTargeter.class));
	}

	@Test
	void shouldInstantiateDefaultTargeterWhenHystrixFeignClassIsMissingAndFeignCircuitBreakerIsDisabled() {
		runner.withClassLoader(new FilteredClassLoader(HystrixFeign.class))
				.withPropertyValues("feign.circuitbreaker.enabled=false")
				.run(ctx -> assertOnlyOneTargeterPresent(ctx, DefaultTargeter.class));
	}

	@Test
	void shouldInstantiateFeignCircuitBreakerTargeterWhenEnabled() {
		runner.withBean(CircuitBreakerFactory.class,
				() -> mock(CircuitBreakerFactory.class))
				.withPropertyValues("feign.circuitbreaker.enabled=true")
				.run(ctx -> assertOnlyOneTargeterPresent(ctx,
						FeignCircuitBreakerTargeter.class));
	}

	@Test
	void shouldInstantiateFeignCircuitBreakerTargeterWhenBothHystrixAndCircuitBreakerAreEnabled() {
		runner.withBean(CircuitBreakerFactory.class,
				() -> mock(CircuitBreakerFactory.class))
				.withPropertyValues("feign.hystrix.enabled=true",
						"feign.circuitbreaker.enabled=true")
				.run(ctx -> assertOnlyOneTargeterPresent(ctx,
						FeignCircuitBreakerTargeter.class));
	}

	private void assertOnlyOneTargeterPresent(ConfigurableApplicationContext ctx,
			Class<?> beanClass) {
		assertThat(ctx.getBeansOfType(Targeter.class)).hasSize(1)
				.hasValueSatisfying(new Condition<>(beanClass::isInstance, String
						.format("Targeter should be an instance of %s", beanClass)));
	}

}
