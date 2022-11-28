package org.springframework.cloud.openfeign.aot;

import java.net.URL;

import org.apache.catalina.webresources.TomcatURLStreamHandlerFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebApplicationContext;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.aot.ApplicationContextAotGenerator;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.test.tools.CompileWithForkedClassLoader;
import org.springframework.core.test.tools.TestCompiler;
import org.springframework.javapoet.ClassName;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FeignChildContextInitializer}.
 *
 * @author Olga Maciaszek-Sharma
 */
@ExtendWith(OutputCaptureExtension.class)
public class FeignChildContextInitializerTests {

	private static final Log LOG = LogFactory.getLog(FeignChildContextInitializerTests.class);

	@BeforeEach
	@AfterEach
	void reset() {
		ReflectionTestUtils.setField(TomcatURLStreamHandlerFactory.class, "instance", null);
		ReflectionTestUtils.setField(URL.class, "factory", null);
	}

	@Test
	@CompileWithForkedClassLoader
	@SuppressWarnings("unchecked")
	void shouldStartFeignChildContextsFromAotContributions(CapturedOutput output) {
		WebApplicationContextRunner contextRunner = new WebApplicationContextRunner(
			AnnotationConfigServletWebApplicationContext::new)
			.withConfiguration(AutoConfigurations.of(ServletWebServerFactoryAutoConfiguration.class,
				FeignAutoConfiguration.class))
			.withConfiguration(UserConfigurations.of(TestFeignConfiguration.class));
		contextRunner.prepare(context -> {
			TestGenerationContext generationContext = new TestGenerationContext(TestTarget.class);
			ClassName className = new ApplicationContextAotGenerator().processAheadOfTime(
				(GenericApplicationContext) context.getSourceApplicationContext(), generationContext);
			generationContext.writeGeneratedContent();
			TestCompiler compiler = TestCompiler.forSystem();
			compiler.with(generationContext).compile(compiled -> {
				ServletWebServerApplicationContext freshApplicationContext = new ServletWebServerApplicationContext();
				ApplicationContextInitializer<GenericApplicationContext> initializer = compiled
					.getInstance(ApplicationContextInitializer.class, className.toString());
				initializer.initialize(freshApplicationContext);
				assertThat(output).isNotEmpty();

				// TODO
			});
		});
	}

	static class TestTarget {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableFeignClients(clients = {TestFeignClient.class, TestFeignClientWithConfig.class}, defaultConfiguration = DefaultConfiguration.class)
	public static class TestFeignConfiguration {

	}

	public static class TestConfiguration {

		@Bean
		TestBean testBean() {
			LOG.debug("Instantiating bean from Test custom config");
			return new TestBean();
		}

	}

	public static class DefaultConfiguration {

		@Bean
		TestBean defaultTestBean() {
			LOG.debug("Instantiating bean from default custom config");
			return new TestBean();
		}

	}

	public static class TestBean {

	}

	@FeignClient("test")
	interface TestFeignClient {

		void test();

	}

	// TODO: verify other annotation parameters
	@FeignClient(value = "test-with-config", configuration = TestConfiguration.class)
	interface TestFeignClientWithConfig {

		void test();

	}

}
