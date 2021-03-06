/*
 * Copyright 2013-2014 SmartBear Software
 * Copyright 2014-2015 The TestFX Contributors
 *
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence"); You may
 * not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 * http://ec.europa.eu/idabc/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the Licence for the
 * specific language governing permissions and limitations under the Licence.
 */
package org.testfx.util;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class WaitForAsyncUtilsTest {

    // ---------------------------------------------------------------------------------------------
    // FIELDS.
    // ---------------------------------------------------------------------------------------------

    @Rule
    public ExpectedException thrown = ExpectedException.none();


    @Rule
    public Timeout globalTimeout = Timeout.millis(1000);

    // ---------------------------------------------------------------------------------------------
    // FEATURE METHODS.
    // ---------------------------------------------------------------------------------------------

    @Test
    public void async_callable() throws Exception {
        // when:
        Future<String> future = WaitForAsyncUtils.async(() -> "foo");

        // then:
        WaitForAsyncUtils.sleepWithException(10, MILLISECONDS);
        assertThat(future.get(), Matchers.is("foo"));
        waitForThreads(future);
    }

    @Test
    public void async_callable_with_sleep() throws Exception {
        // when:
        Future<String> future = WaitForAsyncUtils.async(() -> {
            WaitForAsyncUtils.sleepWithException(50, MILLISECONDS);
            return "foo";
        });

        // then:
        assertThat(future.isDone(), Matchers.is(false));
        WaitForAsyncUtils.sleep(100, MILLISECONDS);
        assertThat(future.get(), Matchers.is("foo"));
        waitForThreads(future);
    }

    @Test
    public void async_callable_with_exception() throws Throwable {
        // given:
        WaitForAsyncUtils.printException = false;
        thrown.expect(UnsupportedOperationException.class);
        Callable<Void> callable = () -> {
            throw new UnsupportedOperationException();
        };
        WaitForAsyncUtils.clearExceptions();

        // when:
        Future<Void> future = WaitForAsyncUtils.async(callable);

        // then:
        waitForException(future);
        try {
            WaitForAsyncUtils.checkException();
            fail("checkException didn't detect Exception");
        }
        catch (Throwable e) {
            if (!(e.getCause() instanceof UnsupportedOperationException)) {
                throw e;
            }
        }
        WaitForAsyncUtils.waitFor(50, MILLISECONDS, future);
        waitForThreads(future);
    }

    @Test
    public void clearExceptionsTest() throws Throwable {
        // given:
        WaitForAsyncUtils.printException = false;
        Callable<Void> callable = () -> {
            throw new UnsupportedOperationException();
        };
        WaitForAsyncUtils.clearExceptions();

        // when:
        Future<Void> future = WaitForAsyncUtils.async(callable);

        // then:
        waitForException(future);
        WaitForAsyncUtils.clearExceptions();
        WaitForAsyncUtils.checkException();
        waitForThreads(future);
    }

    @Test
    public void autoCheckExceptionTest() throws Throwable {
        // given:
        WaitForAsyncUtils.printException = false;
        WaitForAsyncUtils.autoCheckException = true;
        Callable<Void> callable = () -> {
            throw new UnsupportedOperationException();
        };
        WaitForAsyncUtils.clearExceptions();

        // when:
        Future<Void> future = WaitForAsyncUtils.async(callable);
        waitForThreads(future);

        // then:
        try {
            future = WaitForAsyncUtils.async(callable);
            fail("No exception thrown by autoCheck");
        }
        catch (Throwable e) {
            if (!(e instanceof UnsupportedOperationException)) {
                throw e;
            }
        }

        WaitForAsyncUtils.clearExceptions();
        waitForThreads(future);
    }

    @Test
    public void unhandledExceptionTest() throws Throwable {
        // given:
        WaitForAsyncUtils.printException = false;
        Callable<Void> callable = () -> {
            throw new NullPointerException();
        };
        WaitForAsyncUtils.clearExceptions();

        // when:
        Future<Void> future = WaitForAsyncUtils.async(callable);
        try {
            WaitForAsyncUtils.waitFor(50, MILLISECONDS, future);
        }
        catch (Throwable ignore) {
        }

        // then:
        WaitForAsyncUtils.printException = true;
        try {
            WaitForAsyncUtils.checkException();
        }
        catch (Throwable e) {
            fail("Handled exception not removed from stack");
        }
        WaitForAsyncUtils.clearExceptions();
        waitForThreads(future);
    }

    @Test
    public void waitFor_with_future() throws Exception {
        // when:
        Future<Void> future = WaitForAsyncUtils.async(() -> null);

        // then:
        WaitForAsyncUtils.waitFor(50, MILLISECONDS, future);
        waitForThreads(future);
    }

    @Test
    public void waitFor_with_future_with_sleep() throws Exception {
        // when:
        Future<Void> future = WaitForAsyncUtils.async(() -> {
            WaitForAsyncUtils.sleepWithException(100, MILLISECONDS);
            return null;
        });

        // then:
        thrown.expect(TimeoutException.class);
        WaitForAsyncUtils.waitFor(50, MILLISECONDS, future);
        waitForThreads(future);
    }

    @Test
    public void waitFor_with_future_cancelled() throws Throwable {
        // given:
        WaitForAsyncUtils.printException = false;

        // when:
        Future<Void> future = WaitForAsyncUtils.async(() -> {
            WaitForAsyncUtils.sleepWithException(100, MILLISECONDS);
            return null;
        });
        future.cancel(true);
        waitForThreads(future);
        try {
            Thread.sleep(50);
        }
        catch (Exception ignore) {
        }

        // then:
        try {
            WaitForAsyncUtils.checkException();
            fail("checkException didn't detect Exception");
        }
        catch (Throwable e) {
            if (!(e instanceof CancellationException)) {
                throw e;
            }
        }
        thrown.expect(CancellationException.class);
        WaitForAsyncUtils.waitFor(50, MILLISECONDS, future);
    }

    @Test
    public void waitFor_with_booleanCallable() throws Exception {
        // expect:
        WaitForAsyncUtils.waitFor(250, MILLISECONDS, () -> true);
    }

    @Test
    public void waitFor_with_booleanCallable_with_sleep() throws Exception {
        // expect:
        WaitForAsyncUtils.waitFor(250, MILLISECONDS, () -> {
            WaitForAsyncUtils.sleepWithException(50, MILLISECONDS);
            return true;
        });
    }

    @Test
    public void waitFor_with_booleanCallable_with_false() throws Exception {
        // expect:
        thrown.expect(TimeoutException.class);
        WaitForAsyncUtils.waitFor(250, MILLISECONDS, () -> false);
    }

    @Test
    public void waitFor_with_booleanCallable_with_exception() throws Exception {
        // expect:
        thrown.expectCause(instanceOf(UnsupportedOperationException.class));
        WaitForAsyncUtils.waitFor(250, MILLISECONDS, () -> {
            throw new UnsupportedOperationException();
        });
    }

    @Test
    public void waitFor_with_booleanValue() throws Exception {
        // given:
        BooleanProperty property = new SimpleBooleanProperty(false);

        // when:
        WaitForAsyncUtils.async(() -> {
            WaitForAsyncUtils.sleepWithException(50, MILLISECONDS);
            property.set(true);
            return null;
        });

        // then:
        WaitForAsyncUtils.waitFor(250, MILLISECONDS, property);
    }

    @Test
    public void waitFor_with_booleanValue_with_false() throws Exception {
        // given:
        BooleanProperty property = new SimpleBooleanProperty(false);

        // when:
        WaitForAsyncUtils.async(() -> {
            WaitForAsyncUtils.sleepWithException(50, MILLISECONDS);
            property.set(false);
            return null;
        });

        // then:
        thrown.expect(TimeoutException.class);
        WaitForAsyncUtils.waitFor(250, MILLISECONDS, property);
    }

    protected void waitForException(Future<?> f) throws InterruptedException {
        Thread.sleep(50);
        assertTrue(f.isDone());
    }

    protected void waitForThreads(Future<?> f) {
        while (!f.isDone()) {
            try {
                Thread.sleep(1);
            }
            catch (Exception ignore) {
            }
        }
        try {
            Thread.sleep(50);
        }
        catch (Exception ignore) {
        }
    }

}
