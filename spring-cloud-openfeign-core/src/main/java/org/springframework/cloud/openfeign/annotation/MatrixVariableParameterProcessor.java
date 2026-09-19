/*
 * Copyright 2013-present the original author or authors.
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

package org.springframework.cloud.openfeign.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import feign.MethodMetadata;

import org.springframework.cloud.openfeign.AnnotatedParameterProcessor;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.MatrixVariable;

import static feign.Util.checkState;
import static feign.Util.emptyToNull;

/**
 * {@link MatrixVariable} annotation processor.
 *
 * Can expand maps or single objects. A {@link Map} typed variable is expanded by Feign
 * itself through a path-style URI template expression. For any other type, a value that
 * is a {@link Collection} or an array is joined with {@code ,}, which is the separator a
 * matrix variable uses for repeated values, and nested collections and arrays are
 * flattened the same way; any other value is assigned from its {@code toString()} method.
 *
 * @author Matt King
 * @see AnnotatedParameterProcessor
 */
public class MatrixVariableParameterProcessor implements AnnotatedParameterProcessor {

	private static final Class<MatrixVariable> ANNOTATION = MatrixVariable.class;

	@Override
	public Class<? extends Annotation> getAnnotationType() {
		return ANNOTATION;
	}

	@Override
	public boolean processArgument(AnnotatedParameterContext context, Annotation annotation, Method method) {
		int parameterIndex = context.getParameterIndex();
		Class<?> parameterType = method.getParameterTypes()[parameterIndex];
		MethodMetadata data = context.getMethodMetadata();
		String name = ANNOTATION.cast(annotation).value();

		checkState(emptyToNull(name) != null, "MatrixVariable annotation was empty on param %s.",
				context.getParameterIndex());

		context.setParameterName(name);

		if (Map.class.isAssignableFrom(parameterType)) {
			pathStyleTemplateVariable(data, name);
		}
		else {
			data.indexToExpander().put(parameterIndex, this::expandValue);
			prefixTemplateVariable(data, name);
		}

		return true;
	}

	/**
	 * Moves the {@code ;name=} prefix of the matrix variable out of the expanded value
	 * and into the URI template, so that it stays a literal. Feign always pct-encodes the
	 * values it substitutes into a URI template, which would turn the separators into
	 * {@code %3B} and {@code %3D} and stop the server from reading the segment as matrix
	 * variables.
	 */
	private void prefixTemplateVariable(MethodMetadata data, String name) {
		String uri = data.template().url();
		String variable = "{" + name + "}";

		if (uri.contains(variable)) {
			data.template().uri(uri.replace(variable, ";" + name + "=" + variable));
		}
	}

	/**
	 * Turns the URI template variable of a {@link Map} typed matrix variable into a
	 * path-style expression, so that Feign expands the map into {@code ;key=value} pairs
	 * itself and encodes only the keys and the values. Writing the pairs in an expander
	 * instead would have Feign pct-encode the separators along with them.
	 */
	private void pathStyleTemplateVariable(MethodMetadata data, String name) {
		String uri = data.template().url();
		String variable = "{" + name + "}";

		if (uri.contains(variable)) {
			data.template().uri(uri.replace(variable, "{;" + name + "}"));
		}
	}

	private String expandValue(Object value) {
		if (value.getClass().isArray()) {
			return expandValue(CollectionUtils.arrayToList(value));
		}

		if (value instanceof Collection<?> values) {
			return StringUtils.collectionToCommaDelimitedString(
					values.stream().filter(Objects::nonNull).map(this::expandValue).toList());
		}

		return value.toString();
	}

}
