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

import feign.AsyncClient;
import feign.hc5.AsyncApacheHttp5Client;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.nio.AsyncClientConnectionManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.commons.httpclient.HttpClientConfiguration;
import org.springframework.cloud.test.ModifiedClassPathRunner;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Thanh Nguyen Ky
 */
@RunWith(ModifiedClassPathRunner.class)
public class FeignAsyncHttpClient5ConfigurationTests {

	private ConfigurableApplicationContext context;

	@Before
	public void setUp() {
		this.context = new SpringApplicationBuilder()
				.properties("feign.httpclient.hc5.asyncEnabled=true", "feign.httpclient.enabled=false")
				.web(WebApplicationType.NONE).sources(HttpClientConfiguration.class, FeignAutoConfiguration.class)
				.run();
	}

	@After
	public void tearDown() {
		if (context != null) {
			context.close();
		}
	}

	@Test
	public void verifyAsyncHttpClient5AutoConfig() {
		CloseableHttpAsyncClient httpAsyncClient = context.getBean(CloseableHttpAsyncClient.class);
		assertThat(httpAsyncClient).isNotNull();
		AsyncClientConnectionManager connectionManager = context.getBean(AsyncClientConnectionManager.class);
		assertThat(connectionManager).isInstanceOf(PoolingAsyncClientConnectionManager.class);
		AsyncClient feignAsyncClient = context.getBean(AsyncClient.class);
		assertThat(feignAsyncClient).isInstanceOf(AsyncApacheHttp5Client.class);
	}

}
