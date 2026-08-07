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

import tools.jackson.databind.json.JsonMapper;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.http.converter.autoconfigure.ClientHttpMessageConvertersCustomizer;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.util.ClassUtils;

/**
 * Class that mimics {@link HttpMessageConverters} and the default implementation there.
 * Applies the {@link HttpMessageConverterCustomizer}s and gathers all the converters into
 * a {@link List}.
 * <p>
 * When a {@link JsonMapper} is available (Boot's application mapper, including
 * {@code JacksonModule} beans such as {@code JavaxMoneyModule}), it is applied as the
 * JSON converter after {@link ClientHttpMessageConvertersCustomizer}s so Feign uses the
 * same modules as MVC (gh-1376).
 *
 * @author seonwoo_jung
 * @author Olga Maciaszek-Sharma
 */
public class FeignHttpMessageConverters {

	private static final boolean JACKSON_JSON_MAPPER_PRESENT = ClassUtils
		.isPresent("tools.jackson.databind.json.JsonMapper", FeignHttpMessageConverters.class.getClassLoader());

	private final ObjectProvider<ClientHttpMessageConvertersCustomizer> customizers;

	private final ObjectProvider<HttpMessageConverterCustomizer> cloudCustomizers;

	private final ObjectProvider<JsonMapper> jsonMapper;

	private volatile List<HttpMessageConverter<?>> converters;

	/**
	 * Create an instance without an explicit application {@link JsonMapper} provider.
	 * @param customizers Boot client HTTP message converter customizers
	 * @param cloudCustomizers OpenFeign HTTP message converter customizers
	 */
	public FeignHttpMessageConverters(ObjectProvider<ClientHttpMessageConvertersCustomizer> customizers,
			ObjectProvider<HttpMessageConverterCustomizer> cloudCustomizers) {
		this(customizers, cloudCustomizers, new EmptyObjectProvider<>());
	}

	/**
	 * Create an instance that wires the application {@link JsonMapper} into the JSON
	 * converter when present.
	 * @param customizers Boot client HTTP message converter customizers
	 * @param cloudCustomizers OpenFeign HTTP message converter customizers
	 * @param jsonMapper provider for the application {@link JsonMapper}
	 */
	public FeignHttpMessageConverters(ObjectProvider<ClientHttpMessageConvertersCustomizer> customizers,
			ObjectProvider<HttpMessageConverterCustomizer> cloudCustomizers, ObjectProvider<JsonMapper> jsonMapper) {
		this.customizers = customizers;
		this.cloudCustomizers = cloudCustomizers;
		this.jsonMapper = jsonMapper;
	}

	public List<HttpMessageConverter<?>> getConverters() {
		initConvertersIfRequired();
		return converters;
	}

	private void initConvertersIfRequired() {
		if (this.converters == null) {
			synchronized (this) {
				if (this.converters == null) {
					List<HttpMessageConverter<?>> converters = new ArrayList<>();
					HttpMessageConverters.ClientBuilder builder = HttpMessageConverters.forClient();
					// TODO: allow disabling of registerDefaults
					builder.registerDefaults();
					// TODO: check if already added? Howto order?

					this.customizers.orderedStream().forEach(customizer -> customizer.customize(builder));
					// Prefer the application JsonMapper (with JacksonModule beans) over
					// any classpath-default mapper from registerDefaults() — gh-1376.
					applyApplicationJsonMapper(builder);
					HttpMessageConverters hmc = builder.build();
					hmc.forEach(converter -> converters.add(converter));
					cloudCustomizers.forEach(customizer -> customizer.accept(converters));
					// Publish only once fully built so concurrent callers never
					// observe a partially populated list.
					this.converters = converters;
				}
			}
		}
	}

	private void applyApplicationJsonMapper(HttpMessageConverters.ClientBuilder builder) {
		if (!JACKSON_JSON_MAPPER_PRESENT) {
			return;
		}
		JsonMapper mapper = this.jsonMapper.getIfAvailable();
		if (mapper != null) {
			builder.withJsonConverter(new JacksonJsonHttpMessageConverter(mapper));
		}
	}

	/**
	 * Resolve an {@link ObjectProvider} for the application {@link JsonMapper} without
	 * requiring Jackson on the classpath of the calling configuration class.
	 * @param beanFactory the bean factory (typically the Feign child context, which
	 * parents the main application context)
	 * @return a provider for {@link JsonMapper}, or an empty provider if Jackson is
	 * absent
	 */
	@SuppressWarnings("unchecked")
	public static ObjectProvider<JsonMapper> jsonMapperProvider(
			org.springframework.beans.factory.BeanFactory beanFactory) {
		if (!JACKSON_JSON_MAPPER_PRESENT) {
			return new EmptyObjectProvider<>();
		}
		try {
			Class<?> jsonMapperClass = ClassUtils.forName("tools.jackson.databind.json.JsonMapper",
					FeignHttpMessageConverters.class.getClassLoader());
			return (ObjectProvider<JsonMapper>) beanFactory.getBeanProvider(jsonMapperClass);
		}
		catch (ClassNotFoundException ex) {
			return new EmptyObjectProvider<>();
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
