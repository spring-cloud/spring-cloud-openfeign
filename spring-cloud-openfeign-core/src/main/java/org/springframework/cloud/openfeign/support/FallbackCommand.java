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

package org.springframework.cloud.openfeign.support;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixThreadPoolKey;

/**
 * Convenience class for implementing feign fallbacks that return {@link HystrixCommand}.
 * Also useful for return types of {@link rx.Observable} and
 * {@link java.util.concurrent.Future}. For those return types, just call
 * {@link FallbackCommand#observe()} or {@link FallbackCommand#queue()} respectively.
 *
 * @param <T> result type
 * @author Spencer Gibb
 */
public class FallbackCommand<T> extends HystrixCommand<T> {

	private T result;

	public FallbackCommand(T result) {
		this(result, "fallback");
	}

	protected FallbackCommand(T result, String groupname) {
		super(HystrixCommandGroupKey.Factory.asKey(groupname));
		this.result = result;
	}

	public FallbackCommand(T result, HystrixCommandGroupKey group) {
		super(group);
		this.result = result;
	}

	public FallbackCommand(T result, HystrixCommandGroupKey group,
			int executionIsolationThreadTimeoutInMilliseconds) {
		super(group, executionIsolationThreadTimeoutInMilliseconds);
		this.result = result;
	}

	public FallbackCommand(T result, HystrixCommandGroupKey group,
			HystrixThreadPoolKey threadPool) {
		super(group, threadPool);
		this.result = result;
	}

	public FallbackCommand(T result, HystrixCommandGroupKey group,
			HystrixThreadPoolKey threadPool,
			int executionIsolationThreadTimeoutInMilliseconds) {
		super(group, threadPool, executionIsolationThreadTimeoutInMilliseconds);
		this.result = result;
	}

	public FallbackCommand(T result, Setter setter) {
		super(setter);
		this.result = result;
	}

	@Override
	protected T run() throws Exception {
		return this.result;
	}

}
