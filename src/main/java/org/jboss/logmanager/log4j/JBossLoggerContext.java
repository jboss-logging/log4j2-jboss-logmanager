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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.spi.LoggerContext;
import org.jboss.logmanager.LogContext;
import org.jboss.logmanager.Logger;

/**
 * Represents a {@link LoggerContext} backed by a {@link LogContext}.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@SuppressWarnings({"Convert2Lambda", "SynchronizationOnLocalVariableOrMethodParameter"})
class JBossLoggerContext implements LoggerContext {
    private static final Logger.AttachmentKey<Map<Key, ExtendedLogger>> LOGGER_KEY = new Logger.AttachmentKey<>();
    private final LogContext logContext;
    private final Object externalContext;

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
        return getOrCreateLogger(new Key(name, null));
    }

    @Override
    public ExtendedLogger getLogger(final String name, final MessageFactory messageFactory) {
        return getOrCreateLogger(new Key(name, messageFactory));
    }

    @Override
    public boolean hasLogger(final String name) {
        return logContext.getAttachment(name, LOGGER_KEY) != null;
    }

    @Override
    public boolean hasLogger(final String name, final MessageFactory messageFactory) {
        return hasLogger(name, messageFactory == null ? null : messageFactory.getClass());
    }

    @Override
    public boolean hasLogger(final String name, final Class<? extends MessageFactory> messageFactoryClass) {
        final Map<Key, ExtendedLogger> loggers = logContext.getAttachment(name, LOGGER_KEY);
        if (loggers == null) {
            return false;
        }
        synchronized (loggers) {
            for (Key key : loggers.keySet()) {
                if (messageFactoryClass == null && key.messageFactory == null) {
                    return true;
                } else if (key.messageFactory != null && key.messageFactory.getClass() == messageFactoryClass) {
                    return true;
                }
            }
        }
        return false;
    }

    private ExtendedLogger getOrCreateLogger(final Key key) {
        final Map<Key, ExtendedLogger> loggers = getLoggers(logContext, key.name);
        synchronized (loggers) {
            return loggers.computeIfAbsent(key, new Function<Key, ExtendedLogger>() {
                @Override
                public ExtendedLogger apply(final Key key) {
                    if (key.messageFactory == null) {
                        return new JBossLogger(logContext.getLogger(key.name));
                    }
                    return new JBossLogger(logContext.getLogger(key.name), key.messageFactory);
                }
            });
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(logContext, externalContext);
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
        return Objects.equals(logContext, other.logContext) && Objects.equals(externalContext, other.externalContext);
    }

    private static Map<Key, ExtendedLogger> getLoggers(final LogContext context, final String name) {
        Map<Key, ExtendedLogger> result = context.getAttachment(name, LOGGER_KEY);
        if (result == null) {
            final Logger lmLogger = context.getLogger(name);
            result = new HashMap<>();
            final Map<Key, ExtendedLogger> appearing = lmLogger.attachIfAbsent(LOGGER_KEY, result);
            if (appearing != null) {
                result = appearing;
            }
        }
        return result;
    }

    private static class Key {
        final String name;
        final MessageFactory messageFactory;

        private Key(final String name, final MessageFactory messageFactory) {
            this.name = name;
            this.messageFactory = messageFactory;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, messageFactory);
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof Key)) {
                return false;
            }
            final Key other = (Key) obj;
            return Objects.equals(name, other.name) && Objects.equals(messageFactory, other.messageFactory);
        }

        @Override
        public String toString() {
            return "Key(name=" + name + ", messageFactory=" + messageFactory + ")";
        }
    }
}
