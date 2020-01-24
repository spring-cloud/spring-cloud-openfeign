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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.zip.GZIPInputStream;

import feign.FeignException;
import feign.Response;
import feign.codec.Decoder;

import org.springframework.cloud.openfeign.encoding.HttpEncoding;

/**
 * When response is compressed as gzip, this decompresses and uses {@link SpringDecoder}
 * to decode.
 *
 * @author Jaesik Kim
 */
public class DefaultGzipDecoder implements Decoder {

	private Decoder decoder;

	public DefaultGzipDecoder(Decoder decoder) {
		this.decoder = decoder;
	}

	@Override
	public Object decode(final Response response, Type type)
			throws IOException, FeignException {
		Collection<String> encoding = response.headers()
				.containsKey(HttpEncoding.CONTENT_ENCODING_HEADER)
						? response.headers().get(HttpEncoding.CONTENT_ENCODING_HEADER)
						: null;

		if (encoding != null) {
			if (encoding.contains(HttpEncoding.GZIP_ENCODING)) {
				String decompressedBody = decompress(response);
				if (decompressedBody != null) {
					Response decompressedResponse = response.toBuilder()
							.body(decompressedBody.getBytes()).build();
					return decoder.decode(decompressedResponse, type);
				}
			}
		}
		return decoder.decode(response, type);
	}

	private String decompress(Response response) throws IOException {
		if (response.body() == null) {
			return null;
		}
		try (GZIPInputStream gzipInputStream = new GZIPInputStream(
				response.body().asInputStream());
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(gzipInputStream, StandardCharsets.UTF_8))) {
			String outputString = "";
			String line;
			while ((line = reader.readLine()) != null) {
				outputString += line;
			}
			return outputString;
		}
	}

}
