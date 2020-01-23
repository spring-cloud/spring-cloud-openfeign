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

package org.springframework.cloud.openfeign.beans;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import feign.codec.EncodeException;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.FeignClientBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.instanceOf;

/**
 * @author Dave Syer
 * @author Spencer Gibb
 * @author Jakub Narloch
 * @author Erik Kringen
 * @author Halvdan Hoem Grelland
 * @author Aaron Whiteside
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = FeignClientTests.Application.class,
		webEnvironment = WebEnvironment.RANDOM_PORT,
		value = { "spring.application.name=feignclienttest",
				"logging.level.org.springframework.cloud.openfeign.valid=DEBUG",
				"feign.httpclient.enabled=false", "feign.okhttp.enabled=false" })
@DirtiesContext
public class FeignClientTests {

	@Rule
	public ExpectedException expected = ExpectedException.none();

	@Value("${local.server.port}")
	private int port = 0;

	@Autowired
	private TestClient testClient;

	@Autowired
	private ApplicationContext context;

	@Qualifier("uniquequalifier")
	@Autowired
	private org.springframework.cloud.openfeign.beans.extra.TestClient extraClient;

	@Qualifier("build-by-builder")
	@Autowired
	private TestClient buildByBuilder;

	@Autowired
	private MultipartClient multipartClient;

	@Test
	@Ignore // FIXME: 3.0.0
	public void testAnnotations() {
		Map<String, Object> beans = this.context
				.getBeansWithAnnotation(FeignClient.class);
		assertThat(beans.containsKey(TestClient.class.getName()))
				.as("Wrong clients: " + beans).isTrue();
	}

	@Test
	@Ignore // FIXME: 3.0.0
	public void testClient() {
		assertThat(this.testClient).as("testClient was null").isNotNull();
		assertThat(this.extraClient).as("extraClient was null").isNotNull();
		assertThat(Proxy.isProxyClass(this.testClient.getClass()))
				.as("testClient is not a java Proxy").isTrue();
		InvocationHandler invocationHandler = Proxy.getInvocationHandler(this.testClient);
		assertThat(invocationHandler).as("invocationHandler was null").isNotNull();
	}

	@Test
	@Ignore // FIXME: 3.0.0
	public void extraClient() {
		assertThat(this.extraClient).as("extraClient was null").isNotNull();
		assertThat(Proxy.isProxyClass(this.extraClient.getClass()))
				.as("extraClient is not a java Proxy").isTrue();
		InvocationHandler invocationHandler = Proxy
				.getInvocationHandler(this.extraClient);
		assertThat(invocationHandler).as("invocationHandler was null").isNotNull();
	}

	@Test
	@Ignore // FIXME: 3.0.0
	public void buildByBuilder() {
		assertThat(this.buildByBuilder).as("buildByBuilder was null").isNotNull();
		assertThat(Proxy.isProxyClass(this.buildByBuilder.getClass()))
				.as("buildByBuilder is not a java Proxy").isTrue();
		InvocationHandler invocationHandler = Proxy
				.getInvocationHandler(this.buildByBuilder);
		assertThat(invocationHandler).as("invocationHandler was null").isNotNull();
	}

	@Test
	@Ignore // FIXME: 3.0.0
	public void testSingleRequestPart() {
		String response = this.multipartClient.singlePart("abc");
		assertThat(response).isEqualTo("abc");
	}

	@Test
	@Ignore // FIXME: 3.0.0
	public void testMultipleRequestParts() {
		MockMultipartFile file = new MockMultipartFile("file", "hello.bin", null,
				"hello".getBytes());
		String response = this.multipartClient.multipart("abc", "123", file);
		assertThat(response).isEqualTo("abc123hello.bin");
	}

	@Test
	@Ignore // FIXME: 3.0.0
	public void testRequestPartWithListOfMultipartFiles() {
		List<MultipartFile> multipartFiles = Arrays.asList(
				new MockMultipartFile("file1", "hello1.bin", null, "hello".getBytes()),
				new MockMultipartFile("file2", "hello2.bin", null, "hello".getBytes()));
		String partNames = this.multipartClient
				.requestPartListOfMultipartFilesReturnsPartNames(multipartFiles);
		assertThat(partNames).isEqualTo("files,files");
		String fileNames = this.multipartClient
				.requestPartListOfMultipartFilesReturnsFileNames(multipartFiles);
		assertThat(fileNames).contains("hello1.bin", "hello2.bin");
	}

	@Test
	@Ignore // FIXME: 3.0.0
	public void testRequestBodyWithSingleMultipartFile() {
		String partName = UUID.randomUUID().toString();
		MockMultipartFile file1 = new MockMultipartFile(partName, "hello1.bin", null,
				"hello".getBytes());
		String response = this.multipartClient.requestBodySingleMultipartFile(file1);
		assertThat(response).isEqualTo(partName);
	}

	@Test
	@Ignore // FIXME: 3.0.0
	public void testRequestBodyWithListOfMultipartFiles() {
		MockMultipartFile file1 = new MockMultipartFile("file1", "hello1.bin", null,
				"hello".getBytes());
		MockMultipartFile file2 = new MockMultipartFile("file2", "hello2.bin", null,
				"hello".getBytes());
		String response = this.multipartClient
				.requestBodyListOfMultipartFiles(Arrays.asList(file1, file2));
		assertThat(response).contains("file1", "file2");
	}

	@Test
	@Ignore // FIXME: 3.0.0
	public void testRequestBodyWithMap() {
		MockMultipartFile file1 = new MockMultipartFile("file1", "hello1.bin", null,
				"hello".getBytes());
		MockMultipartFile file2 = new MockMultipartFile("file2", "hello2.bin", null,
				"hello".getBytes());
		Map<String, Object> form = new HashMap<>();
		form.put("file1", file1);
		form.put("file2", file2);
		form.put("hello", "world");
		String response = this.multipartClient.requestBodyMap(form);
		assertThat(response).contains("file1", "file2", "hello");
	}

	@Test
	@Ignore // FIXME: 3.0.0
	public void testInvalidMultipartFile() {
		MockMultipartFile file = new MockMultipartFile("file1", "hello1.bin", null,
				"hello".getBytes());
		expected.expectCause(instanceOf(EncodeException.class));
		this.multipartClient.invalid(file);
	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	@RestController
	@EnableFeignClients
	@Import(FeignClientBuilder.class)
	protected static class Application {

		@Bean("build-by-builder")
		public TestClient buildByBuilder(final FeignClientBuilder feignClientBuilder) {
			return feignClientBuilder.forType(TestClient.class, "builderapp").build();
		}

		@RequestMapping(method = RequestMethod.GET, value = "/hello")
		public Hello getHello() {
			return new Hello("hello world 1");
		}

	}

	public static class Hello {

		private String message;

		public Hello() {
		}

		public Hello(String message) {
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
			return this.message != null ? this.message.hashCode() : 0;
		}

	}

	@Configuration(proxyBeanMethods = false)
	public static class TestDefaultFeignConfig {

	}

	@FeignClient(name = "localapp8")
	protected interface MultipartClient {

		@RequestMapping(method = RequestMethod.POST, path = "/singlePart",
				consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
				produces = MediaType.TEXT_PLAIN_VALUE)
		String singlePart(@RequestPart("hello") String hello);

		@RequestMapping(method = RequestMethod.POST, path = "/multipart",
				consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
				produces = MediaType.TEXT_PLAIN_VALUE)
		String multipart(@RequestPart("hello") String hello,
				@RequestPart("world") String world,
				@RequestPart("file") MultipartFile file);

		@RequestMapping(method = RequestMethod.POST, path = "/multipartNames",
				consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
				produces = MediaType.TEXT_PLAIN_VALUE)
		String requestPartListOfMultipartFilesReturnsPartNames(
				@RequestPart("files") List<MultipartFile> files);

		@RequestMapping(method = RequestMethod.POST, path = "/multipartFilenames",
				consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
				produces = MediaType.TEXT_PLAIN_VALUE)
		String requestPartListOfMultipartFilesReturnsFileNames(
				@RequestPart("files") List<MultipartFile> files);

		@RequestMapping(method = RequestMethod.POST, path = "/multipartNames",
				consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
				produces = MediaType.TEXT_PLAIN_VALUE)
		String requestBodyListOfMultipartFiles(@RequestBody List<MultipartFile> files);

		@RequestMapping(method = RequestMethod.POST, path = "/multipartNames",
				consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
				produces = MediaType.TEXT_PLAIN_VALUE)
		String requestBodySingleMultipartFile(@RequestBody MultipartFile file);

		@RequestMapping(method = RequestMethod.POST, path = "/multipartNames",
				consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
				produces = MediaType.TEXT_PLAIN_VALUE)
		String requestBodyMap(@RequestBody Map<String, ?> form);

		@RequestMapping(method = RequestMethod.POST, path = "/invalid",
				consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE,
				produces = MediaType.TEXT_PLAIN_VALUE)
		String invalid(@RequestBody MultipartFile file);

	}

}
