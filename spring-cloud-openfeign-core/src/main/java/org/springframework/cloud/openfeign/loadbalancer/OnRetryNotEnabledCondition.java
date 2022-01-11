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

package org.springframework.cloud.openfeign.loadbalancer;

import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryFactory;
import org.springframework.retry.support.RetryTemplate;

/**
 * A condition that verifies that {@link RetryTemplate} is on the classpath, a
 * {@link LoadBalancedRetryFactory} bean is present and
 * <code>spring.cloud.loadbalancer.retry.enabled</code> is not set to <code>false</code>.
 *
 * @author Olga Maciaszek-Sharma
 * @since 2.2.6
 */
public class OnRetryNotEnabledCondition extends AnyNestedCondition {

	public OnRetryNotEnabledCondition() {
		super(ConfigurationPhase.REGISTER_BEAN);
	}

	@ConditionalOnMissingClass("org.springframework.retry.support.RetryTemplate")
	static class OnNoRetryTemplateCondition {

	}

	@ConditionalOnMissingBean(LoadBalancedRetryFactory.class)
	static class OnRetryFactoryCondition {

	}

	@ConditionalOnProperty(value = "spring.cloud.loadbalancer.retry.enabled", havingValue = "false")
	static class OnLoadBalancerRetryEnabledCondition {

	}

}
