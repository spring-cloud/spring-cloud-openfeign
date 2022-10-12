/*
 * Copyright 2013-2022 the original author or authors.
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
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.Module;
import feign.Capability;
import feign.Client;
import feign.Feign;
import feign.Target;
import feign.hc5.ApacheHttp5Client;
import feign.httpclient.ApacheHttpClient;
import feign.okhttp.OkHttpClient;
import jakarta.annotation.PreDestroy;
import okhttp3.ConnectionPool;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.interceptor.CacheInterceptor;
import org.springframework.cloud.client.actuator.HasFeatures;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.commons.httpclient.ApacheHttpClientConnectionManagerFactory;
import org.springframework.cloud.commons.httpclient.ApacheHttpClientFactory;
import org.springframework.cloud.commons.httpclient.OkHttpClientConnectionPoolFactory;
import org.springframework.cloud.commons.httpclient.OkHttpClientFactory;
import org.springframework.cloud.openfeign.security.OAuth2AccessTokenInterceptor;
import org.springframework.cloud.openfeign.support.FeignEncoderProperties;
import org.springframework.cloud.openfeign.support.FeignHttpClientProperties;
import org.springframework.cloud.openfeign.support.PageJacksonModule;
import org.springframework.cloud.openfeign.support.SortJacksonModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

/**
 * @author Spencer Gibb
 * @author Julien Roy
 * @author Grzegorz Poznachowski
 * @author Nikita Konev
 * @author Tim Peeters
 * @author Olga Maciaszek-Sharma
 * @author Nguyen Ky Thanh
 * @author Andrii Bohutskyi
 * @author Kwangyong Kim
 * @author Sam Kruglov
 * @author Wojciech Mąka
 * @author Dangzhicairang(小水牛)
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Feign.class)
@EnableConfigurationProperties({ FeignClientProperties.class, FeignHttpClientProperties.class,
		FeignEncoderProperties.class })
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

	@Bean
	@ConditionalOnProperty(value = "spring.cloud.openfeign.cache.enabled", matchIfMissing = true)
	@ConditionalOnBean(CacheInterceptor.class)
	public Capability cachingCapability(CacheInterceptor cacheInterceptor) {
		return new CachingCapability(cacheInterceptor);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ Module.class, Page.class, Sort.class })
	@ConditionalOnProperty(value = "spring.cloud.openfeign.autoconfiguration.jackson.enabled", havingValue = "true",
			matchIfMissing = true)
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
	@ConditionalOnProperty(value = "spring.cloud.openfeign.circuitbreaker.enabled", havingValue = "true")
	protected static class CircuitBreakerPresentFeignTargeterConfiguration {

		@Bean
		@ConditionalOnMissingBean(CircuitBreakerFactory.class)
		public Targeter defaultFeignTargeter() {
			return new DefaultTargeter();
		}

		@Bean
		@ConditionalOnMissingBean(CircuitBreakerNameResolver.class)
		@ConditionalOnProperty(value = "spring.cloud.openfeign.circuitbreaker.alphanumeric-ids.enabled",
				havingValue = "false")
		public CircuitBreakerNameResolver circuitBreakerNameResolver() {
			return new DefaultCircuitBreakerNameResolver();
		}

		@Bean
		@ConditionalOnMissingBean(CircuitBreakerNameResolver.class)
		@ConditionalOnProperty(value = "spring.cloud.openfeign.circuitbreaker.alphanumeric-ids.enabled",
				havingValue = "true", matchIfMissing = true)
		public CircuitBreakerNameResolver alphanumericCircuitBreakerNameResolver() {
			return new AlphanumericCircuitBreakerNameResolver();
		}

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnBean(CircuitBreakerFactory.class)
		public Targeter circuitBreakerFeignTargeter(CircuitBreakerFactory circuitBreakerFactory,
				@Value("${spring.cloud.openfeign.circuitbreaker.group.enabled:false}") boolean circuitBreakerGroupEnabled,
				CircuitBreakerNameResolver circuitBreakerNameResolver) {
			return new FeignCircuitBreakerTargeter(circuitBreakerFactory, circuitBreakerGroupEnabled,
					circuitBreakerNameResolver);
		}

		static class DefaultCircuitBreakerNameResolver implements CircuitBreakerNameResolver {

			@Override
			public String resolveCircuitBreakerName(String feignClientName, Target<?> target, Method method) {
				return Feign.configKey(target.type(), method);
			}

		}

		static class AlphanumericCircuitBreakerNameResolver extends DefaultCircuitBreakerNameResolver {

			@Override
			public String resolveCircuitBreakerName(String feignClientName, Target<?> target, Method method) {
				return super.resolveCircuitBreakerName(feignClientName, target, method).replaceAll("[^a-zA-Z0-9]", "");
			}

		}

	}

	// the following configuration is for alternate feign clients if
	// SC loadbalancer is not on the class path.
	// see corresponding configurations in FeignLoadBalancerAutoConfiguration
	// for load-balanced clients.
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(ApacheHttpClient.class)
	@ConditionalOnMissingBean(CloseableHttpClient.class)
	@ConditionalOnProperty(value = "spring.cloud.openfeign.httpclient.enabled", matchIfMissing = true)
	@Conditional(HttpClient5DisabledConditions.class)
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
	@ConditionalOnProperty("spring.cloud.openfeign.okhttp.enabled")
	protected static class OkHttpFeignConfiguration {

		private okhttp3.OkHttpClient okHttpClient;

		@Bean
		@ConditionalOnMissingBean(ConnectionPool.class)
		public ConnectionPool httpClientConnectionPool(FeignHttpClientProperties httpClientProperties,
				OkHttpClientConnectionPoolFactory connectionPoolFactory) {
			int maxTotalConnections = httpClientProperties.getMaxConnections();
			long timeToLive = httpClientProperties.getTimeToLive();
			TimeUnit ttlUnit = httpClientProperties.getTimeToLiveUnit();
			return connectionPoolFactory.create(maxTotalConnections, timeToLive, ttlUnit);
		}

		@Bean
		public okhttp3.OkHttpClient client(OkHttpClientFactory httpClientFactory, ConnectionPool connectionPool,
				FeignHttpClientProperties httpClientProperties) {
			boolean followRedirects = httpClientProperties.isFollowRedirects();
			int connectTimeout = httpClientProperties.getConnectionTimeout();
			boolean disableSslValidation = httpClientProperties.isDisableSslValidation();
			Duration readTimeout = httpClientProperties.getOkHttp().getReadTimeout();
			this.okHttpClient = httpClientFactory.createBuilder(disableSslValidation)
					.connectTimeout(connectTimeout, TimeUnit.MILLISECONDS).followRedirects(followRedirects)
					.readTimeout(readTimeout).connectionPool(connectionPool).build();
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
	@ConditionalOnClass(ApacheHttp5Client.class)
	@ConditionalOnMissingBean(org.apache.hc.client5.http.impl.classic.CloseableHttpClient.class)
	@ConditionalOnProperty(value = "spring.cloud.openfeign.httpclient.hc5.enabled", havingValue = "true")
	@Import(org.springframework.cloud.openfeign.clientconfig.HttpClient5FeignConfiguration.class)
	protected static class HttpClient5FeignConfiguration {

		@Bean
		@ConditionalOnMissingBean(Client.class)
		public Client feignClient(org.apache.hc.client5.http.impl.classic.CloseableHttpClient httpClient5) {
			return new ApacheHttp5Client(httpClient5);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(OAuth2AuthorizedClientManager.class)
	@ConditionalOnProperty("spring.cloud.openfeign.oauth2.enabled")
	protected static class Oauth2FeignConfiguration {

		@Bean
		@ConditionalOnBean({ OAuth2AuthorizedClientService.class, ClientRegistrationRepository.class })
		@ConditionalOnMissingBean
		OAuth2AuthorizedClientManager feignOAuth2AuthorizedClientManager(
				ClientRegistrationRepository clientRegistrationRepository,
				OAuth2AuthorizedClientService oAuth2AuthorizedClientService) {
			return new AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository,
					oAuth2AuthorizedClientService);

		}

		@Bean
		@ConditionalOnBean(OAuth2AuthorizedClientManager.class)
		public OAuth2AccessTokenInterceptor defaultOAuth2AccessTokenInterceptor(
				@Value("${spring.cloud.openfeign.oauth2.clientRegistrationId:}") String clientRegistrationId,
				OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager) {
			return new OAuth2AccessTokenInterceptor(clientRegistrationId, oAuth2AuthorizedClientManager);
		}

	}

}
