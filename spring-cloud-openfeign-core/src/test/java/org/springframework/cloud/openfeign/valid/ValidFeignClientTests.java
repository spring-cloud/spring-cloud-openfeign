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

package org.springframework.cloud.openfeign.valid;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
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
import java.util.stream.Collectors;

import feign.Client;
import feign.Logger;
import feign.RequestInterceptor;
import feign.codec.EncodeException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.ServiceInstanceListSuppliers;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.FeignFormatterRegistrar;
import org.springframework.cloud.openfeign.loadbalancer.FeignBlockingLoadBalancerClient;
import org.springframework.cloud.openfeign.support.AbstractFormWriter;
import org.springframework.cloud.openfeign.support.JsonFormWriter;
import org.springframework.cloud.openfeign.test.NoSecurityConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.format.Formatter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

/**
 * @author Spencer Gibb
 * @author Jakub Narloch
 * @author Erik Kringen
 * @author Halvdan Hoem Grelland
 * @author Aaron Whiteside
 * @author Darren Foong
 * @author Olga Maciaszek-Sharma
 */
@SpringBootTest(classes = ValidFeignClientTests.Application.class, webEnvironment = WebEnvironment.RANDOM_PORT,
		value = { "spring.application.name=feignclienttest", "spring.cloud.openfeign.httpclient.hc5.enabled=false",
				"spring.cloud.openfeign.okhttp.enabled=false", "spring.cloud.openfeign.circuitbreaker.enabled=true",
				"spring.cloud.loadbalancer.retry.enabled=false" })
@DirtiesContext
class ValidFeignClientTests {

	public static final String HELLO_WORLD_1 = "hello world 1";

	public static final String OI_TERRA_2 = "oi terra 2";

	public static final String MYHEADER1 = "myheader1";

	public static final String MYHEADER2 = "myheader2";

	@Autowired
	private TestClient testClient;

	@Autowired
	private TestClientServiceId testClientServiceId;

	@Autowired
	private DecodingTestClient decodingTestClient;

	@Autowired
	@Qualifier("localapp2FeignClient")
	private DecodingTestClient namedFeignClient;

	@Autowired
	private Client feignClient;

	@Autowired
	private MultipartClient multipartClient;

	private static ArrayList<Hello> getHelloList() {
		ArrayList<Hello> hellos = new ArrayList<>();
		hellos.add(new Hello(HELLO_WORLD_1));
		hellos.add(new Hello(OI_TERRA_2));
		return hellos;
	}

	@Test
	void testClient() {
		assertThat(testClient).as("testClient was null").isNotNull();
		assertThat(Proxy.isProxyClass(testClient.getClass())).as("testClient is not a java Proxy").isTrue();
		InvocationHandler invocationHandler = Proxy.getInvocationHandler(testClient);
		assertThat(invocationHandler).as("invocationHandler was null").isNotNull();
	}

	@Test
	void testRequestMappingClassLevelPropertyReplacement() {
		Hello hello = testClient.getHelloUsingPropertyPlaceHolder();
		assertThat(hello).as("hello was null").isNotNull();
		assertThat(hello).as("first hello didn't match").isEqualTo(new Hello(OI_TERRA_2));
	}

	@Test
	public void testSimpleType() {
		Hello hello = testClient.getHello();
		assertThat(hello).as("hello was null").isNotNull();
		assertThat(hello).as("first hello didn't match").isEqualTo(new Hello(HELLO_WORLD_1));
	}

	@Test
	void testOptional() {
		Optional<Hello> hello = testClient.getOptionalHello();
		assertThat(hello).isNotNull().isPresent().contains(new Hello(HELLO_WORLD_1));
	}

	@Test
	void testGenericType() {
		List<Hello> hellos = testClient.getHellos();
		assertThat(hellos).as("hellos was null").isNotNull();
		assertThat(getHelloList()).as("hellos didn't match").isEqualTo(hellos);
	}

	@Test
	void testRequestInterceptors() {
		List<String> headers = testClient.getHelloHeaders();
		assertThat(headers).as("headers was null").isNotNull();
		assertThat(headers.contains("myheader1value")).as("headers didn't contain myheader1value").isTrue();
		assertThat(headers.contains("myheader2value")).as("headers didn't contain myheader2value").isTrue();
	}

	@Test
	void testHeaderPlaceholders() {
		String header = testClient.getHelloHeadersPlaceholders();
		assertThat(header).as("header was null").isNotNull();
		assertThat(header).as("header was wrong").isEqualTo("myPlaceholderHeaderValue");
	}

	@Test
	void testFeignClientType() {
		assertThat(feignClient).isInstanceOf(FeignBlockingLoadBalancerClient.class);
		FeignBlockingLoadBalancerClient client = (FeignBlockingLoadBalancerClient) feignClient;
		Client delegate = client.getDelegate();
		assertThat(delegate).isInstanceOf(Client.Default.class);
	}

	@Test
	void testServiceId() {
		assertThat(testClientServiceId).as("testClientServiceId was null").isNotNull();
		final Hello hello = testClientServiceId.getHello();
		assertThat(hello).as("The hello response was null").isNotNull();
		assertThat(hello).as("first hello didn't match").isEqualTo(new Hello(HELLO_WORLD_1));
	}

	@Test
	void testParams() {
		List<String> list = Arrays.asList("a", "1", "test");
		List<String> params = testClient.getParams(list);
		assertThat(params).as("params was null").isNotNull();
		assertThat(params.size()).as("params size was wrong").isEqualTo(list.size());
	}

	@Test
	void testFormattedParams() {
		List<LocalDate> list = Arrays.asList(LocalDate.of(2001, 1, 1), LocalDate.of(2018, 6, 10));
		List<LocalDate> params = testClient.getFormattedParams(list);
		assertThat(params).as("params was null").isNotNull();
		assertThat(params).as("params not converted correctly").isEqualTo(list);
	}

	@Test
	void testNoContentResponse() {
		ResponseEntity<Void> response = testClient.noContent();
		assertThat(response).as("response was null").isNotNull();
		assertThat(response.getStatusCode()).as("status code was wrong").isEqualTo(HttpStatus.NO_CONTENT);
	}

	@Test
	void testHeadResponse() {
		ResponseEntity<Void> response = testClient.head();
		assertThat(response).as("response was null").isNotNull();
		assertThat(response.getStatusCode()).as("status code was wrong").isEqualTo(HttpStatus.OK);
	}

	@Test
	void testHttpEntity() {
		HttpEntity<Hello> entity = testClient.getHelloEntity();
		assertThat(entity).as("entity was null").isNotNull();
		Hello hello = entity.getBody();
		assertThat(hello).as("hello was null").isNotNull();
		assertThat(hello).as("first hello didn't match").isEqualTo(new Hello(HELLO_WORLD_1));
	}

	@Test
	void testMoreComplexHeader() {
		String response = testClient.moreComplexContentType("{\"value\":\"OK\"}");
		assertThat(response).as("response was null").isNotNull();
		assertThat(response).as("didn't respond with {\"value\":\"OK\"}").isEqualTo("{\"value\":\"OK\"}");
	}

	@Test
	void testDecodeNotFound() {
		ResponseEntity<String> response = decodingTestClient.notFound();
		assertThat(response).as("response was null").isNotNull();
		assertThat(response.getStatusCode()).as("status code was wrong").isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody()).as("response body was not null").isNull();
	}

	@Test
	void testOptionalNotFound() {
		Optional<String> s = decodingTestClient.optional();
		assertThat(s).isNotPresent();
	}

	@Test
	void testConvertingExpander() {
		assertThat(testClient.getToString(Arg.A)).isEqualTo(Arg.A.toString());
		assertThat(testClient.getToString(Arg.B)).isEqualTo(Arg.B.toString());

		assertThat(testClient.getToString(new OtherArg("foo"))).isEqualTo("bar");
		List<OtherArg> args = new ArrayList<>();
		args.add(new OtherArg("foo"));
		args.add(new OtherArg("goo"));
		List<String> expectedResult = new ArrayList<>();
		expectedResult.add("bar");
		expectedResult.add("goo");
		assertThat(testClient.getToString(args)).isEqualTo(expectedResult);
	}

	@Test
	void testFormURLEncoded() {
		Hello hello = new Hello(HELLO_WORLD_1);
		Hello response = testClient.postFormUrlEncoded(hello);
		assertThat(response).isEqualTo(hello);
	}

	@Test
	void namedFeignClientWorks() {
		assertThat(namedFeignClient).as("namedFeignClient was null").isNotNull();
	}

	@Test
	void testSingleRequestPart() {
		String response = multipartClient.singlePart("abc");
		assertThat(response).isEqualTo("abc");
	}

	@Test
	void testSinglePojoRequestPart() {
		String response = multipartClient.singlePojoPart(new Hello(HELLO_WORLD_1));
		assertThat(response).isEqualTo(HELLO_WORLD_1);
	}

	@Test
	void testMultipleRequestParts() {
		MockMultipartFile file = new MockMultipartFile("file", "hello.bin", null, "hello".getBytes());
		String response = multipartClient.multipart("abc", "123", file);
		assertThat(response).isEqualTo("abc123hello.bin");
	}

	@Test
	void testMultiplePojoRequestParts() {
		Hello pojo1 = new Hello(HELLO_WORLD_1);
		Hello pojo2 = new Hello(OI_TERRA_2);
		MockMultipartFile file = new MockMultipartFile("file", "hello.bin", null, "hello".getBytes());
		String response = multipartClient.multipartPojo("abc", "123", pojo1, pojo2, file);
		assertThat(response).isEqualTo("abc123hello world 1oi terra 2hello.bin");
	}

	@Test
	void testRequestPartWithListOfMultipartFiles() {
		List<MultipartFile> multipartFiles = Arrays.asList(
				new MockMultipartFile("file1", "hello1.bin", null, "hello".getBytes()),
				new MockMultipartFile("file2", "hello2.bin", null, "hello".getBytes()));
		String partNames = multipartClient.requestPartListOfMultipartFilesReturnsPartNames(multipartFiles);
		assertThat(partNames).isEqualTo("files,files");
		String fileNames = multipartClient.requestPartListOfMultipartFilesReturnsFileNames(multipartFiles);
		assertThat(fileNames).contains("hello1.bin", "hello2.bin");
	}

	@Test
	void testRequestPartWithListOfPojosAndListOfMultipartFiles() {
		Hello pojo1 = new Hello(HELLO_WORLD_1);
		Hello pojo2 = new Hello(OI_TERRA_2);
		MockMultipartFile file1 = new MockMultipartFile("file1", "hello1.bin", null, "hello".getBytes());
		MockMultipartFile file2 = new MockMultipartFile("file2", "hello2.bin", null, "hello".getBytes());
		String response = multipartClient.requestPartListOfPojosAndListOfMultipartFiles(Arrays.asList(pojo1, pojo2),
				Arrays.asList(file1, file2));
		assertThat(response).isEqualTo("hello world 1oi terra 2hello1.binhello2.bin");
	}

	@Test
	void testRequestBodyWithSingleMultipartFile() {
		String partName = UUID.randomUUID().toString();
		MockMultipartFile file1 = new MockMultipartFile(partName, "hello1.bin", null, "hello".getBytes());
		String response = multipartClient.requestBodySingleMultipartFile(file1);
		assertThat(response).isEqualTo(partName);
	}

	@Test
	void testRequestBodyWithListOfMultipartFiles() {
		MockMultipartFile file1 = new MockMultipartFile("file1", "hello1.bin", null, "hello".getBytes());
		MockMultipartFile file2 = new MockMultipartFile("file2", "hello2.bin", null, "hello".getBytes());
		String response = multipartClient.requestBodyListOfMultipartFiles(Arrays.asList(file1, file2));
		assertThat(response).contains("file1", "file2");
	}

	@Test
	void testRequestBodyWithMap() {
		MockMultipartFile file1 = new MockMultipartFile("file1", "hello1.bin", null, "hello".getBytes());
		MockMultipartFile file2 = new MockMultipartFile("file2", "hello2.bin", null, "hello".getBytes());
		Map<String, Object> form = new HashMap<>();
		form.put("file1", file1);
		form.put("file2", file2);
		form.put("hello", "world");
		String response = multipartClient.requestBodyMap(form);
		assertThat(response).contains("file1", "file2", "hello");
	}

	@Test
	void testInvalidMultipartFile() {
		assertThatExceptionOfType(EncodeException.class).isThrownBy(() -> {
			MockMultipartFile file = new MockMultipartFile("file1", "hello1.bin", null, "hello".getBytes());
			multipartClient.invalid(file);
		});
	}

	protected enum Arg {

		A, B;

		@Override
		public String toString() {
			return name().toLowerCase(Locale.ENGLISH);
		}

	}

	@FeignClient(name = "localapp8")
	protected interface MultipartClient {

		@PostMapping(path = "/singlePart", consumes = MULTIPART_FORM_DATA_VALUE, produces = TEXT_PLAIN_VALUE)
		String singlePart(@RequestPart("hello") String hello);

		@PostMapping(path = "/singlePojoPart", consumes = MULTIPART_FORM_DATA_VALUE, produces = TEXT_PLAIN_VALUE)
		String singlePojoPart(@RequestPart("hello") Hello hello);

		@PostMapping(path = "/multipart", consumes = MULTIPART_FORM_DATA_VALUE, produces = TEXT_PLAIN_VALUE)
		String multipart(@RequestPart("hello") String hello, @RequestPart("world") String world,
				@RequestPart("file") MultipartFile file);

		@PostMapping(path = "/multipartPojo", consumes = MULTIPART_FORM_DATA_VALUE, produces = TEXT_PLAIN_VALUE)
		String multipartPojo(@RequestPart("hello") String hello, @RequestPart("world") String world,
				@RequestPart("pojo1") Hello pojo1, @RequestPart("pojo2") Hello pojo2,
				@RequestPart("file") MultipartFile file);

		@PostMapping(path = "/multipartNames", consumes = MULTIPART_FORM_DATA_VALUE, produces = TEXT_PLAIN_VALUE)
		String requestPartListOfMultipartFilesReturnsPartNames(@RequestPart("files") List<MultipartFile> files);

		@PostMapping(path = "/multipartFilenames", consumes = MULTIPART_FORM_DATA_VALUE, produces = TEXT_PLAIN_VALUE)
		String requestPartListOfMultipartFilesReturnsFileNames(@RequestPart("files") List<MultipartFile> files);

		@PostMapping(path = "/multipartPojosFiles", consumes = MULTIPART_FORM_DATA_VALUE, produces = TEXT_PLAIN_VALUE)
		String requestPartListOfPojosAndListOfMultipartFiles(@RequestPart("pojos") List<Hello> pojos,
				@RequestPart("files") List<MultipartFile> files);

		@PostMapping(path = "/multipartNames", consumes = MULTIPART_FORM_DATA_VALUE, produces = TEXT_PLAIN_VALUE)
		String requestBodyListOfMultipartFiles(@RequestBody List<MultipartFile> files);

		@PostMapping(path = "/multipartNames", consumes = MULTIPART_FORM_DATA_VALUE, produces = TEXT_PLAIN_VALUE)
		String requestBodySingleMultipartFile(@RequestBody MultipartFile file);

		@PostMapping(path = "/multipartNames", consumes = MULTIPART_FORM_DATA_VALUE, produces = TEXT_PLAIN_VALUE)
		String requestBodyMap(@RequestBody Map<String, ?> form);

		@PostMapping(path = "/invalid", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE,
				produces = TEXT_PLAIN_VALUE)
		String invalid(@RequestBody MultipartFile file);

	}

	@FeignClient(name = "localapp", configuration = TestClientConfig.class)
	protected interface TestClient {

		@GetMapping("/hello")
		Hello getHello();

		@GetMapping("/hello")
		Optional<Hello> getOptionalHello();

		@GetMapping("${feignClient.methodLevelRequestMappingPath}")
		Hello getHelloUsingPropertyPlaceHolder();

		@GetMapping("/hellos")
		List<Hello> getHellos();

		@GetMapping("/hellostrings")
		List<String> getHelloStrings();

		@GetMapping("/helloheaders")
		List<String> getHelloHeaders();

		@GetMapping(path = "/helloheadersplaceholders",
				headers = "myPlaceholderHeader=${feignClient.myPlaceholderHeader}")
		String getHelloHeadersPlaceholders();

		@GetMapping("/helloparams")
		List<String> getParams(@RequestParam("params") List<String> params);

		@GetMapping("/formattedparams")
		List<LocalDate> getFormattedParams(
				@RequestParam("params") @DateTimeFormat(pattern = "dd-MM-yyyy") List<LocalDate> params);

		@GetMapping("/noContent")
		ResponseEntity<Void> noContent();

		@RequestMapping(method = RequestMethod.HEAD, path = "/head")
		ResponseEntity<Void> head();

		@RequestMapping(method = RequestMethod.GET, path = "/hello")
		HttpEntity<Hello> getHelloEntity();

		@RequestMapping(method = RequestMethod.POST, consumes = "application/vnd.io.spring.cloud.test.v1+json",
				produces = "application/vnd.io.spring.cloud.test.v1+json", path = "/complex")
		String moreComplexContentType(String body);

		@GetMapping("/tostring")
		String getToString(@RequestParam("arg") Arg arg);

		@GetMapping("/tostring2")
		String getToString(@RequestParam("arg") OtherArg arg);

		@GetMapping("/tostringcollection")
		Collection<String> getToString(@RequestParam("arg") Collection<OtherArg> args);

		@PostMapping(path = "/form-urlencoded", consumes = APPLICATION_FORM_URLENCODED_VALUE)
		Hello postFormUrlEncoded(Hello hello);

	}

	@FeignClient(name = "localapp1")
	protected interface TestClientServiceId {

		@GetMapping("/hello")
		Hello getHello();

	}

	@FeignClient(name = "localapp2", dismiss404 = true)
	protected interface DecodingTestClient {

		@GetMapping("/notFound")
		ResponseEntity<String> notFound();

		@GetMapping("/notFound")
		Optional<String> optional();

	}

	protected static class OtherArg {

		public final String value;

		public OtherArg(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return value;
		}

	}

	public static class TestClientConfig {

		@Bean
		public RequestInterceptor interceptor1() {
			return template -> template.header(MYHEADER1, "myheader1value");
		}

		@Bean
		public RequestInterceptor interceptor2() {
			return template -> template.header(MYHEADER2, "myheader2value");
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	@RestController
	@EnableFeignClients(
			clients = { TestClientServiceId.class, TestClient.class, DecodingTestClient.class, MultipartClient.class },
			defaultConfiguration = TestDefaultFeignConfig.class)
	@LoadBalancerClients({

			@LoadBalancerClient(name = "localapp", configuration = LocalLoadBalancerClientConfiguration.class),

			@LoadBalancerClient(name = "localapp1", configuration = LocalLoadBalancerClientConfiguration.class),

			@LoadBalancerClient(name = "localapp2", configuration = LocalLoadBalancerClientConfiguration.class),
			@LoadBalancerClient(name = "localapp8", configuration = LocalLoadBalancerClientConfiguration.class) })
	@Import(NoSecurityConfiguration.class)
	protected static class Application {

		public static void main(String[] args) {
			new SpringApplicationBuilder(Application.class)
					.properties("spring.application.name=feignclienttest", "management.contextPath=/admin").run(args);
		}

		@Bean
		FeignFormatterRegistrar feignFormatterRegistrar() {
			return registry -> registry.addFormatter(new Formatter<OtherArg>() {

				@Override
				public String print(OtherArg object, Locale locale) {
					if ("foo".equals(object.value)) {
						return "bar";
					}
					return object.value;
				}

				@Override
				public OtherArg parse(String text, Locale locale) {
					return new OtherArg(text);
				}
			});
		}

		@Bean
		public AbstractFormWriter jsonFormWriter() {
			return new JsonFormWriter();
		}

		@GetMapping("/hello")
		public Hello getHello() {
			return new Hello(HELLO_WORLD_1);
		}

		@GetMapping("/hello2")
		public Hello getHello2() {
			return new Hello(OI_TERRA_2);
		}

		@GetMapping("/hellos")
		public List<Hello> getHellos() {
			ArrayList<Hello> hellos = getHelloList();
			return hellos;
		}

		@GetMapping("/hellostrings")
		public List<String> getHelloStrings() {
			ArrayList<String> hellos = new ArrayList<>();
			hellos.add(HELLO_WORLD_1);
			hellos.add(OI_TERRA_2);
			return hellos;
		}

		@GetMapping("/helloheaders")
		public List<String> getHelloHeaders(@RequestHeader(MYHEADER1) String myheader1,
				@RequestHeader(MYHEADER2) String myheader2) {
			ArrayList<String> headers = new ArrayList<>();
			headers.add(myheader1);
			headers.add(myheader2);
			return headers;
		}

		@GetMapping("/helloheadersplaceholders")
		public String getHelloHeadersPlaceholders(@RequestHeader("myPlaceholderHeader") String myPlaceholderHeader) {
			return myPlaceholderHeader;
		}

		@GetMapping("/helloparams")
		public List<String> getParams(@RequestParam("params") List<String> params) {
			return params;
		}

		@GetMapping("/formattedparams")
		public List<LocalDate> getFormattedParams(
				@RequestParam("params") @DateTimeFormat(pattern = "dd-MM-yyyy") List<LocalDate> params) {
			return params;
		}

		@GetMapping("/noContent")
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

		@GetMapping("/notFound")
		ResponseEntity<String> notFound() {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body((String) null);
		}

		@RequestMapping(method = RequestMethod.POST, consumes = "application/vnd.io.spring.cloud.test.v1+json",
				produces = "application/vnd.io.spring.cloud.test.v1+json", path = "/complex")
		String complex(@RequestBody String body, @RequestHeader("Content-Length") int contentLength) {
			if (contentLength <= 0) {
				throw new IllegalArgumentException("Invalid Content-Length " + contentLength);
			}
			return body;
		}

		@GetMapping("/tostring")
		String getToString(@RequestParam("arg") Arg arg) {
			return arg.toString();
		}

		@GetMapping("/tostring2")
		String getToString(@RequestParam("arg") OtherArg arg) {
			return arg.value;
		}

		@GetMapping("/tostringcollection")
		Collection<String> getToString(@RequestParam("arg") Collection<OtherArg> args) {
			List<String> result = new ArrayList<>();
			for (OtherArg arg : args) {
				result.add(arg.value);
			}
			return result;
		}

		@PostMapping(path = "/singlePart", consumes = MULTIPART_FORM_DATA_VALUE, produces = TEXT_PLAIN_VALUE)
		String singlePart(@RequestPart("hello") String hello) {
			return hello;
		}

		@PostMapping(path = "/singlePojoPart", consumes = MULTIPART_FORM_DATA_VALUE, produces = TEXT_PLAIN_VALUE)
		String singlePojoPart(@RequestPart("hello") Hello hello) {
			return hello.getMessage();
		}

		@PostMapping(path = "/multipart", consumes = MULTIPART_FORM_DATA_VALUE, produces = TEXT_PLAIN_VALUE)
		String multipart(@RequestPart("hello") String hello, @RequestPart("world") String world,
				@RequestPart("file") MultipartFile file) {
			return hello + world + file.getOriginalFilename();
		}

		@PostMapping(path = "/multipartPojo", consumes = MULTIPART_FORM_DATA_VALUE, produces = TEXT_PLAIN_VALUE)
		String multipartPojo(@RequestPart("hello") String hello, @RequestPart("world") String world,
				@RequestPart("pojo1") Hello pojo1, @RequestPart("pojo2") Hello pojo2,
				@RequestPart("file") MultipartFile file) {
			return hello + world + pojo1.getMessage() + pojo2.getMessage() + file.getOriginalFilename();
		}

		@PostMapping(path = "/multipartNames", consumes = MULTIPART_FORM_DATA_VALUE, produces = TEXT_PLAIN_VALUE)
		String multipartNames(HttpServletRequest request) throws Exception {
			return request.getParts().stream().map(Part::getName).collect(Collectors.joining(","));
		}

		@PostMapping(path = "/multipartFilenames", consumes = MULTIPART_FORM_DATA_VALUE, produces = TEXT_PLAIN_VALUE)
		String multipartFilenames(HttpServletRequest request) throws Exception {
			return request.getParts().stream().map(Part::getSubmittedFileName).collect(Collectors.joining(","));
		}

		@PostMapping(path = "/multipartPojosFiles", consumes = MULTIPART_FORM_DATA_VALUE, produces = TEXT_PLAIN_VALUE)
		String requestPartListOfPojosAndListOfMultipartFiles(@RequestPart("pojos") List<Hello> pojos,
				@RequestPart("files") List<MultipartFile> files) {
			StringBuilder result = new StringBuilder();

			for (Hello pojo : pojos) {
				result.append(pojo.getMessage());
			}

			for (MultipartFile file : files) {
				result.append(file.getOriginalFilename());
			}

			return result.toString();
		}

		@PostMapping(path = "/form-urlencoded", consumes = APPLICATION_FORM_URLENCODED_VALUE)
		Hello postFormUrlEncoded(Hello hello) {
			return hello;
		}

	}

	public static class Hello {

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

	@Configuration(proxyBeanMethods = false)
	public static class TestDefaultFeignConfig {

		@Bean
		Logger.Level feignLoggerLevel() {
			return Logger.Level.FULL;
		}

	}

	// Load balancer with fixed server list for "local" pointing to localhost
	@Configuration(proxyBeanMethods = false)
	public static class LocalLoadBalancerClientConfiguration {

		@LocalServerPort
		private int port = 0;

		@Bean
		public ServiceInstanceListSupplier staticServiceInstanceListSupplier() {
			return ServiceInstanceListSuppliers.from("local",
					new DefaultServiceInstance("local-1", "local", "localhost", port, false));
		}

	}

}
