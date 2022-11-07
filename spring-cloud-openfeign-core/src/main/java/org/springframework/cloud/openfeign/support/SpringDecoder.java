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

package org.springframework.cloud.openfeign.support;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.List;

import feign.FeignException;
import feign.Response;
import feign.codec.DecodeException;
import feign.codec.Decoder;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.HttpMessageConverterExtractor;

import static org.springframework.cloud.openfeign.support.FeignUtils.getHttpHeaders;

/**
 * @author Spencer Gibb
 * @author Olga Maciaszek-Sharma
 */
public class SpringDecoder implements Decoder {

	private final ObjectFactory<HttpMessageConverters> messageConverters;

	private final ObjectProvider<HttpMessageConverterCustomizer> customizers;

	/**
	 * @deprecated in favour of
	 * {@link SpringDecoder#SpringDecoder(ObjectFactory, ObjectProvider)}
	 */
	@Deprecated
	public SpringDecoder(ObjectFactory<HttpMessageConverters> messageConverters) {
		this(messageConverters, new EmptyObjectProvider<>());
	}

	public SpringDecoder(ObjectFactory<HttpMessageConverters> messageConverters,
			ObjectProvider<HttpMessageConverterCustomizer> customizers) {
		this.messageConverters = messageConverters;
		this.customizers = customizers;
	}

	@Override
	public Object decode(final Response response, Type type) throws IOException, FeignException {
		if (type instanceof Class || type instanceof ParameterizedType || type instanceof WildcardType) {
			List<HttpMessageConverter<?>> converters = messageConverters.getObject().getConverters();
			customizers.forEach(customizer -> customizer.accept(converters));
			@SuppressWarnings({ "unchecked", "rawtypes" })
			HttpMessageConverterExtractor<?> extractor = new HttpMessageConverterExtractor(type, converters);

			return extractor.extractData(new FeignResponseAdapter(response));
		}
		throw new DecodeException(response.status(), "type is not an instance of Class or ParameterizedType: " + type,
				response.request());
	}

	private final class FeignResponseAdapter implements ClientHttpResponse {

		private final Response response;

		private FeignResponseAdapter(Response response) {
			this.response = response;
		}

		@Override
		public HttpStatus getStatusCode() {
			return HttpStatus.valueOf(response.status());
		}

		@Override
		public int getRawStatusCode() {
			return response.status();
		}

		@Override
		public String getStatusText() {
			return response.reason();
		}

		@Override
		public void close() {
			try {
				response.body().close();
			}
			catch (IOException ex) {
				// Ignore exception on close...
			}
		}

		@Override
		public InputStream getBody() throws IOException {
			return response.body().asInputStream();
		}

		@Override
		public HttpHeaders getHeaders() {
			return getHttpHeaders(response.headers());
		}

	}

}
