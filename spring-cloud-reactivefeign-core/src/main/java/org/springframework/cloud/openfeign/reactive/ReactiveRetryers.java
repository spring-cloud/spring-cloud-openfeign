/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.openfeign.reactive;

import java.util.Date;

import feign.RetryableException;

/**
 * @author Sergii Karpenko
 */
public class ReactiveRetryers {

	public static ReactiveRetryPolicy retry(int maxRetries) {
		return (error, attemptNo) -> attemptNo <= maxRetries ? 0 : -1;
	}

	public static ReactiveRetryPolicy retryWithDelay(int maxRetries, long period) {
		return (error, attemptNo) -> {
			if (attemptNo <= maxRetries) {
				long delay;
				Date retryAfter;
				// "Retry-After" header set
				if (error instanceof RetryableException
						&& (retryAfter = ((RetryableException) error)
								.retryAfter()) != null) {
					delay = retryAfter.getTime() - System.currentTimeMillis();
					delay = Math.min(delay, period);
					delay = Math.max(delay, 0);
				}
				else {
					delay = period;
				}
				return delay;
			}
			else {
				return -1;
			}
		};
	}

}
