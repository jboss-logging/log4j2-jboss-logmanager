/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2016 Red Hat, Inc., and individual contributors
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

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.spi.LoggerContext;
import org.apache.logging.log4j.spi.LoggerRegistry;
import org.jboss.logmanager.LogContext;

/**
 * Represents a {@link LoggerContext} backed by a {@link LogContext}.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class JBossLoggerContext implements LoggerContext {
    private final LogContext logContext;
    private final Object externalContext;
    private final LoggerRegistry<JBossLogger> loggerRegistry = new LoggerRegistry<>();
    private final ConcurrentMap<String, Object> map = new ConcurrentHashMap<>();

    /**
     * Creates a new logger context.
     *
     * @param logContext      the JBoss Log Manager context to use
     * @param externalContext the external context provided
     */
    JBossLoggerContext(final LogContext logContext, final Object externalContext) {
        this.logContext = logContext;
        this.externalContext = externalContext;
    }

    @Override
    public Object getExternalContext() {
        return externalContext;
    }

    @Override
    public ExtendedLogger getLogger(final String name) {
        return getLogger(name, null);
    }

    @Override
    public ExtendedLogger getLogger(final String name, final MessageFactory messageFactory) {
        JBossLogger logger = loggerRegistry.getLogger(name, messageFactory);
        if (logger != null) {
            AbstractLogger.checkMessageFactory(logger, messageFactory);
            return logger;
        }
        logger = new JBossLogger(logContext.getLogger(name), messageFactory);
        loggerRegistry.putIfAbsent(name, messageFactory, logger);
        return loggerRegistry.getLogger(name, messageFactory);
    }

    @Override
    public boolean hasLogger(final String name) {
        return loggerRegistry.hasLogger(name);
    }

    @Override
    public boolean hasLogger(final String name, final MessageFactory messageFactory) {
        return loggerRegistry.hasLogger(name, messageFactory);
    }

    @Override
    public boolean hasLogger(final String name, final Class<? extends MessageFactory> messageFactoryClass) {
        return loggerRegistry.hasLogger(name, messageFactoryClass);
    }

    @Override
    public Object getObject(final String key) {
        return map.get(key);
    }

    @Override
    public Object putObject(final String key, final Object value) {
        return map.put(key, value);
    }

    @Override
    public Object putObjectIfAbsent(final String key, final Object value) {
        return map.putIfAbsent(key, value);
    }

    @Override
    public Object removeObject(final String key) {
        return map.remove(key);
    }

    @Override
    public boolean removeObject(final String key, final Object value) {
        return map.remove(key, value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(logContext, loggerRegistry, externalContext);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof JBossLoggerContext)) {
            return false;
        }
        final JBossLoggerContext other = (JBossLoggerContext) obj;
        return Objects.equals(logContext, other.logContext) && Objects.equals(loggerRegistry, other.loggerRegistry)
                && Objects.equals(externalContext, other.externalContext);
    }

    /**
     * Returns the JBoss Log Manager log context associated with the log4j logger context.
     *
     * @return the JBoss Log Manager log context
     */
    LogContext getLogContext() {
        return logContext;
    }
}
