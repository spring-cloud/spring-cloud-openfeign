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

package org.springframework.cloud.openfeign.support;

import feign.codec.Decoder;
import feign.optionals.OptionalDecoder;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures Default Gzip Decoder.
 *
 * @author Jaesik Kim
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty("feign.compression.response.enabled")
// The OK HTTP client uses "transparent" compression.
// If the accept-encoding header is present, it disables transparent compression
@ConditionalOnMissingBean(type = "okhttp3.OkHttpClient")
@AutoConfigureAfter(FeignAutoConfiguration.class)
public class DefaultGzipDecoderConfiguration {

	private ObjectFactory<HttpMessageConverters> messageConverters;

	public DefaultGzipDecoderConfiguration(
			ObjectFactory<HttpMessageConverters> messageConverters) {
		this.messageConverters = messageConverters;
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty("feign.compression.response.useGzipDecoder")
	public Decoder defaultGzipDecoder() {
		return new OptionalDecoder(new ResponseEntityDecoder(
				new DefaultGzipDecoder(new SpringDecoder(messageConverters))));
	}

}
