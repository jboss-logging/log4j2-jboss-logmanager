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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.status.StatusListener;
import org.apache.logging.log4j.status.StatusLogger;
import org.jboss.logmanager.LogContext;
import org.jboss.logmanager.Logger;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class JBossStatusListener implements StatusListener {
    private static final String NAME = "org.jboss.logmanager.log4j.status";
    private static final Logger.AttachmentKey<StatusListener> STATUS_LISTENER_KEY = new Logger.AttachmentKey<>();
    private final Logger logger;
    private final LevelTranslator levelTranslator = LevelTranslator.getInstance();
    private final Level level;

    private JBossStatusListener(final Logger logger) {
        this.logger = logger;
        level = StatusLogger.getLogger().getLevel();
    }

    static void registerIfAbsent(final LogContext logContext) {
        final Logger logger = logContext.getLogger(NAME);
        StatusListener listener = logger.getAttachment(STATUS_LISTENER_KEY);
        if (listener == null) {
            listener = new JBossStatusListener(logger);
            if (logger.attachIfAbsent(STATUS_LISTENER_KEY, listener) == null) {
                StatusLogger.getLogger().registerListener(listener);
            }
        }
    }

    static void remove(final LogContext logContext) {
        final Logger logger = logContext.getLogger(NAME);
        logger.detach(STATUS_LISTENER_KEY);
    }

    @Override
    public void log(final StatusData data) {
        logger.log(
                levelTranslator.translateLevel(data.getLevel()),
                data.getMessage().getFormattedMessage(),
                data.getThrowable()
        );
    }

    @Override
    public Level getStatusLevel() {
        return level;
    }

    @Override
    public void close() {
        logger.detach(STATUS_LISTENER_KEY);
    }

}
