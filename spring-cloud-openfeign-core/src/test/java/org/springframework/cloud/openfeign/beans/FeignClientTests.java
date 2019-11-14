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

package org.springframework.cloud.openfeign.beans;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Objects;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.FeignClientBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = FeignClientTests.Application.class,
		webEnvironment = WebEnvironment.RANDOM_PORT,
		value = { "spring.application.name=feignclienttest",
				"logging.level.org.springframework.cloud.openfeign.valid=DEBUG",
				"feign.httpclient.enabled=false", "feign.okhttp.enabled=false" })
@DirtiesContext
public class FeignClientTests {

	@Value("${local.server.port}")
	private int port = 0;

	@Autowired
	private TestClient testClient;

	@Autowired
	private ApplicationContext context;

	@Qualifier("uniquequalifier")
	@Autowired
	private org.springframework.cloud.openfeign.beans.extra.TestClient extraClient;

	@Qualifier("build-by-builder")
	@Autowired
	private TestClient buildByBuilder;

	@Test
	public void testAnnotations() {
		Map<String, Object> beans = this.context
				.getBeansWithAnnotation(FeignClient.class);
		assertThat(beans.containsKey(TestClient.class.getName()))
				.as("Wrong clients: " + beans).isTrue();
	}

	@Test
	public void testClient() {
		assertThat(this.testClient).as("testClient was null").isNotNull();
		assertThat(this.extraClient).as("extraClient was null").isNotNull();
		assertThat(Proxy.isProxyClass(this.testClient.getClass()))
				.as("testClient is not a java Proxy").isTrue();
		InvocationHandler invocationHandler = Proxy.getInvocationHandler(this.testClient);
		assertThat(invocationHandler).as("invocationHandler was null").isNotNull();
	}

	@Test
	public void extraClient() {
		assertThat(this.extraClient).as("extraClient was null").isNotNull();
		assertThat(Proxy.isProxyClass(this.extraClient.getClass()))
				.as("extraClient is not a java Proxy").isTrue();
		InvocationHandler invocationHandler = Proxy
				.getInvocationHandler(this.extraClient);
		assertThat(invocationHandler).as("invocationHandler was null").isNotNull();
	}

	@Test
	public void buildByBuilder() {
		assertThat(this.buildByBuilder).as("buildByBuilder was null").isNotNull();
		assertThat(Proxy.isProxyClass(this.buildByBuilder.getClass()))
				.as("buildByBuilder is not a java Proxy").isTrue();
		InvocationHandler invocationHandler = Proxy
				.getInvocationHandler(this.buildByBuilder);
		assertThat(invocationHandler).as("invocationHandler was null").isNotNull();
	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	@RestController
	@EnableFeignClients
	@Import(FeignClientBuilder.class)
	protected static class Application {

		@Bean("build-by-builder")
		public TestClient buildByBuilder(final FeignClientBuilder feignClientBuilder) {
			return feignClientBuilder.forType(TestClient.class, "builderapp").build();
		}

		@RequestMapping(method = RequestMethod.GET, value = "/hello")
		public Hello getHello() {
			return new Hello("hello world 1");
		}

	}

	public static class Hello {

		private String message;

		public Hello() {
		}

		public Hello(String message) {
			this.message = message;
		}

		public String getMessage() {
			return this.message;
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

			return Objects.equals(this.message, that.message);
		}

		@Override
		public int hashCode() {
			return this.message != null ? this.message.hashCode() : 0;
		}

	}

	@Configuration(proxyBeanMethods = false)
	public static class TestDefaultFeignConfig {

	}

}
