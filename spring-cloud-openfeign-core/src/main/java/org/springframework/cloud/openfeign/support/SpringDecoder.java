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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;

import feign.FeignException;
import feign.Response;
import feign.codec.DecodeException;
import feign.codec.Decoder;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.HttpMessageConverterExtractor;

import static org.springframework.cloud.openfeign.support.FeignUtils.getHttpHeaders;

/**
 * @author Spencer Gibb
 * @author Olga Maciaszek-Sharma
 * @author Maksym Pasichenko
 */
public class SpringDecoder implements Decoder {

	private final ObjectProvider<FeignHttpMessageConverters> converters;

	public SpringDecoder(ObjectProvider<FeignHttpMessageConverters> converters) {
		this.converters = converters;
	}

	@Override
	public Object decode(final Response response, Type type) throws IOException, FeignException {
		if (type instanceof Class || type instanceof ParameterizedType || type instanceof WildcardType) {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			HttpMessageConverterExtractor<?> extractor = new HttpMessageConverterExtractor(type,
					converters.getObject().getConverters());

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
		public HttpStatusCode getStatusCode() {
			return HttpStatusCode.valueOf(response.status());
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
			return response.body() != null ? response.body().asInputStream() : null;
		}

		@Override
		public HttpHeaders getHeaders() {
			return getHttpHeaders(response.headers());
		}

	}

}
