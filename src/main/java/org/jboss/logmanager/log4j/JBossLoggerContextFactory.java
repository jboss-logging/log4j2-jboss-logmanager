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

import org.apache.logging.log4j.spi.LoggerContext;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.apache.logging.log4j.status.StatusListener;
import org.apache.logging.log4j.status.StatusLogger;
import org.jboss.logmanager.LogContext;
import org.jboss.logmanager.Logger;

/**
 * A context factory backed by JBoss Log Manager.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
// TODO (jrp) what happens with attachments when two class loaders try to use the same logger name?
public class JBossLoggerContextFactory implements LoggerContextFactory {
    private static final Logger.AttachmentKey<LoggerContextFactory> CONTEXT_FACTORY_KEY = new Logger.AttachmentKey<>();
    private static final Logger.AttachmentKey<LoggerContext> CONTEXT_KEY = new Logger.AttachmentKey<>();
    private static final Logger.AttachmentKey<StatusListener> LISTENER_KEY = new Logger.AttachmentKey<>();

    static {
        // Configure a StatusListener on the system log context
        configureStatusListener(LogContext.getSystemLogContext());
    }

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
        // TODO (jrp) we may need to consider not actually removing the context if this is the LogContext.getSystemLogContext()
        final LogContext logContext = LogContext.getLogContext();
        final LoggerContext currentContext = logContext.getAttachment("", CONTEXT_KEY);
        if (currentContext != null && currentContext.equals(context)) {
            final Logger rootLogger = logContext.getLogger("");
            rootLogger.detach(CONTEXT_FACTORY_KEY);
            rootLogger.detach(CONTEXT_KEY);
            rootLogger.detach(LISTENER_KEY);
        }
    }

    private static LoggerContext getLoggerContext(final ClassLoader classLoader, final Object externalContext, final boolean currentContext) {
        if (currentContext || classLoader == null) {
            return getLoggerContext(LogContext.getLogContext(), externalContext);
        }
        final ClassLoader current = getTccl();
        try {
            setTccl(classLoader);
            return getLoggerContext(LogContext.getLogContext(), externalContext);
        } finally {
            setTccl(current);
        }
    }

    private static LoggerContext getLoggerContext(final LogContext context, final Object externalContext) {
        final Logger rootLogger = context.getLogger("");
        LoggerContext result = rootLogger.getAttachment(CONTEXT_KEY);
        if (result == null) {
            result = new JBossLoggerContext(context, externalContext);
            final LoggerContext appearing = rootLogger.attachIfAbsent(CONTEXT_KEY, result);
            if (appearing != null) {
                result = appearing;
            }
            configureStatusListener(context);
        }
        return result;
    }

    private static void configureStatusListener(final LogContext context) {
        final Logger rootLogger = context.getLogger("");
        // Setup a listener to write status messages to
        StatusListener listener = rootLogger.getAttachment(LISTENER_KEY);
        if (listener == null) {
            listener = new JBossStatusListener();
            final StatusListener appearingListener = rootLogger.attachIfAbsent(LISTENER_KEY, listener);
            if (appearingListener != null) {
                listener = appearingListener;
            }
            StatusLogger.getLogger().registerListener(listener);
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
