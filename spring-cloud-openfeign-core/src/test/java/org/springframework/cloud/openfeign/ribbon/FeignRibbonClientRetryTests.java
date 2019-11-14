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

package org.springframework.cloud.openfeign.ribbon;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.netflix.ribbon.StaticServerList;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.test.NoSecurityConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Tests the Feign Retryer, not ribbon retry.
 *
 * @author Spencer Gibb
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = FeignRibbonClientRetryTests.Application.class,
		webEnvironment = RANDOM_PORT,
		value = { "spring.application.name=feignclientretrytest",
				"feign.okhttp.enabled=false", "feign.httpclient.enabled=false",
				"feign.hystrix.enabled=false", "localapp.ribbon.MaxAutoRetries=2",
				"localapp.ribbon.MaxAutoRetriesNextServer=3" })
@DirtiesContext
public class FeignRibbonClientRetryTests {

	@Value("${local.server.port}")
	private int port = 0;

	@Autowired
	private TestClient testClient;

	@Test
	public void testClient() {
		assertThat(this.testClient).as("testClient was null").isNotNull();
		assertThat(Proxy.isProxyClass(this.testClient.getClass()))
				.as("testClient is not a java Proxy").isTrue();
		InvocationHandler invocationHandler = Proxy.getInvocationHandler(this.testClient);
		assertThat(invocationHandler).as("invocationHandler was null").isNotNull();
	}

	@Test
	public void testRetries() {
		int retryMe = this.testClient.retryMe();
		assertThat(1).as("retryCount didn't match").isEqualTo(retryMe);
		// TODO: not sure how to verify retry happens. Debugging through it, it works
		// maybe the assertEquals above is enough because of the bogus servers
	}

	@FeignClient("localapp")
	protected interface TestClient {

		@RequestMapping(method = RequestMethod.GET, value = "/hello")
		Hello getHello();

		@RequestMapping(method = RequestMethod.GET, value = "/retryme")
		int retryMe();

	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	@RestController
	@EnableFeignClients(clients = TestClient.class)
	@RibbonClient(name = "localapp", configuration = LocalRibbonClientConfiguration.class)
	@Import(NoSecurityConfiguration.class)
	public static class Application {

		private AtomicInteger retries = new AtomicInteger(1);

		@RequestMapping(method = RequestMethod.GET, value = "/hello")
		public Hello getHello() {
			return new Hello("hello world 1");
		}

		@RequestMapping(method = RequestMethod.GET, value = "/retryme")
		public int retryMe() {
			return this.retries.getAndIncrement();
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

	}

}

// Load balancer with fixed server list for "local" pointing to localhost
// some bogus servers are thrown in to test retry
@Configuration(proxyBeanMethods = false)
class LocalRibbonClientConfiguration {

	@Value("${local.server.port}")
	private int port = 0;

	@Bean
	public ServerList<Server> ribbonServerList() {
		return new StaticServerList<>(new Server("mybadhost", 80),
				new Server("mybadhost2", 10002), new Server("mybadhost3", 10003),
				new Server("localhost", this.port));
	}

}
