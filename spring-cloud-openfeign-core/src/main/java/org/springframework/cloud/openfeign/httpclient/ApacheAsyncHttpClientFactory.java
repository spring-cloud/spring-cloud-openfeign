/*
 * Copyright 2013-2020 the original author or authors.
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

package org.springframework.cloud.openfeign.httpclient;

import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;

/**
 * Factory for creating a new {@link CloseableHttpAsyncClient}.
 *
 * @author Nguyen Ky Thanh
 */
public interface ApacheAsyncHttpClientFactory {

	/**
	 * Creates an {@link HttpAsyncClientBuilder} that can be used to create a new
	 * {@link CloseableHttpAsyncClient}.
	 * @return A {@link HttpAsyncClientBuilder}.
	 */
	HttpAsyncClientBuilder createBuilder();

}
