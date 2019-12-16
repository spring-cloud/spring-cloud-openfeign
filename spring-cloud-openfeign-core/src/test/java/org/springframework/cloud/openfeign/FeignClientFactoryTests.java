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

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import feign.Client;
import feign.InvocationHandlerFactory;
import org.junit.Test;

import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.loadbalancer.blocking.client.BlockingLoadBalancerClient;
import org.springframework.cloud.loadbalancer.config.LoadBalancerAutoConfiguration;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

/**
 * @author Spencer Gibb
 */
public class FeignClientFactoryTests {

	@Test
	public void testChildContexts() {
		AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext();
		parent.refresh();
		FeignContext context = new FeignContext();
		context.setApplicationContext(parent);
		context.setConfigurations(Arrays.asList(getSpec("foo", FooConfig.class),
				getSpec("bar", BarConfig.class)));

		Foo foo = context.getInstance("foo", Foo.class);
		assertThat(foo).as("foo was null").isNotNull();

		Bar bar = context.getInstance("bar", Bar.class);
		assertThat(bar).as("bar was null").isNotNull();

		Bar foobar = context.getInstance("foo", Bar.class);
		assertThat(foobar).as("bar was not null").isNull();
	}

	@Test
	public void shouldRedirectToDelegateWhenUrlSet() {
		new ApplicationContextRunner().withUserConfiguration(TestConfig.class)
				.run(this::defaultClientUsed);
	}

	@SuppressWarnings({ "unchecked", "ConstantConditions" })
	private void defaultClientUsed(AssertableApplicationContext context) {
		Proxy target = context.getBean(FeignClientFactoryBean.class).getTarget();
		Object invocationHandler = ReflectionTestUtils.getField(target, "h");
		Map<Method, InvocationHandlerFactory.MethodHandler> dispatch = (Map<Method, InvocationHandlerFactory.MethodHandler>) ReflectionTestUtils
				.getField(invocationHandler, "dispatch");
		Method key = new ArrayList<>(dispatch.keySet()).get(0);
		Object client = ReflectionTestUtils.getField(dispatch.get(key), "client");
		assertThat(client).isInstanceOf(Client.Default.class);
	}

	private FeignClientSpecification getSpec(String name, Class<?> configClass) {
		return new FeignClientSpecification(name, new Class[] { configClass });
	}

	interface TestType {

		@RequestMapping(value = "/", method = GET)
		String hello();

	}

	@Configuration
	static class TestConfig {

		@Bean
		BlockingLoadBalancerClient loadBalancerClient() {
			return new BlockingLoadBalancerClient(new LoadBalancerClientFactory());
		}

		@Bean
		FeignContext feignContext() {
			FeignContext feignContext = new FeignContext();
			feignContext.setConfigurations(
					Collections.singletonList(new FeignClientSpecification("test",
							new Class[] { LoadBalancerAutoConfiguration.class })));
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

		@Bean
		FeignClientFactoryBean feignClientFactoryBean() {
			FeignClientFactoryBean feignClientFactoryBean = new FeignClientFactoryBean();
			feignClientFactoryBean.setContextId("test");
			feignClientFactoryBean.setName("test");
			feignClientFactoryBean.setType(TestType.class);
			feignClientFactoryBean.setPath("");
			feignClientFactoryBean.setUrl("http://some.absolute.url");
			return feignClientFactoryBean;
		}

	}

	static class FooConfig {

		@Bean
		Foo foo() {
			return new Foo();
		}

	}

	static class Foo {

	}

	static class BarConfig {

		@Bean
		Bar bar() {
			return new Bar();
		}

	}

	static class Bar {

	}

}
