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

import java.lang.reflect.Method;

import feign.Target;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.openfeign.FeignAutoConfiguration.CircuitBreakerPresentFeignTargeterConfiguration.DefaultCircuitBreakerNameResolver;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Tim Peeters
 * @author Olga Maciaszek-Sharma
 * @author Andrii Bohutskyi
 * @author Kwangyong Kim
 */
class FeignAutoConfigurationTests {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(FeignAutoConfiguration.class))
			.withPropertyValues("feign.httpclient.enabled=false");

	@Test
	void shouldInstantiateDefaultTargeterWhenFeignCircuitBreakerIsDisabled() {
		runner.withPropertyValues("feign.circuitbreaker.enabled=false")
				.run(ctx -> assertOnlyOneTargeterPresent(ctx, DefaultTargeter.class));
	}

	@Test
	void shouldInstantiateFeignCircuitBreakerTargeterWhenEnabled() {
		runner.withBean(CircuitBreakerFactory.class, () -> mock(CircuitBreakerFactory.class))
				.withPropertyValues("feign.circuitbreaker.enabled=true").run(ctx -> {
					assertOnlyOneTargeterPresent(ctx, FeignCircuitBreakerTargeter.class);
					assertThatFeignCircuitBreakerTargeterHasGroupEnabledPropertyWithValue(ctx, false);
					assertThatFeignCircuitBreakerTargeterHasSameCircuitBreakerNameResolver(ctx,
							DefaultCircuitBreakerNameResolver.class);
				});
	}

	@Test
	void shouldInstantiateFeignCircuitBreakerTargeterWithEnabledGroup() {
		runner.withBean(CircuitBreakerFactory.class, () -> mock(CircuitBreakerFactory.class))
				.withPropertyValues("feign.circuitbreaker.enabled=true")
				.withPropertyValues("feign.circuitbreaker.group.enabled=true").run(ctx -> {
					assertOnlyOneTargeterPresent(ctx, FeignCircuitBreakerTargeter.class);
					assertThatFeignCircuitBreakerTargeterHasGroupEnabledPropertyWithValue(ctx, true);
				});
	}

	@Test
	void shouldInstantiateFeignCircuitBreakerTargeterWhenEnabledWithCustomCircuitBreakerNameResolver() {
		runner.withBean(CircuitBreakerFactory.class, () -> mock(CircuitBreakerFactory.class))
				.withBean(CircuitBreakerNameResolver.class, CustomCircuitBreakerNameResolver::new)
				.withPropertyValues("feign.circuitbreaker.enabled=true").run(ctx -> {
					assertOnlyOneTargeterPresent(ctx, FeignCircuitBreakerTargeter.class);
					assertThatFeignCircuitBreakerTargeterHasSameCircuitBreakerNameResolver(ctx,
							CustomCircuitBreakerNameResolver.class);
				});
	}

	private void assertOnlyOneTargeterPresent(ConfigurableApplicationContext ctx, Class<?> beanClass) {
		assertThat(ctx.getBeansOfType(Targeter.class)).hasSize(1).hasValueSatisfying(new Condition<>(
				beanClass::isInstance, String.format("Targeter should be an instance of %s", beanClass)));

	}

	private void assertThatFeignCircuitBreakerTargeterHasGroupEnabledPropertyWithValue(
			ConfigurableApplicationContext ctx, boolean expectedValue) {
		final FeignCircuitBreakerTargeter bean = ctx.getBean(FeignCircuitBreakerTargeter.class);
		assertThat(bean).hasFieldOrPropertyWithValue("circuitBreakerGroupEnabled", expectedValue);
	}

	private void assertThatFeignCircuitBreakerTargeterHasSameCircuitBreakerNameResolver(
			ConfigurableApplicationContext ctx, Class<?> beanClass) {
		final CircuitBreakerNameResolver bean = ctx.getBean(CircuitBreakerNameResolver.class);
		assertThat(bean).isExactlyInstanceOf(beanClass);
	}

	static class CustomCircuitBreakerNameResolver implements CircuitBreakerNameResolver {

		@Override
		public String resolveCircuitBreakerName(String feignClientName, Target<?> target, Method method) {
			return feignClientName + "_" + method.getName();
		}

	}

}
