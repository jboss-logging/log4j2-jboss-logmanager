/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2023 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.logmanager.log4j;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Map;

import org.apache.logging.log4j.spi.ThreadContextMap;
import org.jboss.logmanager.MDC;

/**
 * A {@link ThreadContextMap} implementation which delegates to {@link MDC}.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class ThreadContextMDCMap implements ThreadContextMap {
    private static final MethodHandle IS_EMPTY;

    static {
        // Use MDC.isEmpty() if it's available
        MethodHandle handle = null;
        try {
            handle = MethodHandles.publicLookup()
                    .findStatic(MDC.class, "isEmpty", MethodType.methodType(boolean.class));
        } catch (NoSuchMethodException | IllegalAccessException ignore) {
        }
        IS_EMPTY = handle;
    }

    @Override
    public void clear() {
        MDC.clear();
    }

    @Override
    public boolean containsKey(final String key) {
        return MDC.get(key) != null;
    }

    @Override
    public String get(final String key) {
        return MDC.get(key);
    }

    @Override
    public Map<String, String> getCopy() {
        return MDC.copy();
    }

    @Override
    public Map<String, String> getImmutableMapOrNull() {
        final Map<String, String> copy = MDC.copy();
        return copy.isEmpty() ? null : Map.copyOf(copy);
    }

    @Override
    public boolean isEmpty() {
        if (IS_EMPTY != null) {
            try {
                return (boolean) IS_EMPTY.invoke();
            } catch (Throwable ignore) {
                // Ignore and fall through to the fallback
            }
        }
        // Fallback to a simple copy/isEmpty. This will not perform well, but also is likely not used much
        return MDC.copy().isEmpty();
    }

    @Override
    public void put(final String key, final String value) {
        if (value == null) {
            MDC.remove(key);
        } else {
            MDC.put(key, value);
        }
    }

    @Override
    public void remove(final String key) {
        MDC.remove(key);
    }
}
