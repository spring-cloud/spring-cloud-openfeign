package org.springframework.cloud.openfeign.aot;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.lang.model.element.Modifier;

import org.springframework.aot.generate.GeneratedMethod;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.aot.BeanRegistrationCode;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.cloud.openfeign.FeignClientFactory;
import org.springframework.cloud.openfeign.FeignClientSpecification;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.aot.ApplicationContextAotGenerator;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.javapoet.ClassName;
import org.springframework.util.Assert;

/**
 * A {@link BeanRegistrationAotProcessor} that creates an {@link BeanRegistrationAotContribution} for
 * Feign child contexts.
 *
 * @author Olga Maciaszek-Sharma
 * @since 4.0.0
 */
public class FeignChildContextInitializer implements BeanRegistrationAotProcessor {

	private final ApplicationContext applicationContext;

	private final FeignClientFactory feignClientFactory;


	public FeignChildContextInitializer(ApplicationContext applicationContext, FeignClientFactory feignClientFactory) {
		this.applicationContext = applicationContext;
		this.feignClientFactory = feignClientFactory;
	}

	@Override
	public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
		Assert.isInstanceOf(ConfigurableApplicationContext.class, applicationContext);
		ConfigurableApplicationContext context = ((ConfigurableApplicationContext) applicationContext);
		BeanFactory applicationBeanFactory = context.getBeanFactory();
		if (!((registeredBean.getBeanClass().equals(FeignClientFactory.class))
			&& registeredBean.getBeanFactory().equals(applicationBeanFactory))) {
			return null;
		}
		Set<String> contextIds = new HashSet<>(getContextIdsFromConfig());
		Map<String, GenericApplicationContext> childContextAotContributions = contextIds.stream()
			.map(contextId -> Map.entry(contextId, buildChildContext(contextId)))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		return new AotContribution(childContextAotContributions);
	}

	private GenericApplicationContext buildChildContext(String contextId) {
		GenericApplicationContext childContext = feignClientFactory.buildContext(contextId);
		feignClientFactory.registerBeans(contextId, childContext);
		return childContext;
	}

	private Collection<String> getContextIdsFromConfig() {
		Map<String, FeignClientSpecification> configurations = feignClientFactory.getConfigurations();
		return configurations.keySet().stream().filter(key -> !key.startsWith("default."))
			.collect(Collectors.toSet());
	}


	private static class AotContribution implements BeanRegistrationAotContribution {

		private final Map<String, GenericApplicationContext> childContexts;

		public AotContribution(Map<String, GenericApplicationContext> childContexts) {
			this.childContexts = childContexts.entrySet().stream()
				.filter(entry -> entry.getValue() != null)
				.map(entry -> Map.entry(entry.getKey(), entry.getValue()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		}

		@Override
		public void applyTo(GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode) {
			Map<String, ClassName> generatedInitializerClassNames = childContexts.entrySet()
				.stream()
				.map(entry -> {
					String name = entry.getValue().getDisplayName();
					name = name.replaceAll("[-]", "_");
					GenerationContext childGenerationContext = generationContext.withName(name);
					ClassName initializerClassName = new ApplicationContextAotGenerator()
						.processAheadOfTime(entry.getValue(), childGenerationContext);
					return Map.entry(entry.getKey(), initializerClassName);
				})
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
			GeneratedMethod postProcessorMethod = beanRegistrationCode.getMethods()
				.add("addFeignChildContextInitializer",
					method -> {
						method.addJavadoc("Use AOT child context management initialization")
							.addModifiers(Modifier.PRIVATE, Modifier.STATIC)
							.addParameter(RegisteredBean.class, "registeredBean")
							.addParameter(FeignClientFactory.class, "instance")
							.returns(FeignClientFactory.class)
							.addStatement("$T<String, Object> initializers = new $T<>()", Map.class, HashMap.class);
						generatedInitializerClassNames.keySet()
							.forEach(contextId -> method.addStatement("initializers.put($S, new $L())", contextId,
								generatedInitializerClassNames.get(contextId)));
						method.addStatement("return instance.withApplicationContextInitializers(initializers)");
					});
			beanRegistrationCode.addInstancePostProcessor(postProcessorMethod.toMethodReference());
		}
	}

}
