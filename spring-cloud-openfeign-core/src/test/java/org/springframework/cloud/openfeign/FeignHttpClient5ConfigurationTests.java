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

import feign.Client;
import feign.hc5.ApacheHttp5Client;
import feign.httpclient.ApacheHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.commons.httpclient.HttpClientConfiguration;
import org.springframework.cloud.test.ClassPathExclusions;
import org.springframework.cloud.test.ModifiedClassPathRunner;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Nguyen Ky Thanh
 */
public class FeignHttpClient5ConfigurationTests {

	private static void verifyHc4BeansAvailable(ConfigurableApplicationContext context) {
		org.apache.http.impl.client.CloseableHttpClient httpClient4 = context
				.getBean(org.apache.http.impl.client.CloseableHttpClient.class);
		assertThat(httpClient4).isNotNull();
		org.apache.http.conn.HttpClientConnectionManager connectionManager4 = context
				.getBean(org.apache.http.conn.HttpClientConnectionManager.class);
		assertThat(connectionManager4).isInstanceOf(
				org.apache.http.impl.conn.PoolingHttpClientConnectionManager.class);
		Client client = context.getBean(Client.class);
		assertThat(client).isInstanceOf(ApacheHttpClient.class);
	}

	private static void verifyHc5BeansAvailable(ConfigurableApplicationContext context) {
		CloseableHttpClient httpClient = context.getBean(CloseableHttpClient.class);
		assertThat(httpClient).isNotNull();
		HttpClientConnectionManager connectionManager = context
				.getBean(HttpClientConnectionManager.class);
		assertThat(connectionManager)
				.isInstanceOf(PoolingHttpClientConnectionManager.class);
		Client client = context.getBean(Client.class);
		assertThat(client).isInstanceOf(ApacheHttp5Client.class);
	}

	@RunWith(ModifiedClassPathRunner.class)
	@ClassPathExclusions("ribbon-loadbalancer-{version:\\d.*}.jar")
	public static class WithoutLoadBalancerInClasspath {

		@Test
		public void verifyHttpClient5AutoConfig() {
			ConfigurableApplicationContext context = new SpringApplicationBuilder()
					.properties("feign.httpclient.hc5.enabled=true",
							"feign.httpclient.enabled=false")
					.web(WebApplicationType.NONE)
					.sources(HttpClientConfiguration.class, FeignAutoConfiguration.class)
					.run();

			verifyHc5BeansAvailable(context);

			if (context != null) {
				context.close();
			}
		}

		@Test
		public void hc5ShouldWinIfTheBothVersionsAvailable() {
			ConfigurableApplicationContext context = new SpringApplicationBuilder()
					.properties("feign.httpclient.hc5.enabled=true",
							"feign.httpclient.enabled=true")
					.web(WebApplicationType.NONE)
					.sources(HttpClientConfiguration.class, FeignAutoConfiguration.class)
					.run();

			Client client = context.getBean(Client.class);
			assertThat(client).isInstanceOf(ApacheHttp5Client.class);

			if (context != null) {
				context.close();
			}
		}

		@Test
		public void hc4ShouldBeTheDefaultIfHc5NotEnabled() {
			ConfigurableApplicationContext context = new SpringApplicationBuilder()
					.properties("feign.httpclient.hc5.enabled=false",
							"feign.httpclient.enabled=true")
					.web(WebApplicationType.NONE)
					.sources(HttpClientConfiguration.class, FeignAutoConfiguration.class)
					.run();

			verifyHc4BeansAvailable(context);

			if (context != null) {
				context.close();
			}
		}

	}

	@RunWith(ModifiedClassPathRunner.class)
	@ClassPathExclusions({ "ribbon-loadbalancer-{version:\\d.*}.jar",
			"feign-hc5-{version:\\d.*}.jar", "httpclient5-{version:\\d.*}.jar",
			"httpcore5-{version:\\d.*}.jar", "httpcore5-h2-{version:\\d.*}.jar" })
	public static class WithoutLoadBalancerAndHc5InClasspath {

		@Test
		public void hc4ShouldWinEvenHc5ConfigEnabled() {
			ConfigurableApplicationContext context = new SpringApplicationBuilder()
					.properties("feign.httpclient.hc5.enabled=true")
					.web(WebApplicationType.NONE)
					.sources(HttpClientConfiguration.class, FeignAutoConfiguration.class)
					.run();

			verifyHc4BeansAvailable(context);

			if (context != null) {
				context.close();
			}
		}

		@Test
		public void hc4ShouldBeTheDefault() {
			ConfigurableApplicationContext context = new SpringApplicationBuilder()
					.web(WebApplicationType.NONE)
					.sources(HttpClientConfiguration.class, FeignAutoConfiguration.class)
					.run();

			verifyHc4BeansAvailable(context);

			if (context != null) {
				context.close();
			}
		}

	}

}
