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

package org.springframework.cloud.openfeign;

import feign.Client;
import feign.hc5.ApacheHttp5Client;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.commons.httpclient.HttpClientConfiguration;
import org.springframework.cloud.test.ModifiedClassPathRunner;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Thanh Nguyen Ky
 */
@RunWith(ModifiedClassPathRunner.class)
public class FeignHttpClient5ConfigurationTests {

	private ConfigurableApplicationContext context;

	@Before
	public void setUp() {
		this.context = new SpringApplicationBuilder()
				.properties("debug=true", "feign.httpclient5.enabled=true", "feign.httpclient.enabled=false")
				.web(WebApplicationType.NONE).sources(HttpClientConfiguration.class, FeignAutoConfiguration.class)
				.run();
	}

	@After
	public void tearDown() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void verifyHttpClient5AutoConfig() throws Exception {
		CloseableHttpClient httpClient = this.context.getBean(CloseableHttpClient.class);
		assertThat(httpClient).isNotNull();
		HttpClientConnectionManager connectionManager = this.context.getBean(HttpClientConnectionManager.class);
		assertThat(connectionManager).isInstanceOf(PoolingHttpClientConnectionManager.class);
		Client client = this.context.getBean(Client.class);
		assertThat(client).isInstanceOf(ApacheHttp5Client.class);
	}

	@Test
	public void noMoreHttpClient4Instances() throws Exception {
		assertThatThrownBy(() -> this.context.getBean(org.apache.http.impl.client.CloseableHttpClient.class))
				.isInstanceOf(NoSuchBeanDefinitionException.class);
		assertThatThrownBy(() -> this.context.getBean(org.apache.http.conn.HttpClientConnectionManager.class))
				.isInstanceOf(NoSuchBeanDefinitionException.class);
	}

}
