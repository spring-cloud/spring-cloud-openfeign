package org.springframework.cloud.openfeign.aot;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.lang.model.element.Modifier;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.MethodReference;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.aot.BeanFactoryInitializationCode;
import org.springframework.beans.factory.aot.BeanRegistrationExcludeFilter;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
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
 * @author Olga Maciaszek-Sharma
 */
public class FeignClientBeanFactoryInitializationAotProcessor implements BeanRegistrationExcludeFilter, BeanFactoryInitializationAotProcessor {


	private final GenericApplicationContext context;

	private final Map<String, BeanDefinition> feignClientBeanDefinitions;

	public FeignClientBeanFactoryInitializationAotProcessor(GenericApplicationContext context, FeignClientFactory feignClientFactory) {
		this.context = context;
		this.feignClientBeanDefinitions = getFeignClientBeanDefinitions(feignClientFactory);
	}

	@Override
	public boolean isExcludedFromAotProcessing(RegisteredBean registeredBean) {
		return feignClientBeanDefinitions
			.containsKey(registeredBean.getBeanClass().getName());
	}

	private Map<String, BeanDefinition> getFeignClientBeanDefinitions(FeignClientFactory feignClientFactory) {
		Map<String, FeignClientSpecification> configurations = feignClientFactory.getConfigurations();
		return configurations.values().stream()
			.map(FeignClientSpecification::getClassName)
			.filter(Objects::nonNull)
			.filter(className -> !className.equals("default"))
			.map(className -> Map.entry(className, context.getBeanDefinition(className)))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	@Override
	public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {
		if (!feignClientBeanDefinitions.isEmpty()) {
			return new AotContribution(feignClientBeanDefinitions);
		}
		return null;
	}

	private static class AotContribution implements BeanFactoryInitializationAotContribution {

		private final Map<String, BeanDefinition> feignClientBeanDefinitions;

		private AotContribution(Map<String, BeanDefinition> feignClientBeanDefinitions) {
			this.feignClientBeanDefinitions = feignClientBeanDefinitions;
		}

		@Override
		public void applyTo(GenerationContext generationContext, BeanFactoryInitializationCode beanFactoryInitializationCode) {
			Set<String> feignClientRegistrationMethods = feignClientBeanDefinitions.values()
				.stream()
				.map(beanDefinition -> {
					Assert.notNull(beanDefinition, "beanDefinition cannot be null");
					Assert.isInstanceOf(GenericBeanDefinition.class, beanDefinition);
					GenericBeanDefinition registeredBeanDefinition = (GenericBeanDefinition) beanDefinition;
					Object factoryBeanObject = registeredBeanDefinition.getAttribute("feignClientsRegistrarFactoryBean");
					Assert.isInstanceOf(FeignClientFactoryBean.class, factoryBeanObject);
					FeignClientFactoryBean factoryBean = (FeignClientFactoryBean) factoryBeanObject;
					Assert.notNull(factoryBean, "factoryBean  cannot be null");
					return beanFactoryInitializationCode.getMethods()
						.add(buildMethodName(factoryBean.getType().getSimpleName()),
							method -> generateFeignClientRegistrationMethod(method, factoryBean, registeredBeanDefinition))
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
			feignClientRegistrationMethods.forEach(feignClientRegistrationMethod -> method.addStatement("$N(registry)", feignClientRegistrationMethod));
		}

		private void generateFeignClientRegistrationMethod(MethodSpec.Builder method, FeignClientFactoryBean registeredFactoryBean, GenericBeanDefinition registeredBeanDefinition) {
			String qualifiers = "{\"" + String.join("\",\"", registeredFactoryBean.getQualifiers()) + "\"}";
			method
				.addJavadoc("register Feign Client: $L", registeredBeanDefinition.getBeanClassName())
				.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
				.addParameter(BeanDefinitionRegistry.class, "registry")
				.addStatement("Class clazz = $T.resolveClassName(\"$L\", null)", ClassUtils.class, registeredBeanDefinition.getBeanClassName())
				.addStatement("$T beanFactory = registry instanceof $T ? ($T) registry : null",
					ConfigurableBeanFactory.class, ConfigurableBeanFactory.class, ConfigurableBeanFactory.class)
				.addStatement("$T factoryBean = new $T()", FeignClientFactoryBean.class, FeignClientFactoryBean.class)
				.addStatement("factoryBean.setBeanFactory(beanFactory)")
				.addStatement("factoryBean.setName(\"$L\")", registeredFactoryBean.getName())
				.addStatement("factoryBean.setContextId(\"$L\")", registeredFactoryBean.getContextId())
				.addStatement("factoryBean.setType($T.class)", registeredFactoryBean.getType())
				.addStatement("factoryBean.setUrl($L)", registeredFactoryBean.getUrl())
				.addStatement("factoryBean.setPath($L)", registeredFactoryBean.getPath())
				.addStatement("factoryBean.setDismiss404($L)", registeredFactoryBean.isDismiss404())
				.addStatement("factoryBean.setFallback($T.class)", registeredFactoryBean.getFallback())
				.addStatement("factoryBean.setFallbackFactory($T.class)", registeredFactoryBean.getFallbackFactory())
				.addStatement("$T definition = $T.genericBeanDefinition(clazz, () -> factoryBean.getObject())", BeanDefinitionBuilder.class,
					BeanDefinitionBuilder.class)
				.addStatement("definition.setAutowireMode($L)", registeredBeanDefinition.getAutowireMode())
				.addStatement("definition.setLazyInit($L)", registeredBeanDefinition.getLazyInit())
				.addStatement("$T beanDefinition = definition.getBeanDefinition()", AbstractBeanDefinition.class)
				.addStatement("beanDefinition.setAttribute(\"$L\", $T.class)", FactoryBean.OBJECT_TYPE_ATTRIBUTE, registeredFactoryBean.getType())
				.addStatement("beanDefinition.setAttribute(\"feignClientsRegistrarFactoryBean\", factoryBean)")
				.addStatement("beanDefinition.setPrimary($L)", registeredBeanDefinition.isPrimary())
				.addStatement("$T holder = new $T(beanDefinition, \"$L\",  new String[]$L)", BeanDefinitionHolder.class,
					BeanDefinitionHolder.class, registeredBeanDefinition.getBeanClassName(), qualifiers)
				.addStatement("$T.registerBeanDefinition(holder, registry) ", BeanDefinitionReaderUtils.class);
		}
	}

}
