package org.springframework.cloud.openfeign.reactive;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.cloud.openfeign.reactive.testcase.IcecreamServiceApi;
import org.springframework.cloud.openfeign.reactive.testcase.IcecreamServiceApiBroken;

import static org.hamcrest.Matchers.containsString;

/**
 * @author Sergii Karpenko
 */

abstract public class ContractTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    abstract protected <T> ReactiveFeign.Builder<T> builder();

    @Test
    public void shouldFailOnBrokenContract() {

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage(containsString("Broken Contract"));

        this.<IcecreamServiceApi>builder()
                .contract(targetType -> {throw new IllegalArgumentException("Broken Contract");})
                .target(IcecreamServiceApi.class, "http://localhost:8888");
    }

    @Test
    public void shouldFailIfNotReactiveContract() {

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage(containsString("IcecreamServiceApiBroken#findOrder(int)"));

        this.<IcecreamServiceApiBroken>builder()
                .target(IcecreamServiceApiBroken.class, "http://localhost:8888");
    }

}
