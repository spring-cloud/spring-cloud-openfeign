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

package org.springframework.cloud.openfeign.valid;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import feign.Client;
import feign.Logger;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import rx.Single;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.FeignFormatterRegistrar;
import org.springframework.cloud.openfeign.test.NoSecurityConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Spencer Gibb
 * @author Jakub Narloch
 * @author Erik Kringen
 * @author Halvdan Hoem Grelland
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = FeignClientTests.Application.class,
		webEnvironment = WebEnvironment.RANDOM_PORT,
		value = { "spring.application.name=feignclienttest",
				"logging.level.org.springframework.cloud.openfeign.valid=DEBUG",
				"feign.httpclient.enabled=false", "feign.okhttp.enabled=false",
				"feign.hystrix.enabled=true" })
@DirtiesContext
public class FeignClientTests {

	public static final String HELLO_WORLD_1 = "hello world 1";

	public static final String OI_TERRA_2 = "oi terra 2";

	public static final String MYHEADER1 = "myheader1";

	public static final String MYHEADER2 = "myheader2";

	@Value("${local.server.port}")
	private int port = 0;

	@Autowired
	private TestClient testClient;

	@Autowired
	private TestClientServiceId testClientServiceId;

	@Autowired
	private DecodingTestClient decodingTestClient;

	@Autowired
	private Client feignClient;

	private static ArrayList<Hello> getHelloList() {
		ArrayList<Hello> hellos = new ArrayList<>();
		hellos.add(new Hello(HELLO_WORLD_1));
		hellos.add(new Hello(OI_TERRA_2));
		return hellos;
	}

	@Test
	@Ignore // FIXME: 3.0.0
	public void testClient() {
		assertThat(this.testClient).as("testClient was null").isNotNull();
		assertThat(Proxy.isProxyClass(this.testClient.getClass()))
				.as("testClient is not a java Proxy").isTrue();
		InvocationHandler invocationHandler = Proxy.getInvocationHandler(this.testClient);
		assertThat(invocationHandler).as("invocationHandler was null").isNotNull();
	}

	@Test
	@Ignore // FIXME: 3.0.0
	public void testRequestMappingClassLevelPropertyReplacement() {
		Hello hello = this.testClient.getHelloUsingPropertyPlaceHolder();
		assertThat(hello).as("hello was null").isNotNull();
		assertThat(hello).as("first hello didn't match").isEqualTo(new Hello(OI_TERRA_2));
	}

	@Test
	@Ignore // FIXME: 3.0.0
	public void testSimpleType() {
		Hello hello = this.testClient.getHello();
		assertThat(hello).as("hello was null").isNotNull();
		assertThat(hello).as("first hello didn't match")
				.isEqualTo(new Hello(HELLO_WORLD_1));
	}

	@Test
	@Ignore // FIXME: 3.0.0
	public void testOptional() {
		Optional<Hello> hello = this.testClient.getOptionalHello();
		assertThat(hello).isNotNull().isPresent().contains(new Hello(HELLO_WORLD_1));
	}

	@Test
	@Ignore // FIXME: 3.0.0
	public void testGenericType() {
		List<Hello> hellos = this.testClient.getHellos();
		assertThat(hellos).as("hellos was null").isNotNull();
		assertThat(getHelloList()).as("hellos didn't match").isEqualTo(hellos);
	}

	@Test
	@Ignore // FIXME: 3.0.0
	public void testRequestInterceptors() {
		List<String> headers = this.testClient.getHelloHeaders();
		assertThat(headers).as("headers was null").isNotNull();
		assertThat(headers.contains("myheader1value"))
				.as("headers didn't contain myheader1value").isTrue();
		assertThat(headers.contains("myheader2value"))
				.as("headers didn't contain myheader2value").isTrue();
	}

	@Test
	@Ignore // FIXME: 3.0.0
	public void testHeaderPlaceholders() {
		String header = this.testClient.getHelloHeadersPlaceholders();
		assertThat(header).as("header was null").isNotNull();
		assertThat(header).as("header was wrong").isEqualTo("myPlaceholderHeaderValue");
	}

	@Test
	@Ignore // FIXME 3.0.0
	public void testFeignClientType() throws IllegalAccessException {
		// assertThat(this.feignClient).isInstanceOf(LoadBalancerFeignClient.class);
		// LoadBalancerFeignClient client = (LoadBalancerFeignClient) this.feignClient;
		// Client delegate = client.getDelegate();
		// assertThat(delegate).isInstanceOf(Client.Default.class);
	}

	@Test
	@Ignore // FIXME 3.0.0
	public void testServiceId() {
		assertThat(this.testClientServiceId).as("testClientServiceId was null")
				.isNotNull();
		final Hello hello = this.testClientServiceId.getHello();
		assertThat(hello).as("The hello response was null").isNotNull();
		assertThat(hello).as("first hello didn't match")
				.isEqualTo(new Hello(HELLO_WORLD_1));
	}

	@Test
	@Ignore // FIXME 3.0.0
	public void testParams() {
		List<String> list = Arrays.asList("a", "1", "test");
		List<String> params = this.testClient.getParams(list);
		assertThat(params).as("params was null").isNotNull();
		assertThat(params.size()).as("params size was wrong").isEqualTo(list.size());
	}

	@Test
	@Ignore // FIXME 3.0.0
	public void testFormattedParams() {
		List<LocalDate> list = Arrays.asList(LocalDate.of(2001, 1, 1),
				LocalDate.of(2018, 6, 10));
		List<LocalDate> params = this.testClient.getFormattedParams(list);
		assertThat(params).as("params was null").isNotNull();
		assertThat(params).as("params not converted correctly").isEqualTo(list);
	}

	@Test
	@Ignore // FIXME 3.0.0
	public void testSingle() {
		Single<Hello> single = this.testClient.getHelloSingle();
		assertThat(single).as("single was null").isNotNull();
		Hello hello = single.toBlocking().value();
		assertThat(hello).as("hello was null").isNotNull();
		assertThat(hello).as("first hello didn't match")
				.isEqualTo(new Hello(HELLO_WORLD_1));
	}

	@Test
	@Ignore // FIXME 3.0.0
	public void testNoContentResponse() {
		ResponseEntity<Void> response = this.testClient.noContent();
		assertThat(response).as("response was null").isNotNull();
		assertThat(response.getStatusCode()).as("status code was wrong")
				.isEqualTo(HttpStatus.NO_CONTENT);
	}

	@Test
	@Ignore // FIXME 3.0.0
	public void testHeadResponse() {
		ResponseEntity<Void> response = this.testClient.head();
		assertThat(response).as("response was null").isNotNull();
		assertThat(response.getStatusCode()).as("status code was wrong")
				.isEqualTo(HttpStatus.OK);
	}

	@Test
	@Ignore // FIXME 3.0.0
	public void testHttpEntity() {
		HttpEntity<Hello> entity = this.testClient.getHelloEntity();
		assertThat(entity).as("entity was null").isNotNull();
		Hello hello = entity.getBody();
		assertThat(hello).as("hello was null").isNotNull();
		assertThat(hello).as("first hello didn't match")
				.isEqualTo(new Hello(HELLO_WORLD_1));
	}

	@Test
	@Ignore // FIXME 3.0.0
	public void testMoreComplexHeader() {
		String response = this.testClient.moreComplexContentType("{\"value\":\"OK\"}");
		assertThat(response).as("response was null").isNotNull();
		assertThat(response).as("didn't respond with {\"value\":\"OK\"}")
				.isEqualTo("{\"value\":\"OK\"}");
	}

	@Test
	@Ignore // FIXME 3.0.0
	public void testDecodeNotFound() {
		ResponseEntity<String> response = this.decodingTestClient.notFound();
		assertThat(response).as("response was null").isNotNull();
		assertThat(response.getStatusCode()).as("status code was wrong")
				.isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody()).as("response body was not null").isNull();
	}

	@Test
	@Ignore // FIXME 3.0.0
	public void testOptionalNotFound() {
		Optional<String> s = this.decodingTestClient.optional();
		assertThat(s).isNotPresent();
	}

	@Test
	@Ignore // FIXME 3.0.0
	public void testConvertingExpander() {
		assertThat(this.testClient.getToString(Arg.A)).isEqualTo(Arg.A.toString());
		assertThat(this.testClient.getToString(Arg.B)).isEqualTo(Arg.B.toString());

		assertThat(this.testClient.getToString(new OtherArg("foo"))).isEqualTo("bar");
		List<OtherArg> args = new ArrayList<>();
		args.add(new OtherArg("foo"));
		args.add(new OtherArg("goo"));
		List<String> expectedResult = new ArrayList<>();
		expectedResult.add("bar");
		expectedResult.add("goo");
		assertThat(this.testClient.getToString(args)).isEqualTo(expectedResult);
	}

	@Test
	@Ignore // FIXME 3.0.0
	public void namedFeignClientWorks() {
		// FIXME: 3.0.0
		// assertThat(this.namedHystrixClient).as("namedHystrixClient was
		// null").isNotNull();
	}

	protected enum Arg {

		A, B;

		@Override
		public String toString() {
			return name().toLowerCase(Locale.ENGLISH);
		}

	}

	@FeignClient(name = "localapp", configuration = TestClientConfig.class)
	protected interface TestClient {

		@RequestMapping(method = RequestMethod.GET, path = "/hello")
		Hello getHello();

		@RequestMapping(method = RequestMethod.GET, path = "/hello")
		Optional<Hello> getOptionalHello();

		@RequestMapping(method = RequestMethod.GET,
				path = "${feignClient.methodLevelRequestMappingPath}")
		Hello getHelloUsingPropertyPlaceHolder();

		@RequestMapping(method = RequestMethod.GET, path = "/hello")
		Single<Hello> getHelloSingle();

		@RequestMapping(method = RequestMethod.GET, path = "/hellos")
		List<Hello> getHellos();

		@RequestMapping(method = RequestMethod.GET, path = "/hellostrings")
		List<String> getHelloStrings();

		@RequestMapping(method = RequestMethod.GET, path = "/helloheaders")
		List<String> getHelloHeaders();

		@RequestMapping(method = RequestMethod.GET, path = "/helloheadersplaceholders",
				headers = "myPlaceholderHeader=${feignClient.myPlaceholderHeader}")
		String getHelloHeadersPlaceholders();

		@RequestMapping(method = RequestMethod.GET, path = "/helloparams")
		List<String> getParams(@RequestParam("params") List<String> params);

		@RequestMapping(method = RequestMethod.GET, path = "/formattedparams")
		List<LocalDate> getFormattedParams(@RequestParam("params") @DateTimeFormat(
				pattern = "dd-MM-yyyy") List<LocalDate> params);

		@RequestMapping(method = RequestMethod.GET, path = "/noContent")
		ResponseEntity<Void> noContent();

		@RequestMapping(method = RequestMethod.HEAD, path = "/head")
		ResponseEntity<Void> head();

		@RequestMapping(method = RequestMethod.GET, path = "/hello")
		HttpEntity<Hello> getHelloEntity();

		@RequestMapping(method = RequestMethod.POST,
				consumes = "application/vnd.io.spring.cloud.test.v1+json",
				produces = "application/vnd.io.spring.cloud.test.v1+json",
				path = "/complex")
		String moreComplexContentType(String body);

		@RequestMapping(method = RequestMethod.GET, path = "/tostring")
		String getToString(@RequestParam("arg") Arg arg);

		@RequestMapping(method = RequestMethod.GET, path = "/tostring2")
		String getToString(@RequestParam("arg") OtherArg arg);

		@RequestMapping(method = RequestMethod.GET, path = "/tostringcollection")
		Collection<String> getToString(@RequestParam("arg") Collection<OtherArg> args);

	}

	@FeignClient(name = "localapp1")
	protected interface TestClientServiceId {

		@RequestMapping(method = RequestMethod.GET, path = "/hello")
		Hello getHello();

	}

	@FeignClient(name = "localapp2", decode404 = true)
	protected interface DecodingTestClient {

		@RequestMapping(method = RequestMethod.GET, path = "/notFound")
		ResponseEntity<String> notFound();

		@RequestMapping(method = RequestMethod.GET, path = "/notFound")
		Optional<String> optional();

	}

	protected static class OtherArg {

		public final String value;

		public OtherArg(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return this.value;
		}

	}

	public static class TestClientConfig {

		@Bean
		public RequestInterceptor interceptor1() {
			return new RequestInterceptor() {
				@Override
				public void apply(RequestTemplate template) {
					template.header(MYHEADER1, "myheader1value");
				}
			};
		}

		@Bean
		public RequestInterceptor interceptor2() {
			return new RequestInterceptor() {
				@Override
				public void apply(RequestTemplate template) {
					template.header(MYHEADER2, "myheader2value");
				}
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	@RestController
	@EnableFeignClients(
			clients = { TestClientServiceId.class, TestClient.class,
					DecodingTestClient.class },
			defaultConfiguration = TestDefaultFeignConfig.class)
	/*
	 * @RibbonClients({
	 *
	 * @RibbonClient(name = "localapp", configuration =
	 * LocalRibbonClientConfiguration.class),
	 *
	 * @RibbonClient(name = "localapp1", configuration =
	 * LocalRibbonClientConfiguration.class),
	 *
	 * @RibbonClient(name = "localapp2", configuration =
	 * LocalRibbonClientConfiguration.class),
	 *
	 * @RibbonClient(name = "localapp3", configuration =
	 * LocalRibbonClientConfiguration.class),
	 *
	 * @RibbonClient(name = "localapp4", configuration =
	 * LocalRibbonClientConfiguration.class),
	 *
	 * @RibbonClient(name = "localapp5", configuration =
	 * LocalRibbonClientConfiguration.class),
	 *
	 * @RibbonClient(name = "localapp6", configuration =
	 * LocalRibbonClientConfiguration.class),
	 *
	 * @RibbonClient(name = "localapp7", configuration =
	 * LocalRibbonClientConfiguration.class) })
	 */
	@Import(NoSecurityConfiguration.class)
	protected static class Application {

		public static void main(String[] args) {
			new SpringApplicationBuilder(Application.class)
					.properties("spring.application.name=feignclienttest",
							"management.contextPath=/admin")
					.run(args);
		}

		@Bean
		FeignFormatterRegistrar feignFormatterRegistrar() {
			return new FeignFormatterRegistrar() {

				@Override
				public void registerFormatters(FormatterRegistry registry) {
					registry.addFormatter(new Formatter<OtherArg>() {

						@Override
						public String print(OtherArg object, Locale locale) {
							if ("foo".equals(object.value)) {
								return "bar";
							}
							return object.value;
						}

						@Override
						public OtherArg parse(String text, Locale locale)
								throws ParseException {
							return new OtherArg(text);
						}
					});
				}
			};
		}

		@RequestMapping(method = RequestMethod.GET, path = "/hello")
		public Hello getHello() {
			return new Hello(HELLO_WORLD_1);
		}

		@RequestMapping(method = RequestMethod.GET, path = "/hello2")
		public Hello getHello2() {
			return new Hello(OI_TERRA_2);
		}

		@RequestMapping(method = RequestMethod.GET, path = "/hellos")
		public List<Hello> getHellos() {
			ArrayList<Hello> hellos = getHelloList();
			return hellos;
		}

		@RequestMapping(method = RequestMethod.GET, path = "/hellostrings")
		public List<String> getHelloStrings() {
			ArrayList<String> hellos = new ArrayList<>();
			hellos.add(HELLO_WORLD_1);
			hellos.add(OI_TERRA_2);
			return hellos;
		}

		@RequestMapping(method = RequestMethod.GET, path = "/helloheaders")
		public List<String> getHelloHeaders(@RequestHeader(MYHEADER1) String myheader1,
				@RequestHeader(MYHEADER2) String myheader2) {
			ArrayList<String> headers = new ArrayList<>();
			headers.add(myheader1);
			headers.add(myheader2);
			return headers;
		}

		@RequestMapping(method = RequestMethod.GET, path = "/helloheadersplaceholders")
		public String getHelloHeadersPlaceholders(
				@RequestHeader("myPlaceholderHeader") String myPlaceholderHeader) {
			return myPlaceholderHeader;
		}

		@RequestMapping(method = RequestMethod.GET, path = "/helloparams")
		public List<String> getParams(@RequestParam("params") List<String> params) {
			return params;
		}

		@RequestMapping(method = RequestMethod.GET, path = "/formattedparams")
		public List<LocalDate> getFormattedParams(@RequestParam("params") @DateTimeFormat(
				pattern = "dd-MM-yyyy") List<LocalDate> params) {
			return params;
		}

		@RequestMapping(method = RequestMethod.GET, path = "/noContent")
		ResponseEntity<Void> noContent() {
			return ResponseEntity.noContent().build();
		}

		@RequestMapping(method = RequestMethod.HEAD, path = "/head")
		ResponseEntity<Void> head() {
			return ResponseEntity.ok().build();
		}

		@RequestMapping(method = RequestMethod.GET, path = "/fail")
		String fail() {
			throw new RuntimeException("always fails");
		}

		@RequestMapping(method = RequestMethod.GET, path = "/notFound")
		ResponseEntity<String> notFound() {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body((String) null);
		}

		@RequestMapping(method = RequestMethod.POST,
				consumes = "application/vnd.io.spring.cloud.test.v1+json",
				produces = "application/vnd.io.spring.cloud.test.v1+json",
				path = "/complex")
		String complex(@RequestBody String body,
				@RequestHeader("Content-Length") int contentLength) {
			if (contentLength <= 0) {
				throw new IllegalArgumentException(
						"Invalid Content-Length " + contentLength);
			}
			return body;
		}

		@RequestMapping(method = RequestMethod.GET, path = "/tostring")
		String getToString(@RequestParam("arg") Arg arg) {
			return arg.toString();
		}

		@RequestMapping(method = RequestMethod.GET, path = "/tostring2")
		String getToString(@RequestParam("arg") OtherArg arg) {
			return arg.value;
		}

		@RequestMapping(method = RequestMethod.GET, path = "/tostringcollection")
		Collection<String> getToString(@RequestParam("arg") Collection<OtherArg> args) {
			List<String> result = new ArrayList<>();
			for (OtherArg arg : args) {
				result.add(arg.value);
			}
			return result;
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
			return Objects.hash(this.message);
		}

	}

	@Configuration(proxyBeanMethods = false)
	public static class TestDefaultFeignConfig {

		@Bean
		Logger.Level feignLoggerLevel() {
			return Logger.Level.FULL;
		}

	}

	// Load balancer with fixed server list for "local" pointing to localhost
	@Configuration(proxyBeanMethods = false)
	public static class LocalRibbonClientConfiguration {

		@Value("${local.server.port}")
		private int port = 0;

		/*
		 * @Bean public ServerList<Server> ribbonServerList() { return new
		 * StaticServerList<>(new Server("localhost", this.port)); }
		 */

	}

}
