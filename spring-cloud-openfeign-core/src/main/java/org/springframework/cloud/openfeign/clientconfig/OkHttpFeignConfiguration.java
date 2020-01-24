/*
 * Copyright 2013-2019 the original author or authors.
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

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.commons.httpclient.OkHttpClientConnectionPoolFactory;
import org.springframework.cloud.commons.httpclient.OkHttpClientFactory;
import org.springframework.cloud.openfeign.support.FeignHttpClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Defualt configuration for {@link OkHttpClient}.
 *
 * @author Ryan Baxter
 * @author Marcin Grzejszczak
 * @author Spencer Gibb
 * @author Olga Maciaszek-Sharma
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnMissingBean(okhttp3.OkHttpClient.class)
public class OkHttpFeignConfiguration {

	private okhttp3.OkHttpClient okHttpClient;

	@Bean
	@ConditionalOnMissingBean(ConnectionPool.class)
	public ConnectionPool httpClientConnectionPool(
			FeignHttpClientProperties httpClientProperties,
			OkHttpClientConnectionPoolFactory connectionPoolFactory) {
		Integer maxTotalConnections = httpClientProperties.getMaxConnections();
		Long timeToLive = httpClientProperties.getTimeToLive();
		TimeUnit ttlUnit = httpClientProperties.getTimeToLiveUnit();
		return connectionPoolFactory.create(maxTotalConnections, timeToLive, ttlUnit);
	}

	@Bean
	public okhttp3.OkHttpClient client(OkHttpClientFactory httpClientFactory,
			ConnectionPool connectionPool,
			FeignHttpClientProperties httpClientProperties) {
		Boolean followRedirects = httpClientProperties.isFollowRedirects();
		Integer connectTimeout = httpClientProperties.getConnectionTimeout();
		this.okHttpClient = httpClientFactory
				.createBuilder(httpClientProperties.isDisableSslValidation())
				.connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
				.followRedirects(followRedirects).connectionPool(connectionPool).build();
		return this.okHttpClient;
	}

	@PreDestroy
	public void destroy() {
		if (this.okHttpClient != null) {
			this.okHttpClient.dispatcher().executorService().shutdown();
			this.okHttpClient.connectionPool().evictAll();
		}
	}

}
