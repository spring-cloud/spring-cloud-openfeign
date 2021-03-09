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
import java.util.Objects;

import feign.Client;
import feign.Feign;
import feign.Target;
import feign.httpclient.ApacheHttpClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.test.NoSecurityConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.SocketUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.DEFINED_PORT;

/**
 * @author Spencer Gibb
 * @author Olga Maciaszek-Sharma
 */
@SpringBootTest(
		classes = FeignHttpClientUrlTestsWithRetryableLoadBalancer.TestConfig.class,
		webEnvironment = DEFINED_PORT,
		value = { "spring.application.name=feignclienturlwithretryableloadbalancertest",
				"feign.hystrix.enabled=false", "feign.okhttp.enabled=false",
				"spring.cloud.loadbalancer.ribbon.enabled=false" })
@DirtiesContext
class FeignHttpClientUrlTestsWithRetryableLoadBalancer {

	static int port;

	@Autowired
	BeanUrlClientNoProtocol beanClientNoProtocol;

	@Autowired
	private UrlClient urlClient;

	@Autowired
	private BeanUrlClient beanClient;

	@BeforeAll
	static void beforeClass() {
		port = SocketUtils.findAvailableTcpPort();
		System.setProperty("server.port", String.valueOf(port));
	}

	@AfterAll
	static void afterClass() {
		System.clearProperty("server.port");
	}

	@Test
	void testUrlHttpClient() {
		assertThat(urlClient).as("UrlClient was null").isNotNull();
		Hello hello = urlClient.getHello();
		assertThat(hello).as("hello was null").isNotNull();
		assertThat(hello).as("first hello didn't match")
				.isEqualTo(new Hello("hello world 1"));
	}

	@Test
	void testBeanUrl() {
		Hello hello = beanClient.getHello();
		assertThat(hello).as("hello was null").isNotNull();
		assertThat(hello).as("first hello didn't match")
				.isEqualTo(new Hello("hello world 1"));
	}

	@Test
	void testBeanUrlNoProtocol() {
		Hello hello = beanClientNoProtocol.getHello();
		assertThat(hello).as("hello was null").isNotNull();
		assertThat(hello).as("first hello didn't match")
				.isEqualTo(new Hello("hello world 1"));
	}

	// this tests that
	@FeignClient(name = "localappurl", url = "http://localhost:${server.port}/")
	protected interface UrlClient {

		@RequestMapping(method = RequestMethod.GET, value = "/hello")
		Hello getHello();

	}

	@FeignClient(name = "beanappurl", url = "#{SERVER_URL}path")
	protected interface BeanUrlClient {

		@RequestMapping(method = RequestMethod.GET, value = "/hello")
		Hello getHello();

	}

	@FeignClient(name = "beanappurlnoprotocol", url = "#{SERVER_URL_NO_PROTOCOL}path")
	protected interface BeanUrlClientNoProtocol {

		@RequestMapping(method = RequestMethod.GET, value = "/hello")
		Hello getHello();

	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	@RestController
	@EnableFeignClients(clients = { UrlClient.class, BeanUrlClient.class,
			BeanUrlClientNoProtocol.class })
	@Import(NoSecurityConfiguration.class)
	protected static class TestConfig {

		@GetMapping("/hello")
		public Hello getHello() {
			return new Hello("hello world 1");
		}

		@GetMapping("/path/hello")
		public Hello getHelloWithPath() {
			return getHello();
		}

		@Bean(name = "SERVER_URL")
		public String serverUrl() {
			return "http://localhost:" + port + "/";
		}

		@Bean(name = "SERVER_URL_NO_PROTOCOL")
		public String serverUrlNoProtocol() {
			return "localhost:" + port + "/";
		}

		@Bean
		public Targeter feignTargeter() {
			return new Targeter() {
				@Override
				public <T> T target(FeignClientFactoryBean factory, Feign.Builder feign,
						FeignContext context, Target.HardCodedTarget<T> target) {
					Field field = ReflectionUtils.findField(Feign.Builder.class,
							"client");
					ReflectionUtils.makeAccessible(field);
					Client client = (Client) ReflectionUtils.getField(field, feign);
					if (target.name().equals("localappurl")) {
						assertThat(client).isInstanceOf(ApacheHttpClient.class)
								.as("client was wrong type");
					}
					return feign.target(target);
				}
			};
		}

	}

	public static class Hello {

		private String message;

		Hello() {

		}

		Hello(String message) {
			this.message = message;
		}

		public String getMessage() {
			return message;
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
			return Objects.equals(message, that.message);
		}

		@Override
		public int hashCode() {
			return Objects.hash(message);
		}

	}

}
