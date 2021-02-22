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

import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import feign.AsyncClient;
import feign.hc5.AsyncApacheHttp5Client;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.nio.AsyncClientConnectionManager;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.core5.http.ssl.TLS;
import org.apache.hc.core5.http2.HttpVersionPolicy;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.pool.PoolReusePolicy;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.openfeign.support.FeignAsyncHttpClientProperties;
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
	public AsyncClientConnectionManager connectionManager(FeignHttpClientProperties httpClientProperties,
			FeignAsyncHttpClientProperties asyncHttpClientProperties) {
		return PoolingAsyncClientConnectionManagerBuilder.create()
				.setMaxConnTotal(httpClientProperties.getMaxConnections())
				.setMaxConnPerRoute(httpClientProperties.getMaxConnectionsPerRoute())
				.setTlsStrategy(ClientTlsStrategyBuilder.create().setSslContext(SSLContexts.createSystemDefault())
						.setTlsVersions(TLS.V_1_3, TLS.V_1_2).build())
				.setPoolConcurrencyPolicy(asyncHttpClientProperties.getPoolConcurrencyPolicy())
				.setConnPoolPolicy(PoolReusePolicy.LIFO)
				.setConnectionTimeToLive(
						TimeValue.of(httpClientProperties.getTimeToLive(), httpClientProperties.getTimeToLiveUnit()))
				.build();
	}

	@Bean
	public CloseableHttpAsyncClient httpClient(AsyncClientConnectionManager connectionManager,
			FeignHttpClientProperties httpClientProperties) {
		final RequestConfig defaultRequestConfig = RequestConfig.custom()
				.setConnectTimeout(Timeout.of(httpClientProperties.getConnectionTimeout(), TimeUnit.MILLISECONDS))
				.setRedirectsEnabled(httpClientProperties.isFollowRedirects()).build();
		this.asyncHttpClient5 = HttpAsyncClients.custom().disableCookieManagement().useSystemProperties()
				.setConnectionManager(connectionManager).setDefaultRequestConfig(defaultRequestConfig)
				.setVersionPolicy(HttpVersionPolicy.NEGOTIATE).build();
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
		if (this.asyncHttpClient5 != null) {
			this.asyncHttpClient5.close(CloseMode.GRACEFUL);
		}
	}

}
