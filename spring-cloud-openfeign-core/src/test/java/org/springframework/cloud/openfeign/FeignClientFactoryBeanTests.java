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
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import feign.Client;
import feign.Client.Default;
import feign.InvocationHandlerFactory;
import feign.InvocationHandlerFactory.MethodHandler;
import feign.Target;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.loadbalancer.blocking.client.BlockingLoadBalancerClient;
import org.springframework.cloud.loadbalancer.config.LoadBalancerAutoConfiguration;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.cloud.openfeign.loadbalancer.FeignBlockingLoadBalancerClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

/**
 * @author Spencer Gibb
 * @author Benjamin Einaudi
 */
public class FeignClientFactoryBeanTests {

	public static final String FACTORY_NAME = "test";

	@ParameterizedTest
	@ValueSource(strings = { "/some/path", "/some/path/", "some/path", "some/path/" })
	public void shouldNormalizePathWhenUrlNotProvided(String configurationPath) {
		new ApplicationContextRunner().withUserConfiguration(LoadBalancerTestConfig.class)
				.withBean(FeignClientFactoryBean.class, () -> feignClientFactoryBean("", configurationPath))
				.run(context -> assertThat(target(buildMethodHandler(context)).url())
						.isEqualTo("http://" + FACTORY_NAME + "/some/path"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "/some/path", "/some/path/", "some/path", "some/path/" })
	public void shouldNormalizePathWhenUrlProvided(String configurationPath) {
		new ApplicationContextRunner().withUserConfiguration(LoadBalancerTestConfig.class)
				.withBean(FeignClientFactoryBean.class, () -> feignClientFactoryBean("domain.org", configurationPath))
				.run(context -> assertThat(target(buildMethodHandler(context)).url())
						.isEqualTo("http://domain.org/some/path"));
	}

	@Test
	@DisabledForJreRange(min = JRE.JAVA_16)
	@SuppressWarnings({ "ConstantConditions" })
	public void shouldPrependProtocolToNameWhenUrlNotSetAndUseProvidedLoadBalanceClient() {
		new ApplicationContextRunner().withUserConfiguration(LoadBalancerTestConfig.class)
				.withBean(FeignClientFactoryBean.class, () -> feignClientFactoryBean("", "")).run(context -> {
					MethodHandler methodHandler = buildMethodHandler(context);
					assertThat(target(methodHandler).url()).isEqualTo("http://" + FACTORY_NAME);
					assertThat(client(methodHandler)).isInstanceOf(FeignBlockingLoadBalancerClient.class);
				});
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "lb://some-service-name", "lbs://some-secured-service-name" })
	@DisabledForJreRange(min = JRE.JAVA_16)
	public void shouldFailWhenNoLoadBalanceClientProvidedAndLoadBalanceRequired(String configuredUrl) {
		new ApplicationContextRunner().withUserConfiguration(NoClientTestConfig.class)
				.withBean(FeignClientFactoryBean.class, () -> feignClientFactoryBean(configuredUrl, ""))
				.run(context -> assertThatExceptionOfType(IllegalStateException.class)
						.isThrownBy(() -> context.getBean(FeignClientFactoryBean.class).getTarget())
						.withMessageContaining("No Feign Client for loadBalancing defined"));
	}

	@Test
	@DisabledForJreRange(min = JRE.JAVA_16)
	@SuppressWarnings({ "ConstantConditions" })
	public void shouldPrependProtocolToUrlWhenUrlSetWithoutProtocol() {
		final String targetUrl = "some.absolute.url";
		new ApplicationContextRunner().withUserConfiguration(NoClientTestConfig.class)
				.withBean(FeignClientFactoryBean.class, () -> feignClientFactoryBean(targetUrl, ""))
				.run(context -> assertThat(target(buildMethodHandler(context)).url()).isEqualTo("http://" + targetUrl));
	}

	@Test
	@DisabledForJreRange(min = JRE.JAVA_16)
	public void shouldUseLoadBalanceClientWhenUrlUsesLoadBalanceProtocolAndOverrideUrl() {
		new ApplicationContextRunner().withUserConfiguration(LoadBalancerTestConfig.class)
				.withBean(FeignClientFactoryBean.class, () -> feignClientFactoryBean("lb://some-service-name", ""))
				.run(context -> {
					final MethodHandler methodHandler = buildMethodHandler(context);
					assertThat(target(methodHandler).url()).isEqualTo("http://some-service-name");
					assertThat(client(methodHandler)).isInstanceOf(FeignBlockingLoadBalancerClient.class);
				});
	}

	@Test
	@DisabledForJreRange(min = JRE.JAVA_16)
	public void shouldNotRequireLoadBalanceClientWhenUrlSetAndUseDefault() {
		new ApplicationContextRunner().withUserConfiguration(NoClientTestConfig.class)
				.withBean(FeignClientFactoryBean.class, () -> feignClientFactoryBean("http://some.absolute.url", ""))
				.run(context -> assertThat(client(buildMethodHandler(context))).isInstanceOf(Client.Default.class));
	}

	@Test
	@DisabledForJreRange(min = JRE.JAVA_16)
	public void shouldRedirectToDelegateWhenUrlSet() {
		new ApplicationContextRunner().withUserConfiguration(LoadBalancerTestConfig.class)
				.withBean(FeignClientFactoryBean.class, () -> feignClientFactoryBean("http://some.absolute.url", ""))
				.run(context -> assertThat(client(buildMethodHandler(context))).isInstanceOf(Client.Default.class));
	}

	@SuppressWarnings({ "unchecked", "ConstantConditions" })
	private MethodHandler buildMethodHandler(AssertableApplicationContext context) {
		Proxy target = context.getBean(FeignClientFactoryBean.class).getTarget();
		Object invocationHandler = ReflectionTestUtils.getField(target, "h");
		Map<Method, InvocationHandlerFactory.MethodHandler> dispatch = (Map<Method, InvocationHandlerFactory.MethodHandler>) ReflectionTestUtils
				.getField(invocationHandler, "dispatch");
		Method key = new ArrayList<>(dispatch.keySet()).get(0);
		return dispatch.get(key);
	}

	private Client client(MethodHandler methodHandler) {
		return (Client) ReflectionTestUtils.getField(methodHandler, "client");
	}

	private Target<?> target(MethodHandler methodHandler) {
		return (Target<?>) ReflectionTestUtils.getField(methodHandler, "target");
	}

	private FeignClientFactoryBean feignClientFactoryBean(String url, String path) {
		FeignClientFactoryBean feignClientFactoryBean = new FeignClientFactoryBean();
		feignClientFactoryBean.setContextId("test");
		feignClientFactoryBean.setName(FeignClientFactoryBeanTests.FACTORY_NAME);
		feignClientFactoryBean.setType(TestType.class);
		feignClientFactoryBean.setPath(path);
		feignClientFactoryBean.setUrl(url);
		return feignClientFactoryBean;
	}

	interface TestType {

		@RequestMapping(value = "/", method = GET)
		String hello();

	}

	static class AbstractTestConfig {

		@Bean
		FeignContext feignContext() {
			FeignContext feignContext = new FeignContext();
			feignContext.setConfigurations(Collections.singletonList(
					new FeignClientSpecification("test", new Class[] { LoadBalancerAutoConfiguration.class })));
			return feignContext;
		}

		@Bean
		FeignClientProperties feignClientProperties() {
			return new FeignClientProperties();
		}

		@Bean
		Targeter targeter() {
			return new DefaultTargeter();
		}

	}

	@Configuration
	static class LoadBalancerTestConfig extends AbstractTestConfig {

		@Bean
		Client loadBalancerClient() {
			final BlockingLoadBalancerClient loadBalancerClient = new BlockingLoadBalancerClient(
					new LoadBalancerClientFactory(), new LoadBalancerProperties());
			return new FeignBlockingLoadBalancerClient(new Default(null, null), loadBalancerClient,
					new LoadBalancerProperties(), new LoadBalancerClientFactory());
		}

	}

	@Configuration
	static class NoClientTestConfig extends AbstractTestConfig {

	}

}
