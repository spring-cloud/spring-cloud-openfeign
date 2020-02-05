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

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * @author Eko Kurniawan Khannedy
 */
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

	public FeignClientUsingPropertiesTests() {
		this.fooFactoryBean = new FeignClientFactoryBean();
		this.fooFactoryBean.setContextId("foo");
		this.fooFactoryBean.setType(FeignClientFactoryBean.class);

		this.barFactoryBean = new FeignClientFactoryBean();
		this.barFactoryBean.setContextId("bar");
		this.barFactoryBean.setType(FeignClientFactoryBean.class);

		this.unwrapFactoryBean = new FeignClientFactoryBean();
		this.unwrapFactoryBean.setContextId("unwrap");
		this.unwrapFactoryBean.setType(FeignClientFactoryBean.class);

		this.formFactoryBean = new FeignClientFactoryBean();
		this.formFactoryBean.setContextId("form");
		this.formFactoryBean.setType(FeignClientFactoryBean.class);
	}

	public FooClient fooClient() {
		this.fooFactoryBean.setApplicationContext(this.applicationContext);
		return this.fooFactoryBean.feign(this.context).target(FooClient.class,
				"http://localhost:" + this.port);
	}

	public BarClient barClient() {
		this.barFactoryBean.setApplicationContext(this.applicationContext);
		return this.barFactoryBean.feign(this.context).target(BarClient.class,
				"http://localhost:" + this.port);
	}

	public UnwrapClient unwrapClient() {
		this.unwrapFactoryBean.setApplicationContext(this.applicationContext);
		return this.unwrapFactoryBean.feign(this.context).target(UnwrapClient.class,
				"http://localhost:" + this.port);
	}

	public FormClient formClient() {
		this.formFactoryBean.setApplicationContext(this.applicationContext);
		return this.formFactoryBean.feign(this.context).target(FormClient.class,
				"http://localhost:" + this.port);
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

	protected interface FooClient {

		@RequestMapping(method = RequestMethod.GET, value = "/foo")
		String foo();

	}

	protected interface BarClient {

		@RequestMapping(method = RequestMethod.GET, value = "/bar")
		String bar();

	}

	protected interface UnwrapClient {

		@RequestMapping(method = RequestMethod.GET, value = "/bar") // intentionally /bar
		String unwrap() throws IOException;

	}

	protected interface FormClient {

		@RequestMapping(value = "/form", method = RequestMethod.POST,
				consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
		String form(Map<String, String> form);

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

		@RequestMapping(method = RequestMethod.GET, value = "/bar")
		public String bar() throws InterruptedException {
			Thread.sleep(2000L);
			return "OK";
		}

		@RequestMapping(value = "/form", method = RequestMethod.POST,
				consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
		public String form(HttpServletRequest request) {
			return request.getParameter("form");
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
