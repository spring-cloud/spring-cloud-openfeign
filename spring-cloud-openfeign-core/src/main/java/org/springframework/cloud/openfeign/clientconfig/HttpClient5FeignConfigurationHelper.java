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

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.ssl.TLS;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.pool.PoolReusePolicy;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;

import org.springframework.cloud.openfeign.support.FeignHttpClientProperties;

/**
 * @author Nguyen Ky Thanh
 */
public final class HttpClient5FeignConfigurationHelper {

	private HttpClient5FeignConfigurationHelper() {
	}

	public static HttpClientConnectionManager connectionManager(FeignHttpClientProperties httpClientProperties) {
		return PoolingHttpClientConnectionManagerBuilder.create()
				.setMaxConnTotal(httpClientProperties.getMaxConnections())
				.setMaxConnPerRoute(httpClientProperties.getMaxConnectionsPerRoute())
				.setConnPoolPolicy(PoolReusePolicy.LIFO)
				.setConnectionTimeToLive(
						TimeValue.of(httpClientProperties.getTimeToLive(), httpClientProperties.getTimeToLiveUnit()))
				.setPoolConcurrencyPolicy(httpClientProperties.getPoolConcurrencyPolicy())
				.setSSLSocketFactory(SSLConnectionSocketFactoryBuilder.create()
						.setSslContext(SSLContexts.createSystemDefault()).setTlsVersions(TLS.V_1_3, TLS.V_1_2).build())
				.setDefaultSocketConfig(
						SocketConfig.custom().setSoTimeout(Timeout.of(httpClientProperties.getSocketTimeout(),
								httpClientProperties.getSocketTimeoutUnit())).build())
				.build();
	}

	public static CloseableHttpClient httpClient(HttpClientConnectionManager connectionManager,
			FeignHttpClientProperties httpClientProperties) {
		return HttpClients.custom().disableCookieManagement().useSystemProperties()
				.setConnectionManager(connectionManager)
				.setDefaultRequestConfig(RequestConfig.custom()
						.setConnectTimeout(
								Timeout.of(httpClientProperties.getConnectionTimeout(), TimeUnit.MILLISECONDS))
						.setRedirectsEnabled(httpClientProperties.isFollowRedirects()).build())
				.build();
	}

	public static void destroy(CloseableHttpClient httpClient5) {
		if (httpClient5 != null) {
			httpClient5.close(CloseMode.GRACEFUL);
		}
	}

}
