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

package org.springframework.cloud.openfeign.async;

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
import org.apache.hc.client5.http.cookie.StandardCookieSpec;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.nio.AsyncClientConnectionManager;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.core5.http.nio.ssl.TlsStrategy;
import org.apache.hc.core5.http.ssl.TLS;
import org.apache.hc.core5.http2.HttpVersionPolicy;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.apache.hc.core5.pool.PoolReusePolicy;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.support.FeignHttpClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Nguyen Ky Thanh
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnMissingBean(CloseableHttpAsyncClient.class)
@EnableConfigurationProperties(FeignHttpClientProperties.class)
public class AsyncHttpClient5FeignConfiguration {

	private static final Log LOG = LogFactory
			.getLog(AsyncHttpClient5FeignConfiguration.class);

	private CloseableHttpAsyncClient asyncHttpClient5;

	@Bean
	@ConditionalOnMissingBean(HttpClientConnectionManager.class)
	public AsyncClientConnectionManager asyncHc5ConnectionManager(
			FeignHttpClientProperties httpClientProperties) {
		return PoolingAsyncClientConnectionManagerBuilder.create()
				.setTlsStrategy(
						clientTlsStrategy(httpClientProperties.isDisableSslValidation()))
				.setMaxConnTotal(httpClientProperties.getMaxConnections())
				.setMaxConnPerRoute(httpClientProperties.getMaxConnectionsPerRoute())
				.setConnPoolPolicy(PoolReusePolicy.valueOf(
						httpClientProperties.getAsyncHc5().getPoolReusePolicy().name()))
				.setPoolConcurrencyPolicy(
						PoolConcurrencyPolicy.valueOf(httpClientProperties.getAsyncHc5()
								.getPoolConcurrencyPolicy().name()))
				.setConnectionTimeToLive(
						TimeValue.of(httpClientProperties.getTimeToLive(),
								httpClientProperties.getTimeToLiveUnit()))
				.build();
	}

	@Bean
	public CloseableHttpAsyncClient asyncHttpClient5(
			AsyncClientConnectionManager asyncClientConnectionManager,
			FeignHttpClientProperties httpClientProperties) {
		final IOReactorConfig ioReactorConfig = IOReactorConfig.custom()
				.setSoTimeout(Timeout
						.ofMilliseconds(httpClientProperties.getConnectionTimerRepeat()))
				.build();
		asyncHttpClient5 = HttpAsyncClients.custom().disableCookieManagement()
				.useSystemProperties() // Need for proxy
				.setVersionPolicy(HttpVersionPolicy.valueOf(
						httpClientProperties.getAsyncHc5().getHttpVersionPolicy().name())) // Need
																							// for
																							// proxy
				.setConnectionManager(asyncClientConnectionManager)
				.evictExpiredConnections().setIOReactorConfig(ioReactorConfig)
				.setDefaultRequestConfig(
						RequestConfig.custom()
								.setConnectTimeout(Timeout.of(
										httpClientProperties.getConnectionTimeout(),
										TimeUnit.MILLISECONDS))
								.setResponseTimeout(Timeout.of(
										httpClientProperties.getAsyncHc5()
												.getResponseTimeout(),
										httpClientProperties.getAsyncHc5()
												.getResponseTimeoutUnit()))
								.setCookieSpec(StandardCookieSpec.STRICT).build())
				.build();
		asyncHttpClient5.start();
		return asyncHttpClient5;
	}

	@PreDestroy
	public void destroy() {
		if (asyncHttpClient5 != null) {
			asyncHttpClient5.close(CloseMode.GRACEFUL);
		}
	}

	private TlsStrategy clientTlsStrategy(boolean isDisableSslValidation) {
		final ClientTlsStrategyBuilder clientTlsStrategyBuilder = ClientTlsStrategyBuilder
				.create();

		if (isDisableSslValidation) {
			try {
				final SSLContext disabledSslContext = SSLContext.getInstance("SSL");
				disabledSslContext.init(null, new TrustManager[] {
						new AsyncHttpClient5FeignConfiguration.DisabledValidationTrustManager() },
						new SecureRandom());
				clientTlsStrategyBuilder.setSslContext(disabledSslContext);
			}
			catch (NoSuchAlgorithmException e) {
				LOG.warn("Error creating SSLContext", e);
			}
			catch (KeyManagementException e) {
				LOG.warn("Error creating SSLContext", e);
			}
		}
		else {
			clientTlsStrategyBuilder.setSslContext(SSLContexts.createSystemDefault());
		}

		return clientTlsStrategyBuilder.setTlsVersions(TLS.V_1_3, TLS.V_1_2).build();
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
