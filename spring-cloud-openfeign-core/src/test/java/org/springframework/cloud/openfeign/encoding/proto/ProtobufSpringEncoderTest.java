/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.openfeign.encoding.proto;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.protobuf.InvalidProtocolBufferException;
import feign.RequestTemplate;
import feign.httpclient.ApacheHttpClient;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.http.converter.protobuf.ProtobufHttpMessageConverter;

import static feign.Request.Body.encoded;
import static feign.Request.HttpMethod.POST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Test {@link SpringEncoder} with {@link ProtobufHttpMessageConverter}
 *
 * @author ScienJus
 */
@RunWith(MockitoJUnitRunner.class)
public class ProtobufSpringEncoderTest {

	@Mock
	private HttpClient httpClient;

	// a protobuf object with some content
	private org.springframework.cloud.openfeign.encoding.proto.Request request = org.springframework.cloud.openfeign.encoding.proto.Request
			.newBuilder().setId(1000000).setMsg("Erlang/OTP 最初是爱立信为开发电信设备系统设计的编程语言平台，"
					+ "电信设备(路由器、接入网关、…)典型设计是通过背板连接主控板卡与多块业务板卡的分布式系统。")
			.build();

	@Test
	public void testProtobuf() throws IOException, URISyntaxException {
		// protobuf convert to request by feign and ProtobufHttpMessageConverter
		RequestTemplate requestTemplate = newRequestTemplate();
		newEncoder().encode(this.request, Request.class, requestTemplate);
		HttpEntity entity = toApacheHttpEntity(requestTemplate);
		byte[] bytes = read(entity.getContent(), (int) entity.getContentLength());

		assertThat(this.request.toByteArray()).isEqualTo(bytes);
		org.springframework.cloud.openfeign.encoding.proto.Request copy = org.springframework.cloud.openfeign.encoding.proto.Request
				.parseFrom(bytes);
		assertThat(copy).isEqualTo(this.request);
	}

	@Test
	public void testProtobufWithCharsetWillFail() throws IOException, URISyntaxException {
		// protobuf convert to request by feign and ProtobufHttpMessageConverter
		RequestTemplate requestTemplate = newRequestTemplate();
		newEncoder().encode(this.request, Request.class, requestTemplate);
		// set a charset
		requestTemplate.body(
				encoded(requestTemplate.requestBody().asBytes(), StandardCharsets.UTF_8));
		HttpEntity entity = toApacheHttpEntity(requestTemplate);
		byte[] bytes = read(entity.getContent(), (int) entity.getContentLength());

		// http request-body is different with original protobuf body
		assertThat(this.request.toByteArray().length).isNotEqualTo(bytes.length);
		try {
			org.springframework.cloud.openfeign.encoding.proto.Request copy = org.springframework.cloud.openfeign.encoding.proto.Request
					.parseFrom(bytes);
			fail("Expected an InvalidProtocolBufferException to be thrown");
		}
		catch (InvalidProtocolBufferException e) {
			// success
		}
	}

	private SpringEncoder newEncoder() {
		ObjectFactory<HttpMessageConverters> converters = new ObjectFactory<HttpMessageConverters>() {
			@Override
			public HttpMessageConverters getObject() throws BeansException {
				return new HttpMessageConverters(new ProtobufHttpMessageConverter());
			}
		};
		return new SpringEncoder(converters);
	}

	private RequestTemplate newRequestTemplate() {
		RequestTemplate requestTemplate = new RequestTemplate();
		requestTemplate.method(POST);
		return requestTemplate;
	}

	private HttpEntity toApacheHttpEntity(RequestTemplate requestTemplate)
			throws IOException, URISyntaxException {
		final List<HttpUriRequest> request = new ArrayList<>(1);
		BDDMockito.given(this.httpClient.execute(ArgumentMatchers.<HttpUriRequest>any()))
				.will(new Answer<HttpResponse>() {
					@Override
					public HttpResponse answer(InvocationOnMock invocationOnMock)
							throws Throwable {
						request.add((HttpUriRequest) invocationOnMock.getArguments()[0]);
						return new BasicHttpResponse(new BasicStatusLine(
								new ProtocolVersion("http", 1, 1), 200, null));
					}
				});
		new ApacheHttpClient(this.httpClient).execute(
				requestTemplate.resolve(new HashMap<>()).request(),
				new feign.Request.Options());
		HttpUriRequest httpUriRequest = request.get(0);
		return ((HttpEntityEnclosingRequestBase) httpUriRequest).getEntity();
	}

	private byte[] read(InputStream in, int length) throws IOException {
		byte[] bytes = new byte[length];
		in.read(bytes);
		return bytes;
	}

}
