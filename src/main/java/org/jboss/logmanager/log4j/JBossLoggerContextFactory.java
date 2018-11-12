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

import java.net.URI;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

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
@SuppressWarnings({"SynchronizationOnLocalVariableOrMethodParameter", "Convert2Lambda"})
public class JBossLoggerContextFactory implements LoggerContextFactory {
    private static final Logger.AttachmentKey<Map<Object, LoggerContext>> CONTEXT_KEY = new Logger.AttachmentKey<>();

    @Override
    public LoggerContext getContext(final String fqcn, final ClassLoader loader, final Object externalContext, final boolean currentContext) {
        return getLoggerContext(loader, externalContext, currentContext);
    }

    @Override
    public LoggerContext getContext(final String fqcn, final ClassLoader loader, final Object externalContext, final boolean currentContext, final URI configLocation, final String name) {
        try {
            return getLoggerContext(loader, externalContext, currentContext);
        } finally {
            // Done in a finally block as the StatusLogger may not be configured until the call to getLoggerContext()
            if (configLocation != null) {
                StatusLogger.getLogger().warn("Configuration is not allowed for the JBoss Log Manager binding. Ignoring configuration file {}.", configLocation);
            }
        }
    }

    @Override
    public void removeContext(final LoggerContext context) {
        final LogContext logContext = LogContext.getLogContext();
        final Map<Object, LoggerContext> contexts = logContext.getAttachment("", CONTEXT_KEY);
        if (contexts != null) {
            synchronized (contexts) {
                final Iterator<LoggerContext> iter = contexts.values().iterator();
                while (iter.hasNext()) {
                    final LoggerContext c = iter.next();
                    if (c.equals(context)) {
                        iter.remove();
                        break;
                    }
                }
                if (contexts.isEmpty()) {
                    final Logger rootLogger = logContext.getLogger("");
                    rootLogger.detach(CONTEXT_KEY);
                    JBossStatusListener.remove(logContext);
                }
            }
        }
    }

    private LoggerContext getLoggerContext(final ClassLoader classLoader, final Object externalContext, final boolean currentContext) {
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
        final Logger rootLogger = logContext.getLogger("");
        Map<Object, LoggerContext> contexts = rootLogger.getAttachment(CONTEXT_KEY);
        if (contexts == null) {
            contexts = new HashMap<>();
            final Map<Object, LoggerContext> appearing = rootLogger.attachIfAbsent(CONTEXT_KEY, contexts);
            if (appearing != null) {
                contexts = appearing;
            }
        }
        synchronized (contexts) {
            JBossStatusListener.registerIfAbsent(logContext);
            return contexts.computeIfAbsent(externalContext, new Function<Object, LoggerContext>() {
                @Override
                public LoggerContext apply(final Object o) {
                    return new JBossLoggerContext(logContext, externalContext);
                }
            });
        }
    }

    private static ClassLoader getTccl() {
        if (System.getSecurityManager() == null) {
            return Thread.currentThread().getContextClassLoader();
        }
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            @Override
            public ClassLoader run() {
                return Thread.currentThread().getContextClassLoader();
            }
        });
    }

    private static void setTccl(final ClassLoader classLoader) {
        if (System.getSecurityManager() == null) {
            Thread.currentThread().setContextClassLoader(classLoader);
        } else {
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    Thread.currentThread().setContextClassLoader(classLoader);
                    return null;
                }
            });
        }
    }
}
