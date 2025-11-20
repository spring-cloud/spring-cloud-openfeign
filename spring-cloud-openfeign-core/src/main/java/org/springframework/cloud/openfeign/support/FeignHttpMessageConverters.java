/*
 * Copyright 2013-present the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.StringHttpMessageConverter;

/**
 * Class that mimics {@link HttpMessageConverters} and the default implementation there.
 * Applies the {@link HttpMessageConverterCustomizer}s and gathers all the converters into
 * a {@link List}.
 */
public class FeignHttpMessageConverters {

	private final ObjectProvider<HttpMessageConverter<?>> messageConverters;

	private final ObjectProvider<HttpMessageConverterCustomizer> customizers;

	private List<HttpMessageConverter<?>> converters;

	public FeignHttpMessageConverters(ObjectProvider<HttpMessageConverter<?>> messageConverters,
			ObjectProvider<HttpMessageConverterCustomizer> customizers) {
		this.messageConverters = messageConverters;
		this.customizers = customizers;
	}

	public List<HttpMessageConverter<?>> getConverters() {
		initConvertersIfRequired();
		return converters;
	}

	private void initConvertersIfRequired() {
		if (this.converters == null) {
			this.converters = new ArrayList<>();
			HttpMessageConverters.ClientBuilder builder = HttpMessageConverters.forClient();
			// TODO: allow disabling of registerDefaults
			builder.registerDefaults();
			// TODO: check if already added? Howto order?
			this.messageConverters.forEach((converter) -> {
				if (converter instanceof StringHttpMessageConverter) {
					builder.withStringConverter(converter);
				}
				/*
				 * else if (converter instanceof
				 * KotlinSerializationJsonHttpMessageConverter) {
				 * builder.withKotlinSerializationJsonConverter(converter); }
				 */
				else if (supportsMediaType(converter, MediaType.APPLICATION_JSON)) {
					builder.withJsonConverter(converter);
				}
				else if (supportsMediaType(converter, MediaType.APPLICATION_XML)) {
					builder.withXmlConverter(converter);
				}
				else {
					builder.addCustomConverter(converter);
				}
			});
			HttpMessageConverters hmc = builder.build();
			hmc.forEach(converter -> converters.add(converter));
			customizers.forEach(customizer -> customizer.accept(this.converters));
		}
	}

	private static boolean supportsMediaType(HttpMessageConverter<?> converter, MediaType mediaType) {
		for (MediaType supportedMediaType : converter.getSupportedMediaTypes()) {
			if (supportedMediaType.equalsTypeAndSubtype(mediaType)) {
				return true;
			}
		}
		return false;
	}

}
