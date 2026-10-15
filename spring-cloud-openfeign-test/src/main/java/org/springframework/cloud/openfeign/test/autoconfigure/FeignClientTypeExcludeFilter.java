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

package org.springframework.cloud.openfeign.test.autoconfigure;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import feign.Capability;
import feign.Client;
import feign.Contract;
import feign.Logger;
import feign.QueryMapEncoder;
import feign.RequestInterceptor;
import feign.ResponseInterceptor;
import feign.Retryer;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;

import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.http.converter.autoconfigure.ClientHttpMessageConvertersCustomizer;
import org.springframework.boot.test.context.filter.annotation.StandardAnnotationCustomizableTypeExcludeFilter;
import org.springframework.cloud.openfeign.AnnotatedParameterProcessor;
import org.springframework.cloud.openfeign.CircuitBreakerNameResolver;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import org.springframework.cloud.openfeign.FeignFormatterRegistrar;
import org.springframework.cloud.openfeign.FeignLoggerFactory;
import org.springframework.cloud.openfeign.clientconfig.FeignClientConfigurer;
import org.springframework.cloud.openfeign.support.HttpMessageConverterCustomizer;
import org.springframework.util.ClassUtils;

/**
 * {@link TypeExcludeFilter} for {@link FeignClientTest @FeignClientTest}.
 *
 * @author Sharang Gupta
 */
class FeignClientTypeExcludeFilter extends StandardAnnotationCustomizableTypeExcludeFilter<FeignClientTest> {

	private static final String[] OPTIONAL_INCLUDES = { "tools.jackson.databind.JacksonModule",
			"org.springframework.boot.jackson.JacksonComponent", "com.fasterxml.jackson.databind.Module",
			"org.springframework.boot.jackson2.JsonComponent" };

	private static final Set<Class<?>> KNOWN_INCLUDES;

	static {
		Set<Class<?>> includes = new LinkedHashSet<>();
		includes.add(EnableFeignClients.class);
		includes.add(RequestInterceptor.class);
		includes.add(ResponseInterceptor.class);
		includes.add(Encoder.class);
		includes.add(Decoder.class);
		includes.add(ErrorDecoder.class);
		includes.add(Contract.class);
		includes.add(Retryer.class);
		includes.add(Logger.class);
		includes.add(Capability.class);
		includes.add(QueryMapEncoder.class);
		includes.add(Client.class);
		includes.add(FeignBuilderCustomizer.class);
		includes.add(FeignFormatterRegistrar.class);
		includes.add(FeignLoggerFactory.class);
		includes.add(AnnotatedParameterProcessor.class);
		includes.add(FeignClientConfigurer.class);
		includes.add(FallbackFactory.class);
		includes.add(CircuitBreakerNameResolver.class);
		includes.add(HttpMessageConverterCustomizer.class);
		includes.add(ClientHttpMessageConvertersCustomizer.class);
		for (String optionalInclude : OPTIONAL_INCLUDES) {
			try {
				includes.add(ClassUtils.forName(optionalInclude, null));
			}
			catch (Exception ex) {
				// Ignore
			}
		}
		KNOWN_INCLUDES = Collections.unmodifiableSet(includes);
	}

	FeignClientTypeExcludeFilter(Class<?> testClass) {
		super(testClass);
	}

	@Override
	protected Set<Class<?>> getKnownIncludes() {
		return KNOWN_INCLUDES;
	}

}
