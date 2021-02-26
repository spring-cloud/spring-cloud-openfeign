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

package org.springframework.cloud.openfeign.clientconfig;

import javax.annotation.PreDestroy;

import feign.AsyncClient;
import feign.hc5.AsyncApacheHttp5Client;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.nio.AsyncClientConnectionManager;
import org.apache.hc.client5.http.protocol.HttpClientContext;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.openfeign.support.FeignHttpClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Default configuration for {@link CloseableHttpAsyncClient}.
 *
 * @author Nguyen Ky Thanh
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnMissingBean(CloseableHttpAsyncClient.class)
public class AsyncHttpClient5FeignConfiguration {

	private CloseableHttpAsyncClient asyncHttpClient5;

	@Bean
	@ConditionalOnMissingBean(AsyncClientConnectionManager.class)
	public AsyncClientConnectionManager connectionManager(FeignHttpClientProperties httpClientProperties) {
		return AsyncHttpClient5FeignConfigurationHelper.connectionManager(httpClientProperties);
	}

	@Bean
	public CloseableHttpAsyncClient httpClient(AsyncClientConnectionManager connectionManager,
			FeignHttpClientProperties httpClientProperties) {
		asyncHttpClient5 = AsyncHttpClient5FeignConfigurationHelper.httpClient(connectionManager, httpClientProperties);
		asyncHttpClient5.start();
		return asyncHttpClient5;
	}

	@Bean
	@ConditionalOnMissingBean(AsyncClient.class)
	public AsyncClient<HttpClientContext> feignClient(CloseableHttpAsyncClient httpAsyncClient) {
		return new AsyncApacheHttp5Client(httpAsyncClient);
	}

	@PreDestroy
	public void destroy() {
		AsyncHttpClient5FeignConfigurationHelper.destroy(asyncHttpClient5);
	}

}
