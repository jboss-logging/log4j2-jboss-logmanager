/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2017 Red Hat, Inc., and individual contributors
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.jboss.logmanager.ExtHandler;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.formatters.PatternFormatter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class ThreadContextMapTestCase {
    private final org.jboss.logmanager.Logger jbossRootLogger = org.jboss.logmanager.Logger.getLogger("");

    @Before
    public void setup() {
        jbossRootLogger.clearHandlers();
    }

    @Test
    public void testPut() throws Exception {
        final String key = "test.key";
        final TestHandler handler = new TestHandler(key);
        jbossRootLogger.addHandler(handler);
        ThreadContext.put(key, "test value");

        final Logger logger = LogManager.getLogger();

        logger.info("Test message");

        Assert.assertEquals("test value", handler.pollLastValue());

        ThreadContext.remove(key);

        logger.info("Test message");
        Assert.assertEquals("", handler.pollLastValue());
    }

    @Test
    public void test() throws Exception {
        final String key1 = "test.key.1";
        final String key2 = "test.key.2";
        final String key3 = "test.key.3";

        ThreadContext.put(key1, "test value 1");
        ThreadContext.put(key2, "test value 2");
        ThreadContext.put(key3, "test value 3");

        Assert.assertFalse("The context should not be empty.", ThreadContext.isEmpty());

        ThreadContext.trim(2);
        Assert.assertEquals(2, ThreadContext.getDepth());
    }

    private static class TestHandler extends ExtHandler {
        private final Deque<String> collected = new ArrayDeque<>();

        protected TestHandler(final String propertyName) {
            setFormatter(new PatternFormatter("%X{" + propertyName + "}"));
        }

        @Override
        protected void doPublish(final ExtLogRecord record) {
            final Formatter formatter = getFormatter();
            collected.addLast(formatter.format(record));
        }

        String pollLastValue() {
            return collected.pollLast();
        }
    }

}
