/*
 * Copyright 2022-2022 the original author or authors.
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

package org.springframework.cloud.openfeign.aot;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.lang.model.element.Modifier;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.MethodReference;
import org.springframework.aot.hint.ProxyHints;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.aot.BeanFactoryInitializationCode;
import org.springframework.beans.factory.aot.BeanRegistrationExcludeFilter;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.cloud.openfeign.FeignClientFactory;
import org.springframework.cloud.openfeign.FeignClientFactoryBean;
import org.springframework.cloud.openfeign.FeignClientSpecification;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.javapoet.MethodSpec;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * A {@link BeanFactoryInitializationAotProcessor} that creates an
 * {@link BeanFactoryInitializationAotContribution} that registers bean definitions and
 * proxy hints for Feign client beans.
 *
 * @author Olga Maciaszek-Sharma
 * @since 4.0.0
 */
public class FeignClientBeanFactoryInitializationAotProcessor
		implements BeanRegistrationExcludeFilter, BeanFactoryInitializationAotProcessor {

	private final GenericApplicationContext context;

	private final Map<String, BeanDefinition> feignClientBeanDefinitions;

	public FeignClientBeanFactoryInitializationAotProcessor(GenericApplicationContext context,
			FeignClientFactory feignClientFactory) {
		this.context = context;
		this.feignClientBeanDefinitions = getFeignClientBeanDefinitions(feignClientFactory);
	}

	@Override
	public boolean isExcludedFromAotProcessing(RegisteredBean registeredBean) {
		return registeredBean.getBeanClass().equals(FeignClientFactoryBean.class)
				|| feignClientBeanDefinitions.containsKey(registeredBean.getBeanClass().getName());
	}

	private Map<String, BeanDefinition> getFeignClientBeanDefinitions(FeignClientFactory feignClientFactory) {
		Map<String, FeignClientSpecification> configurations = feignClientFactory.getConfigurations();
		return configurations.values().stream().map(FeignClientSpecification::getClassName).filter(Objects::nonNull)
				.filter(className -> !className.equals("default"))
				.map(className -> Map.entry(className, context.getBeanDefinition(className)))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	@Override
	public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {
		BeanFactory applicationBeanFactory = context.getBeanFactory();
		if (feignClientBeanDefinitions.isEmpty() || !beanFactory.equals(applicationBeanFactory)) {
			return null;
		}
		return new AotContribution(feignClientBeanDefinitions);
	}

	private static final class AotContribution implements BeanFactoryInitializationAotContribution {

		private final Map<String, BeanDefinition> feignClientBeanDefinitions;

		private AotContribution(Map<String, BeanDefinition> feignClientBeanDefinitions) {
			this.feignClientBeanDefinitions = feignClientBeanDefinitions;
		}

		@Override
		public void applyTo(GenerationContext generationContext,
				BeanFactoryInitializationCode beanFactoryInitializationCode) {
			ProxyHints proxyHints = generationContext.getRuntimeHints().proxies();
			Set<String> feignClientRegistrationMethods = feignClientBeanDefinitions.values().stream()
					.map(beanDefinition -> {
						Assert.notNull(beanDefinition, "beanDefinition cannot be null");
						Assert.isInstanceOf(GenericBeanDefinition.class, beanDefinition);
						GenericBeanDefinition registeredBeanDefinition = (GenericBeanDefinition) beanDefinition;
						MutablePropertyValues feignClientProperties = registeredBeanDefinition.getPropertyValues();
						String className = (String) feignClientProperties.get("type");
						Assert.notNull(className, "className cannot be null");
						Class clazz = ClassUtils.resolveClassName(className, null);
						proxyHints.registerJdkProxy(clazz);
						return beanFactoryInitializationCode.getMethods()
								.add(buildMethodName(className), method -> generateFeignClientRegistrationMethod(method,
										feignClientProperties, registeredBeanDefinition))
								.getName();
					}).collect(Collectors.toSet());
			MethodReference initializerMethod = beanFactoryInitializationCode.getMethods()
					.add("initialize", method -> generateInitializerMethod(method, feignClientRegistrationMethods))
					.toMethodReference();
			beanFactoryInitializationCode.addInitializer(initializerMethod);
		}

		private String buildMethodName(String clientName) {
			return "register" + clientName + "FeignClient";
		}

		private void generateInitializerMethod(MethodSpec.Builder method, Set<String> feignClientRegistrationMethods) {
			method.addModifiers(Modifier.PUBLIC);
			method.addParameter(DefaultListableBeanFactory.class, "registry");
			feignClientRegistrationMethods.forEach(feignClientRegistrationMethod -> method.addStatement("$N(registry)",
					feignClientRegistrationMethod));
		}

		private void generateFeignClientRegistrationMethod(MethodSpec.Builder method,
				MutablePropertyValues feignClientPropertyValues, GenericBeanDefinition registeredBeanDefinition) {
			Object feignQualifiers = feignClientPropertyValues.get("qualifiers");
			Assert.notNull(feignQualifiers, "Feign qualifiers cannot be null");
			String qualifiers = "{\"" + String.join("\",\"", (String[]) feignQualifiers) + "\"}";
			method.addJavadoc("register Feign Client: $L", feignClientPropertyValues.get("type"))
					.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
					.addParameter(BeanDefinitionRegistry.class, "registry")
					.addStatement("Class clazz = $T.resolveClassName(\"$L\", null)", ClassUtils.class,
							feignClientPropertyValues.get("type"))
					.addStatement("$T definition = $T.genericBeanDefinition($T.class)", BeanDefinitionBuilder.class,
							BeanDefinitionBuilder.class, FeignClientFactoryBean.class)
					.addStatement("definition.addPropertyValue(\"name\",\"$L\")", feignClientPropertyValues.get("name"))
					.addStatement("definition.addPropertyValue(\"contextId\", \"$L\")",
							feignClientPropertyValues.get("contextId"))
					.addStatement("definition.addPropertyValue(\"type\", clazz)")
					.addStatement("definition.addPropertyValue(\"url\", \"$L\")", feignClientPropertyValues.get("url"))
					.addStatement("definition.addPropertyValue(\"path\", \"$L\")",
							feignClientPropertyValues.get("path"))
					.addStatement("definition.addPropertyValue(\"dismiss404\", $L)",
							feignClientPropertyValues.get("dismiss404"))
					.addStatement("definition.addPropertyValue(\"fallback\", $T.class)",
							feignClientPropertyValues.get("fallback"))
					.addStatement("definition.addPropertyValue(\"fallbackFactory\", $T.class)",
							feignClientPropertyValues.get("fallbackFactory"))
					.addStatement("definition.setAutowireMode($L)", registeredBeanDefinition.getAutowireMode())
					.addStatement("definition.setLazyInit($L)",
							registeredBeanDefinition.getLazyInit() != null ? registeredBeanDefinition.getLazyInit()
									: false)
					.addStatement("$T beanDefinition = definition.getBeanDefinition()", AbstractBeanDefinition.class)
					.addStatement("beanDefinition.setAttribute(\"$L\", clazz)", FactoryBean.OBJECT_TYPE_ATTRIBUTE)
					.addStatement("beanDefinition.setPrimary($L)", registeredBeanDefinition.isPrimary())
					.addStatement("$T holder = new $T(beanDefinition, \"$L\",  new String[]$L)",
							BeanDefinitionHolder.class, BeanDefinitionHolder.class,
							feignClientPropertyValues.get("type"), qualifiers)
					.addStatement("$T.registerBeanDefinition(holder, registry) ", BeanDefinitionReaderUtils.class);
		}

	}

}
