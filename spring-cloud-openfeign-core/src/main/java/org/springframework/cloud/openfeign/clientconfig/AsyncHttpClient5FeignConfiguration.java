package org.springframework.cloud.openfeign.clientconfig;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import feign.AsyncClient;
import feign.hc5.AsyncApacheHttp5Client;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.nio.AsyncClientConnectionManager;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.core5.http.ssl.TLS;
import org.apache.hc.core5.http2.HttpVersionPolicy;
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
 * Default configuration for {@link CloseableHttpAsyncClient}.
 *
 * @author Nguyen Ky Thanh
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnMissingBean(CloseableHttpAsyncClient.class)
public class AsyncHttpClient5FeignConfiguration {

	private static final Log LOG = LogFactory.getLog(AsyncHttpClient5FeignConfiguration.class);

	private CloseableHttpAsyncClient asyncHttpClient5;

	@Bean
	@ConditionalOnMissingBean(AsyncClientConnectionManager.class)
	public AsyncClientConnectionManager connectionManager(FeignHttpClientProperties httpClientProperties) {
		final PoolingAsyncClientConnectionManager connectionManager = PoolingAsyncClientConnectionManagerBuilder.create()
			.setMaxConnPerRoute(httpClientProperties.getMaxConnectionsPerRoute())
			.setMaxConnTotal(httpClientProperties.getMaxConnections())
			.setTlsStrategy(ClientTlsStrategyBuilder.create()
				.setSslContext(SSLContexts.createSystemDefault())
				.setTlsVersions(TLS.V_1_3, TLS.V_1_2)
				.build())
			.setPoolConcurrencyPolicy(PoolConcurrencyPolicy.LAX)
			.setConnPoolPolicy(PoolReusePolicy.LIFO)
			.setConnectionTimeToLive(TimeValue.of(
				httpClientProperties.getTimeToLive(),
				httpClientProperties.getTimeToLiveUnit()))
			.build();
		return connectionManager;
	}

	@Bean
	public CloseableHttpAsyncClient httpClient(AsyncClientConnectionManager connectionManager,
		FeignHttpClientProperties httpClientProperties) {

		RequestConfig defaultRequestConfig = RequestConfig.custom()
				.setConnectTimeout(Timeout.of(httpClientProperties.getConnectionTimeout(), TimeUnit.MILLISECONDS))
				.setRedirectsEnabled(httpClientProperties.isFollowRedirects()).build();

		this.asyncHttpClient5 = HttpAsyncClients.custom()
			.setConnectionManager(connectionManager)
			.setDefaultRequestConfig(defaultRequestConfig)
			.setVersionPolicy(HttpVersionPolicy.NEGOTIATE)
			.build();

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
			try {
				this.asyncHttpClient5.close();
			}
			catch (IOException e) {
				if (LOG.isErrorEnabled()) {
					LOG.error("Could not correctly close asyncHttpClient5.");
				}
			}
		}
	}
}
