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

package org.springframework.cloud.openfeign;

import java.util.Objects;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.test.NoSecurityConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Jaesik Kim
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = DefaultGzipDecoderTests.Application.class,
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		value = { "spring.application.name=defaultGzipDecoderTests",
				"feign.compression.response.enabled=true",
				"feign.compression.response.useGzipDecoder=true",
				"feign.client.config.default.loggerLevel=full",
				"logging.level.org.springframework.cloud.openfeign=DEBUG" })
@DirtiesContext
public class DefaultGzipDecoderTests extends FeignClientFactoryBean {

	@Autowired
	FeignContext context;

	@Value("${local.server.port}")
	private int port = 0;

	public DefaultGzipDecoderTests() {
		setName("tests");
		setContextId("test");
	}

	public TestClient testClient() {
		setType(this.getClass());
		return feign(context).target(TestClient.class, "http://localhost:" + this.port);
	}

	@Test
	public void testBodyDecompress() {
		ResponseEntity<Hello> response = testClient().getGzipResponse();
		assertThat(response).as("response was null").isNotNull();
		assertThat(response.getStatusCode()).as("wrong status code")
				.isEqualTo(HttpStatus.OK);
		Hello hello = response.getBody();
		assertThat(hello).as("hello was null").isNotNull();
		assertThat(hello).as("first hello didn't match")
				.isEqualTo(new Hello("hello world via response"));
	}

	@Test
	public void testNullBodyDecompress() {
		ResponseEntity<Hello> response = testClient().getNullResponse();
		assertThat(response).as("response was null").isNotNull();
		assertThat(response.getStatusCode()).as("wrong status code")
				.isEqualTo(HttpStatus.OK);
		Hello hello = response.getBody();
		assertThat(hello).as("hello was not null").isNull();
		assertThat(hello).as("null hello didn't match").isEqualTo(null);
	}

	@Test
	public void testCharsetDecompress() {
		ResponseEntity<Hello> response = testClient().getUtf8Response();
		assertThat(response).as("response was null").isNotNull();
		assertThat(response.getStatusCode()).as("wrong status code")
				.isEqualTo(HttpStatus.OK);
		Hello hello = response.getBody();
		assertThat(hello).as("hello was null").isNotNull();
		assertThat(hello).as("utf8 hello didn't match")
				.isEqualTo(new Hello("안녕하세요 means Hello in Korean"));
	}

	private static class Hello {

		private String message;

		Hello() {
		}

		Hello(String message) {
			this.message = message;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			Hello that = (Hello) o;
			return Objects.equals(message, that.message);
		}

		@Override
		public int hashCode() {
			return Objects.hash(message);
		}

	}

	protected interface TestClient {

		@GetMapping("/helloGzipResponse")
		ResponseEntity<Hello> getGzipResponse();

		@GetMapping("/nullGzipResponse")
		ResponseEntity<Hello> getNullResponse();

		@GetMapping("/utf8Response")
		ResponseEntity<Hello> getUtf8Response();

	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	@RestController
	@Import(NoSecurityConfiguration.class)
	protected static class Application implements TestClient {

		@Override
		public ResponseEntity<Hello> getGzipResponse() {
			return ResponseEntity.ok(new Hello("hello world via response"));
		}

		@Override
		public ResponseEntity<Hello> getNullResponse() {
			return ResponseEntity.ok(null);
		}

		@Override
		public ResponseEntity<Hello> getUtf8Response() {
			return ResponseEntity.ok(new Hello("안녕하세요 means Hello in Korean"));
		}

	}

}
