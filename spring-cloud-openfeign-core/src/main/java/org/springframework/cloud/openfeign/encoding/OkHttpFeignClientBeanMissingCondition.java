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

package org.springframework.cloud.openfeign.encoding;

import feign.Client;
import feign.okhttp.OkHttpClient;

import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Condition;

/**
 * A {@link Condition} that verifies whether the conditions for creating Feign
 * {@link Client} beans that either are of type {@link OkHttpClient} or have a delegate of
 * type {@link OkHttpClient} are not met.
 *
 * @author Olga Maciaszek-Sharma
 * @since 4.0.2
 */
public class OkHttpFeignClientBeanMissingCondition extends AnyNestedCondition {

	public OkHttpFeignClientBeanMissingCondition() {
		super(ConfigurationPhase.REGISTER_BEAN);
	}

	@ConditionalOnMissingClass("feign.okhttp.OkHttpClient")
	static class FeignOkHttpClientPresent {

	}

	@ConditionalOnProperty(value = "spring.cloud.openfeign.okhttp.enabled", havingValue = "false")
	static class FeignOkHttpClientEnabled {

	}

}
