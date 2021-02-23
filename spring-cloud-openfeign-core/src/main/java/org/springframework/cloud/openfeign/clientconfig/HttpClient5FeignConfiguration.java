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

package org.springframework.cloud.openfeign.clientconfig;

import javax.annotation.PreDestroy;

import feign.Client;
import feign.hc5.ApacheHttp5Client;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.openfeign.support.FeignHttpClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Default configuration for {@link CloseableHttpClient}.
 *
 * @author Nguyen Ky Thanh
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnMissingBean(CloseableHttpClient.class)
public class HttpClient5FeignConfiguration {

	private CloseableHttpClient httpClient5;

	@Bean
	@ConditionalOnMissingBean(HttpClientConnectionManager.class)
	public HttpClientConnectionManager connectionManager(FeignHttpClientProperties httpClientProperties) {
		return HttpClient5FeignConfigurationHelper.connectionManager(httpClientProperties);
	}

	@Bean
	public CloseableHttpClient httpClient(HttpClientConnectionManager connectionManager,
			FeignHttpClientProperties httpClientProperties) {
		this.httpClient5 = HttpClient5FeignConfigurationHelper.httpClient(connectionManager, httpClientProperties);
		return this.httpClient5;
	}

	@Bean
	@ConditionalOnMissingBean(Client.class)
	public Client feignClient(CloseableHttpClient httpClient5) {
		return new ApacheHttp5Client(httpClient5);
	}

	@PreDestroy
	public void destroy() {
		HttpClient5FeignConfigurationHelper.destroy(this.httpClient5);
	}

}
