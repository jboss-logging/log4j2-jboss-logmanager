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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.logmanager.ExtHandler;
import org.jboss.logmanager.ExtLogRecord;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class LoggerTestCase {

    private QueueHandler handler;
    private org.jboss.logmanager.Logger lmLogger;

    @Before
    public void setup() {
        lmLogger = org.jboss.logmanager.Logger.getLogger("org.jboss.logmanager.log4j");
        final QueueHandler handler = new QueueHandler();
        lmLogger.addHandler(handler);
        this.handler = handler;
    }

    @After
    public void tearDown() {
        handler.close();
        lmLogger.removeHandler(handler);
    }

    @Test
    public void testLogger() throws Exception {
        final Logger logger = LogManager.getLogger(LoggerTestCase.class);
        logger.info("Test message");
        final ExtLogRecord record = handler.queue.poll(1, TimeUnit.SECONDS);
        Assert.assertNotNull(record);
        Assert.assertEquals("Test message", record.getMessage());
    }

    private static class QueueHandler extends ExtHandler {
        private final BlockingQueue<ExtLogRecord> queue = new LinkedBlockingDeque<>();

        @Override
        protected void doPublish(final ExtLogRecord record) {
            queue.add(record);
            super.doPublish(record);
        }

        @Override
        public void close() {
            queue.clear();
            super.close();
        }
    }
}
