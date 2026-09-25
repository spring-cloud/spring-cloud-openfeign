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
import java.util.regex.Pattern;

import feign.MethodMetadata;

import org.springframework.cloud.openfeign.AnnotatedParameterProcessor;
import org.springframework.web.bind.annotation.PathVariable;

import static feign.Util.checkState;
import static feign.Util.emptyToNull;

/**
 * {@link PathVariable} parameter processor.
 *
 * @author Jakub Narloch
 * @author Abhijit Sarkar
 * @author Yanming Zhou
 * @see AnnotatedParameterProcessor
 */
public class PathVariableParameterProcessor implements AnnotatedParameterProcessor {

	private static final Class<PathVariable> ANNOTATION = PathVariable.class;

	@Override
	public Class<? extends Annotation> getAnnotationType() {
		return ANNOTATION;
	}

	@Override
	public boolean processArgument(AnnotatedParameterContext context, Annotation annotation, Method method) {
		String name = ANNOTATION.cast(annotation).value();
		checkState(emptyToNull(name) != null, "PathVariable annotation was empty on param %s.",
				context.getParameterIndex());
		context.setParameterName(name);

		MethodMetadata data = context.getMethodMetadata();
		Pattern varNamePattern = Pattern.compile("\\{" + Pattern.quote(name) + "(:[^}]+)?\\}");
		if (!varNamePattern.matcher(data.template().url()).find()
				&& !matchesMapValues(data.template().queries(), varNamePattern)
				&& !matchesMapValues(data.template().headers(), varNamePattern)) {
			data.formParams().add(name);
		}
		return true;
	}

	private boolean matchesMapValues(Map<String, Collection<String>> map, Pattern pattern) {
		for (Collection<String> entry : map.values()) {
			for (String value : entry) {
				if (pattern.matcher(value).find()) {
					return true;
				}
			}
		}
		return false;
	}

}
