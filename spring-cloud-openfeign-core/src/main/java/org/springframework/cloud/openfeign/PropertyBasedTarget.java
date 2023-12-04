/*
 * Copyright 2013-2023 the original author or authors.
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

import feign.Target;

/**
 * A {@link HardCodedTarget} implementation that resolves url from properties when the
 * initial call is made. Using it allows specifying the url at runtime in an AOT-packaged
 * application or a native image by setting the value of the
 * `spring.cloud.openfeign.client.config.[clientId].url`.
 *
 * @author Olga Maciaszek-Sharma
 * @author Can Bezmen
 * @see FeignClientProperties.FeignClientConfiguration#getUrl()
 */
public class PropertyBasedTarget<T> extends Target.HardCodedTarget<T> {

	private String url;

	private final FeignClientProperties.FeignClientConfiguration config;

	private final String path;

	public PropertyBasedTarget(Class<T> type, String name, FeignClientProperties.FeignClientConfiguration config,
			String path) {
		super(type, name, config.getUrl());
		this.config = config;
		this.path = path;
	}

	public PropertyBasedTarget(Class<T> type, String name, FeignClientProperties.FeignClientConfiguration config) {
		super(type, name, config.getUrl());
		this.config = config;
		path = "";
	}

	@Override
	public String url() {
		if (url == null) {
			url = config.getUrl() + path;
		}
		return url;
	}

}
