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

package org.springframework.cloud.openfeign.clientconfig;

/**
 * Additional Feign Client configuration that are not included in
 * {@link org.springframework.cloud.openfeign.FeignClient}.
 *
 * @author Matt King
 */
public interface FeignClientConfigurer {

	/**
	 * @return whether to mark the feign proxy as a primary bean. Defaults to true.
	 */
	default boolean primary() {
		return true;
	}

	/**
	 * FALSE will only apply configurations from classes listed in
	 * <code>configuration()</code>. Will still use parent instance of
	 * {@link feign.codec.Decoder}, {@link feign.codec.Encoder}, and
	 * {@link feign.Contract} if none are provided.
	 * @return weather to inherit parent context for client configuration.
	 */
	default boolean inheritParentConfiguration() {
		return true;
	}

}
