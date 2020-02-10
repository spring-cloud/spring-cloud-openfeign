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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import feign.MethodMetadata;
import feign.Param;
import org.junit.Before;
import org.junit.Test;

import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.NumberFormat;
import org.springframework.format.number.NumberStyleFormatter;
import org.springframework.format.support.FormattingConversionServiceFactoryBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.MatrixVariable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

/**
 * @author chadjaros
 * @author Halvdan Hoem Grelland
 * @author Aram Peres
 * @author Aaron Whiteside
 */
public class SpringMvcContractTests {

	private static final Class<?> EXECUTABLE_TYPE;

	static {
		Class<?> executableType;
		try {
			executableType = Class.forName("java.lang.reflect.Executable");
		}
		catch (ClassNotFoundException ex) {
			executableType = null;
		}
		EXECUTABLE_TYPE = executableType;
	}

	private SpringMvcContract contract;

	/**
	 * For abstract (e.g. interface) methods, only Java 8 Parameter names (compiler arg
	 * -parameters) can supply parameter names; bytecode-based strategies use local
	 * variable declarations, of which there are none for abstract methods.
	 * @param m method
	 * @return whether a parameter name was found
	 * @throws IllegalArgumentException if method has no parameters
	 */
	private static boolean hasJava8ParameterNames(Method m) {
		org.springframework.util.Assert.isTrue(m.getParameterTypes().length > 0,
				"method has no parameters");
		if (EXECUTABLE_TYPE != null) {
			Method getParameters = ReflectionUtils.findMethod(EXECUTABLE_TYPE,
					"getParameters");
			try {
				Object[] parameters = (Object[]) getParameters.invoke(m);
				Method isNamePresent = ReflectionUtils
						.findMethod(parameters[0].getClass(), "isNamePresent");
				return Boolean.TRUE.equals(isNamePresent.invoke(parameters[0]));
			}
			catch (IllegalAccessException | IllegalArgumentException
					| InvocationTargetException ex) {
			}
		}
		return false;
	}

	@Before
	public void setup() {
		FormattingConversionServiceFactoryBean conversionServiceFactoryBean = new FormattingConversionServiceFactoryBean();
		conversionServiceFactoryBean.afterPropertiesSet();
		ConversionService conversionService = conversionServiceFactoryBean.getObject();

		this.contract = new SpringMvcContract(Collections.emptyList(), conversionService);
	}

	@Test
	public void testProcessAnnotationOnMethod_Simple() throws Exception {
		Method method = TestTemplate_Simple.class.getDeclaredMethod("getTest",
				String.class);
		MethodMetadata data = this.contract
				.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().url()).isEqualTo("/test/{id}");
		assertThat(data.template().method()).isEqualTo("GET");
		assertThat(data.template().headers().get("Accept").iterator().next())
				.isEqualTo(MediaType.APPLICATION_JSON_VALUE);
	}

	@Test
	public void testProcessAnnotations_Simple() throws Exception {
		Method method = TestTemplate_Simple.class.getDeclaredMethod("getTest",
				String.class);
		MethodMetadata data = this.contract
				.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().url()).isEqualTo("/test/{id}");
		assertThat(data.template().method()).isEqualTo("GET");
		assertThat(data.template().headers().get("Accept").iterator().next())
				.isEqualTo(MediaType.APPLICATION_JSON_VALUE);

		assertThat(data.indexToName().get(0).iterator().next()).isEqualTo("id");
	}

	@Test
	public void testProcessAnnotations_SimpleGetMapping() throws Exception {
		Method method = TestTemplate_Simple.class.getDeclaredMethod("getMappingTest",
				String.class);
		MethodMetadata data = this.contract
				.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().url()).isEqualTo("/test/{id}");
		assertThat(data.template().method()).isEqualTo("GET");
		assertThat(data.template().headers().get("Accept").iterator().next())
				.isEqualTo(MediaType.APPLICATION_JSON_VALUE);

		assertThat(data.indexToName().get(0).iterator().next()).isEqualTo("id");
	}

	@Test
	public void testProcessAnnotations_Class_AnnotationsGetSpecificTest()
			throws Exception {
		Method method = TestTemplate_Class_Annotations.class
				.getDeclaredMethod("getSpecificTest", String.class, String.class);
		MethodMetadata data = this.contract
				.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().url()).isEqualTo("/prepend/{classId}/test/{testId}");
		assertThat(data.template().method()).isEqualTo("GET");

		assertThat(data.indexToName().get(0).iterator().next()).isEqualTo("classId");
		assertThat(data.indexToName().get(1).iterator().next()).isEqualTo("testId");
	}

	@Test
	public void testProcessAnnotations_Class_AnnotationsGetAllTests() throws Exception {
		Method method = TestTemplate_Class_Annotations.class
				.getDeclaredMethod("getAllTests", String.class);
		MethodMetadata data = this.contract
				.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().url()).isEqualTo("/prepend/{classId}");
		assertThat(data.template().method()).isEqualTo("GET");

		assertThat(data.indexToName().get(0).iterator().next()).isEqualTo("classId");
	}

	@Test
	public void testProcessAnnotations_ExtendedInterface() throws Exception {
		Method extendedMethod = TestTemplate_Extended.class.getMethod("getAllTests",
				String.class);
		MethodMetadata extendedData = this.contract.parseAndValidateMetadata(
				extendedMethod.getDeclaringClass(), extendedMethod);

		Method method = TestTemplate_Class_Annotations.class
				.getDeclaredMethod("getAllTests", String.class);
		MethodMetadata data = this.contract
				.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().url()).isEqualTo(extendedData.template().url());
		assertThat(data.template().method()).isEqualTo(extendedData.template().method());

		assertThat(data.indexToName().get(0).iterator().next())
				.isEqualTo(data.indexToName().get(0).iterator().next());
	}

	@Test
	public void testProcessAnnotations_SimplePost() throws Exception {
		Method method = TestTemplate_Simple.class.getDeclaredMethod("postTest",
				TestObject.class);
		MethodMetadata data = this.contract
				.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().url()).isEqualTo("/");
		assertThat(data.template().method()).isEqualTo("POST");
		assertThat(data.template().headers().get("Accept").iterator().next())
				.isEqualTo(MediaType.APPLICATION_JSON_VALUE);

	}

	@Test
	public void testProcessAnnotations_SimplePostMapping() throws Exception {
		Method method = TestTemplate_Simple.class.getDeclaredMethod("postMappingTest",
				TestObject.class);
		MethodMetadata data = this.contract
				.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().url()).isEqualTo("/");
		assertThat(data.template().method()).isEqualTo("POST");
		assertThat(data.template().headers().get("Accept").iterator().next())
				.isEqualTo(MediaType.APPLICATION_JSON_VALUE);

	}

	@Test
	public void testProcessAnnotationsOnMethod_Advanced() throws Exception {
		Method method = TestTemplate_Advanced.class.getDeclaredMethod("getTest",
				String.class, String.class, Integer.class);
		MethodMetadata data = this.contract
				.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().url())
				.isEqualTo("/advanced/test/{id}?amount=" + "{amount}");
		assertThat(data.template().method()).isEqualTo("PUT");
		assertThat(data.template().headers().get("Accept").iterator().next())
				.isEqualTo(MediaType.APPLICATION_JSON_VALUE);
	}

	@Test
	public void testProcessAnnotationsOnMethod_Advanced_UnknownAnnotation()
			throws Exception {
		Method method = TestTemplate_Advanced.class.getDeclaredMethod("getTest",
				String.class, String.class, Integer.class);
		this.contract.parseAndValidateMetadata(method.getDeclaringClass(), method);

		// Don't throw an exception and this passes
	}

	@Test
	public void testProcessAnnotations_Advanced() throws Exception {
		Method method = TestTemplate_Advanced.class.getDeclaredMethod("getTest",
				String.class, String.class, Integer.class);
		MethodMetadata data = this.contract
				.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().url())
				.isEqualTo("/advanced/test/{id}?amount=" + "{amount}");
		assertThat(data.template().method()).isEqualTo("PUT");
		assertThat(data.template().headers().get("Accept").iterator().next())
				.isEqualTo(MediaType.APPLICATION_JSON_VALUE);

		assertThat(data.indexToName().get(0).iterator().next())
				.isEqualTo("Authorization");
		assertThat(data.indexToName().get(1).iterator().next()).isEqualTo("id");
		assertThat(data.indexToName().get(2).iterator().next()).isEqualTo("amount");
		assertThat(data.indexToExpander().get(2)).isNotNull();

		assertThat(data.template().headers().get("Authorization").iterator().next())
				.isEqualTo("{Authorization}");
		assertThat(data.template().queries().get("amount").iterator().next())
				.isEqualTo("{amount}");
	}

	@Test
	public void testProcessAnnotations_Aliased() throws Exception {
		Method method = TestTemplate_Advanced.class.getDeclaredMethod("getTest2",
				String.class, Integer.class);
		MethodMetadata data = this.contract
				.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().url())
				.isEqualTo("/advanced/test2?amount=" + "{amount}");
		assertThat(data.template().method()).isEqualTo("PUT");
		assertThat(data.template().headers().get("Accept").iterator().next())
				.isEqualTo(MediaType.APPLICATION_JSON_VALUE);

		assertThat(data.indexToName().get(0).iterator().next())
				.isEqualTo("Authorization");
		assertThat(data.indexToName().get(1).iterator().next()).isEqualTo("amount");

		assertThat(data.template().headers().get("Authorization").iterator().next())
				.isEqualTo("{Authorization}");
		assertThat(data.template().queries().get("amount").iterator().next())
				.isEqualTo("{amount}");
	}

	@Test
	public void testProcessAnnotations_DateTimeFormatParam() throws Exception {
		Method method = TestTemplate_DateTimeFormatParameter.class
				.getDeclaredMethod("getTest", LocalDateTime.class);
		MethodMetadata data = this.contract
				.parseAndValidateMetadata(method.getDeclaringClass(), method);

		Param.Expander expander = data.indexToExpander().get(0);
		assertThat(expander).isNotNull();

		LocalDateTime input = LocalDateTime.of(2001, 10, 12, 23, 56, 3);

		DateTimeFormatter formatter = DateTimeFormatter
				.ofPattern(TestTemplate_DateTimeFormatParameter.CUSTOM_PATTERN);

		String expected = formatter.format(input);

		assertThat(expander.expand(input)).isEqualTo(expected);
	}

	@Test
	public void testProcessAnnotations_NumberFormatParam() throws Exception {
		Method method = TestTemplate_NumberFormatParameter.class
				.getDeclaredMethod("getTest", BigDecimal.class);
		MethodMetadata data = this.contract
				.parseAndValidateMetadata(method.getDeclaringClass(), method);

		Param.Expander expander = data.indexToExpander().get(0);
		assertThat(expander).isNotNull();

		NumberStyleFormatter formatter = new NumberStyleFormatter(
				TestTemplate_NumberFormatParameter.CUSTOM_PATTERN);

		BigDecimal input = BigDecimal.valueOf(1220.345);

		String expected = formatter.print(input, Locale.getDefault());
		String actual = expander.expand(input);

		assertThat(actual).isEqualTo(expected);
	}

	@Test
	public void testProcessAnnotations_Advanced2() throws Exception {
		Method method = TestTemplate_Advanced.class.getDeclaredMethod("getTest");
		MethodMetadata data = this.contract
				.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().url()).isEqualTo("/advanced");
		assertThat(data.template().method()).isEqualTo("GET");
		assertThat(data.template().headers().get("Accept").iterator().next())
				.isEqualTo(MediaType.APPLICATION_JSON_VALUE);
	}

	@Test
	public void testProcessAnnotations_Advanced3() throws Exception {
		Method method = TestTemplate_Simple.class.getDeclaredMethod("getTest");
		MethodMetadata data = this.contract
				.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().url()).isEqualTo("/");
		assertThat(data.template().method()).isEqualTo("GET");
		assertThat(data.template().headers().get("Accept").iterator().next())
				.isEqualTo(MediaType.APPLICATION_JSON_VALUE);
	}

	@Test
	public void testProcessAnnotations_ListParams() throws Exception {
		Method method = TestTemplate_ListParams.class.getDeclaredMethod("getTest",
				List.class);
		MethodMetadata data = this.contract
				.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().url()).isEqualTo("/test?id=" + "{id}");
		assertThat(data.template().method()).isEqualTo("GET");
		assertThat(data.template().queries().get("id").toString()).isEqualTo("[{id}]");
		assertThat(data.indexToExpander().get(0)).isNotNull();
	}

	@Test
	public void testProcessAnnotations_ListParamsWithoutName() throws Exception {
		Method method = TestTemplate_ListParamsWithoutName.class
				.getDeclaredMethod("getTest", List.class);
		MethodMetadata data = this.contract
				.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().url()).isEqualTo("/test?id=" + "{id}");
		assertThat(data.template().method()).isEqualTo("GET");
		assertThat(data.template().queries().get("id").toString()).isEqualTo("[{id}]");
		assertThat(data.indexToExpander().get(0)).isNotNull();
	}

	@Test
	public void testProcessAnnotations_MapParams() throws Exception {
		Method method = TestTemplate_MapParams.class.getDeclaredMethod("getTest",
				Map.class);
		MethodMetadata data = this.contract
				.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().url()).isEqualTo("/test");
		assertThat(data.template().method()).isEqualTo("GET");
		assertThat(data.queryMapIndex()).isNotNull();
		assertThat(data.queryMapIndex().intValue()).isEqualTo(0);
	}

	@Test
	public void testProcessHeaders() throws Exception {
		Method method = TestTemplate_Headers.class.getDeclaredMethod("getTest",
				String.class);
		MethodMetadata data = this.contract
				.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().url()).isEqualTo("/test/{id}");
		assertThat(data.template().method()).isEqualTo("GET");
		assertThat(data.template().headers().get("X-Foo").iterator().next())
				.isEqualTo("bar");
	}

	@Test
	public void testProcessHeadersWithoutValues() throws Exception {
		Method method = TestTemplate_HeadersWithoutValues.class
				.getDeclaredMethod("getTest", String.class);
		MethodMetadata data = this.contract
				.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().url()).isEqualTo("/test/{id}");
		assertThat(data.template().method()).isEqualTo("GET");
		assertThat(data.template().headers().isEmpty()).isTrue();
	}

	@Test
	public void testProcessAnnotations_Fallback() throws Exception {
		Method method = TestTemplate_Advanced.class.getDeclaredMethod("getTestFallback",
				String.class, String.class, Integer.class);

		assumeTrue("does not have java 8 parameter names",
				hasJava8ParameterNames(method));

		MethodMetadata data = this.contract
				.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().url())
				.isEqualTo("/advanced/testfallback/{id}?amount=" + "{amount}");
		assertThat(data.template().method()).isEqualTo("PUT");
		assertThat(data.template().headers().get("Accept").iterator().next())
				.isEqualTo(MediaType.APPLICATION_JSON_VALUE);

		assertThat(data.indexToName().get(0).iterator().next())
				.isEqualTo("Authorization");
		assertThat(data.indexToName().get(1).iterator().next()).isEqualTo("id");
		assertThat(data.indexToName().get(2).iterator().next()).isEqualTo("amount");

		assertThat(data.template().headers().get("Authorization").iterator().next())
				.isEqualTo("{Authorization}");
		assertThat(data.template().queries().get("amount").iterator().next())
				.isEqualTo("{amount}");
	}

	@Test
	public void testProcessHeaderMap() throws Exception {
		Method method = TestTemplate_HeaderMap.class.getDeclaredMethod("headerMap",
				MultiValueMap.class, String.class);
		MethodMetadata data = this.contract
				.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().url()).isEqualTo("/headerMap");
		assertThat(data.template().method()).isEqualTo("GET");
		assertThat(data.headerMapIndex().intValue()).isEqualTo(0);
		Map<String, Collection<String>> headers = data.template().headers();
		assertThat(headers.get("aHeader").iterator().next()).isEqualTo("{aHeader}");
	}

	@Test(expected = IllegalStateException.class)
	public void testProcessHeaderMapMoreThanOnce() throws Exception {
		Method method = TestTemplate_HeaderMap.class.getDeclaredMethod(
				"headerMapMoreThanOnce", MultiValueMap.class, MultiValueMap.class);
		this.contract.parseAndValidateMetadata(method.getDeclaringClass(), method);
	}

	@Test
	public void testProcessQueryMap() throws Exception {
		Method method = TestTemplate_QueryMap.class.getDeclaredMethod("queryMap",
				MultiValueMap.class, String.class);
		MethodMetadata data = this.contract
				.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().url()).isEqualTo("/queryMap?aParam=" + "{aParam}");
		assertThat(data.template().method()).isEqualTo("GET");
		assertThat(data.queryMapIndex().intValue()).isEqualTo(0);
		Map<String, Collection<String>> params = data.template().queries();
		assertThat(params.get("aParam").iterator().next()).isEqualTo("{aParam}");
	}

	@Test
	public void testProcessQueryMapObject() throws Exception {
		Method method = TestTemplate_QueryMap.class.getDeclaredMethod("queryMapObject",
				TestObject.class, String.class);
		MethodMetadata data = this.contract
				.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().url())
				.isEqualTo("/queryMapObject?aParam=" + "{aParam}");
		assertThat(data.template().method()).isEqualTo("GET");
		assertThat(data.queryMapIndex().intValue()).isEqualTo(0);
		Map<String, Collection<String>> params = data.template().queries();
		assertThat(params.get("aParam").iterator().next()).isEqualTo("{aParam}");
	}

	@Test(expected = IllegalStateException.class)
	public void testProcessQueryMapMoreThanOnce() throws Exception {
		Method method = TestTemplate_QueryMap.class.getDeclaredMethod(
				"queryMapMoreThanOnce", MultiValueMap.class, MultiValueMap.class);
		this.contract.parseAndValidateMetadata(method.getDeclaringClass(), method);
	}

	@Test
	public void testMatrixVariable_MapParam() throws Exception {
		Method method = TestTemplate_MatrixVariable.class
				.getDeclaredMethod("matrixVariable", Map.class);
		MethodMetadata data = this.contract
				.parseAndValidateMetadata(method.getDeclaringClass(), method);

		Map<String, String> testMap = new HashMap<>();
		testMap.put("param", "value");

		assertThat(data.template().method()).isEqualTo("GET");
		assertThat(data.template().url()).isEqualTo("/matrixVariable/{params}");
		assertThat(";param=value")
				.isEqualTo(data.indexToExpander().get(0).expand(testMap));
	}

	@Test
	public void testMatrixVariable_ObjectParam() throws Exception {
		Method method = TestTemplate_MatrixVariable.class
				.getDeclaredMethod("matrixVariableObject", Object.class);
		MethodMetadata data = this.contract
				.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().method()).isEqualTo("GET");
		assertThat(data.template().url()).isEqualTo("/matrixVariableObject/{param}");
		assertThat(";param=value")
				.isEqualTo(data.indexToExpander().get(0).expand("value"));
	}

	@Test
	public void testAddingTemplatedParameterWithTheSameKey()
			throws NoSuchMethodException {
		Method method = TestTemplate_Advanced.class.getDeclaredMethod(
				"testAddingTemplatedParamForExistingKey", String.class);
		MethodMetadata data = contract
				.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().headers().get("Accept")).contains("application/json",
				"{Accept}");
	}

	@Test
	public void testMultipleRequestPartAnnotations() throws NoSuchMethodException {
		Method method = TestTemplate_RequestPart.class.getDeclaredMethod(
				"requestWithMultipleParts", MultipartFile.class, String.class);

		MethodMetadata data = contract
				.parseAndValidateMetadata(method.getDeclaringClass(), method);
		assertThat(data.formParams()).contains("file", "id");
	}

	public interface TestTemplate_Simple {

		@RequestMapping(value = "/test/{id}", method = RequestMethod.GET,
				produces = MediaType.APPLICATION_JSON_VALUE)
		ResponseEntity<TestObject> getTest(@PathVariable("id") String id);

		@RequestMapping(method = RequestMethod.GET,
				produces = MediaType.APPLICATION_JSON_VALUE)
		TestObject getTest();

		@GetMapping(value = "/test/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
		ResponseEntity<TestObject> getMappingTest(@PathVariable("id") String id);

		@RequestMapping(method = RequestMethod.POST,
				produces = MediaType.APPLICATION_JSON_VALUE)
		TestObject postTest(@RequestBody TestObject object);

		@PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
		TestObject postMappingTest(@RequestBody TestObject object);

	}

	@RequestMapping("/prepend/{classId}")
	public interface TestTemplate_Class_Annotations {

		@RequestMapping(value = "/test/{testId}", method = RequestMethod.GET)
		TestObject getSpecificTest(@PathVariable("classId") String classId,
				@PathVariable("testId") String testId);

		@RequestMapping(method = RequestMethod.GET)
		TestObject getAllTests(@PathVariable("classId") String classId);

	}

	public interface TestTemplate_Extended extends TestTemplate_Class_Annotations {

	}

	public interface TestTemplate_Headers {

		@RequestMapping(value = "/test/{id}", method = RequestMethod.GET,
				headers = "X-Foo=bar")
		ResponseEntity<TestObject> getTest(@PathVariable("id") String id);

	}

	public interface TestTemplate_HeadersWithoutValues {

		@RequestMapping(value = "/test/{id}", method = RequestMethod.GET,
				headers = { "X-Foo", "!X-Bar", "X-Baz!=fooBar" })
		ResponseEntity<TestObject> getTest(@PathVariable("id") String id);

	}

	public interface TestTemplate_ListParams {

		@RequestMapping(value = "/test", method = RequestMethod.GET)
		ResponseEntity<TestObject> getTest(@RequestParam("id") List<String> id);

	}

	public interface TestTemplate_ListParamsWithoutName {

		@RequestMapping(value = "/test", method = RequestMethod.GET)
		ResponseEntity<TestObject> getTest(@RequestParam List<String> id);

	}

	public interface TestTemplate_MapParams {

		@RequestMapping(value = "/test", method = RequestMethod.GET)
		ResponseEntity<TestObject> getTest(@RequestParam Map<String, String> params);

	}

	public interface TestTemplate_HeaderMap {

		@RequestMapping(path = "/headerMap")
		String headerMap(@RequestHeader MultiValueMap<String, String> headerMap,
				@RequestHeader(name = "aHeader") String aHeader);

		@RequestMapping(path = "/headerMapMoreThanOnce")
		String headerMapMoreThanOnce(
				@RequestHeader MultiValueMap<String, String> headerMap1,
				@RequestHeader MultiValueMap<String, String> headerMap2);

	}

	public interface TestTemplate_QueryMap {

		@RequestMapping(path = "/queryMap")
		String queryMap(@RequestParam MultiValueMap<String, String> queryMap,
				@RequestParam(name = "aParam") String aParam);

		@RequestMapping(path = "/queryMapMoreThanOnce")
		String queryMapMoreThanOnce(@RequestParam MultiValueMap<String, String> queryMap1,
				@RequestParam MultiValueMap<String, String> queryMap2);

		@RequestMapping(path = "/queryMapObject")
		String queryMapObject(@SpringQueryMap TestObject queryMap,
				@RequestParam(name = "aParam") String aParam);

	}

	public interface TestTemplate_RequestPart {

		@RequestMapping(path = "/requestPart", method = RequestMethod.POST,
				consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
		void requestWithMultipleParts(@RequestPart("file") MultipartFile file,
				@RequestPart("id") String identifier);

	}

	public interface TestTemplate_MatrixVariable {

		@RequestMapping(path = "/matrixVariable/{params}")
		String matrixVariable(@MatrixVariable("params") Map<String, Object> params);

		@RequestMapping(path = "/matrixVariableObject/{param}")
		String matrixVariableObject(@MatrixVariable("param") Object object);

	}

	@JsonAutoDetect
	@RequestMapping("/advanced")
	public interface TestTemplate_Advanced {

		@ExceptionHandler
		@RequestMapping(path = "/test/{id}", method = RequestMethod.PUT,
				produces = MediaType.APPLICATION_JSON_VALUE)
		ResponseEntity<TestObject> getTest(@RequestHeader("Authorization") String auth,
				@PathVariable("id") String id, @RequestParam("amount") Integer amount);

		@RequestMapping(path = "/test2", method = RequestMethod.PUT,
				produces = MediaType.APPLICATION_JSON_VALUE)
		ResponseEntity<TestObject> getTest2(
				@RequestHeader(name = "Authorization") String auth,
				@RequestParam(name = "amount") Integer amount);

		@ExceptionHandler
		@RequestMapping(path = "/testfallback/{id}", method = RequestMethod.PUT,
				produces = MediaType.APPLICATION_JSON_VALUE)
		ResponseEntity<TestObject> getTestFallback(@RequestHeader String Authorization,
				@PathVariable String id, @RequestParam Integer amount);

		@RequestMapping(method = RequestMethod.GET,
				produces = MediaType.APPLICATION_JSON_VALUE)
		TestObject getTest();

		@GetMapping(produces = "application/json")
		String testAddingTemplatedParamForExistingKey(
				@RequestHeader("Accept") String accept);

	}

	public interface TestTemplate_DateTimeFormatParameter {

		String CUSTOM_PATTERN = "dd-MM-yyyy HH:mm";

		@RequestMapping(method = RequestMethod.GET)
		String getTest(@RequestParam(name = "localDateTime") @DateTimeFormat(
				pattern = CUSTOM_PATTERN) LocalDateTime localDateTime);

	}

	public interface TestTemplate_NumberFormatParameter {

		String CUSTOM_PATTERN = "$###,###.###";

		@RequestMapping(method = RequestMethod.GET)
		String getTest(@RequestParam("amount") @NumberFormat(
				pattern = CUSTOM_PATTERN) BigDecimal amount);

	}

	@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE,
			setterVisibility = NONE)
	public class TestObject {

		public String something;

		public Double number;

		public TestObject() {
		}

		public TestObject(String something, Double number) {
			this.something = something;
			this.number = number;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			TestObject that = (TestObject) o;

			if (this.number != null ? !this.number.equals(that.number)
					: that.number != null) {
				return false;
			}
			if (this.something != null ? !this.something.equals(that.something)
					: that.something != null) {
				return false;
			}

			return true;
		}

		@Override
		public int hashCode() {
			int result = (this.something != null ? this.something.hashCode() : 0);
			result = 31 * result + (this.number != null ? this.number.hashCode() : 0);
			return result;
		}

		@Override
		public String toString() {
			return new StringBuilder("TestObject{").append("something='")
					.append(this.something).append("', ").append("number=")
					.append(this.number).append("}").toString();
		}

	}

}
