/*
 * Copyright 2015-2022 the original author or authors.
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

package org.springframework.cloud.openfeign.security;

import org.springframework.security.oauth2.client.token.AccessTokenProvider;

/**
 * Interface for configurer beans working with
 * {@link OAuth2FeignRequestInterceptorBuilder} in order to provide custom interceptors
 * for {@link AccessTokenProvider} managed internally by
 * {@link OAuth2FeignRequestInterceptor}.
 *
 * @author Wojciech Mąka
 * @since 3.1.1
 */
@FunctionalInterface
public interface OAuth2FeignRequestInterceptorConfigurer {

	void customize(OAuth2FeignRequestInterceptorBuilder requestInterceptorBuilder);

}
