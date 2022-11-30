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

import java.net.URL;

import org.apache.catalina.webresources.TomcatURLStreamHandlerFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.aot.AotDetector;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.util.TestPropertyValues;
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
import org.springframework.web.bind.annotation.GetMapping;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AOT processing tests.
 *
 * @author Olga Maciaszek-Sharma
 */
@ExtendWith(OutputCaptureExtension.class)
public class FeignAotTests {

	private static final Log LOG = LogFactory.getLog(FeignAotTests.class);

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
						.withConfiguration(UserConfigurations.of(TestFeignConfiguration.class))
						.withPropertyValues("logging.level.org.springframework.cloud=DEBUG");
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
				assertThat(output).contains("Creating a FeignClientFactoryBean.");
				assertThat(output).contains("Refreshing FeignClientFactory-test-with-config",
						"Refreshing FeignClientFactory-test");
				assertThat(output).doesNotContain("Instantiating bean from Test custom config",
						"Instantiating bean from default custom config");
				TestPropertyValues.of(AotDetector.AOT_ENABLED + "=true")
						.applyToSystemProperties(freshApplicationContext::refresh);
				assertThat(output).contains("Instantiating bean from Test custom config",
						"Instantiating bean from default custom config");
				assertThat(freshApplicationContext.getBean(TestFeignClient.class)).isNotNull();
				assertThat(freshApplicationContext.getBean(TestFeignClientWithConfig.class)).isNotNull();
			});
		});
	}

	static class TestTarget {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableFeignClients(clients = { TestFeignClient.class, TestFeignClientWithConfig.class },
			defaultConfiguration = DefaultConfiguration.class)
	public static class TestFeignConfiguration {

		@Autowired
		TestFeignClient testFeignClient;

		@Autowired
		TestFeignClientWithConfig testFeignClientWithConfig;

	}

	public static class TestConfiguration {

		@Bean
		TestBean testBean() {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Instantiating bean from Test custom config");
			}
			return new TestBean();
		}

	}

	public static class DefaultConfiguration {

		@Bean
		TestBean defaultTestBean() {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Instantiating bean from default custom config");
			}
			return new TestBean();
		}

	}

	public static class TestBean {

	}

	@FeignClient(value = "test", dismiss404 = true, url = "http://example.com")
	interface TestFeignClient {

		@GetMapping
		void test();

	}

	@FeignClient(value = "test-with-config", configuration = TestConfiguration.class)
	interface TestFeignClientWithConfig {

		@GetMapping
		void test();

	}

}
