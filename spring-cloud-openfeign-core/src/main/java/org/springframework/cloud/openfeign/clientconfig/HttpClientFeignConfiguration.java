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

package org.springframework.cloud.openfeign.clientconfig;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import jakarta.annotation.PreDestroy;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.commons.httpclient.ApacheHttpClientConnectionManagerFactory;
import org.springframework.cloud.commons.httpclient.ApacheHttpClientFactory;
import org.springframework.cloud.openfeign.support.FeignHttpClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Default configuration for {@link CloseableHttpClient}.
 *
 * @author Ryan Baxter
 * @author Marcin Grzejszczak
 * @author Spencer Gibb
 * @author Olga Maciaszek-Sharma
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnMissingBean(CloseableHttpClient.class)
public class HttpClientFeignConfiguration {

	private static final Log LOG = LogFactory.getLog(HttpClientFeignConfiguration.class);

	private final Timer connectionManagerTimer = new Timer("FeignApacheHttpClientConfiguration.connectionManagerTimer",
			true);

	private CloseableHttpClient httpClient;

	@Autowired(required = false)
	private RegistryBuilder registryBuilder;

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
	@ConditionalOnProperty(value = "spring.cloud.openfeign.compression.response.enabled", havingValue = "true")
	public CloseableHttpClient customHttpClient(HttpClientConnectionManager httpClientConnectionManager,
			FeignHttpClientProperties httpClientProperties) {
		HttpClientBuilder builder = HttpClientBuilder.create().disableCookieManagement().useSystemProperties();
		this.httpClient = createClient(builder, httpClientConnectionManager, httpClientProperties);
		return this.httpClient;
	}

	@Bean
	@ConditionalOnProperty(value = "spring.cloud.openfeign.compression.response.enabled", havingValue = "false",
			matchIfMissing = true)
	public CloseableHttpClient httpClient(ApacheHttpClientFactory httpClientFactory,
			HttpClientConnectionManager httpClientConnectionManager, FeignHttpClientProperties httpClientProperties) {
		this.httpClient = createClient(httpClientFactory.createBuilder(), httpClientConnectionManager,
				httpClientProperties);
		return this.httpClient;
	}

	private CloseableHttpClient createClient(HttpClientBuilder builder,
			HttpClientConnectionManager httpClientConnectionManager, FeignHttpClientProperties httpClientProperties) {
		RequestConfig defaultRequestConfig = RequestConfig.custom()
				.setConnectTimeout(httpClientProperties.getConnectionTimeout())
				.setRedirectsEnabled(httpClientProperties.isFollowRedirects()).build();
		CloseableHttpClient httpClient = builder.setDefaultRequestConfig(defaultRequestConfig)
				.setConnectionManager(httpClientConnectionManager).build();
		return httpClient;
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
