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

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;

import feign.RequestTemplate;
import feign.codec.EncodeException;
import feign.codec.Encoder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.openfeign.FeignContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractGenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.CONTENT_LENGTH;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

/**
 * @author Spencer Gibb
 * @author Olga Maciaszek-Sharma
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = SpringEncoderTests.Application.class,
		webEnvironment = WebEnvironment.RANDOM_PORT, value = {
				"spring.application.name=springencodertest", "spring.jmx.enabled=false" })
@DirtiesContext
public class SpringEncoderTests {

	@Autowired
	private FeignContext context;

	@Autowired
	@Qualifier("myHttpMessageConverter")
	private HttpMessageConverter<?> myConverter;

	@Test
	public void testCustomHttpMessageConverter() {
		Encoder encoder = this.context.getInstance("foo", Encoder.class);
		assertThat(encoder).isNotNull();
		RequestTemplate request = new RequestTemplate();

		encoder.encode("hi", MyType.class, request);

		Collection<String> contentTypeHeader = request.headers().get("Content-Type");
		assertThat(contentTypeHeader).as("missing content type header").isNotNull();
		assertThat(contentTypeHeader.isEmpty()).as("missing content type header")
				.isFalse();

		String header = contentTypeHeader.iterator().next();
		assertThat(header).as("content type header is wrong")
				.isEqualTo("application/mytype");

		assertThat(request.requestCharset()).as("request charset is null").isNotNull();
		assertThat(request.requestCharset()).as("request charset is wrong")
				.isEqualTo(Charset.forName("UTF-8"));
	}

	@Test
	public void testBinaryData() {
		Encoder encoder = this.context.getInstance("foo", Encoder.class);
		assertThat(encoder).isNotNull();

		RequestTemplate request = new RequestTemplate();

		encoder.encode("hi".getBytes(), null, request);

		assertThat(((List) request.headers().get(CONTENT_TYPE)).get(0))
				.as("Request Content-Type is not octet-stream")
				.isEqualTo(APPLICATION_OCTET_STREAM_VALUE);
	}

	@Test(expected = EncodeException.class)
	public void testMultipartFile1() {
		Encoder encoder = this.context.getInstance("foo", Encoder.class);
		assertThat(encoder).isNotNull();
		RequestTemplate request = new RequestTemplate();

		MultipartFile multipartFile = new MockMultipartFile("test_multipart_file",
				"hi".getBytes());
		encoder.encode(multipartFile, MultipartFile.class, request);

		assertThat(request.requestCharset()).as("request charset is not null").isNull();
	}

	// gh-105, gh-107
	@Test
	public void testMultipartFile2() {
		Encoder encoder = this.context.getInstance("foo", Encoder.class);
		assertThat(encoder).isNotNull();
		RequestTemplate request = new RequestTemplate();
		request.header(ACCEPT, MediaType.MULTIPART_FORM_DATA_VALUE);
		request.header(CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE);

		MultipartFile multipartFile = new MockMultipartFile("test_multipart_file",
				"hi".getBytes());
		encoder.encode(multipartFile, MultipartFile.class, request);

		assertThat((String) ((List) request.headers().get(CONTENT_TYPE)).get(0))
				.as("Request Content-Type is not multipart/form-data")
				.contains("multipart/form-data; charset=UTF-8; boundary=");
		assertThat(request.headers().get(CONTENT_TYPE).size())
				.as("There is more than one Content-Type request header").isEqualTo(1);
		assertThat(((List) request.headers().get(ACCEPT)).get(0))
				.as("Request Accept header is not multipart/form-data")
				.isEqualTo(MULTIPART_FORM_DATA_VALUE);
		assertThat(((List) request.headers().get(CONTENT_LENGTH)).get(0))
				.as("Request Content-Length is not equal to 186").isEqualTo("186");
		assertThat(new String(request.requestBody().asBytes()))
				.as("Body content cannot be decoded").contains("hi");
	}

	protected interface TestClient {

	}

	protected static class MyType {

		private String value;

		public String getValue() {
			return this.value;
		}

		public void setValue(String value) {
			this.value = value;
		}

	}

	@Configuration
	@EnableAutoConfiguration
	@RestController
	protected static class Application implements TestClient {

		@Bean
		HttpMessageConverter<?> myHttpMessageConverter() {
			return new MyHttpMessageConverter();
		}

		private static class MyHttpMessageConverter
				extends AbstractGenericHttpMessageConverter<Object> {

			MyHttpMessageConverter() {
				super(new MediaType("application", "mytype"));
			}

			@Override
			protected boolean supports(Class<?> clazz) {
				return false;
			}

			@Override
			public boolean canRead(Class<?> clazz, MediaType mediaType) {
				return true;
			}

			@Override
			public boolean canWrite(Class<?> clazz, MediaType mediaType) {
				return clazz == String.class;
			}

			@Override
			protected void writeInternal(Object o, Type type,
					HttpOutputMessage outputMessage)
					throws IOException, HttpMessageNotWritableException {

			}

			@Override
			protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage)
					throws IOException, HttpMessageNotReadableException {
				return null;
			}

			@Override
			public Object read(Type type, Class<?> contextClass,
					HttpInputMessage inputMessage)
					throws IOException, HttpMessageNotReadableException {
				return null;
			}

		}

	}

	class MediaTypeMatcher implements ArgumentMatcher<MediaType> {

		private MediaType mediaType;

		MediaTypeMatcher(String type, String subtype) {
			this.mediaType = new MediaType(type, subtype);
		}

		@Override
		public boolean matches(MediaType argument) {
			return this.mediaType.equals(argument);
		}

		@Override
		public String toString() {
			final StringBuffer sb = new StringBuffer("MediaTypeMatcher{");
			sb.append("mediaType=").append(this.mediaType);
			sb.append('}');
			return sb.toString();
		}

	}

}
