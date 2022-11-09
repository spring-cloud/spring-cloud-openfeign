/*
 * Copyright 2013-2022 the original author or authors.
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

package org.springframework.cloud.openfeign.test;

// TODO: Bring back when there's corresponding HttpClient 5 support in Commons

//
// import java.io.IOException;
// import java.lang.reflect.Field;
// import java.util.concurrent.TimeUnit;
//
// import feign.Client;
// import feign.hc5.ApacheHttp5Client;
//
// import org.apache.hc.client5.http.classic.HttpClient;
// import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
// import org.apache.hc.client5.http.io.HttpClientConnectionManager;
// import org.apache.http.config.RegistryBuilder;
// import org.junit.jupiter.api.Test;
// import org.mockito.MockingDetails;
// import org.mockito.Mockito;
//
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.SpringBootConfiguration;
// import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
// import org.springframework.boot.test.context.SpringBootTest;
// import
// org.springframework.cloud.commons.httpclient.ApacheHttpClientConnectionManagerFactory;
// import org.springframework.cloud.commons.httpclient.ApacheHttpClientFactory;
// import
// org.springframework.cloud.commons.httpclient.DefaultApacheHttpClientConnectionManagerFactory;
// import org.springframework.cloud.commons.httpclient.DefaultApacheHttpClientFactory;
// import org.springframework.cloud.openfeign.EnableFeignClients;
// import org.springframework.cloud.openfeign.FeignClient;
// import
// org.springframework.cloud.openfeign.loadbalancer.FeignBlockingLoadBalancerClient;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.test.annotation.DirtiesContext;
// import org.springframework.util.ReflectionUtils;
//
// import static org.assertj.core.api.Assertions.assertThat;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.Mockito.doReturn;
// import static org.mockito.Mockito.mock;
// import static org.mockito.Mockito.mockingDetails;
//
/// **
// * @author Ryan Baxter
// * @author Olga Maciaszek-Sharma
// */
// @SpringBootTest(properties = { "spring.cloud.openfeign.okhttp.enabled: false",
// "spring.cloud.loadbalancer.retry.enabled=false" })
// @DirtiesContext
// class ApacheHttpClient5ConfigurationTests {
//
// @Autowired
// ApacheHttpClientConnectionManagerFactory connectionManagerFactory;
//
// @Autowired
// ApacheHttpClientFactory httpClientFactory;
//
// @Autowired
// FeignBlockingLoadBalancerClient feignClient;
//
// @Test
// void testFactories() {
// assertThat(connectionManagerFactory).isInstanceOf(ApacheHttpClientConnectionManagerFactory.class);
// assertThat(connectionManagerFactory)
// .isInstanceOf(ApacheHttpClientConfigurationTestApp.MyApacheHttpClientConnectionManagerFactory.class);
// assertThat(httpClientFactory).isInstanceOf(ApacheHttpClientFactory.class);
// assertThat(httpClientFactory)
// .isInstanceOf(ApacheHttpClientConfigurationTestApp.MyApacheHttpClientFactory.class);
// }
//
// @Test
// void testHttpClientWithFeign() {
// Client delegate = feignClient.getDelegate();
// assertThat(delegate instanceof ApacheHttp5Client).isTrue();
// ApacheHttp5Client apacheHttpClient = (ApacheHttp5Client) delegate;
// HttpClient httpClient = getField(apacheHttpClient, "client");
// MockingDetails httpClientDetails = mockingDetails(httpClient);
// assertThat(httpClientDetails.isMock()).isTrue();
// }
//
// @SuppressWarnings("unchecked")
// protected <T> T getField(Object target, String name) {
// Field field = ReflectionUtils.findField(target.getClass(), name);
// ReflectionUtils.makeAccessible(field);
// Object value = ReflectionUtils.getField(field, target);
// return (T) value;
// }
//
// @SpringBootConfiguration
// @EnableAutoConfiguration
// @EnableFeignClients(clients = { ApacheHttpClientConfigurationTestApp.FooClient.class })
// static class ApacheHttpClientConfigurationTestApp {
//
// @FeignClient(name = "foo")
// interface FooClient {
//
// }
//
// static class MyApacheHttpClientConnectionManagerFactory
// extends DefaultApacheHttpClientConnectionManagerFactory {
//
// @Override
// public HttpClientConnectionManager newConnectionManager(boolean disableSslValidation,
// int maxTotalConnections, int maxConnectionsPerRoute, long timeToLive, TimeUnit
// timeUnit,
// RegistryBuilder registry) {
// return mock(PoolingHttpClientConnectionManager.class);
// }
//
// }
//
// static class MyApacheHttpClientFactory extends DefaultApacheHttpClientFactory {
//
// MyApacheHttpClientFactory(HttpClientBuilder builder) {
// super(builder);
// }
//
// @Override
// public HttpClientBuilder createBuilder() {
// CloseableHttpClient client = mock(CloseableHttpClient.class);
// CloseableHttpResponse response = mock(CloseableHttpResponse.class);
// StatusLine statusLine = mock(StatusLine.class);
// doReturn(200).when(statusLine).getStatusCode();
// Mockito.doReturn(statusLine).when(response).getStatusLine();
// Header[] headers = new BasicHeader[0];
// doReturn(headers).when(response).getAllHeaders();
// try {
// Mockito.doReturn(response).when(client).execute(any(HttpUriRequest.class));
// }
// catch (IOException e) {
// e.printStackTrace();
// }
// HttpClientBuilder builder = mock(HttpClientBuilder.class);
// Mockito.doReturn(client).when(builder).build();
// return builder;
// }
//
// }
//
// @Configuration(proxyBeanMethods = false)
// static class MyConfig {
//
// @Bean
// public ApacheHttpClientFactory apacheHttpClientFactory(HttpClientBuilder builder) {
// return new MyApacheHttpClientFactory(builder);
// }
//
// @Bean
// public ApacheHttpClientConnectionManagerFactory connectionManagerFactory() {
// return new MyApacheHttpClientConnectionManagerFactory();
// }
//
// }
//
// }
//
// }
