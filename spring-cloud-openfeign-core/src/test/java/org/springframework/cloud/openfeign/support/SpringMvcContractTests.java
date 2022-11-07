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
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import feign.MethodMetadata;
import feign.Param;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.cloud.openfeign.CollectionFormat;
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
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.MatrixVariable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static feign.CollectionFormat.CSV;
import static feign.CollectionFormat.SSV;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * @author chadjaros
 * @author Halvdan Hoem Grelland
 * @author Aram Peres
 * @author Aaron Whiteside
 * @author Artyom Romanenko
 * @author Olga Maciaszek-Sharma
 * @author Szymon Linowski
 * @author Sam Kruglov
 * @author Bhavya Agrawal
 **/

class SpringMvcContractTests {

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
		org.springframework.util.Assert.isTrue(m.getParameterTypes().length > 0, "method has no parameters");
		if (EXECUTABLE_TYPE != null) {
			Method getParameters = ReflectionUtils.findMethod(EXECUTABLE_TYPE, "getParameters");
			try {
				Object[] parameters = (Object[]) getParameters.invoke(m);
				Method isNamePresent = ReflectionUtils.findMethod(parameters[0].getClass(), "isNamePresent");
				return Boolean.TRUE.equals(isNamePresent.invoke(parameters[0]));
			}
			catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ignored) {
			}
		}
		return false;
	}

	@BeforeEach
	void setup() {
		contract = new SpringMvcContract(Collections.emptyList(), getConversionService());
	}

	@Test
	void testProcessAnnotationOnMethod_Simple() throws Exception {
		Method method = TestTemplate_Simple.class.getDeclaredMethod("getTest", String.class);
		MethodMetadata data = contract.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().url()).isEqualTo("/test/{id}");
		assertThat(data.template().method()).isEqualTo("GET");
		assertThat(data.template().headers().get("Accept").iterator().next())
				.isEqualTo(MediaType.APPLICATION_JSON_VALUE);
		assertThat(data.template().decodeSlash()).isTrue();
	}

	@Test
	void testProcessAnnotationOnMethod_Simple_RegexPathVariable() throws Exception {
		Method method = TestTemplate_Simple.class.getDeclaredMethod("getTestWithDigitalId", String.class);
		MethodMetadata data = contract.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().url()).isEqualTo("/test/{id:\\d+}");
		assertThat(data.template().method()).isEqualTo("GET");
		assertThat(data.formParams()).isEmpty();
	}

	@Test
	void testProcessAnnotationOnMethod_Simple_SlashEncoded() throws Exception {
		contract = new SpringMvcContract(Collections.emptyList(), getConversionService(), false);

		Method method = TestTemplate_Simple.class.getDeclaredMethod("getTest", String.class);
		MethodMetadata data = contract.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().url()).isEqualTo("/test/{id}");

		assertThat(data.template().decodeSlash()).isFalse();
	}

	@Test
	void testProcessAnnotations_Simple() throws Exception {
		Method method = TestTemplate_Simple.class.getDeclaredMethod("getTest", String.class);
		MethodMetadata data = contract.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().url()).isEqualTo("/test/{id}");
		assertThat(data.template().method()).isEqualTo("GET");
		assertThat(data.template().headers().get("Accept").iterator().next())
				.isEqualTo(MediaType.APPLICATION_JSON_VALUE);

		assertThat(data.indexToName().get(0).iterator().next()).isEqualTo("id");
	}

	@Test
	void testProcessAnnotations_SimpleNoPath() throws Exception {
		Method method = TestTemplate_Simple.class.getDeclaredMethod("getTest");
		MethodMetadata data = contract.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().url()).isEqualTo("/");
		assertThat(data.template().method()).isEqualTo("GET");
		assertThat(data.template().headers().get("Accept").iterator().next())
				.isEqualTo(MediaType.APPLICATION_JSON_VALUE);
	}

	@Test
	void testProcessAnnotations_SimplePathIsOnlyASlash() throws Exception {
		Method method = TestTemplate_Simple.class.getDeclaredMethod("getSlashPath", String.class);
		MethodMetadata data = contract.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().url()).isEqualTo("/?id=" + "{id}");
		assertThat(data.template().method()).isEqualTo("GET");
		assertThat(data.template().headers().get("Accept").iterator().next())
				.isEqualTo(MediaType.APPLICATION_JSON_VALUE);
	}

	@Test
	void testProcessAnnotations_MissingLeadingSlashInPath() throws Exception {
		Method method = TestTemplate_Simple.class.getDeclaredMethod("getTestNoLeadingSlash", String.class);
		MethodMetadata data = contract.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().url()).isEqualTo("/test?name=" + "{name}");
		assertThat(data.template().method()).isEqualTo("GET");
		assertThat(data.template().headers().get("Accept").iterator().next())
				.isEqualTo(MediaType.APPLICATION_JSON_VALUE);
	}

	@Test
	void testProcessAnnotations_SimpleGetMapping() throws Exception {
		Method method = TestTemplate_Simple.class.getDeclaredMethod("getMappingTest", String.class);
		MethodMetadata data = contract.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().url()).isEqualTo("/test/{id}");
		assertThat(data.template().method()).isEqualTo("GET");
		assertThat(data.template().headers().get("Accept").iterator().next())
				.isEqualTo(MediaType.APPLICATION_JSON_VALUE);

		assertThat(data.indexToName().get(0).iterator().next()).isEqualTo("id");
	}

	@Test
	void testProcessAnnotations_Class_Annotations_RequestMapping() {
		assertThatIllegalArgumentException().isThrownBy(() -> {
			Method method = TestTemplate_Class_RequestMapping.class.getDeclaredMethod("getSpecificTest", String.class,
					String.class);
			contract.parseAndValidateMetadata(method.getDeclaringClass(), method);
		});
	}

	@Test
	void testProcessAnnotations_Class_AnnotationsGetAllTests() throws Exception {
		Method method = TestTemplate_Class_Annotations.class.getDeclaredMethod("getAllTests", String.class);
		MethodMetadata data = contract.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().url()).isEqualTo("/");
		assertThat(data.template().method()).isEqualTo("GET");

		assertThat(data.indexToName().get(0).iterator().next()).isEqualTo("classId");
		assertThat(data.template().decodeSlash()).isTrue();
	}

	@Test
	void testProcessAnnotations_ExtendedInterface() throws Exception {
		Method extendedMethod = TestTemplate_Extended.class.getMethod("getAllTests", String.class);
		MethodMetadata extendedData = contract.parseAndValidateMetadata(extendedMethod.getDeclaringClass(),
				extendedMethod);

		Method method = TestTemplate_Class_Annotations.class.getDeclaredMethod("getAllTests", String.class);
		MethodMetadata data = contract.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().url()).isEqualTo(extendedData.template().url());
		assertThat(data.template().method()).isEqualTo(extendedData.template().method());
		assertThat(data.indexToName().get(0).iterator().next()).isEqualTo(data.indexToName().get(0).iterator().next());
		assertThat(data.indexToName().get(0).iterator().next()).isEqualTo(data.indexToName().get(0).iterator().next());
		assertThat(data.template().decodeSlash()).isTrue();
	}

	@Test
	void testProcessAnnotations_SimplePost() throws Exception {
		Method method = TestTemplate_Simple.class.getDeclaredMethod("postTest", TestObject.class);
		MethodMetadata data = contract.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().url()).isEqualTo("/");
		assertThat(data.template().method()).isEqualTo("POST");
		assertThat(data.template().headers().get("Accept").iterator().next())
				.isEqualTo(MediaType.APPLICATION_JSON_VALUE);

	}

	@Test
	void testProcessAnnotations_SimplePostMapping() throws Exception {
		Method method = TestTemplate_Simple.class.getDeclaredMethod("postMappingTest", TestObject.class);
		MethodMetadata data = contract.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().url()).isEqualTo("/");
		assertThat(data.template().method()).isEqualTo("POST");
		assertThat(data.template().headers().get("Accept").iterator().next())
				.isEqualTo(MediaType.APPLICATION_JSON_VALUE);

	}

	@Test
	void testProcessAnnotationsOnMethod_Advanced() throws Exception {
		Method method = TestTemplate_Advanced.class.getDeclaredMethod("getTest", String.class, String.class,
				Integer.class);
		MethodMetadata data = contract.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().url()).isEqualTo("/test/{id}?amount=" + "{amount}");
		assertThat(data.template().method()).isEqualTo("PUT");
		assertThat(data.template().headers().get("Accept").iterator().next())
				.isEqualTo(MediaType.APPLICATION_JSON_VALUE);
	}

	@Test
	void testProcessAnnotationsOnMethod_Advanced_UnknownAnnotation() throws Exception {
		Method method = TestTemplate_Advanced.class.getDeclaredMethod("getTest", String.class, String.class,
				Integer.class);
		contract.parseAndValidateMetadata(method.getDeclaringClass(), method);

		// Don't throw an exception and this passes
	}

	@Test
	void testProcessAnnotationsOnMethod_CollectionFormat() throws NoSuchMethodException {
		Method method = TestTemplate_Advanced.class.getDeclaredMethod("getWithCollectionFormat");

		MethodMetadata data = contract.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().collectionFormat()).isEqualTo(SSV);
	}

	@Test
	void processAnnotationOnClass_CollectionFormat() throws NoSuchMethodException {
		Method method = TestTemplate_Advanced.class.getDeclaredMethod("getWithoutCollectionFormat");

		MethodMetadata data = contract.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().collectionFormat()).isEqualTo(CSV);
	}

	@Test
	void testProcessAnnotations_Advanced() throws Exception {
		Method method = TestTemplate_Advanced.class.getDeclaredMethod("getTest", String.class, String.class,
				Integer.class);
		MethodMetadata data = contract.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().url()).isEqualTo("/test/{id}?amount=" + "{amount}");
		assertThat(data.template().method()).isEqualTo("PUT");
		assertThat(data.template().headers().get("Accept").iterator().next())
				.isEqualTo(MediaType.APPLICATION_JSON_VALUE);

		assertThat(data.indexToName().get(0).iterator().next()).isEqualTo("Authorization");
		assertThat(data.indexToName().get(1).iterator().next()).isEqualTo("id");
		assertThat(data.indexToName().get(2).iterator().next()).isEqualTo("amount");
		assertThat(data.indexToExpander().get(2)).isNotNull();

		assertThat(data.template().headers().get("Authorization").iterator().next()).isEqualTo("{Authorization}");
		assertThat(data.template().queries().get("amount").iterator().next()).isEqualTo("{amount}");
	}

	@Test
	void testProcessAnnotations_Aliased() throws Exception {
		Method method = TestTemplate_Advanced.class.getDeclaredMethod("getTest2", String.class, Integer.class);
		MethodMetadata data = contract.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().url()).isEqualTo("/test2?amount=" + "{amount}");
		assertThat(data.template().method()).isEqualTo("PUT");
		assertThat(data.template().headers().get("Accept").iterator().next())
				.isEqualTo(MediaType.APPLICATION_JSON_VALUE);

		assertThat(data.indexToName().get(0).iterator().next()).isEqualTo("Authorization");
		assertThat(data.indexToName().get(1).iterator().next()).isEqualTo("amount");

		assertThat(data.template().headers().get("Authorization").iterator().next()).isEqualTo("{Authorization}");
		assertThat(data.template().queries().get("amount").iterator().next()).isEqualTo("{amount}");
	}

	@Test
	void testProcessAnnotations_DateTimeFormatParam() throws Exception {
		Method method = TestTemplate_DateTimeFormatParameter.class.getDeclaredMethod("getTest", LocalDateTime.class);
		MethodMetadata data = contract.parseAndValidateMetadata(method.getDeclaringClass(), method);

		Param.Expander expander = data.indexToExpander().get(0);
		assertThat(expander).isNotNull();

		LocalDateTime input = LocalDateTime.of(2001, 10, 12, 23, 56, 3);

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(TestTemplate_DateTimeFormatParameter.CUSTOM_PATTERN);

		String expected = formatter.format(input);

		assertThat(expander.expand(input)).isEqualTo(expected);
	}

	@Test
	void testProcessAnnotations_NumberFormatParam() throws Exception {
		Method method = TestTemplate_NumberFormatParameter.class.getDeclaredMethod("getTest", BigDecimal.class);
		MethodMetadata data = contract.parseAndValidateMetadata(method.getDeclaringClass(), method);

		Param.Expander expander = data.indexToExpander().get(0);
		assertThat(expander).isNotNull();

		NumberStyleFormatter formatter = new NumberStyleFormatter(TestTemplate_NumberFormatParameter.CUSTOM_PATTERN);

		BigDecimal input = BigDecimal.valueOf(1220.345);

		String expected = formatter.print(input, Locale.getDefault());
		String actual = expander.expand(input);

		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void testProcessAnnotations_Advanced2() throws Exception {
		Method method = TestTemplate_Advanced.class.getDeclaredMethod("getTest");
		MethodMetadata data = contract.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().url()).isEqualTo("/");
		assertThat(data.template().method()).isEqualTo("GET");
		assertThat(data.template().headers().get("Accept").iterator().next())
				.isEqualTo(MediaType.APPLICATION_JSON_VALUE);
	}

	@Test
	void testProcessAnnotations_Advanced3() throws Exception {
		Method method = TestTemplate_Simple.class.getDeclaredMethod("getTest");
		MethodMetadata data = contract.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().url()).isEqualTo("/");
		assertThat(data.template().method()).isEqualTo("GET");
		assertThat(data.template().headers().get("Accept").iterator().next())
				.isEqualTo(MediaType.APPLICATION_JSON_VALUE);
		assertThat(data.template().decodeSlash()).isTrue();
	}

	@Test
	void testProcessAnnotations_Advanced3_DecodeSlashFlagNotModified() throws Exception {
		contract = new SpringMvcContract(Collections.emptyList(), getConversionService(), false);

		Method method = TestTemplate_Simple.class.getDeclaredMethod("getTest");
		MethodMetadata data = contract.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().url()).isEqualTo("/");

		assertThat(data.template().decodeSlash()).isTrue();
	}

	@Test
	void testProcessAnnotations_ListParams() throws Exception {
		Method method = TestTemplate_ListParams.class.getDeclaredMethod("getTest", List.class);
		MethodMetadata data = contract.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().url()).isEqualTo("/test?id=" + "{id}");
		assertThat(data.template().method()).isEqualTo("GET");
		assertThat(data.template().queries().get("id").toString()).isEqualTo("[{id}]");
		assertThat(data.indexToExpander().get(0)).isNotNull();
	}

	@Test
	void testProcessAnnotations_ListParamsWithoutName() throws Exception {
		Method method = TestTemplate_ListParamsWithoutName.class.getDeclaredMethod("getTest", List.class);
		MethodMetadata data = contract.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().url()).isEqualTo("/test?id=" + "{id}");
		assertThat(data.template().method()).isEqualTo("GET");
		assertThat(data.template().queries().get("id").toString()).isEqualTo("[{id}]");
		assertThat(data.indexToExpander().get(0)).isNotNull();
	}

	@Test
	void testProcessAnnotations_MapParams() throws Exception {
		Method method = TestTemplate_MapParams.class.getDeclaredMethod("getTest", Map.class);
		MethodMetadata data = contract.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().url()).isEqualTo("/test");
		assertThat(data.template().method()).isEqualTo("GET");
		assertThat(data.queryMapIndex()).isNotNull();
		assertThat(data.queryMapIndex().intValue()).isEqualTo(0);
	}

	@Test
	void testProcessHeaders() throws Exception {
		Method method = TestTemplate_Headers.class.getDeclaredMethod("getTest", String.class);
		MethodMetadata data = contract.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().url()).isEqualTo("/test/{id}");
		assertThat(data.template().method()).isEqualTo("GET");
		assertThat(data.template().headers().get("x-Foo").iterator().next()).isEqualTo("bar");
	}

	@Test
	void testProcessHeadersWithoutValues() throws Exception {
		Method method = TestTemplate_HeadersWithoutValues.class.getDeclaredMethod("getTest", String.class);
		MethodMetadata data = contract.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().url()).isEqualTo("/test/{id}");
		assertThat(data.template().method()).isEqualTo("GET");
		assertThat(data.template().headers().isEmpty()).isTrue();
	}

	@Test
	void testProcessAnnotations_Fallback() throws Exception {
		Method method = TestTemplate_Advanced.class.getDeclaredMethod("getTestFallback", String.class, String.class,
				Integer.class);

		assumeTrue(hasJava8ParameterNames(method), "does not have java 8 parameter names");

		MethodMetadata data = contract.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().url()).isEqualTo("/testfallback/{id}?amount=" + "{amount}");
		assertThat(data.template().method()).isEqualTo("PUT");
		assertThat(data.template().headers().get("Accept").iterator().next())
				.isEqualTo(MediaType.APPLICATION_JSON_VALUE);

		assertThat(data.indexToName().get(0).iterator().next()).isEqualTo("Authorization");
		assertThat(data.indexToName().get(1).iterator().next()).isEqualTo("id");
		assertThat(data.indexToName().get(2).iterator().next()).isEqualTo("amount");

		assertThat(data.template().headers().get("Authorization").iterator().next()).isEqualTo("{Authorization}");
		assertThat(data.template().queries().get("amount").iterator().next()).isEqualTo("{amount}");
	}

	@Test
	void testProcessHeaderMap() throws Exception {
		Method method = TestTemplate_HeaderMap.class.getDeclaredMethod("headerMap", MultiValueMap.class, String.class);
		MethodMetadata data = contract.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().url()).isEqualTo("/headerMap");
		assertThat(data.template().method()).isEqualTo("GET");
		assertThat(data.headerMapIndex().intValue()).isEqualTo(0);
		Map<String, Collection<String>> headers = data.template().headers();
		assertThat(headers.get("aHeader").iterator().next()).isEqualTo("{aHeader}");
	}

	@Test
	void testProcessHeaderMapMoreThanOnce() throws Exception {
		Method method = TestTemplate_HeaderMap.class.getDeclaredMethod("headerMapMoreThanOnce", MultiValueMap.class,
				MultiValueMap.class);
		assertThatExceptionOfType(IllegalStateException.class)
				.isThrownBy(() -> contract.parseAndValidateMetadata(method.getDeclaringClass(), method));
	}

	@Test
	void testProcessQueryMap() throws Exception {
		Method method = TestTemplate_QueryMap.class.getDeclaredMethod("queryMap", MultiValueMap.class, String.class);
		MethodMetadata data = contract.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().url()).isEqualTo("/queryMap?aParam=" + "{aParam}");
		assertThat(data.template().method()).isEqualTo("GET");
		assertThat(data.queryMapIndex().intValue()).isEqualTo(0);
		Map<String, Collection<String>> params = data.template().queries();
		assertThat(params.get("aParam").iterator().next()).isEqualTo("{aParam}");
	}

	@Test
	void testProcessQueryMapObject() throws Exception {
		Method method = TestTemplate_QueryMap.class.getDeclaredMethod("queryMapObject", TestObject.class, String.class);
		MethodMetadata data = contract.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().url()).isEqualTo("/queryMapObject?aParam=" + "{aParam}");
		assertThat(data.template().method()).isEqualTo("GET");
		assertThat(data.queryMapIndex().intValue()).isEqualTo(0);
		Map<String, Collection<String>> params = data.template().queries();
		assertThat(params.get("aParam").iterator().next()).isEqualTo("{aParam}");
	}

	@Test
	void testProcessQueryMapMoreThanOnce() throws Exception {
		Method method = TestTemplate_QueryMap.class.getDeclaredMethod("queryMapMoreThanOnce", MultiValueMap.class,
				MultiValueMap.class);
		assertThatExceptionOfType(IllegalStateException.class)
				.isThrownBy(() -> contract.parseAndValidateMetadata(method.getDeclaringClass(), method));
	}

	@Test
	void testMatrixVariable_MapParam() throws Exception {
		Method method = TestTemplate_MatrixVariable.class.getDeclaredMethod("matrixVariable", Map.class);
		MethodMetadata data = contract.parseAndValidateMetadata(method.getDeclaringClass(), method);

		Map<String, String> testMap = new HashMap<>();
		testMap.put("param", "value");

		assertThat(data.template().method()).isEqualTo("GET");
		assertThat(data.template().url()).isEqualTo("/matrixVariable/{params}");
		assertThat(";param=value").isEqualTo(data.indexToExpander().get(0).expand(testMap));
	}

	@Test
	void testMatrixVariable_ObjectParam() throws Exception {
		Method method = TestTemplate_MatrixVariable.class.getDeclaredMethod("matrixVariableObject", Object.class);
		MethodMetadata data = contract.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().method()).isEqualTo("GET");
		assertThat(data.template().url()).isEqualTo("/matrixVariableObject/{param}");
		assertThat(";param=value").isEqualTo(data.indexToExpander().get(0).expand("value"));
	}

	@Test
	void testMatrixVariableWithNoName() throws NoSuchMethodException {
		Method method = TestTemplate_MatrixVariable.class.getDeclaredMethod("matrixVariableNotNamed", Map.class);
		MethodMetadata data = contract.parseAndValidateMetadata(method.getDeclaringClass(), method);
		Map<String, String> testMap = new HashMap<>();

		testMap.put("param", "value");

		assertThat(data.template().method()).isEqualTo("GET");
		assertThat(data.template().url()).isEqualTo("/matrixVariable/{params}");
		assertThat(";param=value").isEqualTo(data.indexToExpander().get(0).expand(testMap));
	}

	@Test
	void testAddingTemplatedParameterWithTheSameKey() throws NoSuchMethodException {
		Method method = TestTemplate_Advanced.class.getDeclaredMethod("testAddingTemplatedParamForExistingKey",
				String.class);
		MethodMetadata data = contract.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertThat(data.template().headers().get("Accept")).contains("application/json", "{Accept}");
	}

	@Test
	void testMultipleRequestPartAnnotations() throws NoSuchMethodException {
		Method method = TestTemplate_RequestPart.class.getDeclaredMethod("requestWithMultipleParts",
				MultipartFile.class, String.class);

		MethodMetadata data = contract.parseAndValidateMetadata(method.getDeclaringClass(), method);
		assertThat(data.formParams()).contains("file", "id");
	}

	@Test
	void testSingleCookieAnnotation() throws NoSuchMethodException {
		Method method = TestTemplate_Cookies.class.getDeclaredMethod("singleCookie", String.class, String.class);

		MethodMetadata data = contract.parseAndValidateMetadata(method.getDeclaringClass(), method);
		assertThat(data.template().headers().get("cookie").iterator().next()).isEqualTo("cookie1={cookie1}");
	}

	@Test
	void testMultipleCookiesAnnotation() throws NoSuchMethodException {
		Method method = TestTemplate_Cookies.class.getDeclaredMethod("multipleCookies", String.class, String.class,
				String.class);

		MethodMetadata data = contract.parseAndValidateMetadata(method.getDeclaringClass(), method);
		assertThat(data.template().headers().get("cookie").iterator().next())
				.isEqualTo("cookie1={cookie1}; cookie2={cookie2}");
	}

	private ConversionService getConversionService() {
		FormattingConversionServiceFactoryBean conversionServiceFactoryBean = new FormattingConversionServiceFactoryBean();
		conversionServiceFactoryBean.afterPropertiesSet();
		return conversionServiceFactoryBean.getObject();
	}

	public interface TestTemplate_Simple {

		@RequestMapping(value = "/test/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
		ResponseEntity<TestObject> getTest(@PathVariable("id") String id);

		@GetMapping("/test/{id:\\d+}")
		ResponseEntity<TestObject> getTestWithDigitalId(@PathVariable("id") String id);

		@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
		TestObject getTest();

		@GetMapping(value = "/test/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
		ResponseEntity<TestObject> getMappingTest(@PathVariable("id") String id);

		@PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
		TestObject postTest(@RequestBody TestObject object);

		@PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
		TestObject postMappingTest(@RequestBody TestObject object);

		@GetMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
		ResponseEntity<TestObject> getSlashPath(@RequestParam("id") String id);

		@GetMapping(path = "test", produces = MediaType.APPLICATION_JSON_VALUE)
		ResponseEntity<TestObject> getTestNoLeadingSlash(@RequestParam("name") String name);

	}

	@RequestMapping("/prepend/{classId}")
	public interface TestTemplate_Class_RequestMapping {

		@RequestMapping(value = "/test/{testId}", method = RequestMethod.GET)
		TestObject getSpecificTest(@PathVariable("classId") String classId, @PathVariable("testId") String testId);

	}

	public interface TestTemplate_Class_Annotations {

		@GetMapping("/test/{testId}")
		TestObject getSpecificTest(@PathVariable("classId") String classId, @PathVariable("testId") String testId);

		@RequestMapping(method = RequestMethod.GET)
		TestObject getAllTests(@PathVariable("classId") String classId);

	}

	public interface TestTemplate_Extended extends TestTemplate_Class_Annotations {

	}

	public interface TestTemplate_Headers {

		@GetMapping(value = "/test/{id}", headers = "X-Foo=bar")
		ResponseEntity<TestObject> getTest(@PathVariable("id") String id);

	}

	public interface TestTemplate_Cookies {

		@GetMapping("/test/{id}")
		ResponseEntity<TestObject> singleCookie(@PathVariable("id") String id, @CookieValue("cookie1") String cookie1);

		@GetMapping("/test/{id}")
		ResponseEntity<TestObject> multipleCookies(@PathVariable("id") String id,
				@CookieValue("cookie1") String cookie1, @CookieValue("cookie2") String cookie2);

	}

	public interface TestTemplate_HeadersWithoutValues {

		@GetMapping(value = "/test/{id}", headers = { "X-Foo", "!X-Bar", "X-Baz!=fooBar" })
		ResponseEntity<TestObject> getTest(@PathVariable("id") String id);

	}

	public interface TestTemplate_ListParams {

		@GetMapping("/test")
		ResponseEntity<TestObject> getTest(@RequestParam("id") List<String> id);

	}

	public interface TestTemplate_ListParamsWithoutName {

		@GetMapping("/test")
		ResponseEntity<TestObject> getTest(@RequestParam List<String> id);

	}

	public interface TestTemplate_MapParams {

		@GetMapping("/test")
		ResponseEntity<TestObject> getTest(@RequestParam Map<String, String> params);

	}

	public interface TestTemplate_HeaderMap {

		@GetMapping("/headerMap")
		String headerMap(@RequestHeader MultiValueMap<String, String> headerMap,
				@RequestHeader(name = "aHeader") String aHeader);

		@GetMapping("/headerMapMoreThanOnce")
		String headerMapMoreThanOnce(@RequestHeader MultiValueMap<String, String> headerMap1,
				@RequestHeader MultiValueMap<String, String> headerMap2);

	}

	public interface TestTemplate_QueryMap {

		@GetMapping("/queryMap")
		String queryMap(@RequestParam MultiValueMap<String, String> queryMap,
				@RequestParam(name = "aParam") String aParam);

		@GetMapping("/queryMapMoreThanOnce")
		String queryMapMoreThanOnce(@RequestParam MultiValueMap<String, String> queryMap1,
				@RequestParam MultiValueMap<String, String> queryMap2);

		@GetMapping("/queryMapObject")
		String queryMapObject(@SpringQueryMap TestObject queryMap, @RequestParam(name = "aParam") String aParam);

	}

	public interface TestTemplate_RequestPart {

		@PostMapping(path = "/requestPart", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
		void requestWithMultipleParts(@RequestPart("file") MultipartFile file, @RequestPart("id") String identifier);

	}

	public interface TestTemplate_MatrixVariable {

		@GetMapping("/matrixVariable/{params}")
		String matrixVariable(@MatrixVariable("params") Map<String, Object> params);

		@GetMapping("/matrixVariableObject/{param}")
		String matrixVariableObject(@MatrixVariable("param") Object object);

		@GetMapping("/matrixVariable/{params}")
		String matrixVariableNotNamed(@MatrixVariable Map<String, Object> params);

	}

	@JsonAutoDetect
	@CollectionFormat(CSV)
	public interface TestTemplate_Advanced {

		@CollectionFormat(SSV)
		@GetMapping
		ResponseEntity<TestObject> getWithCollectionFormat();

		@GetMapping
		ResponseEntity<TestObject> getWithoutCollectionFormat();

		@ExceptionHandler
		@PutMapping(path = "/test/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
		ResponseEntity<TestObject> getTest(@RequestHeader("Authorization") String auth, @PathVariable("id") String id,
				@RequestParam("amount") Integer amount);

		@PutMapping(path = "/test2", produces = MediaType.APPLICATION_JSON_VALUE)
		ResponseEntity<TestObject> getTest2(@RequestHeader(name = "Authorization") String auth,
				@RequestParam(name = "amount") Integer amount);

		@ExceptionHandler
		@PutMapping(path = "/testfallback/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
		ResponseEntity<TestObject> getTestFallback(@RequestHeader String Authorization, @PathVariable String id,
				@RequestParam Integer amount);

		@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
		TestObject getTest();

		@GetMapping(produces = "application/json")
		String testAddingTemplatedParamForExistingKey(@RequestHeader("Accept") String accept);

	}

	public interface TestTemplate_DateTimeFormatParameter {

		String CUSTOM_PATTERN = "dd-MM-yyyy HH:mm";

		@GetMapping
		String getTest(@RequestParam(name = "localDateTime") @DateTimeFormat(
				pattern = CUSTOM_PATTERN) LocalDateTime localDateTime);

	}

	public interface TestTemplate_NumberFormatParameter {

		String CUSTOM_PATTERN = "$###,###.###";

		@GetMapping
		String getTest(@RequestParam("amount") @NumberFormat(pattern = CUSTOM_PATTERN) BigDecimal amount);

	}

	@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
	public static class TestObject {

		public String something;

		public Double number;

		TestObject() {
		}

		TestObject(String something, Double number) {
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

			if (!Objects.equals(number, that.number)) {
				return false;
			}
			if (!Objects.equals(something, that.something)) {
				return false;
			}

			return true;
		}

		@Override
		public int hashCode() {
			int result = (something != null ? something.hashCode() : 0);
			result = 31 * result + (number != null ? number.hashCode() : 0);
			return result;
		}

		@Override
		public String toString() {
			return new StringBuilder("TestObject{").append("something='").append(something).append("', ")
					.append("number=").append(number).append("}").toString();
		}

	}

}
