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

package org.springframework.cloud.openfeign;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.openfeign.test.NoSecurityConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Spencer Gibb
 * @author Olga Maciaszek-Sharma
 * @author Szymon Linowski
 */
@SpringBootTest(classes = SpringDecoderTests.Application.class, webEnvironment = WebEnvironment.RANDOM_PORT,
		value = { "spring.application.name=springdecodertest", "spring.jmx.enabled=false" })
@DirtiesContext
class SpringDecoderTests extends FeignClientFactoryBean {

	@Autowired
	FeignContext context;

	@LocalServerPort
	private int port = 0;

	SpringDecoderTests() {
		setName("test");
		setContextId("test");
	}

	public TestClient testClient() {
		return testClient(false);
	}

	public TestClient testClient(boolean decode404) {
		setType(this.getClass());
		setDecode404(decode404);
		return feign(this.context).target(TestClient.class, "http://localhost:" + this.port);
	}

	@Test
	void testResponseEntity() {
		ResponseEntity<Hello> response = testClient().getHelloResponse();
		assertThat(response).as("response was null").isNotNull();
		assertThat(response.getStatusCode()).as("wrong status code").isEqualTo(HttpStatus.OK);
		Hello hello = response.getBody();
		assertThat(hello).as("hello was null").isNotNull();
		assertThat(hello).as("first hello didn't match").isEqualTo(new Hello("hello world via response"));
	}

	@Test
	void testSimpleType() {
		Hello hello = testClient().getHello();
		assertThat(hello).as("hello was null").isNotNull();
		assertThat(hello).as("first hello didn't match").isEqualTo(new Hello("hello world 1"));
	}

	@Test
	void testUserParameterizedTypeDecode() {
		List<Hello> hellos = testClient().getHellos();
		assertThat(hellos).as("hellos was null").isNotNull();
		assertThat(hellos.size()).as("hellos was not the right size").isEqualTo(2);
		assertThat(hellos.get(0)).as("first hello didn't match").isEqualTo(new Hello("hello world 1"));
	}

	@Test
	void testSimpleParameterizedTypeDecode() {
		List<String> hellos = testClient().getHelloStrings();
		assertThat(hellos).as("hellos was null").isNotNull();
		assertThat(hellos.size()).as("hellos was not the right size").isEqualTo(2);
		assertThat(hellos.get(0)).as("first hello didn't match").isEqualTo("hello world 1");
	}

	@Test
	@SuppressWarnings("unchecked")
	void testWildcardTypeDecode() {
		ResponseEntity<?> wildcard = testClient().getWildcard();
		assertThat(wildcard).as("wildcard was null").isNotNull();
		assertThat(wildcard.getStatusCode()).as("wrong status code").isEqualTo(HttpStatus.OK);
		Object wildcardBody = wildcard.getBody();
		assertThat(wildcardBody).as("wildcardBody was null").isNotNull();
		assertThat(wildcardBody instanceof Map).as("wildcard not an instance of Map").isTrue();
		Map<String, String> hello = (Map<String, String>) wildcardBody;
		assertThat(hello.get("message")).as("first hello didn't match").isEqualTo("wildcard");
	}

	@Test
	void testResponseEntityVoid() {
		ResponseEntity<Void> response = testClient().getHelloVoid();
		assertThat(response).as("response was null").isNotNull();
		List<String> headerVals = response.getHeaders().get("x-test-header");
		assertThat(headerVals).as("headerVals was null").isNotNull();
		assertThat(headerVals.size()).as("headerVals size was wrong").isEqualTo(1);
		String header = headerVals.get(0);
		assertThat(header).as("header was wrong").isEqualTo("myval");
	}

	@Test
	void test404() {
		Assertions.assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> testClient().getNotFound());
	}

	@Test
	void testDecodes404() {
		final ResponseEntity<String> response = testClient(true).getNotFound();
		assertThat(response).as("response was null").isNotNull();
		assertThat(response.getBody()).as("response body was not null").isNull();
	}

	@Test
	// Issue: https://github.com/spring-cloud/spring-cloud-openfeign/issues/456
	void testResponseEntityHeaders() {
		ResponseEntity<String> response = testClient().getContentType();
		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
	}

	protected interface TestClient {

		@GetMapping("/helloresponse")
		ResponseEntity<Hello> getHelloResponse();

		@GetMapping("/hellovoid")
		ResponseEntity<Void> getHelloVoid();

		@GetMapping("/hello")
		Hello getHello();

		@GetMapping("/hellos")
		List<Hello> getHellos();

		@GetMapping("/hellostrings")
		List<String> getHelloStrings();

		@GetMapping("/hellonotfound")
		ResponseEntity<String> getNotFound();

		@GetMapping("/helloWildcard")
		ResponseEntity<?> getWildcard();

		@GetMapping(path = "/contentType", produces = MediaType.APPLICATION_JSON_VALUE)
		ResponseEntity<String> getContentType();

	}

	public static class Hello {

		private String message;

		Hello() {
		}

		Hello(String message) {
			this.message = message;
		}

		public String getMessage() {
			return this.message;
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
			return Objects.equals(this.message, that.message);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.message);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	@RestController
	@Import(NoSecurityConfiguration.class)
	protected static class Application implements TestClient {

		@Override
		public ResponseEntity<Hello> getHelloResponse() {
			return ResponseEntity.ok(new Hello("hello world via response"));
		}

		@Override
		public ResponseEntity<Void> getHelloVoid() {
			return ResponseEntity.noContent().header("X-test-header", "myval").build();
		}

		@Override
		public Hello getHello() {
			return new Hello("hello world 1");
		}

		@Override
		public List<Hello> getHellos() {
			ArrayList<Hello> hellos = new ArrayList<>();
			hellos.add(new Hello("hello world 1"));
			hellos.add(new Hello("oi terra 2"));
			return hellos;
		}

		@Override
		public List<String> getHelloStrings() {
			ArrayList<String> hellos = new ArrayList<>();
			hellos.add("hello world 1");
			hellos.add("oi terra 2");
			return hellos;
		}

		@Override
		public ResponseEntity<String> getNotFound() {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body((String) null);
		}

		@Override
		public ResponseEntity<?> getWildcard() {
			return ResponseEntity.ok(new Hello("wildcard"));
		}

		@Override
		public ResponseEntity<String> getContentType() {
			return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).body("test");
		}

	}

}
