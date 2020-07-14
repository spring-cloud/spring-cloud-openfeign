package org.springframework.cloud.openfeign.beans;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * @author Olga Maciaszek-Sharma
 */
@SpringBootTest(classes = FeignClientMockBeanTests.Config.class)
public class FeignClientMockBeanTests {

	@MockBean
	private RandomClient randomClient;

	@Autowired
	private TestService testService;

	@Test
	public void randomClientShouldBeMocked() {
		String mockMessage = "Mocked Feign Client";
		when(randomClient.getRandomString()).thenReturn(mockMessage);

		String returnedMessage = testService.testMethod();

		assertThat(returnedMessage).isEqualTo(mockMessage);
	}

	@FeignClient(value = "random-test")
	protected interface RandomClient {

		@GetMapping(value = "/random-test")
		String getRandomString();

	}

	@Configuration
	protected static class Config {

		@Bean
		TestService testService() {
			return new TestService();
		}

	}

}

class TestService {

	@Autowired
	private FeignClientMockBeanTests.RandomClient randomClient;

	public String testMethod() {
		return randomClient.getRandomString();
	}
}
