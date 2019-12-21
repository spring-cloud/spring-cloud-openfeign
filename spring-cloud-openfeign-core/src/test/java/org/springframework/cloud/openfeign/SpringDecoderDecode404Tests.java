package org.springframework.cloud.openfeign;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.openfeign.support.Decode404;
import org.springframework.cloud.openfeign.test.NoSecurityConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Matt King
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = SpringDecoderDecode404Tests.Application.class,
	webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, value = {
	"spring.application.name=springdecodertest", "spring.jmx.enabled=false" })
@DirtiesContext
public class SpringDecoderDecode404Tests extends FeignClientFactoryBean {

	@Autowired
	FeignContext context;

	@Value("${local.server.port}")
	private int port = 0;

	public SpringDecoderDecode404Tests() {
		setName("test");
		setContextId("test");
	}

	public TestClient testClient() {
		setType(this.getClass());
		return feign(this.context).target(TestClient.class,
			"http://localhost:" + this.port);
	}

	@Test
	public void testDecodes404() {
		final ResponseEntity<String> response = testClient().getNotFound();
		assertThat(response).as("response was null").isNotNull();
		assertThat(response.getBody()).as("response body was not null").isNull();
	}

	protected interface TestClient {
		@RequestMapping(method = RequestMethod.GET, value = "/hellonotfound")
		ResponseEntity<String> getNotFound();
	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	@RestController
	@Import(NoSecurityConfiguration.class)
	protected static class Application implements TestClient {

		@Bean
		@Primary
		public Decode404 feignDecode404() {
			return () -> true;
		}

		@Override
		public ResponseEntity<String> getNotFound() {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body((String) null);
		}
	}
}
