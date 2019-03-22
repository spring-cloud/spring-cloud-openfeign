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

import org.springframework.cloud.openfeign.AnnotatedParameterProcessor;
import org.springframework.cloud.openfeign.SpringQueryMap;

/**
 * {@link SpringQueryMap} parameter processor.
 *
 * @author Aram Peres
 * @see AnnotatedParameterProcessor
 */
public class QueryMapParameterProcessor implements AnnotatedParameterProcessor {

	private static final Class<SpringQueryMap> ANNOTATION = SpringQueryMap.class;

	@Override
	public Class<? extends Annotation> getAnnotationType() {
		return ANNOTATION;
	}

	@Override
	public boolean processArgument(AnnotatedParameterContext context,
			Annotation annotation, Method method) {
		int paramIndex = context.getParameterIndex();
		MethodMetadata metadata = context.getMethodMetadata();
		if (metadata.queryMapIndex() == null) {
			metadata.queryMapIndex(paramIndex);
			metadata.queryMapEncoded(SpringQueryMap.class.cast(annotation).encoded());
		}
		return true;
	}

}
