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

import feign.Request;
import feign.RequestTemplate;
import feign.Target;

class PathPrefixedTarget<T> implements Target<T> {
	private final String path;
	private final Target<T> target;

	PathPrefixedTarget(String path, Target<T> target) {
		this.path = path;
		this.target = target;
	}

	@Override
	public Class<T> type() {
		return this.target.type();
	}

	@Override
	public String name() {
		return this.target.name();
	}

	@Override
	public String url() {
		return this.target.url() + this.path;
	}

	@Override
	public Request apply(RequestTemplate input) {
		RequestTemplate template = input.target(this.url());
		return this.target.apply(template);
	}
}
