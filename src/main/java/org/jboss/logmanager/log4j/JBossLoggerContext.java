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

import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.spi.LoggerContext;
import org.jboss.logmanager.LogContext;
import org.jboss.logmanager.Logger;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class JBossLoggerContext implements LoggerContext {
    private static final Logger.AttachmentKey<ExtendedLogger> LOGGER_KEY = new Logger.AttachmentKey<>();
    private final LogContext logContext;
    private final Object externalContext;

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
        final LogContext context = logContext;
        ExtendedLogger result = context.getAttachment(name, LOGGER_KEY);
        if (result == null) {
            final Logger lmLogger = context.getLogger(name);
            result = new JBossLogger(lmLogger);
            final ExtendedLogger appearing = lmLogger.attachIfAbsent(LOGGER_KEY, result);
            if (appearing != null) {
                result = appearing;
            }
        }
        return result;
    }

    @Override
    public ExtendedLogger getLogger(final String name, final MessageFactory messageFactory) {
        final LogContext context = logContext;
        ExtendedLogger result = context.getAttachment(name, LOGGER_KEY);
        if (result == null) {
            final Logger lmLogger = context.getLogger(name);
            result = new JBossLogger(lmLogger, messageFactory);
            final ExtendedLogger appearing = lmLogger.attachIfAbsent(LOGGER_KEY, result);
            if (appearing != null) {
                result = appearing;
            }
        }
        return result;
    }

    @Override
    public boolean hasLogger(final String name) {
        return logContext.getAttachment(name, LOGGER_KEY) != null;
    }

    @Override
    public boolean hasLogger(final String name, final MessageFactory messageFactory) {
        return hasLogger(name) && getLogger(name).getMessageFactory().getClass() == messageFactory.getClass();
    }

    @Override
    public boolean hasLogger(final String name, final Class<? extends MessageFactory> messageFactoryClass) {
        return hasLogger(name) && getLogger(name).getMessageFactory().getClass() == messageFactoryClass;
    }
}
