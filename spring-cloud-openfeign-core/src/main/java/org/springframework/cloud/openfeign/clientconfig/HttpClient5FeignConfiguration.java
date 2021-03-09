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

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.socket.LayeredConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.ssl.TLS;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.apache.hc.core5.pool.PoolReusePolicy;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;

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

	private static final Log LOG = LogFactory.getLog(HttpClient5FeignConfiguration.class);

	private CloseableHttpClient httpClient5;

	@Bean
	@ConditionalOnMissingBean(HttpClientConnectionManager.class)
	public HttpClientConnectionManager hc5ConnectionManager(
			FeignHttpClientProperties httpClientProperties) {
		return PoolingHttpClientConnectionManagerBuilder.create()
				.setSSLSocketFactory(httpsSSLConnectionSocketFactory(
						httpClientProperties.isDisableSslValidation()))
				.setMaxConnTotal(httpClientProperties.getMaxConnections())
				.setMaxConnPerRoute(httpClientProperties.getMaxConnectionsPerRoute())
				.setConnPoolPolicy(PoolReusePolicy.valueOf(
						httpClientProperties.getHc5().getPoolReusePolicy().name()))
				.setPoolConcurrencyPolicy(PoolConcurrencyPolicy.valueOf(
						httpClientProperties.getHc5().getPoolConcurrencyPolicy().name()))
				.setConnectionTimeToLive(
						TimeValue.of(httpClientProperties.getTimeToLive(),
								httpClientProperties.getTimeToLiveUnit()))
				.setDefaultSocketConfig(SocketConfig.custom()
						.setSoTimeout(Timeout.of(
								httpClientProperties.getHc5().getSocketTimeout(),
								httpClientProperties.getHc5().getSocketTimeoutUnit()))
						.build())
				.build();
	}

	@Bean
	public CloseableHttpClient httpClient5(HttpClientConnectionManager connectionManager,
			FeignHttpClientProperties httpClientProperties) {
		httpClient5 = HttpClients.custom().disableCookieManagement().useSystemProperties()
				.setConnectionManager(connectionManager).evictExpiredConnections()
				.setDefaultRequestConfig(
						RequestConfig.custom()
								.setConnectTimeout(Timeout.of(
										httpClientProperties.getConnectionTimeout(),
										TimeUnit.MILLISECONDS))
								.setRedirectsEnabled(
										httpClientProperties.isFollowRedirects())
								.build())
				.build();
		return httpClient5;
	}

	@PreDestroy
	public void destroy() {
		if (httpClient5 != null) {
			httpClient5.close(CloseMode.GRACEFUL);
		}
	}

	private LayeredConnectionSocketFactory httpsSSLConnectionSocketFactory(
			boolean isDisableSslValidation) {
		final SSLConnectionSocketFactoryBuilder sslConnectionSocketFactoryBuilder = SSLConnectionSocketFactoryBuilder
				.create().setTlsVersions(TLS.V_1_3, TLS.V_1_2);

		if (isDisableSslValidation) {
			try {
				final SSLContext sslContext = SSLContext.getInstance("SSL");
				sslContext.init(null,
						new TrustManager[] { new DisabledValidationTrustManager() },
						new SecureRandom());
				sslConnectionSocketFactoryBuilder.setSslContext(sslContext);
			}
			catch (NoSuchAlgorithmException e) {
				LOG.warn("Error creating SSLContext", e);
			}
			catch (KeyManagementException e) {
				LOG.warn("Error creating SSLContext", e);
			}
		}
		else {
			sslConnectionSocketFactoryBuilder
					.setSslContext(SSLContexts.createSystemDefault());
		}

		return sslConnectionSocketFactoryBuilder.build();
	}

	static class DisabledValidationTrustManager implements X509TrustManager {

		DisabledValidationTrustManager() {
		}

		public void checkClientTrusted(X509Certificate[] x509Certificates, String s)
				throws CertificateException {
		}

		public void checkServerTrusted(X509Certificate[] x509Certificates, String s)
				throws CertificateException {
		}

		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}

	}

}
