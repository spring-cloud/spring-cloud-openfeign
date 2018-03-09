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
import feign.Feign;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.httpclient.ApacheHttpClient;
import feign.slf4j.Slf4jLogger;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicStatusLine;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.ByteArrayInputStream;
import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * Test path variables
 *
 * @author Dominique Villard
 */
@RunWith(SpringJUnit4ClassRunner.class)
@Import({FeignAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class, FeignClientsConfiguration.class})
@EnableFeignClients(clients = {FeignHttpClientUrlPath2Tests.UrlClient.class})
@DirtiesContext
public class FeignHttpClientUrlPathTests {

	private UrlClient urlClient;

	@Mock
	private HttpClient clientMock;

	@Autowired
	private Feign.Builder builder;

	@Autowired
	private Encoder encoder;

	@Autowired
	private Decoder decoder;

	@Captor
	private ArgumentCaptor<HttpUriRequest> argCaptor;

	@Before
	public void setUp() throws Exception {

		urlClient = builder
				.encoder(encoder)
				.decoder(decoder)
				.client(new ApacheHttpClient(clientMock))
				.logger(new Slf4jLogger(UrlClient.class))
				.logLevel(feign.Logger.Level.FULL)
				.contract(new SpringMvcContract(false))
				.target(UrlClient.class, "http://localhost:9876");

		StatusLine stline = new BasicStatusLine(new ProtocolVersion("http", 1, 1), 200, "OK");

		BasicHttpEntity entity = new BasicHttpEntity();
		entity.setContentType("Content-Type: application/json");
		entity.setContent(new ByteArrayInputStream("{}".getBytes()));

		Header[] headers = {
				new BasicHeader("Content-Type", "application/json;charset=UTF-8")
		};

		HttpResponse response = Mockito.mock(HttpResponse.class);
		when(response.getStatusLine()).thenReturn(stline);
		when(response.getAllHeaders()).thenReturn(headers);
		when(response.getEntity()).thenReturn(entity);

		when(clientMock.execute(any(HttpUriRequest.class))).thenReturn(response);
	}

	@Test
	public void testPathVariable() throws Exception {

		Hello hello = this.urlClient.getHelloUser("toto");
		assertNotNull("hello was null", hello);

		verify(clientMock).execute(argCaptor.capture());

		assertEquals("/hello/toto", argCaptor.getValue().getURI().getRawPath());
	}

	@Test
	public void testEscapedPathVariable() throws Exception {
		Logger.class.cast(LoggerFactory.getLogger(UrlClient.class)).setLevel(Level.DEBUG);

		Hello hello = this.urlClient.getHelloUser("toto/titi");
		assertNotNull("hello was null", hello);

		verify(clientMock).execute(argCaptor.capture());

		assertEquals("/hello/toto%2Ftiti", argCaptor.getValue().getURI().getRawPath());
	}

	public interface UrlClient {

		@GetMapping(value = "/hello/{user}")
		Hello getHelloUser(@PathVariable("user") String user);
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

		@Override
		public String toString() {
			return message;
		}
	}
}
