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

package org.springframework.cloud.openfeign;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.cloud.context.named.NamedContextFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.lang.Nullable;

/**
 * A factory that creates instances of feign classes. It creates a Spring
 * ApplicationContext per client name, and extracts the beans that it needs from there.
 *
 * @author Spencer Gibb
 * @author Dave Syer
 * @author Matt King
 * @author Jasbir Singh
 * @author Olga Maciaszek-Sharma
 * @author changjin wei(魏昌进)
 */
public class FeignClientFactory extends NamedContextFactory<FeignClientSpecification> {

	public FeignClientFactory() {
		this(new HashMap<>());
	}

	public FeignClientFactory(
			Map<String, ApplicationContextInitializer<GenericApplicationContext>> applicationContextInitializers) {
		super(FeignClientsConfiguration.class, "spring.cloud.openfeign", "spring.cloud.openfeign.client.name",
				applicationContextInitializers);
	}

	@Nullable
	public <T> T getInstanceWithoutAncestors(String name, Class<T> type) {
		try {
			return BeanFactoryUtils.beanOfType(getContext(name), type);
		}
		catch (BeansException ex) {
			return null;
		}
	}

	@Nullable
	public <T> T getInstanceWithoutAncestorsForAnnotation(String name, Class<T> type,
			Class<? extends Annotation> annotationType) {
		GenericApplicationContext context = getContext(name);
		String[] beanNames = context.getBeanNamesForAnnotation(annotationType);
		List<T> beans = new ArrayList<>();
		for (String beanName : beanNames) {
			if (context.isTypeMatch(beanName, type)) {
				beans.add((T) context.getBean(beanName));
			}
		}
		if (beans.size() > 1) {
			throw new IllegalStateException("Only one annotated bean for type expected.");
		}
		return beans.isEmpty() ? null : beans.get(0);

	}

	@Nullable
	public <T> Map<String, T> getInstancesWithoutAncestors(String name, Class<T> type) {
		return getContext(name).getBeansOfType(type);
	}

	public <T> T getInstance(String contextName, String beanName, Class<T> type) {
		return getContext(contextName).getBean(beanName, type);
	}

	@SuppressWarnings("unchecked")
	public FeignClientFactory withApplicationContextInitializers(Map<String, Object> applicationContextInitializers) {
		Map<String, ApplicationContextInitializer<GenericApplicationContext>> convertedInitializers = new HashMap<>();
		applicationContextInitializers.keySet()
				.forEach(contextId -> convertedInitializers.put(contextId,
						(ApplicationContextInitializer<GenericApplicationContext>) applicationContextInitializers
								.get(contextId)));
		return new FeignClientFactory(convertedInitializers);
	}

}
