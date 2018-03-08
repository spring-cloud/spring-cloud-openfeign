/*
 * Copyright 2013-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.openfeign;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import feign.slf4j.Slf4jLogger;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.SocketUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.util.UrlPathHelper;

import java.util.Objects;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/**
 * Test path variables
 * @author Dominique Villard
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = FeignHttpClientUrlPathTests.TestAppControllerAndConfig.class, webEnvironment = WebEnvironment.DEFINED_PORT, value = {
		"spring.application.name=feignclienturltest",
		"feign.hystrix.enabled=false",
		"feign.okhttp.enabled=false",
		"feign.client.decodeslash=false",
		// spring must be tweaked to accept encoded / (%2F), withdraw security for test
		"spring.security.filter.dispatcher-types=async,error"})
@DirtiesContext
public class FeignHttpClientUrlPathTests {

	static int port;

	@BeforeClass
	public static void beforeClass() {
		port = SocketUtils.findAvailableTcpPort();
		System.setProperty("server.port", String.valueOf(port));
		// tweak servlet container to support
		System.setProperty("org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH", "true");
	}

	@AfterClass
	public static void afterClass() {
		System.clearProperty("server.port");
		System.clearProperty("org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH");
	}

	@Autowired
	private UrlClient urlClient;

	@Before
	public void setUp() throws Exception {
		assertNotNull("UrlClient was null", this.urlClient);
		Logger.class.cast(LoggerFactory.getLogger(UrlClient.class)).setLevel(Level.DEBUG);
	}

	@FeignClient(name = "localappurl", url = "http://localhost:${server.port}/", configuration = ClientTestConfiguration.class)
	protected interface UrlClient {

		@GetMapping(value = "/hello/{user}")
		Hello getHelloUser(@PathVariable("user") String user);
	}

	@Configuration
	@EnableAutoConfiguration
	@RestController
	//@EnableWebSecurity(debug=true)
	@EnableFeignClients(clients = {UrlClient.class})
	protected static class TestAppControllerAndConfig {

		@RequestMapping(method = RequestMethod.GET, value = "/hello/{user}")
		public Hello getHelloUser(@PathVariable("user") String user) {
			return new Hello("hello "+user);
		}

		@Bean
		public UrlPathHelper mvcUrlPathHelper() {
			UrlPathHelper pathHelper = new UrlPathHelper();
			pathHelper.setUrlDecode(false);
			pathHelper.setDefaultEncoding("UTF-8");
			return pathHelper;
		}

		// didn't find a better way to configure it
		@Bean
		public Optional<HandlerMapping> configureHandlerMapping(HandlerMapping handlerMapping) {
			if (handlerMapping instanceof AbstractHandlerMapping) {
				AbstractHandlerMapping.class.cast(handlerMapping).setUrlDecode(false);
			}
			return Optional.ofNullable(handlerMapping);
		}
	}

	@Test
	public void testPathVariable() {
		Hello hello = this.urlClient.getHelloUser("toto");
		assertEquals("first hello didn't match", new Hello("hello toto"), hello);
	}

	@Test
	public void testEscapedPathVariable() {
		Hello hello = this.urlClient.getHelloUser("toto/titi");
		assertEquals("first hello didn't match", new Hello("hello toto/titi"), hello);
	}

	@Configuration
	public static class ClientTestConfiguration {

		@Bean
		public Targeter feignTargeter() {
			return new DefaultTargeter();
		}

		@Bean
		feign.Logger.Level feignLoggerLevel() {
			return feign.Logger.Level.FULL;
		}

		@Bean
		public feign.Logger logger() {
			return new Slf4jLogger(UrlClient.class);
		}
	}

	public static class Hello {
		private String message;

		public Hello() {
		}

		public Hello(String message) {
			this.message = message;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Hello that = (Hello) o;
			return Objects.equals(message, that.message);
		}

		@Override
		public int hashCode() {
			return Objects.hash(message);
		}
	}
}
