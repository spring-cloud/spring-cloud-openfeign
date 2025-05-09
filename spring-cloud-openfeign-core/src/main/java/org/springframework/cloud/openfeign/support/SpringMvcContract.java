/*
 * Copyright 2013-2025 the original author or authors.
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import feign.Contract;
import feign.Feign;
import feign.MethodMetadata;
import feign.Param;
import feign.QueryMap;
import feign.Request;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.openfeign.AnnotatedParameterProcessor;
import org.springframework.cloud.openfeign.CollectionFormat;
import org.springframework.cloud.openfeign.FeignClientProperties;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.cloud.openfeign.annotation.CookieValueParameterProcessor;
import org.springframework.cloud.openfeign.annotation.MatrixVariableParameterProcessor;
import org.springframework.cloud.openfeign.annotation.PathVariableParameterProcessor;
import org.springframework.cloud.openfeign.annotation.QueryMapParameterProcessor;
import org.springframework.cloud.openfeign.annotation.RequestHeaderParameterProcessor;
import org.springframework.cloud.openfeign.annotation.RequestParamParameterProcessor;
import org.springframework.cloud.openfeign.annotation.RequestPartParameterProcessor;
import org.springframework.cloud.openfeign.encoding.HttpEncoding;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.domain.Pageable;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import static feign.Util.checkState;
import static feign.Util.emptyToNull;
import static org.springframework.cloud.openfeign.support.FeignUtils.addTemplateParameter;
import static org.springframework.core.annotation.AnnotatedElementUtils.findMergedAnnotation;

/**
 * @author Spencer Gibb
 * @author Abhijit Sarkar
 * @author Halvdan Hoem Grelland
 * @author Aram Peres
 * @author Olga Maciaszek-Sharma
 * @author Aaron Whiteside
 * @author Artyom Romanenko
 * @author Darren Foong
 * @author Ram Anaswara
 * @author Sam Kruglov
 * @author Tang Xiong
 * @author Juhyeong An
 */
public class SpringMvcContract extends Contract.BaseContract implements ResourceLoaderAware {

	private static final Log LOG = LogFactory.getLog(SpringMvcContract.class);

	private static final String ACCEPT = "Accept";

	private static final String CONTENT_TYPE = "Content-Type";

	private static final TypeDescriptor STRING_TYPE_DESCRIPTOR = TypeDescriptor.valueOf(String.class);

	private static final TypeDescriptor ITERABLE_TYPE_DESCRIPTOR = TypeDescriptor.valueOf(Iterable.class);

	private static final ParameterNameDiscoverer PARAMETER_NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

	private final Map<Class<? extends Annotation>, AnnotatedParameterProcessor> annotatedArgumentProcessors;

	private final Map<String, Method> processedMethods = new HashMap<>();

	private final ConversionService conversionService;

	private final ConvertingExpanderFactory convertingExpanderFactory;

	private ResourceLoader resourceLoader = new DefaultResourceLoader();

	private final boolean decodeSlash;

	private final boolean removeTrailingSlash;

	public SpringMvcContract() {
		this(Collections.emptyList());
	}

	public SpringMvcContract(List<AnnotatedParameterProcessor> annotatedParameterProcessors) {
		this(annotatedParameterProcessors, new DefaultConversionService());
	}

	public SpringMvcContract(List<AnnotatedParameterProcessor> annotatedParameterProcessors,
			ConversionService conversionService) {
		this(annotatedParameterProcessors, conversionService, true);
	}

	/**
	 * Creates a {@link SpringMvcContract} based on annotatedParameterProcessors,
	 * conversionService and decodeSlash value.
	 * @param annotatedParameterProcessors list of {@link AnnotatedParameterProcessor}
	 * objects used to resolve parameters
	 * @param conversionService {@link ConversionService} used for type conversion
	 * @param decodeSlash indicates whether slashes should be decoded
	 * @deprecated in favour of
	 * {@link SpringMvcContract#SpringMvcContract(List, ConversionService, FeignClientProperties)}
	 */
	@Deprecated(forRemoval = true)
	public SpringMvcContract(List<AnnotatedParameterProcessor> annotatedParameterProcessors,
			ConversionService conversionService, boolean decodeSlash) {
		this(annotatedParameterProcessors, conversionService, decodeSlash, false);
	}

	/**
	 * Creates a {@link SpringMvcContract} based on annotatedParameterProcessors,
	 * conversionService and decodeSlash value.
	 * @param annotatedParameterProcessors list of {@link AnnotatedParameterProcessor}
	 * objects used to resolve parameters
	 * @param conversionService {@link ConversionService} used for type conversion
	 * @param decodeSlash indicates whether slashes should be decoded
	 * @param removeTrailingSlash indicates whether trailing slashes should be removed
	 * @deprecated in favour of
	 * {@link SpringMvcContract#SpringMvcContract(List, ConversionService, FeignClientProperties)}
	 */
	@Deprecated(forRemoval = true)
	public SpringMvcContract(List<AnnotatedParameterProcessor> annotatedParameterProcessors,
			ConversionService conversionService, boolean decodeSlash, boolean removeTrailingSlash) {
		Assert.notNull(annotatedParameterProcessors, "Parameter processors can not be null.");
		Assert.notNull(conversionService, "ConversionService can not be null.");

		List<AnnotatedParameterProcessor> processors = getDefaultAnnotatedArgumentsProcessors();
		processors.addAll(annotatedParameterProcessors);

		annotatedArgumentProcessors = toAnnotatedArgumentProcessorMap(processors);
		this.conversionService = conversionService;
		convertingExpanderFactory = new ConvertingExpanderFactory(conversionService);
		this.decodeSlash = decodeSlash;
		this.removeTrailingSlash = removeTrailingSlash;
	}

	public SpringMvcContract(List<AnnotatedParameterProcessor> annotatedParameterProcessors,
			ConversionService conversionService, FeignClientProperties feignClientProperties) {
		this(annotatedParameterProcessors, conversionService,
				feignClientProperties == null || feignClientProperties.isDecodeSlash(),
				feignClientProperties != null && feignClientProperties.isRemoveTrailingSlash());
	}

	private static TypeDescriptor createTypeDescriptor(Method method, int paramIndex) {
		Parameter parameter = method.getParameters()[paramIndex];
		MethodParameter methodParameter = MethodParameter.forParameter(parameter);
		TypeDescriptor typeDescriptor = new TypeDescriptor(methodParameter);

		// Feign applies the Param.Expander to each element of an Iterable, so in those
		// cases we need to provide a TypeDescriptor of the element.
		if (typeDescriptor.isAssignableTo(ITERABLE_TYPE_DESCRIPTOR)) {
			TypeDescriptor elementTypeDescriptor = getElementTypeDescriptor(typeDescriptor);

			checkState(elementTypeDescriptor != null,
					"Could not resolve element type of Iterable type %s. Not declared?", typeDescriptor);

			typeDescriptor = elementTypeDescriptor;
		}
		return typeDescriptor;
	}

	private static TypeDescriptor getElementTypeDescriptor(TypeDescriptor typeDescriptor) {
		TypeDescriptor elementTypeDescriptor = typeDescriptor.getElementTypeDescriptor();
		// that means it's not a collection, but it is iterable, gh-135
		if (elementTypeDescriptor == null && Iterable.class.isAssignableFrom(typeDescriptor.getType())) {
			ResolvableType type = typeDescriptor.getResolvableType().as(Iterable.class).getGeneric(0);
			if (type.resolve() == null) {
				return null;
			}
			return new TypeDescriptor(type, null, typeDescriptor.getAnnotations());
		}
		return elementTypeDescriptor;
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Override
	protected void processAnnotationOnClass(MethodMetadata data, Class<?> clz) {
		RequestMapping classAnnotation = findMergedAnnotation(clz, RequestMapping.class);
		if (classAnnotation != null) {
			LOG.error("Cannot process class: " + clz.getName()
					+ ". @RequestMapping annotation is not allowed on @FeignClient interfaces.");
			throw new IllegalArgumentException("@RequestMapping annotation not allowed on @FeignClient interfaces");
		}
		CollectionFormat collectionFormat = findMergedAnnotation(clz, CollectionFormat.class);
		if (collectionFormat != null) {
			data.template().collectionFormat(collectionFormat.value());
		}
	}

	@Override
	public MethodMetadata parseAndValidateMetadata(Class<?> targetType, Method method) {
		processedMethods.put(Feign.configKey(targetType, method), method);
		MethodMetadata metadata = super.parseAndValidateMetadata(targetType, method);

		if (isGetMethod(metadata) && method.getParameterCount() > 0 && !hasHttpAnnotations(method)) {
			LOG.warn(String.format(
				"[OpenFeign Warning] Feign method '%s' is declared as GET with parameters, but none of the parameters are annotated " +
				"(e.g. @RequestParam, @RequestHeader, @PathVariable, etc). This may result in fallback to POST at runtime. " +
				"Consider explicitly annotating parameters.",
				method.toGenericString()
			));
		}

		return metadata;
	}

	private boolean isGetMethod(MethodMetadata metadata) {
		return "GET".equalsIgnoreCase(metadata.template().method());
	}

	private boolean hasHttpAnnotations(Method method) {
		for (Parameter parameter : method.getParameters()) {
			for (Annotation annotation : parameter.getAnnotations()) {
				Class<? extends Annotation> annotationType = annotation.annotationType();
				if (annotationType == RequestParam.class ||
					annotationType == RequestHeader.class ||
					annotationType == PathVariable.class ||
					annotationType == SpringQueryMap.class ||
					annotationType == QueryMap.class) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	protected void processAnnotationOnMethod(MethodMetadata data, Annotation methodAnnotation, Method method) {
		if (methodAnnotation instanceof CollectionFormat) {
			CollectionFormat collectionFormat = findMergedAnnotation(method, CollectionFormat.class);
			data.template().collectionFormat(collectionFormat.value());
		}

		if (!(methodAnnotation instanceof RequestMapping)
				&& !methodAnnotation.annotationType().isAnnotationPresent(RequestMapping.class)) {
			return;
		}

		RequestMapping methodMapping = findMergedAnnotation(method, RequestMapping.class);
		// HTTP Method
		RequestMethod[] methods = methodMapping.method();
		if (methods.length == 0) {
			methods = new RequestMethod[] { RequestMethod.GET };
		}
		checkOne(method, methods, "method");
		data.template().method(Request.HttpMethod.valueOf(methods[0].name()));

		// path
		checkAtMostOne(method, methodMapping.value(), "value");
		if (methodMapping.value().length > 0) {
			String pathValue = emptyToNull(methodMapping.value()[0]);
			if (pathValue != null) {
				pathValue = resolve(pathValue);
				// Append path from @RequestMapping if value is present on method
				if (!pathValue.startsWith("/") && !data.template().path().endsWith("/")) {
					pathValue = "/" + pathValue;
				}
				if (removeTrailingSlash && pathValue.endsWith("/")) {
					pathValue = pathValue.substring(0, pathValue.length() - 1);
				}
				data.template().uri(pathValue, true);
				if (data.template().decodeSlash() != decodeSlash) {
					data.template().decodeSlash(decodeSlash);
				}
			}
		}

		// produces
		parseProduces(data, methodMapping);

		// consumes
		parseConsumes(data, methodMapping);

		// headers
		parseHeaders(data, methodMapping);

		// params
		parseParams(data, methodMapping);

		data.indexToExpander(new LinkedHashMap<>());
	}

	private String resolve(String value) {
		if (StringUtils.hasText(value) && resourceLoader instanceof ConfigurableApplicationContext) {
			return ((ConfigurableApplicationContext) resourceLoader).getEnvironment().resolvePlaceholders(value);
		}
		return value;
	}

	private void checkAtMostOne(Method method, Object[] values, String fieldName) {
		checkState(values != null && (values.length == 0 || values.length == 1),
				"Method %s can only contain at most 1 %s field. Found: %s", method.getName(), fieldName,
				values == null ? null : Arrays.asList(values));
	}

	private void checkOne(Method method, Object[] values, String fieldName) {
		checkState(values != null && values.length == 1, "Method %s can only contain 1 %s field. Found: %s",
				method.getName(), fieldName, values == null ? null : Arrays.asList(values));
	}

	@Override
	protected boolean processAnnotationsOnParameter(MethodMetadata data, Annotation[] annotations, int paramIndex) {
		boolean isHttpAnnotation = false;

		try {
			if (Pageable.class.isAssignableFrom(data.method().getParameterTypes()[paramIndex])) {
				// do not set a Pageable as QueryMap if there's an actual QueryMap param
				// present
				if (!queryMapParamPresent(data)) {
					data.queryMapIndex(paramIndex);
					return false;
				}
			}
		}
		catch (NoClassDefFoundError ignored) {
			// Do nothing; added to avoid exceptions if optional dependency not present
		}

		AnnotatedParameterProcessor.AnnotatedParameterContext context = new SimpleAnnotatedParameterContext(data,
				paramIndex);
		Method method = processedMethods.get(data.configKey());
		for (Annotation parameterAnnotation : annotations) {
			AnnotatedParameterProcessor processor = annotatedArgumentProcessors
				.get(parameterAnnotation.annotationType());
			if (processor != null) {
				Annotation processParameterAnnotation;
				// synthesize, handling @AliasFor, while falling back to parameter name on
				// missing String #value():
				processParameterAnnotation = synthesizeWithMethodParameterNameAsFallbackValue(parameterAnnotation,
						method, paramIndex);
				isHttpAnnotation |= processor.processArgument(context, processParameterAnnotation, method);
			}
		}

		if (!isMultipartFormData(data) && isHttpAnnotation && data.indexToExpander().get(paramIndex) == null) {
			TypeDescriptor typeDescriptor = createTypeDescriptor(method, paramIndex);
			if (conversionService.canConvert(typeDescriptor, STRING_TYPE_DESCRIPTOR)) {
				Param.Expander expander = convertingExpanderFactory.getExpander(typeDescriptor);
				if (expander != null) {
					data.indexToExpander().put(paramIndex, expander);
				}
			}
		}
		return isHttpAnnotation;
	}

	private boolean queryMapParamPresent(MethodMetadata data) {
		Annotation[][] paramsAnnotations = data.method().getParameterAnnotations();
		for (int i = 0; i < paramsAnnotations.length; i++) {
			Annotation[] paramAnnotations = paramsAnnotations[i];
			Class<?> parameterType = data.method().getParameterTypes()[i];
			if (Arrays.stream(paramAnnotations)
				.anyMatch(annotation -> Map.class.isAssignableFrom(parameterType) && annotation instanceof RequestParam
						|| annotation instanceof SpringQueryMap || annotation instanceof QueryMap)) {
				return true;
			}
		}
		return false;
	}

	private void parseProduces(MethodMetadata md, RequestMapping annotation) {
		String[] serverProduces = annotation.produces();
		String clientAccepts = serverProduces.length == 0 ? null : emptyToNull(serverProduces[0]);
		if (clientAccepts != null) {
			md.template().header(ACCEPT, clientAccepts);
		}
	}

	private void parseConsumes(MethodMetadata md, RequestMapping annotation) {
		String[] serverConsumes = annotation.consumes();
		String clientProduces = serverConsumes.length == 0 ? null : emptyToNull(serverConsumes[0]);
		if (clientProduces != null) {
			md.template().header(CONTENT_TYPE, clientProduces);
		}
	}

	private void parseHeaders(MethodMetadata md, RequestMapping annotation) {
		// TODO: only supports one header value per key
		if (annotation.headers() != null) {
			for (String header : annotation.headers()) {
				int index = header.indexOf('=');
				if (!header.contains("!=") && index >= 0) {
					md.template()
						.header(resolve(header.substring(0, index)), resolve(header.substring(index + 1).trim()));
				}
			}
		}
	}

	private void parseParams(MethodMetadata data, RequestMapping methodMapping) {
		String[] params = methodMapping.params();
		if (params == null) {
			return;
		}
		for (String param : params) {
			NameValueResolver nameValueResolver = new NameValueResolver(param);
			if (!nameValueResolver.isNegated()) {
				data.template().query(resolve(nameValueResolver.getName()), resolve(nameValueResolver.getValue()));
			}
			else {
				if (LOG.isDebugEnabled()) {
					LOG.debug("Negated params are not supported by Feign and ignored during parameter processing: "
							+ param);
				}
			}
		}
	}

	private Map<Class<? extends Annotation>, AnnotatedParameterProcessor> toAnnotatedArgumentProcessorMap(
			List<AnnotatedParameterProcessor> processors) {
		Map<Class<? extends Annotation>, AnnotatedParameterProcessor> result = new HashMap<>();
		for (AnnotatedParameterProcessor processor : processors) {
			result.put(processor.getAnnotationType(), processor);
		}
		return result;
	}

	private List<AnnotatedParameterProcessor> getDefaultAnnotatedArgumentsProcessors() {

		List<AnnotatedParameterProcessor> annotatedArgumentResolvers = new ArrayList<>();

		annotatedArgumentResolvers.add(new MatrixVariableParameterProcessor());
		annotatedArgumentResolvers.add(new PathVariableParameterProcessor());
		annotatedArgumentResolvers.add(new RequestParamParameterProcessor());
		annotatedArgumentResolvers.add(new RequestHeaderParameterProcessor());
		annotatedArgumentResolvers.add(new QueryMapParameterProcessor());
		annotatedArgumentResolvers.add(new RequestPartParameterProcessor());
		annotatedArgumentResolvers.add(new CookieValueParameterProcessor());

		return annotatedArgumentResolvers;
	}

	private Annotation synthesizeWithMethodParameterNameAsFallbackValue(Annotation parameterAnnotation, Method method,
			int parameterIndex) {
		Map<String, Object> annotationAttributes = AnnotationUtils.getAnnotationAttributes(parameterAnnotation);
		Object defaultValue = AnnotationUtils.getDefaultValue(parameterAnnotation);
		if (defaultValue instanceof String && defaultValue.equals(annotationAttributes.get(AnnotationUtils.VALUE))) {
			Type[] parameterTypes = method.getGenericParameterTypes();
			String[] parameterNames = PARAMETER_NAME_DISCOVERER.getParameterNames(method);
			if (shouldAddParameterName(parameterIndex, parameterTypes, parameterNames)) {
				annotationAttributes.put(AnnotationUtils.VALUE, parameterNames[parameterIndex]);
			}
		}
		return AnnotationUtils.synthesizeAnnotation(annotationAttributes, parameterAnnotation.annotationType(), null);
	}

	private boolean shouldAddParameterName(int parameterIndex, Type[] parameterTypes, String[] parameterNames) {
		// has a parameter name
		return parameterNames != null && parameterNames.length > parameterIndex
		// has a type
				&& parameterTypes != null && parameterTypes.length > parameterIndex;
	}

	private boolean isMultipartFormData(MethodMetadata data) {
		Collection<String> contentTypes = data.template().headers().get(HttpEncoding.CONTENT_TYPE);

		if (contentTypes != null && !contentTypes.isEmpty()) {
			String type = contentTypes.iterator().next();
			try {
				return Objects.equals(MediaType.valueOf(type), MediaType.MULTIPART_FORM_DATA);
			}
			catch (InvalidMediaTypeException ignored) {
				return false;
			}
		}

		return false;
	}

	private record ConvertingExpanderFactory(ConversionService conversionService) {

		Param.Expander getExpander(TypeDescriptor typeDescriptor) {
			return value -> {
				Object converted = conversionService.convert(value, typeDescriptor, STRING_TYPE_DESCRIPTOR);
				return (String) converted;
			};
		}

	}

	private class SimpleAnnotatedParameterContext implements AnnotatedParameterProcessor.AnnotatedParameterContext {

		private final MethodMetadata methodMetadata;

		private final int parameterIndex;

		SimpleAnnotatedParameterContext(MethodMetadata methodMetadata, int parameterIndex) {
			this.methodMetadata = methodMetadata;
			this.parameterIndex = parameterIndex;
		}

		@Override
		public MethodMetadata getMethodMetadata() {
			return methodMetadata;
		}

		@Override
		public int getParameterIndex() {
			return parameterIndex;
		}

		@Override
		public void setParameterName(String name) {
			nameParam(methodMetadata, name, parameterIndex);
		}

		@Override
		public Collection<String> setTemplateParameter(String name, Collection<String> rest) {
			return addTemplateParameter(rest, name);
		}

	}

	private static class NameValueResolver {

		private final String name;

		private final String value;

		private final boolean isNegated;

		NameValueResolver(String expression) {
			int separator = expression.indexOf('=');
			if (separator == -1) {
				isNegated = expression.startsWith("!");
				name = (isNegated ? expression.substring(1) : expression);
				value = null;
			}
			else {
				isNegated = (separator > 0) && (expression.charAt(separator - 1) == '!');
				name = (isNegated ? expression.substring(0, separator - 1) : expression.substring(0, separator));
				value = expression.substring(separator + 1);
			}
		}

		public String getName() {
			return name;
		}

		public String getValue() {
			return value;
		}

		public boolean isNegated() {
			return isNegated;
		}

	}

}
