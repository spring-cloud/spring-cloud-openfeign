package org.springframework.cloud.openfeign.aot;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.lang.model.element.Modifier;

import org.springframework.aot.generate.GeneratedMethod;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.aot.BeanRegistrationCode;
import org.springframework.beans.factory.aot.BeanRegistrationExcludeFilter;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.cloud.openfeign.FeignClientFactory;
import org.springframework.cloud.openfeign.FeignClientFactoryBean;
import org.springframework.cloud.openfeign.FeignClientSpecification;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.aot.ApplicationContextAotGenerator;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.MethodSpec;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * A {@link BeanRegistrationAotProcessor} that creates an {@link BeanRegistrationAotContribution} for
 * Feign child contexts.
 *
 * @author Olga Maciaszek-Sharma
 * @since 4.0.0
 */
public class FeignChildContextInitializer implements BeanRegistrationAotProcessor, ApplicationListener<WebServerInitializedEvent>,
	BeanRegistrationExcludeFilter {

	private final GenericApplicationContext context;

	private final FeignClientFactory feignClientFactory;

	private final Map<String, ApplicationContextInitializer<GenericApplicationContext>> applicationContextInitializers;

	private final Map<String, BeanDefinition> feignClientBeanDefinitions;

	public FeignChildContextInitializer(GenericApplicationContext context, FeignClientFactory feignClientFactory) {
		this(context, feignClientFactory, new HashMap<>());
	}

	public FeignChildContextInitializer(GenericApplicationContext context, FeignClientFactory feignClientFactory, Map<String, ApplicationContextInitializer<GenericApplicationContext>> applicationContextInitializers) {
		this.context = context;
		this.feignClientFactory = feignClientFactory;
		this.applicationContextInitializers = applicationContextInitializers;
		feignClientBeanDefinitions = getFeignClientClassNames();
	}

	@Override
	public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
		Assert.isInstanceOf(ConfigurableApplicationContext.class, context);
		ConfigurableApplicationContext context = this.context;
		BeanFactory applicationBeanFactory = context.getBeanFactory();
		if (!(registeredBean.getBeanClass().equals(getClass())
			&& registeredBean.getBeanFactory().equals(applicationBeanFactory))) {
			return null;
		}
		Set<String> contextIds = new HashSet<>(getContextIdsFromConfig());
		Map<String, GenericApplicationContext> childContextAotContributions = contextIds.stream()
			.map(contextId -> Map.entry(contextId, buildChildContext(contextId)))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		return new AotContribution(childContextAotContributions, feignClientBeanDefinitions);
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

	@SuppressWarnings("unchecked")
	public FeignChildContextInitializer withApplicationContextInitializers(
		Map<String, Object> applicationContextInitializers) {
		Map<String, ApplicationContextInitializer<GenericApplicationContext>> convertedInitializers = new HashMap<>();
		applicationContextInitializers.keySet()
			.forEach(contextId -> convertedInitializers.put(contextId,
				(ApplicationContextInitializer<GenericApplicationContext>) applicationContextInitializers
					.get(contextId)));
		return new FeignChildContextInitializer(context, feignClientFactory,
			convertedInitializers);
	}

	@Override
	public void onApplicationEvent(WebServerInitializedEvent event) {
		if (context.equals(event.getApplicationContext())) {
			applicationContextInitializers.keySet().forEach(contextId -> {
				GenericApplicationContext childContext = feignClientFactory.buildContext(contextId);
				applicationContextInitializers.get(contextId).initialize(childContext);
				feignClientFactory.addContext(contextId, childContext);
				childContext.refresh();
			});
		}
	}

	@Override
	public boolean isBeanExcludedFromAotProcessing() {
		return false;
	}

	@Override
	public boolean isExcludedFromAotProcessing(RegisteredBean registeredBean) {
		return feignClientBeanDefinitions
			.containsKey(registeredBean.getBeanClass().getName());
	}

	private Map<String, BeanDefinition> getFeignClientClassNames() {
		Map<String, FeignClientSpecification> configurations = feignClientFactory.getConfigurations();
		return configurations.values().stream()
			.map(FeignClientSpecification::getClassName)
			.filter(Objects::nonNull)
			.filter(className -> !className.equals("default"))
			.map(className -> Map.entry(className, context.getBeanDefinition(className)))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	private static class AotContribution implements BeanRegistrationAotContribution {

		private final Map<String, GenericApplicationContext> childContexts;
		private final Map<String, BeanDefinition> feignClientBeanDefinitions;

		public AotContribution(Map<String, GenericApplicationContext> childContexts, Map<String, BeanDefinition> feignClientBeanDefinitions) {
			this.childContexts = childContexts.entrySet().stream()
				.filter(entry -> entry.getValue() != null)
				.map(entry -> Map.entry(entry.getKey(), entry.getValue()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
			this.feignClientBeanDefinitions = feignClientBeanDefinitions;
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
							.addParameter(FeignChildContextInitializer.class, "instance")
							.returns(FeignChildContextInitializer.class)
							.addStatement("$T<String, Object> initializers = new $T<>()", Map.class, HashMap.class);
						generatedInitializerClassNames.keySet()
							.forEach(contextId -> method.addStatement("initializers.put($S, new $L())", contextId,
								generatedInitializerClassNames.get(contextId)));
						method.addStatement("return instance.withApplicationContextInitializers(initializers)");
					});
			beanRegistrationCode.addInstancePostProcessor(postProcessorMethod.toMethodReference());

			// TODO: ensure the methods are called
			feignClientBeanDefinitions.values().forEach(beanDefinition -> {
				// TODO: handle problem retrieving
				Assert.notNull(beanDefinition, "beanDefinition cannot be null");
				Assert.isInstanceOf(GenericBeanDefinition.class, beanDefinition);
				GenericBeanDefinition registeredBeanDefinition = (GenericBeanDefinition) beanDefinition;
				Object factoryBeanObject = registeredBeanDefinition.getAttribute("feignClientsRegistrarFactoryBean");
				Assert.isInstanceOf(FeignClientFactoryBean.class, factoryBeanObject);
				FeignClientFactoryBean factoryBean = (FeignClientFactoryBean) factoryBeanObject;
				Assert.notNull(factoryBean, "factoryBean  cannot be null");
				generationContext.getGeneratedClasses()
					// FIXME: correct generation context
					.getOrAddForFeatureComponent(registeredBeanDefinition.getBeanClassName(), generatedInitializerClassNames.get(factoryBean.getContextId()), type -> {
							type.addMethod(buildMethodSpec(factoryBean, registeredBeanDefinition));
						}
					);
			});
		}


		// TODO: verify all factory bean method values from registrar!
		private MethodSpec buildMethodSpec(FeignClientFactoryBean registeredFactoryBean, GenericBeanDefinition registeredBeanDefinition) {
			String qualifiers = "{\"" + String.join("\",\"", registeredFactoryBean.getQualifiers()) + "\"}";
			return MethodSpec.methodBuilder("feignClientRegistration")
				.addJavadoc("registerFeignClient")
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
				.addStatement("String[] qualifiers = new String[]{}")
				.addStatement("$T holder = new $T(beanDefinition, \"$L\",  new String[]$L)", BeanDefinitionHolder.class,
					BeanDefinitionHolder.class, registeredBeanDefinition.getBeanClassName(), qualifiers)
				.addStatement("$T.registerBeanDefinition(holder, registry) ", BeanDefinitionReaderUtils.class)
				.build();
			// TODO
//					.addStatement("Class<?> beanType = $T.class", Class.forName(feignClientBeanDefinition.getBeanClassName()))
//					.addStatement("$T beanDefinition = new $T(beanType)", RootBeanDefinition.class, RootBeanDefinition.class)
//					.addStatement("beanDefinition.setLazyInit($L)", feignClientBeanDefinition.isLazyInit())
//					.addStatement("beanDefinition.setInstanceSupplier(($T<Object>) registeredBean -> new $T())", InstanceSupplier.class, FeignClientFactoryBean.class)
//					.addStatement("return beanDefinition")


		}
	}

}
