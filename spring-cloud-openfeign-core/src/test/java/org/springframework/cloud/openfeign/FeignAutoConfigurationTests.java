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

import java.lang.reflect.Method;

import feign.Target;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.cloud.openfeign.FeignAutoConfiguration.CircuitBreakerPresentFeignTargeterConfiguration.DefaultCircuitBreakerNameResolver;
import org.springframework.cloud.openfeign.security.MockOAuth2ClientContext;
import org.springframework.cloud.openfeign.security.OAuth2FeignRequestInterceptor;
import org.springframework.cloud.openfeign.security.OAuth2FeignRequestInterceptorBuilder;
import org.springframework.cloud.openfeign.security.OAuth2FeignRequestInterceptorConfigurer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.security.oauth2.client.resource.BaseOAuth2ProtectedResourceDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Tim Peeters
 * @author Olga Maciaszek-Sharma
 * @author Andrii Bohutskyi
 * @author Kwangyong Kim
 * @author Wojciech Mąka
 */
class FeignAutoConfigurationTests {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(FeignAutoConfiguration.class))
			.withPropertyValues("spring.cloud.openfeign.httpclient.enabled=false");

	@Test
	void shouldInstantiateDefaultTargeterWhenFeignCircuitBreakerIsDisabled() {
		runner.withPropertyValues("spring.cloud.openfeign.circuitbreaker.enabled=false")
				.run(ctx -> assertOnlyOneTargeterPresent(ctx, DefaultTargeter.class));
	}

	@Test
	void shouldInstantiateFeignCircuitBreakerTargeterWhenEnabled() {
		runner.withBean(CircuitBreakerFactory.class, () -> mock(CircuitBreakerFactory.class))
				.withPropertyValues("spring.cloud.openfeign.circuitbreaker.enabled=true").run(ctx -> {
					assertOnlyOneTargeterPresent(ctx, FeignCircuitBreakerTargeter.class);
					assertThatFeignCircuitBreakerTargeterHasGroupEnabledPropertyWithValue(ctx, false);
					assertThatFeignCircuitBreakerTargeterHasSameCircuitBreakerNameResolver(ctx,
							DefaultCircuitBreakerNameResolver.class);
				});
	}

	@Test
	void shouldInstantiateFeignCircuitBreakerTargeterWithEnabledGroup() {
		runner.withBean(CircuitBreakerFactory.class, () -> mock(CircuitBreakerFactory.class))
				.withPropertyValues("spring.cloud.openfeign.circuitbreaker.enabled=true")
				.withPropertyValues("spring.cloud.openfeign.circuitbreaker.group.enabled=true").run(ctx -> {
					assertOnlyOneTargeterPresent(ctx, FeignCircuitBreakerTargeter.class);
					assertThatFeignCircuitBreakerTargeterHasGroupEnabledPropertyWithValue(ctx, true);
				});
	}

	@Test
	void shouldInstantiateFeignCircuitBreakerTargeterWhenEnabledWithCustomCircuitBreakerNameResolver() {
		runner.withBean(CircuitBreakerFactory.class, () -> mock(CircuitBreakerFactory.class))
				.withBean(CircuitBreakerNameResolver.class, CustomCircuitBreakerNameResolver::new)
				.withPropertyValues("spring.cloud.openfeign.circuitbreaker.enabled=true").run(ctx -> {
					assertOnlyOneTargeterPresent(ctx, FeignCircuitBreakerTargeter.class);
					assertThatFeignCircuitBreakerTargeterHasSameCircuitBreakerNameResolver(ctx,
							CustomCircuitBreakerNameResolver.class);
				});
	}

	@Test
	void shouldInstantiateFeignOAuth2FeignRequestInterceptorWithoutInterceptors() {
		runner.withPropertyValues("spring.cloud.openfeign.oauth2.enabled=true")
				.withBean(MockOAuth2ClientContext.class, "token").withBean(BaseOAuth2ProtectedResourceDetails.class)
				.withBean(LoadBalancerInterceptor.class, () -> mock(LoadBalancerInterceptor.class)).run(ctx -> {
					assertOauth2FeignRequestInterceptorExists(ctx);
					assertAccessTokenProviderInterceptorNotExists(ctx, LoadBalancerInterceptor.class);
				});
	}

	@Test
	void shouldInstantiateFeignOAuth2FeignRequestInterceptorWithLoadBalancedInterceptor() {
		runner.withPropertyValues("spring.cloud.openfeign.oauth2.enabled=true",
				"spring.cloud.openfeign.oauth2.load-balanced=true").withBean(MockOAuth2ClientContext.class, "token")
				.withBean(BaseOAuth2ProtectedResourceDetails.class)
				.withBean(LoadBalancerInterceptor.class, () -> mock(LoadBalancerInterceptor.class)).run(ctx -> {
					assertOauth2FeignRequestInterceptorExists(ctx);
					assertAccessTokenProviderInterceptorExists(ctx, LoadBalancerInterceptor.class);
				});
	}

	@Test
	void shouldInstantiateFeignOAuth2FeignRequestInterceptorWithoutLoadBalancedInterceptorIfNoBeanPresent() {
		runner.withPropertyValues("spring.cloud.openfeign.oauth2.enabled=true",
				"spring.cloud.openfeign.oauth2.load-balanced=true").withBean(MockOAuth2ClientContext.class, "token")
				.withBean(BaseOAuth2ProtectedResourceDetails.class).run(ctx -> {
					assertOauth2FeignRequestInterceptorExists(ctx);
					assertAccessTokenProviderInterceptorNotExists(ctx, LoadBalancerInterceptor.class);
				});
	}

	@Test
	void shouldInstantiateFeignOAuth2FeignRequestInterceptorWithCustomAccessTokenProviderInterceptor() {
		runner.withPropertyValues("spring.cloud.openfeign.oauth2.enabled=true")
				.withBean(MockOAuth2ClientContext.class, "token").withBean(BaseOAuth2ProtectedResourceDetails.class)
				.withBean(CustomOAuth2FeignRequestInterceptorConfigurer.class).run(ctx -> {
					assertOauth2FeignRequestInterceptorExists(ctx);
					assertAccessTokenProviderInterceptorExists(ctx, BasicAuthenticationInterceptor.class);
				});
	}

	private void assertOauth2FeignRequestInterceptorExists(ConfigurableApplicationContext ctx) {
		AssertableApplicationContext context = AssertableApplicationContext.get(() -> ctx);
		assertThat(context).hasSingleBean(OAuth2FeignRequestInterceptor.class);
	}

	private void assertAccessTokenProviderInterceptorExists(ConfigurableApplicationContext ctx,
			Class<? extends ClientHttpRequestInterceptor> clazz) {
		AssertableApplicationContext context = AssertableApplicationContext.get(() -> ctx);
		assertThat(context).getBean(OAuth2FeignRequestInterceptor.class).extracting("accessTokenProvider")
				.extracting("interceptors").asList().first().isInstanceOf(clazz);
	}

	private void assertAccessTokenProviderInterceptorNotExists(ConfigurableApplicationContext ctx,
			Class<? extends ClientHttpRequestInterceptor> clazz) {
		AssertableApplicationContext context = AssertableApplicationContext.get(() -> ctx);
		assertThat(context).getBean(OAuth2FeignRequestInterceptor.class).extracting("accessTokenProvider")
				.extracting("interceptors").asList().filteredOn(obj -> clazz.isAssignableFrom(obj.getClass()))
				.isEmpty();
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

	static class CustomOAuth2FeignRequestInterceptorConfigurer implements OAuth2FeignRequestInterceptorConfigurer {

		@Override
		public void customize(OAuth2FeignRequestInterceptorBuilder requestInterceptorBuilder) {
			requestInterceptorBuilder
					.withAccessTokenProviderInterceptors(new BasicAuthenticationInterceptor("username", "password"));
		}

	}

}
