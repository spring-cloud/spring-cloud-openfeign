package org.springframework.cloud.openfeign.hystrix.security.app;

import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy;

import java.util.concurrent.Callable;

public class CustomConcurrenyStrategy extends HystrixConcurrencyStrategy {
	private boolean hookCalled;

	@Override
	public <T> Callable<T> wrapCallable(Callable<T> callable) {
		this.hookCalled = true;

		return super.wrapCallable(callable);
	}

	public boolean isHookCalled() {
		return hookCalled;
	}
}
