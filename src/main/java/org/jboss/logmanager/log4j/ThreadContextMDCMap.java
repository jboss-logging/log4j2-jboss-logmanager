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

import java.util.Map;

import org.apache.logging.log4j.spi.ThreadContextMap;
import org.jboss.logmanager.MDC;

/**
 * A {@link ThreadContextMap} implementation which delegates to {@link MDC}.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class ThreadContextMDCMap implements ThreadContextMap {
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
        return copy.isEmpty() ? null : copy;
    }

    @Override
    public boolean isEmpty() {
        // Performance here is not great, but we need to use it as MDC.isEmpty() is not available in the log manager
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
