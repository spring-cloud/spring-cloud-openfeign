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
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import feign.Client;
import feign.Feign;
import feign.Logger;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.Target;
import feign.codec.EncodeException;
import feign.hystrix.FallbackFactory;
import feign.hystrix.SetterFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import rx.Observable;
import rx.Single;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.netflix.ribbon.RibbonClients;
import org.springframework.cloud.netflix.ribbon.StaticServerList;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.FeignFormatterRegistrar;
import org.springframework.cloud.openfeign.ribbon.LoadBalancerFeignClient;
import org.springframework.cloud.openfeign.support.FallbackCommand;
import org.springframework.cloud.openfeign.test.NoSecurityConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;

/**
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
				"feign.httpclient.enabled=false", "feign.okhttp.enabled=false",
				"feign.hystrix.enabled=true" })
@DirtiesContext
public class FeignClientTests {

	public static final String HELLO_WORLD_1 = "hello world 1";

	public static final String OI_TERRA_2 = "oi terra 2";

	public static final String MYHEADER1 = "myheader1";

	public static final String MYHEADER2 = "myheader2";

	@Rule
	public ExpectedException expected = ExpectedException.none();

	@Autowired
	HystrixClient hystrixClient;

	@Autowired
	@Qualifier("localapp3FeignClient")
	HystrixClient namedHystrixClient;

	@Autowired
	HystrixSetterFactoryClient hystrixSetterFactoryClient;

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

	@Autowired
	private HystrixClientWithFallBackFactory hystrixClientWithFallBackFactory;

	@Autowired
	private InvalidTypeHystrixClientWithFallBackFactory invalidTypeHystrixClientWithFallBackFactory;

	@Autowired
	private NullHystrixClientWithFallBackFactory nullHystrixClientWithFallBackFactory;

	@Autowired
	private MultipartClient multipartClient;

	private static ArrayList<Hello> getHelloList() {
		ArrayList<Hello> hellos = new ArrayList<>();
		hellos.add(new Hello(HELLO_WORLD_1));
		hellos.add(new Hello(OI_TERRA_2));
		return hellos;
	}

	@Test
	public void testClient() {
		assertThat(this.testClient).as("testClient was null").isNotNull();
		assertThat(Proxy.isProxyClass(this.testClient.getClass()))
				.as("testClient is not a java Proxy").isTrue();
		InvocationHandler invocationHandler = Proxy.getInvocationHandler(this.testClient);
		assertThat(invocationHandler).as("invocationHandler was null").isNotNull();
	}

	@Test
	public void testRequestMappingClassLevelPropertyReplacement() {
		Hello hello = this.testClient.getHelloUsingPropertyPlaceHolder();
		assertThat(hello).as("hello was null").isNotNull();
		assertThat(hello).as("first hello didn't match").isEqualTo(new Hello(OI_TERRA_2));
	}

	@Test
	public void testSimpleType() {
		Hello hello = this.testClient.getHello();
		assertThat(hello).as("hello was null").isNotNull();
		assertThat(hello).as("first hello didn't match")
				.isEqualTo(new Hello(HELLO_WORLD_1));
	}

	@Test
	public void testOptional() {
		Optional<Hello> hello = this.testClient.getOptionalHello();
		assertThat(hello).isNotNull().isPresent().contains(new Hello(HELLO_WORLD_1));
	}

	@Test
	public void testGenericType() {
		List<Hello> hellos = this.testClient.getHellos();
		assertThat(hellos).as("hellos was null").isNotNull();
		assertThat(getHelloList()).as("hellos didn't match").isEqualTo(hellos);
	}

	@Test
	public void testRequestInterceptors() {
		List<String> headers = this.testClient.getHelloHeaders();
		assertThat(headers).as("headers was null").isNotNull();
		assertThat(headers.contains("myheader1value"))
				.as("headers didn't contain myheader1value").isTrue();
		assertThat(headers.contains("myheader2value"))
				.as("headers didn't contain myheader2value").isTrue();
	}

	@Test
	public void testHeaderPlaceholders() {
		String header = this.testClient.getHelloHeadersPlaceholders();
		assertThat(header).as("header was null").isNotNull();
		assertThat(header).as("header was wrong").isEqualTo("myPlaceholderHeaderValue");
	}

	@Test
	public void testFeignClientType() throws IllegalAccessException {
		assertThat(this.feignClient).isInstanceOf(LoadBalancerFeignClient.class);
		LoadBalancerFeignClient client = (LoadBalancerFeignClient) this.feignClient;
		Client delegate = client.getDelegate();
		assertThat(delegate).isInstanceOf(Client.Default.class);
	}

	@Test
	public void testServiceId() {
		assertThat(this.testClientServiceId).as("testClientServiceId was null")
				.isNotNull();
		final Hello hello = this.testClientServiceId.getHello();
		assertThat(hello).as("The hello response was null").isNotNull();
		assertThat(hello).as("first hello didn't match")
				.isEqualTo(new Hello(HELLO_WORLD_1));
	}

	@Test
	public void testParams() {
		List<String> list = Arrays.asList("a", "1", "test");
		List<String> params = this.testClient.getParams(list);
		assertThat(params).as("params was null").isNotNull();
		assertThat(params.size()).as("params size was wrong").isEqualTo(list.size());
	}

	@Test
	public void testFormattedParams() {
		List<LocalDate> list = Arrays.asList(LocalDate.of(2001, 1, 1),
				LocalDate.of(2018, 6, 10));
		List<LocalDate> params = this.testClient.getFormattedParams(list);
		assertThat(params).as("params was null").isNotNull();
		assertThat(params).as("params not converted correctly").isEqualTo(list);
	}

	@Test
	public void testHystrixCommand() throws NoSuchMethodException {
		HystrixCommand<List<Hello>> command = this.testClient.getHellosHystrix();
		assertThat(command).as("command was null").isNotNull();
		assertThat(command.getCommandGroup().name()).as(
				"Hystrix command group name should match the name of the feign client")
				.isEqualTo("localapp");
		String configKey = Feign.configKey(TestClient.class,
				TestClient.class.getMethod("getHellosHystrix", (Class<?>[]) null));
		assertThat(command.getCommandKey().name())
				.as("Hystrix command key name should match the feign config key")
				.isEqualTo(configKey);
		List<Hello> hellos = command.execute();
		assertThat(hellos).as("hellos was null").isNotNull();
		assertThat(getHelloList()).as("hellos didn't match").isEqualTo(hellos);
	}

	@Test
	public void testSingle() {
		Single<Hello> single = this.testClient.getHelloSingle();
		assertThat(single).as("single was null").isNotNull();
		Hello hello = single.toBlocking().value();
		assertThat(hello).as("hello was null").isNotNull();
		assertThat(hello).as("first hello didn't match")
				.isEqualTo(new Hello(HELLO_WORLD_1));
	}

	@Test
	public void testNoContentResponse() {
		ResponseEntity<Void> response = this.testClient.noContent();
		assertThat(response).as("response was null").isNotNull();
		assertThat(response.getStatusCode()).as("status code was wrong")
				.isEqualTo(HttpStatus.NO_CONTENT);
	}

	@Test
	public void testHeadResponse() {
		ResponseEntity<Void> response = this.testClient.head();
		assertThat(response).as("response was null").isNotNull();
		assertThat(response.getStatusCode()).as("status code was wrong")
				.isEqualTo(HttpStatus.OK);
	}

	@Test
	public void testHttpEntity() {
		HttpEntity<Hello> entity = this.testClient.getHelloEntity();
		assertThat(entity).as("entity was null").isNotNull();
		Hello hello = entity.getBody();
		assertThat(hello).as("hello was null").isNotNull();
		assertThat(hello).as("first hello didn't match")
				.isEqualTo(new Hello(HELLO_WORLD_1));
	}

	@Test
	public void testMoreComplexHeader() {
		String response = this.testClient.moreComplexContentType("{\"value\":\"OK\"}");
		assertThat(response).as("response was null").isNotNull();
		assertThat(response).as("didn't respond with {\"value\":\"OK\"}")
				.isEqualTo("{\"value\":\"OK\"}");
	}

	@Test
	public void testDecodeNotFound() {
		ResponseEntity<String> response = this.decodingTestClient.notFound();
		assertThat(response).as("response was null").isNotNull();
		assertThat(response.getStatusCode()).as("status code was wrong")
				.isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody()).as("response body was not null").isNull();
	}

	@Test
	public void testOptionalNotFound() {
		Optional<String> s = this.decodingTestClient.optional();
		assertThat(s).isNotPresent();
	}

	@Test
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
	public void testHystrixFallbackWorks() {
		Hello hello = this.hystrixClient.fail();
		assertThat(hello).as("hello was null").isNotNull();
		assertThat(hello.getMessage()).as("message was wrong").isEqualTo("fallback");
	}

	@Test
	public void testHystrixFallbackSingle() {
		Single<Hello> single = this.hystrixClient.failSingle();
		assertThat(single).as("single was null").isNotNull();
		Hello hello = single.toBlocking().value();
		assertThat(hello).as("hello was null").isNotNull();
		assertThat(hello.getMessage()).as("message was wrong")
				.isEqualTo("fallbacksingle");
	}

	@Test
	public void testHystrixFallbackCommand() {
		HystrixCommand<Hello> command = this.hystrixClient.failCommand();
		assertThat(command).as("command was null").isNotNull();
		Hello hello = command.execute();
		assertThat(hello).as("hello was null").isNotNull();
		assertThat(hello.getMessage()).as("message was wrong")
				.isEqualTo("fallbackcommand");
	}

	@Test
	public void testHystrixFallbackObservable() {
		Observable<Hello> observable = this.hystrixClient.failObservable();
		assertThat(observable).as("observable was null").isNotNull();
		Hello hello = observable.toBlocking().first();
		assertThat(hello).as("hello was null").isNotNull();
		assertThat(hello.getMessage()).as("message was wrong")
				.isEqualTo("fallbackobservable");
	}

	@Test
	public void testHystrixFallbackFuture() throws Exception {
		Future<Hello> future = this.hystrixClient.failFuture();
		assertThat(future).as("future was null").isNotNull();
		Hello hello = future.get(1, TimeUnit.SECONDS);
		assertThat(hello).as("hello was null").isNotNull();
		assertThat(hello.getMessage()).as("message was wrong")
				.isEqualTo("fallbackfuture");
	}

	@Test
	public void testHystrixClientWithFallBackFactory() throws Exception {
		Hello hello = this.hystrixClientWithFallBackFactory.fail();
		assertThat(hello).as("hello was null").isNotNull();
		assertThat(hello.getMessage()).as("hello#message was null").isNotNull();
		assertThat(hello.getMessage().contains("500")).as(
				"hello#message did not contain the cause (status code) of the fallback invocation")
				.isTrue();
	}

	@Test(expected = HystrixRuntimeException.class)
	public void testInvalidTypeHystrixFallbackFactory() throws Exception {
		this.invalidTypeHystrixClientWithFallBackFactory.fail();
	}

	@Test(expected = HystrixRuntimeException.class)
	public void testNullHystrixFallbackFactory() throws Exception {
		this.nullHystrixClientWithFallBackFactory.fail();
	}

	@Test
	public void namedFeignClientWorks() {
		assertThat(this.namedHystrixClient).as("namedHystrixClient was null").isNotNull();
	}

	@Test
	public void testHystrixSetterFactory() {
		HystrixCommand<List<Hello>> command = this.hystrixSetterFactoryClient
				.getHellosHystrix();
		assertThat(command).as("command was null").isNotNull();
		String setterPrefix = TestHystrixSetterFactoryClientConfig.SETTER_PREFIX;
		assertThat(command.getCommandGroup().name()).as(
				"Hystrix command group name should match the name of the feign client with a prefix of "
						+ setterPrefix)
				.isEqualTo(setterPrefix + "localapp5");
		assertThat(command.getCommandKey().name()).as(
				"Hystrix command key name should match the request method (space) request path with a prefix of "
						+ setterPrefix)
				.isEqualTo(setterPrefix + "GET /hellos");
		List<Hello> hellos = command.execute();
		assertThat(hellos).as("hellos was null").isNotNull();
		assertThat(getHelloList()).as("hellos didn't match").isEqualTo(hellos);
	}

	@Test
	public void testSingleRequestPart() {
		String response = this.multipartClient.singlePart("abc");
		assertThat(response).isEqualTo("abc");
	}

	@Test
	public void testMultipleRequestParts() {
		MockMultipartFile file = new MockMultipartFile("file", "hello.bin", null,
				"hello".getBytes());
		String response = this.multipartClient.multipart("abc", "123", file);
		assertThat(response).isEqualTo("abc123hello.bin");
	}

	@Test
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
	public void testRequestBodyWithSingleMultipartFile() {
		String partName = UUID.randomUUID().toString();
		MockMultipartFile file1 = new MockMultipartFile(partName, "hello1.bin", null,
				"hello".getBytes());
		String response = this.multipartClient.requestBodySingleMultipartFile(file1);
		assertThat(response).isEqualTo(partName);
	}

	@Test
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
	public void testInvalidMultipartFile() {
		MockMultipartFile file = new MockMultipartFile("file1", "hello1.bin", null,
				"hello".getBytes());
		expected.expectCause(instanceOf(EncodeException.class));
		this.multipartClient.invalid(file);
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

		@RequestMapping(method = RequestMethod.GET, path = "/hellos")
		HystrixCommand<List<Hello>> getHellosHystrix();

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

	@FeignClient(name = "localapp3", fallback = HystrixClientFallback.class)
	protected interface HystrixClient {

		@RequestMapping(method = RequestMethod.GET, path = "/fail")
		Single<Hello> failSingle();

		@RequestMapping(method = RequestMethod.GET, path = "/fail")
		Hello fail();

		@RequestMapping(method = RequestMethod.GET, path = "/fail")
		HystrixCommand<Hello> failCommand();

		@RequestMapping(method = RequestMethod.GET, path = "/fail")
		Observable<Hello> failObservable();

		@RequestMapping(method = RequestMethod.GET, path = "/fail")
		Future<Hello> failFuture();

	}

	@FeignClient(name = "localapp4", fallbackFactory = HystrixClientFallbackFactory.class)
	protected interface HystrixClientWithFallBackFactory {

		@RequestMapping(method = RequestMethod.GET, path = "/fail")
		Hello fail();

	}

	@FeignClient(name = "localapp6",
			fallbackFactory = InvalidTypeHystrixClientFallbackFactory.class)
	protected interface InvalidTypeHystrixClientWithFallBackFactory {

		@RequestMapping(method = RequestMethod.GET, path = "/fail")
		Hello fail();

	}

	@FeignClient(name = "localapp7",
			fallbackFactory = NullHystrixClientFallbackFactory.class)
	protected interface NullHystrixClientWithFallBackFactory {

		@RequestMapping(method = RequestMethod.GET, path = "/fail")
		Hello fail();

	}

	@FeignClient(name = "localapp5",
			configuration = TestHystrixSetterFactoryClientConfig.class)
	protected interface HystrixSetterFactoryClient {

		@RequestMapping(method = RequestMethod.GET, path = "/hellos")
		HystrixCommand<List<Hello>> getHellosHystrix();

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

	static class HystrixClientFallbackFactory
			implements FallbackFactory<HystrixClientWithFallBackFactory> {

		@Override
		public HystrixClientWithFallBackFactory create(final Throwable cause) {
			return new HystrixClientWithFallBackFactory() {
				@Override
				public Hello fail() {
					assertThat(cause).isNotNull().as("Cause was null");
					return new Hello(
							"Hello from the fallback side: " + cause.getMessage());
				}
			};
		}

	}

	static class InvalidTypeHystrixClientFallbackFactory
			implements FallbackFactory<String> {

		@Override
		public String create(final Throwable cause) {
			return "hello";
		}

	}

	static class NullHystrixClientFallbackFactory implements FallbackFactory<String> {

		@Override
		public String create(final Throwable cause) {
			return null;
		}

	}

	static class HystrixClientFallback implements HystrixClient {

		@Override
		public Hello fail() {
			return new Hello("fallback");
		}

		@Override
		public Single<Hello> failSingle() {
			return Single.just(new Hello("fallbacksingle"));
		}

		@Override
		public HystrixCommand<Hello> failCommand() {
			return new FallbackCommand<>(new Hello("fallbackcommand"));
		}

		@Override
		public Observable<Hello> failObservable() {
			return Observable.just(new Hello("fallbackobservable"));
		}

		@Override
		public Future<Hello> failFuture() {
			return new FallbackCommand<>(new Hello("fallbackfuture")).queue();
		}

	}

	public static class TestHystrixSetterFactoryClientConfig {

		public static final String SETTER_PREFIX = "SETTER-";

		@Bean
		public SetterFactory commandKeyIsRequestLineSetterFactory() {
			return new SetterFactory() {
				@Override
				public HystrixCommand.Setter create(Target<?> target, Method method) {
					String groupKey = SETTER_PREFIX + target.name();
					RequestMapping requestMapping = method
							.getAnnotation(RequestMapping.class);
					String commandKey = SETTER_PREFIX + requestMapping.method()[0] + " "
							+ requestMapping.path()[0];
					return HystrixCommand.Setter
							.withGroupKey(HystrixCommandGroupKey.Factory.asKey(groupKey))
							.andCommandKey(HystrixCommandKey.Factory.asKey(commandKey));
				}
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	@RestController
	@EnableFeignClients(clients = { TestClientServiceId.class, TestClient.class,
			DecodingTestClient.class, HystrixClient.class,
			HystrixClientWithFallBackFactory.class, HystrixSetterFactoryClient.class,
			InvalidTypeHystrixClientWithFallBackFactory.class,
			NullHystrixClientWithFallBackFactory.class, MultipartClient.class },
			defaultConfiguration = TestDefaultFeignConfig.class)
	@RibbonClients({
			@RibbonClient(name = "localapp",
					configuration = LocalRibbonClientConfiguration.class),
			@RibbonClient(name = "localapp1",
					configuration = LocalRibbonClientConfiguration.class),
			@RibbonClient(name = "localapp2",
					configuration = LocalRibbonClientConfiguration.class),
			@RibbonClient(name = "localapp3",
					configuration = LocalRibbonClientConfiguration.class),
			@RibbonClient(name = "localapp4",
					configuration = LocalRibbonClientConfiguration.class),
			@RibbonClient(name = "localapp5",
					configuration = LocalRibbonClientConfiguration.class),
			@RibbonClient(name = "localapp6",
					configuration = LocalRibbonClientConfiguration.class),
			@RibbonClient(name = "localapp7",
					configuration = LocalRibbonClientConfiguration.class),
			@RibbonClient(name = "localapp8",
					configuration = LocalRibbonClientConfiguration.class) })
	@Import(NoSecurityConfiguration.class)
	protected static class Application {

		public static void main(String[] args) {
			new SpringApplicationBuilder(Application.class)
					.properties("spring.application.name=feignclienttest",
							"management.contextPath=/admin")
					.run(args);
		}

		// needs to be in parent context to test multiple HystrixClient beans
		@Bean
		public HystrixClientFallback hystrixClientFallback() {
			return new HystrixClientFallback();
		}

		@Bean
		public HystrixClientFallbackFactory hystrixClientFallbackFactory() {
			return new HystrixClientFallbackFactory();
		}

		@Bean
		public InvalidTypeHystrixClientFallbackFactory invalidTypeHystrixClientFallbackFactory() {
			return new InvalidTypeHystrixClientFallbackFactory();
		}

		@Bean
		public NullHystrixClientFallbackFactory nullHystrixClientFallbackFactory() {
			return new NullHystrixClientFallbackFactory();
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

		@RequestMapping(method = RequestMethod.POST, path = "/singlePart",
				consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
				produces = MediaType.TEXT_PLAIN_VALUE)
		String multipart(@RequestPart("hello") String hello) {
			return hello;
		}

		@RequestMapping(method = RequestMethod.POST, path = "/multipart",
				consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
				produces = MediaType.TEXT_PLAIN_VALUE)
		String multipart(@RequestPart("hello") String hello,
				@RequestPart("world") String world,
				@RequestPart("file") MultipartFile file) {
			return hello + world + file.getOriginalFilename();
		}

		@RequestMapping(method = RequestMethod.POST, path = "/multipartNames",
				consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
				produces = MediaType.TEXT_PLAIN_VALUE)
		String multipartNames(HttpServletRequest request) throws Exception {
			return request.getParts().stream().map(Part::getName)
					.collect(Collectors.joining(","));
		}

		@RequestMapping(method = RequestMethod.POST, path = "/multipartFilenames",
				consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
				produces = MediaType.TEXT_PLAIN_VALUE)
		String multipartFilenames(HttpServletRequest request) throws Exception {
			return request.getParts().stream().map(Part::getSubmittedFileName)
					.collect(Collectors.joining(","));
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

		@Bean
		public ServerList<Server> ribbonServerList() {
			return new StaticServerList<>(new Server("localhost", this.port));
		}

	}

}
