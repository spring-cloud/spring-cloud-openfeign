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

import org.springframework.cloud.openfeign.support.FeignHttpClientProperties;

/**
 * @author Nguyen Ky Thanh
 */
public final class AsyncHttpClient5FeignConfigurationHelper {

	private AsyncHttpClient5FeignConfigurationHelper() {
	}

	public static AsyncClientConnectionManager connectionManager(FeignHttpClientProperties httpClientProperties) {
		return PoolingAsyncClientConnectionManagerBuilder.create()
				.setMaxConnTotal(httpClientProperties.getMaxConnections())
				.setMaxConnPerRoute(httpClientProperties.getMaxConnectionsPerRoute())
				.setTlsStrategy(ClientTlsStrategyBuilder.create().setSslContext(SSLContexts.createSystemDefault())
						.setTlsVersions(TLS.V_1_3, TLS.V_1_2).build())
				.setPoolConcurrencyPolicy(httpClientProperties.getPoolConcurrencyPolicy())
				.setConnPoolPolicy(PoolReusePolicy.LIFO)
				.setConnectionTimeToLive(
						TimeValue.of(httpClientProperties.getTimeToLive(), httpClientProperties.getTimeToLiveUnit()))
				.build();
	}

	public static CloseableHttpAsyncClient httpClient(AsyncClientConnectionManager connectionManager,
			FeignHttpClientProperties httpClientProperties) {
		return HttpAsyncClients.custom().disableCookieManagement().useSystemProperties()
				.setConnectionManager(connectionManager)
				.setDefaultRequestConfig(RequestConfig.custom()
						.setConnectTimeout(
								Timeout.of(httpClientProperties.getConnectionTimeout(), TimeUnit.MILLISECONDS))
						.setRedirectsEnabled(httpClientProperties.isFollowRedirects()).build())
				.setVersionPolicy(HttpVersionPolicy.NEGOTIATE).build();
	}

	@PreDestroy
	public static void destroy(CloseableHttpAsyncClient asyncHttpClient5) {
		if (asyncHttpClient5 != null) {
			asyncHttpClient5.close(CloseMode.GRACEFUL);
		}
	}

}
