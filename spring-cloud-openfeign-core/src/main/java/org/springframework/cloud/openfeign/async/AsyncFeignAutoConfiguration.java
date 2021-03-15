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

import feign.AsyncClient;
import feign.AsyncFeign;
import feign.hc5.AsyncApacheHttp5Client;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.protocol.HttpClientContext;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * An autoconfiguration that instantiates implementations of {@link AsyncClient} and
 * implementations of {@link AsyncTargeter}.
 *
 * @author Nguyen Ky Thanh
 */
@ConditionalOnClass(AsyncFeign.class)
@Configuration(proxyBeanMethods = false)
public class AsyncFeignAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(AsyncApacheHttp5Client.class)
	@ConditionalOnProperty(value = "feign.httpclient.asyncHc5.enabled",
			havingValue = "true")
	@Import(org.springframework.cloud.openfeign.async.AsyncHttpClient5FeignConfiguration.class)
	protected static class AsyncHttpClient5FeignConfiguration {

		@Bean
		public AsyncClient<HttpClientContext> asyncClient(
				CloseableHttpAsyncClient httpAsyncClient) {
			return new AsyncApacheHttp5Client(httpAsyncClient);
		}

	}

	@Configuration(proxyBeanMethods = false)
	protected static class DefaultAsyncFeignTargeterConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public AsyncTargeter asyncTargeter() {
			return new DefaultAsyncTargeter();
		}

	}

}
