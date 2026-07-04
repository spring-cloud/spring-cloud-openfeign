/*
 * Copyright 2013-present the original author or authors.
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
import java.util.Map;
import java.util.function.Supplier;

import feign.Feign;
import feign.InvocationHandlerFactory;
import feign.Target;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.openfeign.FeignAutoConfiguration.CircuitBreakerPresentFeignTargeterConfiguration.AlphanumericCircuitBreakerNameResolver;
import org.springframework.cloud.openfeign.FeignAutoConfiguration.CircuitBreakerPresentFeignTargeterConfiguration.DefaultCircuitBreakerNameResolver;
import org.springframework.cloud.openfeign.security.OAuth2AccessTokenInterceptor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Tim Peeters
 * @author Olga Maciaszek-Sharma
 * @author Andrii Bohutskyi
 * @author Kwangyong Kim
 * @author Wojciech Mąka
 * @author Dangzhicairang(小水牛)
 * @author jaehun lee
 */
class FeignAutoConfigurationTests {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(FeignAutoConfiguration.class))
		.withPropertyValues("spring.cloud.openfeign.httpclient.hc5.enabled=false");

	@Test
	void shouldInstantiateDefaultTargeterWhenFeignCircuitBreakerIsDisabled() {
		runner.withPropertyValues("spring.cloud.openfeign.circuitbreaker.enabled=false")
			.run(ctx -> assertOnlyOneTargeterPresent(ctx, DefaultTargeter.class));
	}

	@Test
	void shouldInstantiateFeignCircuitBreakerTargeterWhenEnabled() {
		runner.withBean(CircuitBreakerFactory.class, () -> mock(CircuitBreakerFactory.class))
			.withPropertyValues("spring.cloud.openfeign.circuitbreaker.enabled=true")
			.run(ctx -> {
				assertOnlyOneTargeterPresent(ctx, FeignCircuitBreakerTargeter.class);
				assertThatFeignCircuitBreakerTargeterHasGroupEnabledPropertyWithValue(ctx, false);
				assertThatFeignCircuitBreakerTargeterHasSameCircuitBreakerNameResolver(ctx,
						AlphanumericCircuitBreakerNameResolver.class);
			});
	}

	@Test
	void shouldInstantiateFeignCircuitBreakerTargeterWithEnabledGroup() {
		runner.withBean(CircuitBreakerFactory.class, () -> mock(CircuitBreakerFactory.class))
			.withPropertyValues("spring.cloud.openfeign.circuitbreaker.enabled=true")
			.withPropertyValues("spring.cloud.openfeign.circuitbreaker.group.enabled=true")
			.run(ctx -> {
				assertOnlyOneTargeterPresent(ctx, FeignCircuitBreakerTargeter.class);
				assertThatFeignCircuitBreakerTargeterHasGroupEnabledPropertyWithValue(ctx, true);
			});
	}

	@Test
	void shouldInstantiateFeignCircuitBreakerTargeterWhenEnabledWithCustomCircuitBreakerNameResolver() {
		runner.withBean(CircuitBreakerFactory.class, () -> mock(CircuitBreakerFactory.class))
			.withBean(CircuitBreakerNameResolver.class, CustomCircuitBreakerNameResolver::new)
			.withPropertyValues("spring.cloud.openfeign.circuitbreaker.enabled=true")
			.run(ctx -> {
				assertOnlyOneTargeterPresent(ctx, FeignCircuitBreakerTargeter.class);
				assertThatFeignCircuitBreakerTargeterHasSameCircuitBreakerNameResolver(ctx,
						CustomCircuitBreakerNameResolver.class);
			});
	}

	@Test
	void shouldConfigureCircuitBreakerFeignBuilderWhenUsedDirectly() {
		CircuitBreakerFactory<?, ?> circuitBreakerFactory = mock(CircuitBreakerFactory.class);
		new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(FeignAutoConfiguration.class, FeignClientsConfiguration.class))
			.withPropertyValues("spring.cloud.openfeign.circuitbreaker.enabled=true",
					"spring.cloud.openfeign.circuitbreaker.group.enabled=true",
					"spring.cloud.openfeign.httpclient.hc5.enabled=false")
			.withBean(CircuitBreakerFactory.class, () -> circuitBreakerFactory)
			.run(ctx -> {
				CircuitBreakerNameResolver circuitBreakerNameResolver = ctx.getBean(CircuitBreakerNameResolver.class);
				Feign.Builder builder = ctx.getBean(Feign.Builder.class);

				assertThat(builder).isInstanceOf(FeignCircuitBreaker.Builder.class)
					.hasFieldOrPropertyWithValue("circuitBreakerFactory", circuitBreakerFactory)
					.hasFieldOrPropertyWithValue("circuitBreakerGroupEnabled", true)
					.hasFieldOrPropertyWithValue("circuitBreakerNameResolver", circuitBreakerNameResolver);
			});
	}

	@Test
	void shouldConfigureCircuitBreakerFeignBuilderWithoutNameResolverBean() throws NoSuchMethodException {
		CircuitBreakerFactory<?, ?> circuitBreakerFactory = mock(CircuitBreakerFactory.class);
		new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(FeignClientsConfiguration.class))
			.withPropertyValues("spring.cloud.openfeign.circuitbreaker.enabled=true",
					"spring.cloud.openfeign.httpclient.hc5.enabled=false")
			.withBean(CircuitBreakerFactory.class, () -> circuitBreakerFactory)
			.run(ctx -> {
				Feign.Builder builder = ctx.getBean(Feign.Builder.class);
				CircuitBreakerNameResolver circuitBreakerNameResolver = (CircuitBreakerNameResolver) ReflectionTestUtils
					.getField(builder, "circuitBreakerNameResolver");
				Method method = DirectCircuitBreakerClient.class.getMethod("get");
				Target<DirectCircuitBreakerClient> target = new Target.HardCodedTarget<>(
						DirectCircuitBreakerClient.class, "directClient", "http://localhost");

				assertThat(builder).isInstanceOf(FeignCircuitBreaker.Builder.class)
					.hasFieldOrPropertyWithValue("circuitBreakerFactory", circuitBreakerFactory)
					.hasFieldOrPropertyWithValue("circuitBreakerNameResolver", circuitBreakerNameResolver);
				assertThat(circuitBreakerNameResolver)
					.isExactlyInstanceOf(AlphanumericCircuitBreakerNameResolver.class);
				assertThat(circuitBreakerNameResolver.resolveCircuitBreakerName("directClient", target, method))
					.isEqualTo(Feign.configKey(target.type(), method).replaceAll("[^a-zA-Z0-9]", ""));
			});
	}

	@Test
	void shouldConfigureDefaultCircuitBreakerFeignBuilderNameResolverWhenAlphanumericIdsDisabled()
			throws NoSuchMethodException {
		CircuitBreakerFactory<?, ?> circuitBreakerFactory = mock(CircuitBreakerFactory.class);
		new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(FeignClientsConfiguration.class))
			.withPropertyValues("spring.cloud.openfeign.circuitbreaker.enabled=true",
					"spring.cloud.openfeign.circuitbreaker.alphanumeric-ids.enabled=false",
					"spring.cloud.openfeign.httpclient.hc5.enabled=false")
			.withBean(CircuitBreakerFactory.class, () -> circuitBreakerFactory)
			.run(ctx -> {
				Feign.Builder builder = ctx.getBean(Feign.Builder.class);
				CircuitBreakerNameResolver circuitBreakerNameResolver = (CircuitBreakerNameResolver) ReflectionTestUtils
					.getField(builder, "circuitBreakerNameResolver");
				Method method = DirectCircuitBreakerClient.class.getMethod("get");
				Target<DirectCircuitBreakerClient> target = new Target.HardCodedTarget<>(
						DirectCircuitBreakerClient.class, "directClient", "http://localhost");

				assertThat(builder).isInstanceOf(FeignCircuitBreaker.Builder.class)
					.hasFieldOrPropertyWithValue("circuitBreakerFactory", circuitBreakerFactory)
					.hasFieldOrPropertyWithValue("circuitBreakerNameResolver", circuitBreakerNameResolver);
				assertThat(circuitBreakerNameResolver).isExactlyInstanceOf(DefaultCircuitBreakerNameResolver.class);
				assertThat(circuitBreakerNameResolver.resolveCircuitBreakerName("directClient", target, method))
					.isEqualTo(Feign.configKey(target.type(), method));
			});
	}

	@Test
	void shouldUseTargetNameWhenFeignClientNameIsNotSet() throws Throwable {
		CircuitBreakerFactory<?, ?> circuitBreakerFactory = mock(CircuitBreakerFactory.class);
		CircuitBreaker circuitBreaker = mock(CircuitBreaker.class);
		Method method = DirectCircuitBreakerClient.class.getMethod("get");
		InvocationHandlerFactory.MethodHandler methodHandler = mock(InvocationHandlerFactory.MethodHandler.class);
		Target<DirectCircuitBreakerClient> target = new Target.HardCodedTarget<>(DirectCircuitBreakerClient.class,
				"directClient", "http://localhost");
		FeignCircuitBreakerInvocationHandler handler = new FeignCircuitBreakerInvocationHandler(circuitBreakerFactory,
				null, target, Map.of(method, methodHandler), null, true,
				(feignClientName, targetType, targetMethod) -> feignClientName + "#" + targetMethod.getName());

		when(circuitBreakerFactory.create("directClient#get", "directClient")).thenReturn(circuitBreaker);
		when(circuitBreaker.run(any(Supplier.class)))
			.thenAnswer(invocation -> invocation.<Supplier<?>>getArgument(0).get());
		when(methodHandler.invoke(null)).thenReturn("ok");

		assertThat(handler.invoke(null, method, null)).isEqualTo("ok");
		verify(circuitBreakerFactory).create("directClient#get", "directClient");
	}

	@Test
	void shouldInstantiateFeignOAuth2FeignRequestInterceptorWithoutInterceptors() {
		runner
			.withPropertyValues("spring.cloud.openfeign.oauth2.enabled=true",
					"spring.cloud.openfeign.oauth2.clientRegistrationId=feign-client")
			.withBean(OAuth2AuthorizedClientService.class, () -> mock(OAuth2AuthorizedClientService.class))
			.withBean(ClientRegistrationRepository.class, () -> mock(ClientRegistrationRepository.class))
			.run(ctx -> {
				assertOauth2AccessTokenInterceptorExists(ctx);
				assertThatOauth2AccessTokenInterceptorHasSpecifiedIdsPropertyWithValue(ctx, "feign-client");
			});
	}

	@Test
	void shouldInstantiateFeignOAuth2FeignRequestInterceptorWithDashCaseProperty() {
		runner
			.withPropertyValues("spring.cloud.openfeign.oauth2.enabled=true",
					"spring.cloud.openfeign.oauth2.client-registration-id=feign-client")
			.withBean(OAuth2AuthorizedClientService.class, () -> mock(OAuth2AuthorizedClientService.class))
			.withBean(ClientRegistrationRepository.class, () -> mock(ClientRegistrationRepository.class))
			.run(ctx -> {
				assertOauth2AccessTokenInterceptorExists(ctx);
				assertThatOauth2AccessTokenInterceptorHasSpecifiedIdsPropertyWithValue(ctx, "feign-client");
			});
	}

	private void assertOauth2AccessTokenInterceptorExists(ConfigurableApplicationContext ctx) {
		AssertableApplicationContext context = AssertableApplicationContext.get(() -> ctx);
		assertThat(context).hasSingleBean(OAuth2AccessTokenInterceptor.class);
	}

	private void assertThatOauth2AccessTokenInterceptorHasSpecifiedIdsPropertyWithValue(
			ConfigurableApplicationContext ctx, String expectedValue) {
		final OAuth2AccessTokenInterceptor bean = ctx.getBean(OAuth2AccessTokenInterceptor.class);
		assertThat(bean).hasFieldOrPropertyWithValue("clientRegistrationId", expectedValue);
	}

	private void assertOnlyOneTargeterPresent(ConfigurableApplicationContext ctx, Class<?> beanClass) {
		assertThat(ctx.getBeansOfType(Targeter.class)).hasSize(1)
			.hasValueSatisfying(new Condition<>(beanClass::isInstance,
					String.format("Targeter should be an instance of %s", beanClass)));

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

	interface DirectCircuitBreakerClient {

		String get();

	}

	static class CustomCircuitBreakerNameResolver implements CircuitBreakerNameResolver {

		@Override
		public String resolveCircuitBreakerName(String feignClientName, Target<?> target, Method method) {
			return feignClientName + "_" + method.getName();
		}

	}

}
