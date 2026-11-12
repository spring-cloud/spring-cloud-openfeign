/*
 * Copyright 2016-present the original author or authors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import feign.Logger;
import feign.slf4j.Slf4jLogger;

import org.springframework.core.env.Environment;

/**
 * @author Venil Noronha
 * @author Olga Maciaszek-Sharma
 */
public class DefaultFeignLoggerFactory implements FeignLoggerFactory {

	private final Logger logger;

	private final Environment environment;

	private final FeignClientProperties feignClientProperties;

	public DefaultFeignLoggerFactory(Logger logger) {
		this(logger, null, null);
	}

	public DefaultFeignLoggerFactory(Logger logger, Environment environment,
			FeignClientProperties feignClientProperties) {
		this.logger = logger;
		this.environment = environment;
		this.feignClientProperties = feignClientProperties;
	}

	@Override
	public Logger create(Class<?> type) {
		if (this.logger != null) {
			return this.logger;
		}

		List<String> sensitiveHeaders = getSensitiveHeaders();

		if (sensitiveHeaders.isEmpty()) {
			return new Slf4jLogger(type);
		}

		return new SensitiveHeadersSlf4jLogger(type, sensitiveHeaders);
	}

	private List<String> getSensitiveHeaders() {
		if (environment == null || feignClientProperties == null) {
			return Collections.emptyList();
		}

		String contextId = environment.getProperty("spring.cloud.openfeign.client.name");
		if (contextId == null) {
			return Collections.emptyList();
		}

		Map<String, FeignClientProperties.FeignClientConfiguration> config = feignClientProperties.getConfig();

		if (config == null) {
			return Collections.emptyList();
		}

		List<String> sensitiveHeaders = new ArrayList<>();

		FeignClientProperties.FeignClientConfiguration defaultConfiguration = config
			.get(feignClientProperties.getDefaultConfig());

		if (defaultConfiguration != null && defaultConfiguration.getLogging() != null
				&& defaultConfiguration.getLogging().getSensitiveHeaders() != null) {
			sensitiveHeaders.addAll(defaultConfiguration.getLogging().getSensitiveHeaders());
		}

		FeignClientProperties.FeignClientConfiguration clientConfiguration = config.get(contextId);

		if (clientConfiguration != null && clientConfiguration.getLogging() != null
				&& clientConfiguration.getLogging().getSensitiveHeaders() != null) {
			sensitiveHeaders.addAll(clientConfiguration.getLogging().getSensitiveHeaders());
		}

		return sensitiveHeaders;
	}

	static class SensitiveHeadersSlf4jLogger extends Slf4jLogger {

		private final List<String> sensitiveHeaders;

		SensitiveHeadersSlf4jLogger(Class<?> type, List<String> sensitiveHeaders) {
			super(type);
			this.sensitiveHeaders = sensitiveHeaders.stream().map(header -> header.toLowerCase(Locale.ROOT)).toList();
		}

		@Override
		protected boolean shouldLogRequestHeader(String header) {
			return !isSensitiveHeader(header) && super.shouldLogRequestHeader(header);
		}

		@Override
		protected boolean shouldLogResponseHeader(String header) {
			return !isSensitiveHeader(header) && super.shouldLogResponseHeader(header);
		}

		private boolean isSensitiveHeader(String header) {
			return sensitiveHeaders.contains(header.toLowerCase(Locale.ROOT));
		}

	}

}
