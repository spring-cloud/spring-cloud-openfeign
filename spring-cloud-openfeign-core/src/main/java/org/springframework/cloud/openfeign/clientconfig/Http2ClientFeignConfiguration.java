/*
 * Copyright 2013-2024 the original author or authors.
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

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.openfeign.clientconfig.http2client.Http2ClientCustomizer;
import org.springframework.cloud.openfeign.support.FeignHttpClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Default configuration for {@link HttpClient}.
 *
 * @author changjin wei(魏昌进)
 * @author Luis Duarte
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnMissingBean(HttpClient.class)
public class Http2ClientFeignConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public HttpClient.Builder httpClientBuilder(FeignHttpClientProperties httpClientProperties) {
		return HttpClient.newBuilder()
			.followRedirects(
					httpClientProperties.isFollowRedirects() ? HttpClient.Redirect.ALWAYS : HttpClient.Redirect.NEVER)
			.version(HttpClient.Version.valueOf(httpClientProperties.getHttp2().getVersion()))
			.connectTimeout(Duration.ofMillis(httpClientProperties.getConnectionTimeout()));
	}

	@Bean
	public HttpClient httpClient(HttpClient.Builder httpClientBuilder, List<Http2ClientCustomizer> customizers) {
		customizers.forEach(customizer -> customizer.customize(httpClientBuilder));
		return httpClientBuilder.build();
	}

}
