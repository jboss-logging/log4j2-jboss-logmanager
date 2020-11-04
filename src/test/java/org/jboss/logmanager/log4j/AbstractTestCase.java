/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2018 Red Hat, Inc., and individual contributors
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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.logging.Formatter;

import org.jboss.logmanager.ExtHandler;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.LogContext;
import org.jboss.logmanager.LogContextSelector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class AbstractTestCase {

    private static class TestLogContextSelector implements LogContextSelector {
        private final LogContext logContext;

        private TestLogContextSelector(final LogContext logContext) {
            this.logContext = logContext;
        }

        @Override
        public LogContext getLogContext() {
            return logContext;
        }
    }

    @BeforeEach
    public void logContextSetup() {
        LogContext.setLogContextSelector(new TestLogContextSelector(LogContext.create()));
    }

    @AfterEach
    public void resetLogContext() {
        LogContext.setLogContextSelector(LogContext.DEFAULT_LOG_CONTEXT_SELECTOR);
    }

    /**
     * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
     */
    static class TestQueueHandler extends ExtHandler {
        private final Deque<ExtLogRecord> collected = new ArrayDeque<>();

        TestQueueHandler() {
        }

        TestQueueHandler(final Formatter formatter) {
            setFormatter(formatter);
        }

        @Override
        protected void doPublish(final ExtLogRecord record) {
            collected.addLast(record);
        }

        ExtLogRecord poll() {
            return collected.pollLast();
        }

        String pollFormatted() {
            final ExtLogRecord record = poll();
            if (record == null) {
                return null;
            }
            final Formatter formatter = getFormatter();
            if (formatter == null) {
                return record.getFormattedMessage();
            }
            return formatter.format(record);
        }
    }
}
