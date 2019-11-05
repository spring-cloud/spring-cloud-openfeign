package org.springframework.cloud.openfeign.loadbalancer;

import java.util.Map;

import feign.Client;
import feign.httpclient.ApacheHttpClient;
import feign.okhttp.OkHttpClient;
import org.junit.jupiter.api.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.commons.httpclient.HttpClientConfiguration;
import org.springframework.cloud.loadbalancer.blocking.client.BlockingLoadBalancerClient;
import org.springframework.cloud.loadbalancer.config.BlockingLoadBalancerClientAutoConfiguration;
import org.springframework.cloud.loadbalancer.config.LoadBalancerAutoConfiguration;
import org.springframework.cloud.netflix.ribbon.RibbonAutoConfiguration;
import org.springframework.cloud.openfeign.ribbon.FeignRibbonClientAutoConfiguration;
import org.springframework.cloud.openfeign.ribbon.LoadBalancerFeignClient;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Olga Maciaszek-Sharma
 */
class FeignLoadBalancerAutoConfigurationTests {

	@Test
	void shouldInstantiateDefaultFeignBlockingLoadBalancerClient() {
		ConfigurableApplicationContext context = new SpringApplicationBuilder()
			.web(WebApplicationType.NONE)
			.properties("spring.cloud.loadbalancer.ribbon.enabled=false",
				"feign.httpclient.enabled=false")
			.sources(HttpClientConfiguration.class,
				LoadBalancerAutoConfiguration.class,
				BlockingLoadBalancerClientAutoConfiguration.class,
				FeignRibbonClientAutoConfiguration.class,
				FeignLoadBalancerAutoConfiguration.class, WebClientAutoConfiguration.class)
			.run();
		assertThatOneBeanPresent(context, BlockingLoadBalancerClient.class);
		assertLoadBalanced(context, Client.Default.class);
		assertThatBeanNotPresent(context, LoadBalancerFeignClient.class);
	}

	@Test
	void shouldInstantiateHttpFeignClientWhenEnabled() {
		ConfigurableApplicationContext context = new SpringApplicationBuilder()
			.web(WebApplicationType.NONE)
			.properties("spring.cloud.loadbalancer.ribbon.enabled=false")
			.sources(HttpClientConfiguration.class,
				LoadBalancerAutoConfiguration.class,
				BlockingLoadBalancerClientAutoConfiguration.class,
				FeignRibbonClientAutoConfiguration.class,
				FeignLoadBalancerAutoConfiguration.class, WebClientAutoConfiguration.class)
			.run();
		assertThatOneBeanPresent(context, BlockingLoadBalancerClient.class);
		assertLoadBalanced(context, ApacheHttpClient.class);
		assertThatBeanNotPresent(context, LoadBalancerFeignClient.class);
	}

	@Test
	void shouldInstantiateOkHttpFeignClientWhenEnabled() {
		ConfigurableApplicationContext context = new SpringApplicationBuilder()
			.web(WebApplicationType.NONE)
			.properties("spring.cloud.loadbalancer.ribbon.enabled=false",
				"feign.httpclient.enabled=false",
				"feign.okhttp.enabled=true")
			.sources(HttpClientConfiguration.class,
				LoadBalancerAutoConfiguration.class,
				BlockingLoadBalancerClientAutoConfiguration.class,
				FeignRibbonClientAutoConfiguration.class,
				FeignLoadBalancerAutoConfiguration.class, WebClientAutoConfiguration.class)
			.run();
		assertThatOneBeanPresent(context, BlockingLoadBalancerClient.class);
		assertLoadBalanced(context, OkHttpClient.class);
		assertThatBeanNotPresent(context, LoadBalancerFeignClient.class);
	}

	@Test
	void shouldNotProcessLoadBalancerConfigurationWhenRibbonEnabled() {
		ConfigurableApplicationContext context = new SpringApplicationBuilder()
			.web(WebApplicationType.NONE)
			.properties("spring.cloud.loadbalancer.ribbon.enabled=true")
			.sources(HttpClientConfiguration.class,
				RibbonAutoConfiguration.class,
				LoadBalancerAutoConfiguration.class,
				BlockingLoadBalancerClientAutoConfiguration.class,
				FeignRibbonClientAutoConfiguration.class,
				FeignLoadBalancerAutoConfiguration.class, WebClientAutoConfiguration.class)
			.run();
		assertThatOneBeanPresent(context, LoadBalancerFeignClient.class);
		assertThatBeanNotPresent(context, BlockingLoadBalancerClient.class);
		assertThatBeanNotPresent(context, FeignBlockingLoadBalancerClient.class);
	}

	private void assertThatOneBeanPresent(ConfigurableApplicationContext context, Class beanClass) {
		Map<String, Object> beans = context.getBeansOfType(beanClass);
		assertThat(beans).hasSize(1);
	}


	private void assertLoadBalanced(ConfigurableApplicationContext context, Class delegateClass) {
		Map<String, FeignBlockingLoadBalancerClient> beans = context
			.getBeansOfType(FeignBlockingLoadBalancerClient.class);
		assertThat(beans).hasSize(1);
		assertThat(beans.get("feignClient").getDelegate())
			.isInstanceOf(delegateClass);
	}

	private void assertThatBeanNotPresent(ConfigurableApplicationContext context, Class beanClass) {
		Map<String, Object> beans = context.getBeansOfType(beanClass);
		assertThat(beans).isEmpty();
	}

}
