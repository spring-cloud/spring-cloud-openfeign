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

package org.springframework.cloud.openfeign.support;

import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Nguyen Ky Thanh
 */
@RunWith(SpringRunner.class)
@DirtiesContext
public class FeignAsyncHttpClientPropertiesTests {

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
		assertThat(getProperties().getPoolConcurrencyPolicy())
				.isEqualTo(FeignAsyncHttpClientProperties.DEFAULT_POOL_CONCURRENCY_POLICY);
	}

	@Test
	public void testCustomization() {
		TestPropertyValues.of("feign.asynchttpclient.poolConcurrencyPolicy=LAX").applyTo(this.context);
		setupContext();
		assertThat(getProperties().getPoolConcurrencyPolicy()).isEqualTo(PoolConcurrencyPolicy.LAX);
	}

	private void setupContext() {
		this.context.register(PropertyPlaceholderAutoConfiguration.class, TestConfiguration.class);
		this.context.refresh();
	}

	private FeignAsyncHttpClientProperties getProperties() {
		return this.context.getBean(FeignAsyncHttpClientProperties.class);
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties
	protected static class TestConfiguration {

		@Bean
		FeignAsyncHttpClientProperties zuulProperties() {
			return new FeignAsyncHttpClientProperties();
		}

	}

}
