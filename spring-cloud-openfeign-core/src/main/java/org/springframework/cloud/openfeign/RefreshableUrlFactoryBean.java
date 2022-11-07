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

package org.springframework.cloud.openfeign;

import java.util.Objects;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.StringUtils;

/**
 * This factory bean creates {@link RefreshableUrl} instance as per the applicable
 * configurations.
 *
 * @author Jasbir Singh
 * @since 4.0.0
 */
public class RefreshableUrlFactoryBean implements FactoryBean<RefreshableUrl>, ApplicationContextAware {

	private ApplicationContext applicationContext;

	private String contextId;

	private RefreshableUrl refreshableUrl;

	@Override
	public Class<?> getObjectType() {
		return RefreshableUrl.class;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public RefreshableUrl getObject() {
		if (refreshableUrl != null) {
			return refreshableUrl;
		}

		FeignClientProperties properties = applicationContext.getBean(FeignClientProperties.class);
		if (Objects.isNull(properties.getConfig())) {
			return new RefreshableUrl(null);
		}
		FeignClientProperties.FeignClientConfiguration configuration = properties.getConfig().get(contextId);
		if (Objects.isNull(configuration) || !StringUtils.hasText(configuration.getUrl())) {
			return new RefreshableUrl(null);
		}

		refreshableUrl = new RefreshableUrl(FeignClientsRegistrar.getUrl(configuration.getUrl()));
		return refreshableUrl;
	}

	public void setContextId(String contextId) {
		this.contextId = contextId;
	}

}
