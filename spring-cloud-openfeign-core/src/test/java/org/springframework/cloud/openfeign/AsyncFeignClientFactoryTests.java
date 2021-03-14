/*
 * Copyright 2013-2021 the original author or authors.
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

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;

import feign.AsyncClient;
import feign.ReflectiveAsyncFeign;
import feign.hc5.AsyncApacheHttp5Client;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.openfeign.async.AsyncFeignAutoConfiguration;
import org.springframework.cloud.openfeign.async.AsyncHttpClient5FeignConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

/**
 * @author Nguyen Ky Thanh
 */
class AsyncFeignClientFactoryTests {

	@Test
	void testChildContexts() {
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
	void asyncHc5ClientShouldBeUsed() {
		new ApplicationContextRunner().withUserConfiguration(TestConfig.class)
				.withUserConfiguration(AsyncHc5Config.class)
				.run(context -> checkClientUsed(context, AsyncApacheHttp5Client.class));
	}

	@Test
	void defaultClientShouldBeUsed() {
		new ApplicationContextRunner().withUserConfiguration(TestConfig.class)
				.run(context -> checkClientUsed(context, AsyncClient.Default.class));
	}

	private void checkClientUsed(AssertableApplicationContext context, Class clientClass)
			throws Exception {
		Proxy target = context.getBean(FeignClientFactoryBean.class).getAsyncTarget();
		Object asyncInvocationHandler = ReflectionTestUtils.getField(target, "h");

		Field field = asyncInvocationHandler.getClass().getDeclaredField("this$0");
		field.setAccessible(true);
		ReflectiveAsyncFeign reflectiveAsyncFeign = (ReflectiveAsyncFeign) field
				.get(asyncInvocationHandler);
		Object client = ReflectionTestUtils.getField(reflectiveAsyncFeign, "client");
		assertThat(client).isInstanceOf(clientClass);
	}

	private FeignClientSpecification getSpec(String name, Class<?> configClass) {
		return new FeignClientSpecification(name, new Class[] { configClass });
	}

	interface TestType {

		@RequestMapping(value = "/", method = GET)
		String hello();

	}

	@Configuration
	@Import(AsyncHttpClient5FeignConfiguration.class)
	static class AsyncHc5Config {

		@Bean
		public AsyncClient<HttpClientContext> asyncClient(
				CloseableHttpAsyncClient httpAsyncClient) {
			return new AsyncApacheHttp5Client(httpAsyncClient);
		}

	}

	@Configuration
	static class TestConfig {

		@Bean
		FeignContext feignContext() {
			FeignContext feignContext = new FeignContext();
			feignContext.setConfigurations(
					Collections.singletonList(new FeignClientSpecification("test",
							new Class[] { AsyncFeignAutoConfiguration.class })));
			return feignContext;
		}

		@Bean
		FeignClientProperties feignClientProperties() {
			return new FeignClientProperties();
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
