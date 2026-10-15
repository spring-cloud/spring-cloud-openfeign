/*
 * Copyright 2013-present the original author or authors.
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

package org.springframework.cloud.openfeign.test.autoconfigure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.OverrideAutoConfiguration;
import org.springframework.boot.test.context.filter.annotation.TypeExcludeFilters;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.env.Environment;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Annotation for a test that focuses <strong>only</strong> on the
 * {@link FeignClient @FeignClient} interfaces of an application.
 * <p>
 * Using this annotation only enables the auto-configuration that is relevant to Feign
 * clients: the Feign, HTTP message converter and JSON auto-configuration. Similarly,
 * component scanning is limited to the configuration classes annotated with
 * {@link EnableFeignClients @EnableFeignClients} and to the components that customize
 * Feign clients, such as {@code RequestInterceptor}, {@code ErrorDecoder} or
 * {@code FeignBuilderCustomizer} components, as well as {@code JacksonModule} and
 * {@code @JacksonComponent} beans. The {@code @Bean} methods of other configuration
 * classes are not loaded, so the beans they declare need an
 * {@link org.springframework.context.annotation.Import @Import}. The auto-configuration
 * of circuit breakers, Micrometer and caching is not enabled either.
 * <p>
 * The Feign clients are the ones that the {@code @EnableFeignClients} annotation of the
 * application registers. A test points a client at a stub server, such as WireMock, with
 * the {@code spring.cloud.openfeign.client.config.<name>.url} property, or with the
 * property that the {@code url} attribute of its {@code @FeignClient} reads, since that
 * attribute takes precedence. A client is only created when it is injected, so the
 * clients that a test does not use need no configuration.
 * <p>
 * For more fine-grained control, the {@link AutoConfigureFeign @AutoConfigureFeign}
 * annotation adds the same auto-configuration to another test slice.
 *
 * @author Sharang Gupta
 * @since 5.1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@BootstrapWith(FeignClientTestContextBootstrapper.class)
@ExtendWith(SpringExtension.class)
@OverrideAutoConfiguration(enabled = false)
@TypeExcludeFilters(FeignClientTypeExcludeFilter.class)
@AutoConfigureFeign
@ImportAutoConfiguration
public @interface FeignClientTest {

	/**
	 * Properties in form {@literal key=value} that should be added to the Spring
	 * {@link Environment} before the test runs.
	 * @return the properties to add
	 */
	String[] properties() default {};

	/**
	 * Determines if default filtering should be used with
	 * {@link SpringBootApplication @SpringBootApplication}. By default, only the
	 * configuration classes annotated with {@code @EnableFeignClients} and the beans that
	 * customize Feign clients are included.
	 * @see #includeFilters()
	 * @see #excludeFilters()
	 * @return if default filters should be used
	 */
	boolean useDefaultFilters() default true;

	/**
	 * A set of include filters which can be used to add otherwise filtered beans to the
	 * application context.
	 * @return include filters to apply
	 */
	ComponentScan.Filter[] includeFilters() default {};

	/**
	 * A set of exclude filters which can be used to filter beans that would otherwise be
	 * added to the application context.
	 * @return exclude filters to apply
	 */
	ComponentScan.Filter[] excludeFilters() default {};

	/**
	 * Auto-configuration exclusions that should be applied for this test.
	 * @return auto-configuration exclusions to apply
	 */
	@AliasFor(annotation = ImportAutoConfiguration.class, attribute = "exclude")
	Class<?>[] excludeAutoConfiguration() default {};

}
