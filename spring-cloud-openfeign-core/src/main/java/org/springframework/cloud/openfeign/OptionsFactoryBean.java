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

package org.springframework.cloud.openfeign;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import feign.Request;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * This factory bean is meant to create {@link Request.Options} instance as per the
 * applicable configurations.
 *
 * @author Jasbir Singh
 */
public class OptionsFactoryBean implements FactoryBean<Request.Options>, ApplicationContextAware {

	private ApplicationContext applicationContext;

	private String contextId;

	private Request.Options options;

	@Override
	public Class<?> getObjectType() {
		return Request.Options.class;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public Request.Options getObject() {
		if (options != null) {
			return options;
		}

		options = new Request.Options();
		FeignClientProperties properties = applicationContext.getBean(FeignClientProperties.class);
		options = createOptionsWithApplicableValues(properties.getConfig().get(properties.getDefaultConfig()), options);
		options = createOptionsWithApplicableValues(properties.getConfig().get(contextId), options);
		return options;
	}

	public void setContextId(String contextId) {
		this.contextId = contextId;
	}

	private Request.Options createOptionsWithApplicableValues(
			FeignClientProperties.FeignClientConfiguration clientConfiguration, Request.Options options) {
		if (Objects.isNull(clientConfiguration)) {
			return options;
		}

		int connectTimeoutMillis = Objects.nonNull(clientConfiguration.getConnectTimeout())
				? clientConfiguration.getConnectTimeout() : options.connectTimeoutMillis();
		int readTimeoutMillis = Objects.nonNull(clientConfiguration.getReadTimeout())
				? clientConfiguration.getReadTimeout() : options.readTimeoutMillis();
		boolean followRedirects = Objects.nonNull(clientConfiguration.isFollowRedirects())
				? clientConfiguration.isFollowRedirects() : options.isFollowRedirects();
		return new Request.Options(connectTimeoutMillis, TimeUnit.MILLISECONDS, readTimeoutMillis,
				TimeUnit.MILLISECONDS, followRedirects);
	}

}
