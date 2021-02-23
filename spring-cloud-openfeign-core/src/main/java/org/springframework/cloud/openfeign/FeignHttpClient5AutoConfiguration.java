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

import javax.annotation.PreDestroy;

import feign.AsyncClient;
import feign.Client;
import feign.Feign;
import feign.hc5.ApacheHttp5Client;
import feign.hc5.AsyncApacheHttp5Client;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.nio.AsyncClientConnectionManager;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.clientconfig.AsyncHttpClient5FeignConfigurationHelper;
import org.springframework.cloud.openfeign.clientconfig.HttpClient5FeignConfigurationHelper;
import org.springframework.cloud.openfeign.support.FeignHttpClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Nguyen Ky Thanh
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Feign.class)
@EnableConfigurationProperties({ FeignClientProperties.class, FeignHttpClientProperties.class })
public class FeignHttpClient5AutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(ApacheHttp5Client.class)
	@ConditionalOnMissingBean(CloseableHttpClient.class)
	@ConditionalOnProperty("feign.httpclient5.enabled")
	protected static class HttpClient5FeignConfiguration {

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

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(AsyncApacheHttp5Client.class)
	@ConditionalOnMissingBean(CloseableHttpAsyncClient.class)
	@ConditionalOnProperty("feign.asynchttpclient5.enabled")
	protected static class AsyncHttpClient5FeignConfiguration {

		private CloseableHttpAsyncClient asyncHttpClient5;

		@Bean
		@ConditionalOnMissingBean(AsyncClientConnectionManager.class)
		public AsyncClientConnectionManager connectionManager(FeignHttpClientProperties httpClientProperties) {
			return AsyncHttpClient5FeignConfigurationHelper.connectionManager(httpClientProperties);
		}

		@Bean
		public CloseableHttpAsyncClient httpClient(AsyncClientConnectionManager connectionManager,
				FeignHttpClientProperties httpClientProperties) {
			this.asyncHttpClient5 = AsyncHttpClient5FeignConfigurationHelper.httpClient(connectionManager,
					httpClientProperties);
			this.asyncHttpClient5.start();
			return this.asyncHttpClient5;
		}

		@Bean
		@ConditionalOnMissingBean(AsyncClient.class)
		public AsyncClient feignClient(CloseableHttpAsyncClient httpAsyncClient) {
			return new AsyncApacheHttp5Client(httpAsyncClient);
		}

		@PreDestroy
		public void destroy() {
			AsyncHttpClient5FeignConfigurationHelper.destroy(this.asyncHttpClient5);
		}

	}

}
