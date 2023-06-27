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

import java.net.URI;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.spi.LoggerContext;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.apache.logging.log4j.status.StatusLogger;
import org.jboss.logmanager.LogContext;
import org.jboss.logmanager.Logger;

/**
 * A context factory backed by JBoss Log Manager.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class JBossLoggerContextFactory implements LoggerContextFactory {
    private static final Logger.AttachmentKey<Map<Object, LoggerContext>> CONTEXT_KEY = new Logger.AttachmentKey<>();
    private static final String ROOT_LOGGER_NAME = "";
    private final ReentrantLock lock = new ReentrantLock();

    @Override
    public LoggerContext getContext(final String fqcn, final ClassLoader loader, final Object externalContext,
            final boolean currentContext) {
        return getLoggerContext(loader, externalContext, currentContext);
    }

    @Override
    public LoggerContext getContext(final String fqcn, final ClassLoader loader, final Object externalContext,
            final boolean currentContext, final URI configLocation, final String name) {
        try {
            return getLoggerContext(loader, externalContext, currentContext);
        } finally {
            // Done in a finally block as the StatusLogger may not be configured until the call to getLoggerContext()
            if (configLocation != null) {
                StatusLogger.getLogger().warn(
                        "Configuration is not allowed for the JBoss Log Manager binding. Ignoring configuration file {}.",
                        configLocation);
            }
        }
    }

    @Override
    public void removeContext(final LoggerContext context) {
        // Check the context type and if it's not a JBossLoggerContext there is nothing for us to do.
        if (context instanceof JBossLoggerContext) {
            final LogContext logContext = ((JBossLoggerContext) context).getLogContext();
            lock.lock();
            try {
                final Map<Object, LoggerContext> contexts = logContext.getAttachment(ROOT_LOGGER_NAME, CONTEXT_KEY);
                if (contexts != null) {
                    final Iterator<LoggerContext> iter = contexts.values().iterator();
                    while (iter.hasNext()) {
                        final LoggerContext c = iter.next();
                        if (c.equals(context)) {
                            iter.remove();
                            break;
                        }
                    }
                    if (contexts.isEmpty()) {
                        final Logger rootLogger = logContext.getLogger(ROOT_LOGGER_NAME);
                        detach(rootLogger);
                        JBossStatusListener.remove(logContext);
                    }
                }
            } finally {
                lock.unlock();
            }
        }
    }

    private LoggerContext getLoggerContext(final ClassLoader classLoader, final Object externalContext,
            final boolean currentContext) {
        if (currentContext || classLoader == null) {
            return getOrCreateLoggerContext(LogContext.getLogContext(), externalContext);
        }
        final ClassLoader current = getTccl();
        try {
            setTccl(classLoader);
            return getOrCreateLoggerContext(LogContext.getLogContext(), externalContext);
        } finally {
            setTccl(current);
        }
    }

    private LoggerContext getOrCreateLoggerContext(final LogContext logContext, final Object externalContext) {
        final Logger rootLogger = logContext.getLogger(ROOT_LOGGER_NAME);
        lock.lock();
        try {
            Map<Object, LoggerContext> contexts = rootLogger.getAttachment(CONTEXT_KEY);
            if (contexts == null) {
                contexts = new HashMap<>();
                attach(rootLogger, contexts);
            }
            JBossStatusListener.registerIfAbsent(logContext);
            return contexts.computeIfAbsent(externalContext, o -> new JBossLoggerContext(logContext, externalContext));
        } finally {
            lock.unlock();
        }
    }

    private static void attach(final Logger logger, final Map<Object, LoggerContext> value) {
        if (System.getSecurityManager() == null) {
            logger.attach(CONTEXT_KEY, value);
        } else {
            AccessController
                    .doPrivileged((PrivilegedAction<Map<Object, LoggerContext>>) () -> logger.attach(CONTEXT_KEY, value));
        }
    }

    private static void detach(final Logger logger) {
        if (System.getSecurityManager() == null) {
            logger.detach(CONTEXT_KEY);
        } else {
            AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
                logger.detach(CONTEXT_KEY);
                return null;
            });
        }
    }

    private static ClassLoader getTccl() {
        if (System.getSecurityManager() == null) {
            return Thread.currentThread().getContextClassLoader();
        }
        return AccessController
                .doPrivileged((PrivilegedAction<ClassLoader>) () -> Thread.currentThread().getContextClassLoader());
    }

    private static void setTccl(final ClassLoader classLoader) {
        if (System.getSecurityManager() == null) {
            Thread.currentThread().setContextClassLoader(classLoader);
        } else {
            AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
                Thread.currentThread().setContextClassLoader(classLoader);
                return null;
            });
        }
    }
}
