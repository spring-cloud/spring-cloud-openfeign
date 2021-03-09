/*
 * Copyright 2013-2021 the original author or authors.
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

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

import feign.InvocationHandlerFactory;
import feign.Request;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.RetryableException;
import feign.Retryer;
import feign.codec.EncodeException;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.test.NoSecurityConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * @author Eko Kurniawan Khannedy
 * @author Olga Maciaszek-Sharma
 * @author Ilia Ilinykh
 */
@SuppressWarnings("FieldMayBeFinal")
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = FeignClientUsingPropertiesTests.Application.class,
		webEnvironment = RANDOM_PORT)
@TestPropertySource("classpath:feign-properties.properties")
@DirtiesContext
public class FeignClientUsingPropertiesTests {

	@Autowired
	FeignContext context;

	@Autowired
	private ApplicationContext applicationContext;

	@Value("${local.server.port}")
	private int port = 0;

	private FeignClientFactoryBean fooFactoryBean;

	private FeignClientFactoryBean barFactoryBean;

	private FeignClientFactoryBean unwrapFactoryBean;

	private FeignClientFactoryBean formFactoryBean;

	private FeignClientFactoryBean defaultHeadersAndQuerySingleParamsFeignClientFactoryBean;

	private FeignClientFactoryBean defaultHeadersAndQueryMultipleParamsFeignClientFactoryBean;

	public FeignClientUsingPropertiesTests() {
		fooFactoryBean = new FeignClientFactoryBean();
		fooFactoryBean.setContextId("foo");
		fooFactoryBean.setType(FeignClientFactoryBean.class);

		barFactoryBean = new FeignClientFactoryBean();
		barFactoryBean.setContextId("bar");
		barFactoryBean.setType(FeignClientFactoryBean.class);

		unwrapFactoryBean = new FeignClientFactoryBean();
		unwrapFactoryBean.setContextId("unwrap");
		unwrapFactoryBean.setType(FeignClientFactoryBean.class);

		formFactoryBean = new FeignClientFactoryBean();
		formFactoryBean.setContextId("form");
		formFactoryBean.setType(FeignClientFactoryBean.class);

		this.defaultHeadersAndQuerySingleParamsFeignClientFactoryBean = new FeignClientFactoryBean();
		this.defaultHeadersAndQuerySingleParamsFeignClientFactoryBean
				.setContextId("singleValue");
		this.defaultHeadersAndQuerySingleParamsFeignClientFactoryBean
				.setType(FeignClientFactoryBean.class);

		this.defaultHeadersAndQueryMultipleParamsFeignClientFactoryBean = new FeignClientFactoryBean();
		this.defaultHeadersAndQueryMultipleParamsFeignClientFactoryBean
				.setContextId("multipleValue");
		this.defaultHeadersAndQueryMultipleParamsFeignClientFactoryBean
				.setType(FeignClientFactoryBean.class);
	}

	public FooClient fooClient() {
		fooFactoryBean.setApplicationContext(applicationContext);
		return fooFactoryBean.feign(context).target(FooClient.class,
				"http://localhost:" + port);
	}

	public BarClient barClient() {
		barFactoryBean.setApplicationContext(applicationContext);
		return barFactoryBean.feign(context).target(BarClient.class,
				"http://localhost:" + port);
	}

	public UnwrapClient unwrapClient() {
		unwrapFactoryBean.setApplicationContext(applicationContext);
		return unwrapFactoryBean.feign(context).target(UnwrapClient.class,
				"http://localhost:" + port);
	}

	public FormClient formClient() {
		formFactoryBean.setApplicationContext(applicationContext);
		return formFactoryBean.feign(context).target(FormClient.class,
				"http://localhost:" + port);
	}

	@Test
	public void testFoo() {
		String response = fooClient().foo();
		assertThat(response).isEqualTo("OK");
	}

	@Test(expected = RetryableException.class)
	public void testBar() {
		barClient().bar();
		fail("it should timeout");
	}

	@Test(expected = SocketTimeoutException.class)
	public void testUnwrap() throws Exception {
		unwrapClient().unwrap();
		fail("it should timeout");
	}

	@Test
	public void testForm() {
		Map<String, String> request = Collections.singletonMap("form", "Data");
		String response = formClient().form(request);
		assertThat(response).isEqualTo("Data");
	}

	@Test
	public void testSingleValue() {
		List<String> response = singleValueClient().singleValue();
		assertThat(response).isEqualTo(Arrays.asList("header", "parameter"));
	}

	@Test
	public void testMultipleValue() {
		List<String> response = multipleValueClient().multipleValue();
		assertThat(response).isEqualTo(
				Arrays.asList("header1", "header2", "parameter1", "parameter2"));
	}

	public SingleValueClient singleValueClient() {
		this.defaultHeadersAndQuerySingleParamsFeignClientFactoryBean
				.setApplicationContext(this.applicationContext);
		return this.defaultHeadersAndQuerySingleParamsFeignClientFactoryBean
				.feign(this.context)
				.target(SingleValueClient.class, "http://localhost:" + this.port);
	}

	public MultipleValueClient multipleValueClient() {
		this.defaultHeadersAndQueryMultipleParamsFeignClientFactoryBean
				.setApplicationContext(this.applicationContext);
		return this.defaultHeadersAndQueryMultipleParamsFeignClientFactoryBean
				.feign(this.context)
				.target(MultipleValueClient.class, "http://localhost:" + this.port);
	}

	@Test
	public void readTimeoutShouldWorkWhenConnectTimeoutNotSet() {
		FeignClientFactoryBean readTimeoutFactoryBean = new FeignClientFactoryBean();
		readTimeoutFactoryBean.setContextId("readTimeout");
		readTimeoutFactoryBean.setType(FeignClientFactoryBean.class);
		readTimeoutFactoryBean.setApplicationContext(applicationContext);

		TimeoutClient client = readTimeoutFactoryBean.feign(context)
				.target(TimeoutClient.class, "http://localhost:" + port);

		Request.Options options = getRequestOptions((Proxy) client);

		assertThat(options.readTimeoutMillis()).isEqualTo(1000);
		assertThat(options.connectTimeoutMillis()).isEqualTo(5000);
	}

	@Test
	public void connectTimeoutShouldWorkWhenReadTimeoutNotSet() {
		FeignClientFactoryBean readTimeoutFactoryBean = new FeignClientFactoryBean();
		readTimeoutFactoryBean.setContextId("connectTimeout");
		readTimeoutFactoryBean.setType(FeignClientFactoryBean.class);
		readTimeoutFactoryBean.setApplicationContext(applicationContext);

		TimeoutClient client = readTimeoutFactoryBean.feign(context)
				.target(TimeoutClient.class, "http://localhost:" + port);

		Request.Options options = getRequestOptions((Proxy) client);

		assertThat(options.connectTimeoutMillis()).isEqualTo(1000);
		assertThat(options.readTimeoutMillis()).isEqualTo(5000);
	}

	@Test
	public void shouldSetFollowRedirects() {
		FeignClientFactoryBean testFactoryBean = new FeignClientFactoryBean();
		testFactoryBean.setContextId("test");
		testFactoryBean.setType(FeignClientFactoryBean.class);
		testFactoryBean.setApplicationContext(applicationContext);

		TimeoutClient client = testFactoryBean.feign(context).target(TimeoutClient.class,
				"http://localhost:" + port);

		Request.Options options = getRequestOptions((Proxy) client);

		assertThat(options.isFollowRedirects()).isFalse();
	}

	private Request.Options getRequestOptions(Proxy client) {
		Object invocationHandler = ReflectionTestUtils.getField(client, "h");
		Map<Method, InvocationHandlerFactory.MethodHandler> dispatch = (Map<Method, InvocationHandlerFactory.MethodHandler>) ReflectionTestUtils
				.getField(Objects.requireNonNull(invocationHandler), "dispatch");
		Method key = new ArrayList<>(dispatch.keySet()).get(0);
		return (Request.Options) ReflectionTestUtils.getField(dispatch.get(key),
				"options");
	}

	protected interface FooClient {

		@GetMapping(path = "/foo")
		String foo();

	}

	protected interface BarClient {

		@GetMapping(path = "/bar")
		String bar();

	}

	protected interface UnwrapClient {

		@GetMapping(path = "/bar") // intentionally /bar
		String unwrap() throws IOException;

	}

	protected interface FormClient {

		@RequestMapping(value = "/form", method = RequestMethod.POST,
				consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
		String form(Map<String, String> form);

	}

	protected interface SingleValueClient {

		@GetMapping(path = "/singleValue")
		List<String> singleValue();

	}

	protected interface MultipleValueClient {

		@GetMapping(path = "/multipleValue")
		List<String> multipleValue();

	}

	protected interface TimeoutClient {

		@GetMapping("/timeouts")
		String timeouts();

	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	@RestController
	@Import(NoSecurityConfiguration.class)
	protected static class Application {

		@RequestMapping(method = RequestMethod.GET, value = "/foo")
		public String foo(HttpServletRequest request) throws IllegalAccessException {
			if ("Foo".equals(request.getHeader("Foo"))
					&& "Bar".equals(request.getHeader("Bar"))) {
				return "OK";
			}
			else {
				throw new IllegalAccessException("It should has Foo and Bar header");
			}
		}

		@GetMapping(path = "/bar")
		public String bar() throws InterruptedException {
			TimeUnit.SECONDS.sleep(2);
			return "OK";
		}

		@PostMapping(path = "/form",
				consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
		public String form(HttpServletRequest request) {
			return request.getParameter("form");
		}

		@GetMapping(path = "/singleValue")
		public List<String> singleValue(@RequestHeader List<String> singleValueHeaders,
				@RequestParam List<String> singleValueParameters) {
			return Stream.of(singleValueHeaders, singleValueParameters)
					.flatMap(Collection::stream).collect(Collectors.toList());
		}

		@GetMapping(path = "/multipleValue")
		public List<String> multipleValue(
				@RequestHeader List<String> multipleValueHeaders,
				@RequestParam List<String> multipleValueParameters) {
			return Stream.of(multipleValueHeaders, multipleValueParameters)
					.flatMap(Collection::stream).collect(Collectors.toList());
		}

	}

	public static class FooRequestInterceptor implements RequestInterceptor {

		@Override
		public void apply(RequestTemplate template) {
			template.header("Foo", "Foo");
		}

	}

	public static class BarRequestInterceptor implements RequestInterceptor {

		@Override
		public void apply(RequestTemplate template) {
			template.header("Bar", "Bar");
		}

	}

	public static class NoRetryer implements Retryer {

		@Override
		public void continueOrPropagate(RetryableException e) {
			throw e;
		}

		@Override
		public Retryer clone() {
			return this;
		}

	}

	public static class DefaultErrorDecoder extends ErrorDecoder.Default {

	}

	public static class FormEncoder implements Encoder {

		@Override
		public void encode(Object o, Type type, RequestTemplate requestTemplate)
				throws EncodeException {
			Map<String, String> form = (Map<String, String>) o;
			StringBuilder builder = new StringBuilder();
			form.forEach((key, value) -> {
				builder.append(key + "=" + value + "&");
			});

			requestTemplate.header(HttpHeaders.CONTENT_TYPE,
					MediaType.APPLICATION_FORM_URLENCODED_VALUE);
			requestTemplate.body(builder.toString());
		}

	}

}
