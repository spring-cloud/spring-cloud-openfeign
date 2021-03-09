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

package org.springframework.cloud.openfeign.support;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.cloud.openfeign.support.FeignHttpClientProperties.Hc5Properties.PoolConcurrencyPolicy;
import org.springframework.cloud.openfeign.support.FeignHttpClientProperties.Hc5Properties.PoolReusePolicy;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cloud.openfeign.support.FeignHttpClientProperties.Hc5Properties.DEFAULT_SOCKET_TIMEOUT;
import static org.springframework.cloud.openfeign.support.FeignHttpClientProperties.Hc5Properties.DEFAULT_SOCKET_TIMEOUT_UNIT;

/**
 * @author Ryan Baxter
 * @author Nguyen Ky Thanh
 */
@RunWith(SpringRunner.class)
@DirtiesContext
public class FeignHttpClientPropertiesTests {

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@After
	public void clear() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testDefaults() {
		setupContext();
		assertThat(getProperties().getConnectionTimeout())
				.isEqualTo(FeignHttpClientProperties.DEFAULT_CONNECTION_TIMEOUT);
		assertThat(getProperties().getMaxConnections())
				.isEqualTo(FeignHttpClientProperties.DEFAULT_MAX_CONNECTIONS);
		assertThat(getProperties().getMaxConnectionsPerRoute())
				.isEqualTo(FeignHttpClientProperties.DEFAULT_MAX_CONNECTIONS_PER_ROUTE);
		assertThat(getProperties().getTimeToLive())
				.isEqualTo(FeignHttpClientProperties.DEFAULT_TIME_TO_LIVE);
		assertThat(getProperties().isDisableSslValidation())
				.isEqualTo(FeignHttpClientProperties.DEFAULT_DISABLE_SSL_VALIDATION);
		assertThat(getProperties().isFollowRedirects())
				.isEqualTo(FeignHttpClientProperties.DEFAULT_FOLLOW_REDIRECTS);
		assertThat(getProperties().getHc5().getPoolConcurrencyPolicy())
				.isEqualTo(PoolConcurrencyPolicy.STRICT);
		assertThat(getProperties().getHc5().getPoolReusePolicy())
				.isEqualTo(PoolReusePolicy.FIFO);
		assertThat(getProperties().getHc5().getSocketTimeout())
				.isEqualTo(DEFAULT_SOCKET_TIMEOUT);
		assertThat(getProperties().getHc5().getSocketTimeoutUnit())
				.isEqualTo(DEFAULT_SOCKET_TIMEOUT_UNIT);
	}

	@Test
	public void testCustomization() {
		TestPropertyValues
				.of("feign.httpclient.maxConnections=2",
						"feign.httpclient.connectionTimeout=2",
						"feign.httpclient.maxConnectionsPerRoute=2",
						"feign.httpclient.timeToLive=2",
						"feign.httpclient.disableSslValidation=true",
						"feign.httpclient.followRedirects=false",
						"feign.httpclient.disableSslValidation=true",
						"feign.httpclient.followRedirects=false",
						"feign.httpclient.hc5.poolConcurrencyPolicy=lax",
						"feign.httpclient.hc5.poolReusePolicy=lifo",
						"feign.httpclient.hc5.socketTimeout=200",
						"feign.httpclient.hc5.socketTimeoutUnit=milliseconds")
				.applyTo(this.context);
		setupContext();
		assertThat(getProperties().getMaxConnections()).isEqualTo(2);
		assertThat(getProperties().getConnectionTimeout()).isEqualTo(2);
		assertThat(getProperties().getMaxConnectionsPerRoute()).isEqualTo(2);
		assertThat(getProperties().getTimeToLive()).isEqualTo(2L);
		assertThat(getProperties().isDisableSslValidation()).isTrue();
		assertThat(getProperties().isFollowRedirects()).isFalse();
		assertThat(getProperties().getHc5().getPoolConcurrencyPolicy())
				.isEqualTo(PoolConcurrencyPolicy.LAX);
		assertThat(getProperties().getHc5().getPoolReusePolicy())
				.isEqualTo(PoolReusePolicy.LIFO);
		assertThat(getProperties().getHc5().getSocketTimeout()).isEqualTo(200);
		assertThat(getProperties().getHc5().getSocketTimeoutUnit())
				.isEqualTo(TimeUnit.MILLISECONDS);
	}

	private void setupContext() {
		this.context.register(PropertyPlaceholderAutoConfiguration.class,
				TestConfiguration.class);
		this.context.refresh();
	}

	private FeignHttpClientProperties getProperties() {
		return this.context.getBean(FeignHttpClientProperties.class);
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties
	protected static class TestConfiguration {

		@Bean
		FeignHttpClientProperties zuulProperties() {
			return new FeignHttpClientProperties();
		}

	}

}
