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

import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.IClientConfig;
import feign.Request;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.FeignContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = LoadBalancerFeignClientOverrideTests.TestConfiguration.class,
		webEnvironment = WebEnvironment.RANDOM_PORT,
		value = { "spring.application.name=loadBalancerFeignClientTests",
				"feign.httpclient.enabled=false", "feign.okhttp.enabled=false" })
@DirtiesContext
public class LoadBalancerFeignClientOverrideTests {

	@Autowired
	private FeignContext context;

	@Test
	public void overrideRequestOptions() {
		// specific ribbon 'bar' configuration via spring bean
		Request.Options barOptions = this.context.getInstance("bar",
				Request.Options.class);
		assertThat(barOptions.connectTimeoutMillis()).isEqualTo(1);
		assertThat(barOptions.readTimeoutMillis()).isEqualTo(2);
		assertOptions(barOptions, "bar", 1, 2);

		// specific ribbon 'foo' configuration via application.yml
		Request.Options fooOptions = this.context.getInstance("foo",
				Request.Options.class);
		assertThat(fooOptions).isEqualTo(LoadBalancerFeignClient.DEFAULT_OPTIONS);
		assertOptions(fooOptions, "foo", 7, 17);

		// generic ribbon default configuration
		Request.Options bazOptions = this.context.getInstance("baz",
				Request.Options.class);
		assertThat(bazOptions).isEqualTo(LoadBalancerFeignClient.DEFAULT_OPTIONS);
		assertOptions(bazOptions, "baz", 3001, 60001);
	}

	void assertOptions(Request.Options options, String name, int expectedConnect,
			int expectedRead) {
		LoadBalancerFeignClient client = this.context.getInstance(name,
				LoadBalancerFeignClient.class);
		IClientConfig config = client.getClientConfig(options, name);
		assertThat(config.get(CommonClientConfigKey.ConnectTimeout, -1).intValue())
				.as("connect was wrong for " + name).isEqualTo(expectedConnect);
		assertThat(config.get(CommonClientConfigKey.ReadTimeout, -1).intValue())
				.as("read was wrong for " + name).isEqualTo(expectedRead);
	}

	@FeignClient(value = "foo", configuration = FooConfiguration.class)
	interface FooClient {

		@RequestMapping("/")
		String get();

	}

	@FeignClient(value = "bar", configuration = BarConfiguration.class)
	interface BarClient {

		@RequestMapping("/")
		String get();

	}

	@FeignClient("baz")
	interface BazClient {

		@RequestMapping("/")
		String get();

	}

	@Configuration(proxyBeanMethods = false)
	@EnableFeignClients(clients = { FooClient.class, BarClient.class, BazClient.class })
	@EnableAutoConfiguration
	protected static class TestConfiguration {

	}

	public static class FooConfiguration {

	}

	public static class BarConfiguration {

		@Bean
		public Request.Options feignRequestOptions() {
			return new Request.Options(1, 2);
		}

	}

}
