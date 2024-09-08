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

package org.springframework.cloud.openfeign.encoding;

import java.util.List;

import feign.RequestTemplate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link FeignContentGzipEncodingInterceptor}
 *
 * @author André Teigler
 */
class FeignContentGzipEncodingInterceptorTest {

	@Test
	void apply_SizeBelowThreshold_NoCompressionHeader() {

		final var properties = new FeignClientEncodingProperties();

		final var template = new RequestTemplate();

		template.header(HttpEncoding.CONTENT_LENGTH, "2047");
		template.header(HttpEncoding.CONTENT_TYPE, "application/json");

		final var interceptor = new FeignContentGzipEncodingInterceptor(properties);

		interceptor.apply(template);

		assertThat(template.headers().containsKey(HttpEncoding.CONTENT_ENCODING_HEADER)).isFalse();
	}

	@Test
	void apply_SizeEqualsThreshold_NoCompressionHeader() {

		final var properties = new FeignClientEncodingProperties();

		final var template = new RequestTemplate();

		template.header(HttpEncoding.CONTENT_LENGTH, "2048");
		template.header(HttpEncoding.CONTENT_TYPE, "application/json");

		final var interceptor = new FeignContentGzipEncodingInterceptor(properties);

		interceptor.apply(template);

		assertThat(template.headers().containsKey(HttpEncoding.CONTENT_ENCODING_HEADER)).isFalse();
	}

	@Test
	void apply_SizeAboveThreshold_DefaultCompressionHeader() {

		final var properties = new FeignClientEncodingProperties();

		final var template = new RequestTemplate();

		template.header(HttpEncoding.CONTENT_LENGTH, "2049");
		template.header(HttpEncoding.CONTENT_TYPE, "application/json");

		final var interceptor = new FeignContentGzipEncodingInterceptor(properties);

		interceptor.apply(template);

		assertThat(template.headers().containsKey(HttpEncoding.CONTENT_ENCODING_HEADER)).isTrue();
		assertThat(template.headers().get(HttpEncoding.CONTENT_ENCODING_HEADER)).isEqualTo(List.of("gzip", "deflate"));
	}

	@Test
	void apply_MimeTypeMismatch_NoCompressionHeader() {

		final var properties = new FeignClientEncodingProperties();
		properties.setMimeTypes(new String[] { "application/xml" });

		final var template = new RequestTemplate();

		template.header(HttpEncoding.CONTENT_LENGTH, "2049");
		template.header(HttpEncoding.CONTENT_TYPE, "application/json");

		final var interceptor = new FeignContentGzipEncodingInterceptor(properties);

		interceptor.apply(template);

		assertThat(template.headers().containsKey(HttpEncoding.CONTENT_ENCODING_HEADER)).isFalse();
	}

	@Test
	void apply_NoMimeTypeSet_DefaultCompressionHeader() {

		final var properties = new FeignClientEncodingProperties();
		properties.setMimeTypes(new String[] {});

		final var template = new RequestTemplate();

		template.header(HttpEncoding.CONTENT_LENGTH, "2049");
		template.header(HttpEncoding.CONTENT_TYPE, "application/json");

		final var interceptor = new FeignContentGzipEncodingInterceptor(properties);

		interceptor.apply(template);

		assertThat(template.headers().containsKey(HttpEncoding.CONTENT_ENCODING_HEADER)).isTrue();
		assertThat(template.headers().get(HttpEncoding.CONTENT_ENCODING_HEADER)).isEqualTo(List.of("gzip", "deflate"));
	}

	@Test
	void apply_NullMimeTypeSet_DefaultCompressionHeader() {

		final var properties = new FeignClientEncodingProperties();
		properties.setMimeTypes(null);

		final var template = new RequestTemplate();

		template.header(HttpEncoding.CONTENT_LENGTH, "2049");
		template.header(HttpEncoding.CONTENT_TYPE, "application/json");

		final var interceptor = new FeignContentGzipEncodingInterceptor(properties);

		interceptor.apply(template);

		assertThat(template.headers().containsKey(HttpEncoding.CONTENT_ENCODING_HEADER)).isTrue();
		assertThat(template.headers().get(HttpEncoding.CONTENT_ENCODING_HEADER)).isEqualTo(List.of("gzip", "deflate"));
	}

	@Test
	void apply_NonDefaultContentEncodingSet_CustomCompressionHeader() {

		final var properties = new FeignClientEncodingProperties();
		properties.setContentEncodings(new String[] { "gzip" });

		final var template = new RequestTemplate();

		template.header(HttpEncoding.CONTENT_LENGTH, "2049");
		template.header(HttpEncoding.CONTENT_TYPE, "application/json");

		final var interceptor = new FeignContentGzipEncodingInterceptor(properties);

		interceptor.apply(template);

		assertThat(template.headers().containsKey(HttpEncoding.CONTENT_ENCODING_HEADER)).isTrue();
		assertThat(template.headers().get(HttpEncoding.CONTENT_ENCODING_HEADER)).isEqualTo(List.of("gzip"));
	}

	@Test
	void apply_NoContentEncodingsSet_DefaultCompressionHeader() {

		final var properties = new FeignClientEncodingProperties();
		properties.setContentEncodings(new String[] {});

		final var template = new RequestTemplate();

		template.header(HttpEncoding.CONTENT_LENGTH, "2049");
		template.header(HttpEncoding.CONTENT_TYPE, "application/json");

		final var interceptor = new FeignContentGzipEncodingInterceptor(properties);

		interceptor.apply(template);

		assertThat(template.headers().containsKey(HttpEncoding.CONTENT_ENCODING_HEADER)).isTrue();
		assertThat(template.headers().get(HttpEncoding.CONTENT_ENCODING_HEADER)).isEqualTo(List.of("gzip", "deflate"));
	}

	@Test
	void apply_NullContentEncodingsSet_DefaultCompressionHeader() {

		final var properties = new FeignClientEncodingProperties();
		properties.setContentEncodings(null);

		final var template = new RequestTemplate();

		template.header(HttpEncoding.CONTENT_LENGTH, "2049");
		template.header(HttpEncoding.CONTENT_TYPE, "application/json");

		final var interceptor = new FeignContentGzipEncodingInterceptor(properties);

		interceptor.apply(template);

		assertThat(template.headers().containsKey(HttpEncoding.CONTENT_ENCODING_HEADER)).isTrue();
		assertThat(template.headers().get(HttpEncoding.CONTENT_ENCODING_HEADER)).isEqualTo(List.of("gzip", "deflate"));
	}

}
