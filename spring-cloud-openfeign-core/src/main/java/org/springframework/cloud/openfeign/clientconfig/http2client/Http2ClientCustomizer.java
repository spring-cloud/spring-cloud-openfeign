/*
 * Copyright 2013-2024 the original author or authors.
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

package org.springframework.cloud.openfeign.clientconfig.http2client;

import java.net.http.HttpClient;

/**
 * Callback interface that can be implemented by beans wishing to further customize the
 * {@link HttpClient} through {@link HttpClient.Builder} retaining its default
 * auto-configuration.
 *
 * @author Luís Duarte
 * @since 4.1.1
 */
@FunctionalInterface
public interface Http2ClientCustomizer {

	/**
	 * Customize JDK's HttpClient.
	 * @param builder the HttpClient.Builder to customize
	 */
	void customize(HttpClient.Builder builder);

}
