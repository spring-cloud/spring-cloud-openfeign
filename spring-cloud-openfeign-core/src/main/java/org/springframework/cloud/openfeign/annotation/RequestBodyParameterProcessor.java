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

package org.springframework.cloud.openfeign.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import feign.MethodMetadata;
import feign.Request;

import org.springframework.cloud.openfeign.AnnotatedParameterProcessor;
import org.springframework.web.bind.annotation.RequestBody;

import static feign.Util.checkState;
import static org.springframework.cloud.openfeign.support.SpringEncoder.OPTIONAL_REQUEST_BODY;

/**
 * {@Link RequestBody} annotation processor.
 *
 * @author Matt King
 * @see AnnotatedParameterProcessor
 */
public class RequestBodyParameterProcessor implements AnnotatedParameterProcessor {

	private static final Class<RequestBody> ANNOTATION = RequestBody.class;

	@Override
	public Class<? extends Annotation> getAnnotationType() {
		return ANNOTATION;
	}

	@Override
	public boolean processArgument(AnnotatedParameterContext context,
			Annotation annotation, Method method) {
		int parameterIndex = context.getParameterIndex();
		MethodMetadata data = context.getMethodMetadata();
		boolean required = ANNOTATION.cast(annotation).required();

		checkState(data.bodyIndex() == null, "Only one request body is allowed.");

		data.bodyIndex(parameterIndex);

		if (!required) {
			data.template().body(Request.Body.bodyTemplate(OPTIONAL_REQUEST_BODY, null));
		}

		return true;
	}

}
