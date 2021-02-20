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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import com.fasterxml.jackson.databind.Module;
import feign.AsyncClient;
import feign.Client;
import feign.Feign;
import feign.RequestInterceptor;
import feign.hc5.AsyncApacheHttp5Client;
import feign.httpclient.ApacheHttpClient;
import feign.okhttp.OkHttpClient;
import okhttp3.ConnectionPool;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.actuator.HasFeatures;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.commons.httpclient.ApacheHttpClientConnectionManagerFactory;
import org.springframework.cloud.commons.httpclient.ApacheHttpClientFactory;
import org.springframework.cloud.commons.httpclient.OkHttpClientConnectionPoolFactory;
import org.springframework.cloud.commons.httpclient.OkHttpClientFactory;
import org.springframework.cloud.openfeign.security.OAuth2FeignRequestInterceptor;
import org.springframework.cloud.openfeign.support.DefaultGzipDecoderConfiguration;
import org.springframework.cloud.openfeign.support.FeignHttpClientProperties;
import org.springframework.cloud.openfeign.support.PageJacksonModule;
import org.springframework.cloud.openfeign.support.SortJacksonModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;

/**
 * @author Spencer Gibb
 * @author Julien Roy
 * @author Grzegorz Poznachowski
 * @author Nikita Konev
 * @author Tim Peeters
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Feign.class)
@EnableConfigurationProperties({ FeignClientProperties.class, FeignHttpClientProperties.class })
@Import(DefaultGzipDecoderConfiguration.class)
public class FeignAutoConfiguration {

	private static final Log LOG = LogFactory.getLog(FeignAutoConfiguration.class);

	@Autowired(required = false)
	private List<FeignClientSpecification> configurations = new ArrayList<>();

	@Bean
	public HasFeatures feignFeature() {
		return HasFeatures.namedFeature("Feign", Feign.class);
	}

	@Bean
	public FeignContext feignContext() {
		FeignContext context = new FeignContext();
		context.setConfigurations(this.configurations);
		return context;
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ Module.class, Page.class, Sort.class })
	@ConditionalOnProperty(value = "feign.autoconfiguration.jackson.enabled", havingValue = "true")
	protected static class FeignJacksonConfiguration {

		@Bean
		@ConditionalOnMissingBean(PageJacksonModule.class)
		public PageJacksonModule pageJacksonModule() {
			return new PageJacksonModule();
		}

		@Bean
		@ConditionalOnMissingBean(SortJacksonModule.class)
		public SortJacksonModule sortModule() {
			return new SortJacksonModule();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Conditional(FeignCircuitBreakerDisabledConditions.class)
	protected static class DefaultFeignTargeterConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public Targeter feignTargeter() {
			return new DefaultTargeter();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(CircuitBreaker.class)
	@ConditionalOnProperty(value = "feign.circuitbreaker.enabled", havingValue = "true")
	protected static class CircuitBreakerPresentFeignTargeterConfiguration {

		@Bean
		@ConditionalOnMissingBean(CircuitBreakerFactory.class)
		public Targeter defaultFeignTargeter() {
			return new DefaultTargeter();
		}

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnBean(CircuitBreakerFactory.class)
		public Targeter circuitBreakerFeignTargeter(CircuitBreakerFactory circuitBreakerFactory) {
			return new FeignCircuitBreakerTargeter(circuitBreakerFactory);
		}

	}

	// the following configuration is for alternate feign clients if
	// SC loadbalancer is not on the class path.
	// see corresponding configurations in FeignLoadBalancerAutoConfiguration
	// for load-balanced clients.
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(ApacheHttpClient.class)
	@ConditionalOnMissingBean(CloseableHttpClient.class)
	@ConditionalOnProperty(value = "feign.httpclient.enabled", matchIfMissing = true)
	protected static class HttpClientFeignConfiguration {

		private final Timer connectionManagerTimer = new Timer(
				"FeignApacheHttpClientConfiguration.connectionManagerTimer", true);

		@Autowired(required = false)
		private RegistryBuilder registryBuilder;

		private CloseableHttpClient httpClient;

		@Bean
		@ConditionalOnMissingBean(HttpClientConnectionManager.class)
		public HttpClientConnectionManager connectionManager(
				ApacheHttpClientConnectionManagerFactory connectionManagerFactory,
				FeignHttpClientProperties httpClientProperties) {
			final HttpClientConnectionManager connectionManager = connectionManagerFactory.newConnectionManager(
					httpClientProperties.isDisableSslValidation(), httpClientProperties.getMaxConnections(),
					httpClientProperties.getMaxConnectionsPerRoute(), httpClientProperties.getTimeToLive(),
					httpClientProperties.getTimeToLiveUnit(), this.registryBuilder);
			this.connectionManagerTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					connectionManager.closeExpiredConnections();
				}
			}, 30000, httpClientProperties.getConnectionTimerRepeat());
			return connectionManager;
		}

		@Bean
		public CloseableHttpClient httpClient(ApacheHttpClientFactory httpClientFactory,
				HttpClientConnectionManager httpClientConnectionManager,
				FeignHttpClientProperties httpClientProperties) {
			RequestConfig defaultRequestConfig = RequestConfig.custom()
					.setConnectTimeout(httpClientProperties.getConnectionTimeout())
					.setRedirectsEnabled(httpClientProperties.isFollowRedirects()).build();
			this.httpClient = httpClientFactory.createBuilder().setConnectionManager(httpClientConnectionManager)
					.setDefaultRequestConfig(defaultRequestConfig).build();
			return this.httpClient;
		}

		@Bean
		@ConditionalOnMissingBean(Client.class)
		public Client feignClient(HttpClient httpClient) {
			return new ApacheHttpClient(httpClient);
		}

		@PreDestroy
		public void destroy() {
			this.connectionManagerTimer.cancel();
			if (this.httpClient != null) {
				try {
					this.httpClient.close();
				}
				catch (IOException e) {
					if (LOG.isErrorEnabled()) {
						LOG.error("Could not correctly close httpClient.");
					}
				}
			}
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(OkHttpClient.class)
	@ConditionalOnMissingBean(okhttp3.OkHttpClient.class)
	@ConditionalOnProperty("feign.okhttp.enabled")
	protected static class OkHttpFeignConfiguration {

		private okhttp3.OkHttpClient okHttpClient;

		@Bean
		@ConditionalOnMissingBean(ConnectionPool.class)
		public ConnectionPool httpClientConnectionPool(FeignHttpClientProperties httpClientProperties,
				OkHttpClientConnectionPoolFactory connectionPoolFactory) {
			Integer maxTotalConnections = httpClientProperties.getMaxConnections();
			Long timeToLive = httpClientProperties.getTimeToLive();
			TimeUnit ttlUnit = httpClientProperties.getTimeToLiveUnit();
			return connectionPoolFactory.create(maxTotalConnections, timeToLive, ttlUnit);
		}

		@Bean
		public okhttp3.OkHttpClient client(OkHttpClientFactory httpClientFactory, ConnectionPool connectionPool,
				FeignHttpClientProperties httpClientProperties) {
			Boolean followRedirects = httpClientProperties.isFollowRedirects();
			Integer connectTimeout = httpClientProperties.getConnectionTimeout();
			Boolean disableSslValidation = httpClientProperties.isDisableSslValidation();
			this.okHttpClient = httpClientFactory.createBuilder(disableSslValidation)
					.connectTimeout(connectTimeout, TimeUnit.MILLISECONDS).followRedirects(followRedirects)
					.connectionPool(connectionPool).build();
			return this.okHttpClient;
		}

		@PreDestroy
		public void destroy() {
			if (this.okHttpClient != null) {
				this.okHttpClient.dispatcher().executorService().shutdown();
				this.okHttpClient.connectionPool().evictAll();
			}
		}

		@Bean
		@ConditionalOnMissingBean(Client.class)
		public Client feignClient(okhttp3.OkHttpClient client) {
			return new OkHttpClient(client);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(AsyncApacheHttp5Client.class)
	@ConditionalOnMissingBean(CloseableHttpAsyncClient.class)
	@ConditionalOnProperty("feign.asynchttpclient5.enabled")
	protected static class AsyncHttpClient5Configuration {
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

			org.apache.hc.client5.http.config.RequestConfig defaultRequestConfig =
				org.apache.hc.client5.http.config.RequestConfig.custom()
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

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(OAuth2ClientContext.class)
	@ConditionalOnProperty("feign.oauth2.enabled")
	protected static class Oauth2FeignConfiguration {

		@Bean
		@ConditionalOnMissingBean(OAuth2FeignRequestInterceptor.class)
		@ConditionalOnBean({ OAuth2ClientContext.class, OAuth2ProtectedResourceDetails.class })
		public RequestInterceptor oauth2FeignRequestInterceptor(OAuth2ClientContext oAuth2ClientContext,
				OAuth2ProtectedResourceDetails resource) {
			return new OAuth2FeignRequestInterceptor(oAuth2ClientContext, resource);
		}

	}

}
