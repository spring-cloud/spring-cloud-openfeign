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

package org.springframework.cloud.openfeign.encoding;

import java.util.List;

import feign.RequestTemplate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link FeignContentGzipEncodingInterceptor}.
 *
 * @author André Teigler
 */
class FeignContentGzipEncodingInterceptorTests {

	@Test
	void shouldNotAddCompressionHeaderWhenSizeBelowThreshold() {
		final FeignClientEncodingProperties properties = new FeignClientEncodingProperties();
		final RequestTemplate template = new RequestTemplate();
		template.header(HttpEncoding.CONTENT_LENGTH, "2047");
		template.header(HttpEncoding.CONTENT_TYPE, "application/json");
		final FeignContentGzipEncodingInterceptor interceptor = new FeignContentGzipEncodingInterceptor(properties);

		interceptor.apply(template);

		assertThat(template.headers()).doesNotContainKey(HttpEncoding.CONTENT_ENCODING_HEADER);
	}

	@Test
	void shouldNotAddCompressionHeaderWhenSizeEqualsThreshold() {
		final FeignClientEncodingProperties properties = new FeignClientEncodingProperties();
		final RequestTemplate template = new RequestTemplate();
		template.header(HttpEncoding.CONTENT_LENGTH, "2048");
		template.header(HttpEncoding.CONTENT_TYPE, "application/json");
		final FeignContentGzipEncodingInterceptor interceptor = new FeignContentGzipEncodingInterceptor(properties);

		interceptor.apply(template);

		assertThat(template.headers()).doesNotContainKey(HttpEncoding.CONTENT_ENCODING_HEADER);
	}

	@Test
	void shouldAddCompressionHeaderWhenSizeAboveThreshold() {
		final FeignClientEncodingProperties properties = new FeignClientEncodingProperties();
		final RequestTemplate template = new RequestTemplate();
		template.header(HttpEncoding.CONTENT_LENGTH, "2049");
		template.header(HttpEncoding.CONTENT_TYPE, "application/json");
		final FeignContentGzipEncodingInterceptor interceptor = new FeignContentGzipEncodingInterceptor(properties);

		interceptor.apply(template);

		assertThat(template.headers()).containsKey(HttpEncoding.CONTENT_ENCODING_HEADER);
		assertThat(template.headers().get(HttpEncoding.CONTENT_ENCODING_HEADER)).isEqualTo(List.of("gzip", "deflate"));
	}

	@Test
	void shouldNotAddCompressionHeaderWhenTypeMismatch() {
		final FeignClientEncodingProperties properties = new FeignClientEncodingProperties();
		properties.setMimeTypes(new String[] { "application/xml" });
		final RequestTemplate template = new RequestTemplate();
		template.header(HttpEncoding.CONTENT_LENGTH, "2049");
		template.header(HttpEncoding.CONTENT_TYPE, "application/json");
		final FeignContentGzipEncodingInterceptor interceptor = new FeignContentGzipEncodingInterceptor(properties);

		interceptor.apply(template);

		assertThat(template.headers()).doesNotContainKey(HttpEncoding.CONTENT_ENCODING_HEADER);
	}

	@Test
	void shouldAddDefaultCompressionHeaderWhenMimeTypeNotSet() {
		final FeignClientEncodingProperties properties = new FeignClientEncodingProperties();
		properties.setMimeTypes(new String[] {});
		final RequestTemplate template = new RequestTemplate();
		template.header(HttpEncoding.CONTENT_LENGTH, "2049");
		template.header(HttpEncoding.CONTENT_TYPE, "application/json");
		final FeignContentGzipEncodingInterceptor interceptor = new FeignContentGzipEncodingInterceptor(properties);

		interceptor.apply(template);

		assertThat(template.headers()).containsKey(HttpEncoding.CONTENT_ENCODING_HEADER);
		assertThat(template.headers().get(HttpEncoding.CONTENT_ENCODING_HEADER)).isEqualTo(List.of("gzip", "deflate"));
	}

	@Test
	void shouldAddDefaultCompressionHeaderWhenNullMimeTypeSet() {
		final FeignClientEncodingProperties properties = new FeignClientEncodingProperties();
		properties.setMimeTypes(null);
		final RequestTemplate template = new RequestTemplate();
		template.header(HttpEncoding.CONTENT_LENGTH, "2049");
		template.header(HttpEncoding.CONTENT_TYPE, "application/json");
		final FeignContentGzipEncodingInterceptor interceptor = new FeignContentGzipEncodingInterceptor(properties);

		interceptor.apply(template);

		assertThat(template.headers()).containsKey(HttpEncoding.CONTENT_ENCODING_HEADER);
		assertThat(template.headers().get(HttpEncoding.CONTENT_ENCODING_HEADER)).isEqualTo(List.of("gzip", "deflate"));
	}

	@Test
	void shouldAddCustomCompressionHeader() {
		final FeignClientEncodingProperties properties = new FeignClientEncodingProperties();
		properties.setContentEncodingTypes(new String[] { "gzip" });
		final RequestTemplate template = new RequestTemplate();
		template.header(HttpEncoding.CONTENT_LENGTH, "2049");
		template.header(HttpEncoding.CONTENT_TYPE, "application/json");
		final FeignContentGzipEncodingInterceptor interceptor = new FeignContentGzipEncodingInterceptor(properties);

		interceptor.apply(template);

		assertThat(template.headers()).containsKey(HttpEncoding.CONTENT_ENCODING_HEADER);
		assertThat(template.headers().get(HttpEncoding.CONTENT_ENCODING_HEADER)).isEqualTo(List.of("gzip"));
	}

	@Test
	void shouldThrowExceptionWhenContentEncodingTypesAreEmpty() {
		final FeignClientEncodingProperties properties = new FeignClientEncodingProperties();
		properties.setContentEncodingTypes(new String[] {});
		final RequestTemplate template = new RequestTemplate();
		template.header(HttpEncoding.CONTENT_LENGTH, "2049");
		template.header(HttpEncoding.CONTENT_TYPE, "application/json");
		final FeignContentGzipEncodingInterceptor interceptor = new FeignContentGzipEncodingInterceptor(properties);

		assertThatThrownBy(() -> interceptor.apply(template)).isInstanceOf(IllegalStateException.class)
			.hasMessage("Invalid ContentEncodingTypes configuration");
	}

	@Test
	void shouldThrowExceptionWhenNullContentEncodingTypes() {
		final FeignClientEncodingProperties properties = new FeignClientEncodingProperties();
		properties.setContentEncodingTypes(null);
		final RequestTemplate template = new RequestTemplate();
		template.header(HttpEncoding.CONTENT_LENGTH, "2049");
		template.header(HttpEncoding.CONTENT_TYPE, "application/json");
		final FeignContentGzipEncodingInterceptor interceptor = new FeignContentGzipEncodingInterceptor(properties);

		assertThatThrownBy(() -> interceptor.apply(template)).isInstanceOf(IllegalStateException.class)
			.hasMessage("Invalid ContentEncodingTypes configuration");
	}

}
