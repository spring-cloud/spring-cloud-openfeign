/*
 * Copyright 2013-2021 the original author or authors.
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

import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * @author Nguyen Ky Thanh
 */
public class HttpClient5DisabledConditions extends AnyNestedCondition {

	public HttpClient5DisabledConditions() {
		super(ConfigurationPhase.PARSE_CONFIGURATION);
	}

	@ConditionalOnMissingClass("feign.hc5.ApacheHttp5Client")
	static class ApacheHttp5ClientClassMissing {

	}

	@ConditionalOnProperty(value = "feign.httpclient.hc5.enabled", havingValue = "false",
			matchIfMissing = true)
	static class HttpClient5Disabled {

	}

}
