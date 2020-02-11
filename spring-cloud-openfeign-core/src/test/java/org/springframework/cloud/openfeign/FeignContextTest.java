package org.springframework.cloud.openfeign;

import feign.Logger;
import feign.RequestInterceptor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.BeansException;

import static org.junit.jupiter.api.Assertions.*;

@RunWith(MockitoJUnitRunner.class)
public class FeignContextTest {

	private FeignContext context = new FeignContext();

	@Test
	public void getInstanceWithoutAncestors() {
		context.getInstanceWithoutAncestors("context", Logger.Level.class);
	}

	@Test
	public void getInstancesWithoutAncestors() {
		context.getInstancesWithoutAncestors("context", RequestInterceptor.class);
	}
}
