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

package org.springframework.cloud.openfeign.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;

import feign.MethodMetadata;

import org.springframework.cloud.openfeign.AnnotatedParameterProcessor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.CookieValue;

import static feign.Util.checkState;
import static feign.Util.emptyToNull;

/**
 * {@link CookieValue} annotation processor.
 *
 * @author Gong Yi
 * @author Olga Maciaszek-Sharma
 *
 */
public class CookieValueParameterProcessor implements AnnotatedParameterProcessor {

	private static final Class<CookieValue> ANNOTATION = CookieValue.class;

	@Override
	public Class<? extends Annotation> getAnnotationType() {
		return ANNOTATION;
	}

	@Override
	public boolean processArgument(AnnotatedParameterContext context, Annotation annotation, Method method) {
		int parameterIndex = context.getParameterIndex();
		MethodMetadata data = context.getMethodMetadata();
		CookieValue cookie = ANNOTATION.cast(annotation);
		String name = cookie.value().trim();
		checkState(emptyToNull(name) != null, "Cookie.name() was empty on parameter %s", parameterIndex);
		context.setParameterName(name);
		String cookieExpression = data.template().headers()
				.getOrDefault(HttpHeaders.COOKIE, Collections.singletonList("")).stream().findFirst().orElse("");
		if (cookieExpression.length() == 0) {
			cookieExpression = String.format("%s={%s}", name, name);
		}
		else {
			cookieExpression += String.format("; %s={%s}", name, name);
		}
		data.template().removeHeader(HttpHeaders.COOKIE);
		data.template().header(HttpHeaders.COOKIE, cookieExpression);
		return true;
	}

}
