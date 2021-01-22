/*
 * Copyright 2021-2021 the original author or authors.
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

import java.util.Optional;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * @author Jonatan Ivanov
 */
class FeignClientMetricsEnabledCondition implements Condition {

	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		FeignClientProperties feignClientProperties = context.getBeanFactory().getBean(FeignClientProperties.class);
		String clientName = context.getEnvironment().getProperty("feign.client.name");

		return Optional.ofNullable(feignClientProperties).map(FeignClientProperties::getConfig)
				.map(configMap -> configMap.get(clientName))
				.map(FeignClientProperties.FeignClientConfiguration::getMetrics)
				.map(FeignClientProperties.FeignClientMetricsConfiguration::getEnabled).orElse(true);
	}

}
