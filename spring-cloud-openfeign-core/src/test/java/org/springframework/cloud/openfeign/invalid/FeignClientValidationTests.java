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

package org.springframework.cloud.openfeign.invalid;

import feign.Feign;
import feign.hystrix.FallbackFactory;
import feign.hystrix.HystrixFeign;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.cloud.client.loadbalancer.LoadBalancerAutoConfiguration;
import org.springframework.cloud.commons.httpclient.HttpClientConfiguration;
import org.springframework.cloud.netflix.ribbon.RibbonAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.ribbon.FeignRibbonClientAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 */
public class FeignClientValidationTests {

	@Rule
	public ExpectedException expected = ExpectedException.none();

	@Test
	public void testServiceIdAndValue() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				LoadBalancerAutoConfiguration.class, RibbonAutoConfiguration.class,
				FeignRibbonClientAutoConfiguration.class,
				NameAndServiceIdConfiguration.class);
		assertThat(context.getBean(NameAndServiceIdConfiguration.Client.class))
				.isNotNull();
		context.close();
	}

	@Test
	public void testDuplicatedClientNames() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.setAllowBeanDefinitionOverriding(false);
		context.register(LoadBalancerAutoConfiguration.class,
				RibbonAutoConfiguration.class, FeignRibbonClientAutoConfiguration.class,
				DuplicatedFeignClientNamesConfiguration.class);
		context.refresh();
		assertThat(
				context.getBean(DuplicatedFeignClientNamesConfiguration.FooClient.class))
						.isNotNull();
		assertThat(
				context.getBean(DuplicatedFeignClientNamesConfiguration.BarClient.class))
						.isNotNull();
		context.close();
	}

	@Test
	public void testNotLegalHostname() {
		this.expected.expectMessage("not legal hostname (foo_bar)");
		new AnnotationConfigApplicationContext(BadHostnameConfiguration.class);
	}

	@Test
	public void testMissingFallback() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				MissingFallbackConfiguration.class)) {
			this.expected.expectMessage("No fallback instance of type");
			assertThat(context.getBean(MissingFallbackConfiguration.Client.class))
					.isNotNull();
		}
	}

	@Test
	public void testWrongFallbackType() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				WrongFallbackTypeConfiguration.class)) {
			this.expected.expectMessage("Incompatible fallback instance");
			assertThat(context.getBean(WrongFallbackTypeConfiguration.Client.class))
					.isNotNull();
		}
	}

	@Test
	public void testMissingFallbackFactory() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				MissingFallbackFactoryConfiguration.class)) {
			this.expected.expectMessage("No fallbackFactory instance of type");
			assertThat(context.getBean(MissingFallbackFactoryConfiguration.Client.class))
					.isNotNull();
		}
	}

	@Test
	public void testWrongFallbackFactoryType() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				WrongFallbackFactoryTypeConfiguration.class)) {
			this.expected.expectMessage("Incompatible fallbackFactory instance");
			assertThat(
					context.getBean(WrongFallbackFactoryTypeConfiguration.Client.class))
							.isNotNull();
		}
	}

	@Configuration(proxyBeanMethods = false)
	@Import({ FeignAutoConfiguration.class, HttpClientConfiguration.class })
	@EnableFeignClients(clients = NameAndServiceIdConfiguration.Client.class)
	protected static class NameAndServiceIdConfiguration {

		@FeignClient(name = "bar", serviceId = "foo")
		interface Client {

			@RequestMapping(method = RequestMethod.GET, value = "/")
			String get();

		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import({ FeignAutoConfiguration.class, HttpClientConfiguration.class })
	@EnableFeignClients(
			clients = { DuplicatedFeignClientNamesConfiguration.FooClient.class,
					DuplicatedFeignClientNamesConfiguration.BarClient.class })
	protected static class DuplicatedFeignClientNamesConfiguration {

		@FeignClient(contextId = "foo", name = "bar")
		interface FooClient {

			@RequestMapping(method = RequestMethod.GET, value = "/")
			String get();

		}

		@FeignClient(name = "bar")
		interface BarClient {

			@RequestMapping(method = RequestMethod.GET, value = "/")
			String get();

		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(FeignAutoConfiguration.class)
	@EnableFeignClients(clients = BadHostnameConfiguration.Client.class)
	protected static class BadHostnameConfiguration {

		@FeignClient("foo_bar")
		interface Client {

			@RequestMapping(method = RequestMethod.GET, value = "/")
			String get();

		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(FeignAutoConfiguration.class)
	@EnableFeignClients(clients = MissingFallbackConfiguration.Client.class)
	protected static class MissingFallbackConfiguration {

		@Bean
		public Feign.Builder feignBuilder() {
			return HystrixFeign.builder();
		}

		@FeignClient(name = "foobar", url = "http://localhost",
				fallback = ClientFallback.class)
		interface Client {

			@RequestMapping(method = RequestMethod.GET, value = "/")
			String get();

		}

		class ClientFallback implements Client {

			@Override
			public String get() {
				return null;
			}

		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(FeignAutoConfiguration.class)
	@EnableFeignClients(clients = WrongFallbackTypeConfiguration.Client.class)
	protected static class WrongFallbackTypeConfiguration {

		@Bean
		Dummy dummy() {
			return new Dummy();
		}

		@Bean
		public Feign.Builder feignBuilder() {
			return HystrixFeign.builder();
		}

		@FeignClient(name = "foobar", url = "http://localhost", fallback = Dummy.class)
		interface Client {

			@RequestMapping(method = RequestMethod.GET, value = "/")
			String get();

		}

		class Dummy {

		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(FeignAutoConfiguration.class)
	@EnableFeignClients(clients = MissingFallbackFactoryConfiguration.Client.class)
	protected static class MissingFallbackFactoryConfiguration {

		@Bean
		public Feign.Builder feignBuilder() {
			return HystrixFeign.builder();
		}

		@FeignClient(name = "foobar", url = "http://localhost",
				fallbackFactory = ClientFallback.class)
		interface Client {

			@RequestMapping(method = RequestMethod.GET, value = "/")
			String get();

		}

		class ClientFallback implements FallbackFactory<Client> {

			@Override
			public Client create(Throwable cause) {
				return null;
			}

		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(FeignAutoConfiguration.class)
	@EnableFeignClients(clients = WrongFallbackFactoryTypeConfiguration.Client.class)
	protected static class WrongFallbackFactoryTypeConfiguration {

		@Bean
		Dummy dummy() {
			return new Dummy();
		}

		@Bean
		public Feign.Builder feignBuilder() {
			return HystrixFeign.builder();
		}

		@FeignClient(name = "foobar", url = "http://localhost",
				fallbackFactory = Dummy.class)
		interface Client {

			@RequestMapping(method = RequestMethod.GET, value = "/")
			String get();

		}

		class Dummy {

		}

	}

}
