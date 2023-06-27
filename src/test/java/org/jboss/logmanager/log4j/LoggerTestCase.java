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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.UUID;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.message.AbstractMessageFactory;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.MessageFactory2;
import org.jboss.logmanager.ExtLogRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class LoggerTestCase extends AbstractTestCase {

    private final String loggerName = LoggerTestCase.class.getPackage().getName();
    private final Marker marker = MarkerManager.getMarker("test");
    private TestQueueHandler handler;
    private org.jboss.logmanager.Logger lmLogger;

    @BeforeEach
    public void setup() {
        lmLogger = org.jboss.logmanager.Logger.getLogger("org.jboss.logmanager.log4j");
        lmLogger.setLevel(java.util.logging.Level.INFO);
        final TestQueueHandler handler = new TestQueueHandler();
        lmLogger.addHandler(handler);
        this.handler = handler;
    }

    @AfterEach
    public void tearDown() {
        handler.close();
        lmLogger.removeHandler(handler);
    }

    @Test
    public void testNamedLogger() {
        final Logger logger = LogManager.getLogger(loggerName);
        logger.info("Test message");
        ExtLogRecord record = handler.poll();
        Assertions.assertNotNull(record);
        Assertions.assertEquals("Test message", record.getMessage());

        logger.info("Test message parameter {}", 1);
        record = handler.poll();
        Assertions.assertNotNull(record);
        Assertions.assertEquals("Test message parameter 1", record.getMessage());
    }

    @SuppressWarnings("PlaceholderCountMatchesArgumentCount")
    @Test
    public void testNamedFormatterLogger() {
        final Logger logger = LogManager.getFormatterLogger(loggerName);
        logger.info("Test message parameter %s", 1);
        final ExtLogRecord record = handler.poll();
        Assertions.assertNotNull(record);
        Assertions.assertEquals("Test message parameter 1", record.getMessage());
    }

    @Test
    public void testCurrentClassLogger() {
        final String expectedName = LoggerTestCase.class.getName();
        final Logger logger = LogManager.getLogger();
        Assertions.assertEquals(expectedName, logger.getName());
        logger.info("Test message");
        final ExtLogRecord record = handler.poll();
        Assertions.assertNotNull(record);
        Assertions.assertEquals("Test message", record.getMessage());
        Assertions.assertEquals(expectedName, record.getLoggerName());
    }

    @Test
    public void testObjectLogger() {
        final String expectedName = LoggerTestCase.class.getName();
        final Logger logger = LogManager.getLogger(this);
        Assertions.assertEquals(expectedName, logger.getName());
        logger.info("Test message");
        final ExtLogRecord record = handler.poll();
        Assertions.assertNotNull(record);
        Assertions.assertEquals("Test message", record.getMessage());
        Assertions.assertEquals(expectedName, record.getLoggerName());
    }

    @Test
    public void testCurrentClassMessageFactoryLogger() {
        final String prefix = generatePrefix();
        final String expectedName = LoggerTestCase.class.getName();
        final MessageFactory messageFactory = new TestMessageFactory(prefix);
        final Logger logger = LogManager.getLogger(messageFactory);
        Assertions.assertEquals(expectedName, logger.getName());
        logger.info("Test message");
        final ExtLogRecord record = handler.poll();
        Assertions.assertNotNull(record);
        Assertions.assertEquals(prefix + "Test message", record.getMessage());
        Assertions.assertEquals(expectedName, record.getLoggerName());
    }

    @Test
    public void testObjectMessageFactoryLogger() {
        final String prefix = generatePrefix();
        final String expectedName = LoggerTestCase.class.getName();
        final MessageFactory messageFactory = new TestMessageFactory(prefix);
        final Logger logger = LogManager.getLogger(this, messageFactory);
        Assertions.assertEquals(expectedName, logger.getName());
        logger.info("Test message");
        final ExtLogRecord record = handler.poll();
        Assertions.assertNotNull(record);
        Assertions.assertEquals(prefix + "Test message", record.getMessage());
        Assertions.assertEquals(expectedName, record.getLoggerName());
    }

    @Test
    public void testNamedMessageFactoryLogger() {
        final String prefix = generatePrefix();
        final String expectedName = loggerName;
        final MessageFactory messageFactory = new TestMessageFactory(prefix);
        final Logger logger = LogManager.getLogger(loggerName, messageFactory);
        Assertions.assertEquals(expectedName, logger.getName());
        logger.info("Test message");
        final ExtLogRecord record = handler.poll();
        Assertions.assertNotNull(record);
        Assertions.assertEquals(prefix + "Test message", record.getMessage());
        Assertions.assertEquals(expectedName, record.getLoggerName());
    }

    @Test
    public void tesTypeMessageFactoryLogger() {
        final String prefix = generatePrefix();
        final String expectedName = LoggerTestCase.class.getName();
        final MessageFactory messageFactory = new TestMessageFactory(prefix);
        final Logger logger = LogManager.getLogger(LoggerTestCase.class, messageFactory);
        Assertions.assertEquals(expectedName, logger.getName());
        logger.info("Test message");
        final ExtLogRecord record = handler.poll();
        Assertions.assertNotNull(record);
        Assertions.assertEquals(prefix + "Test message", record.getMessage());
        Assertions.assertEquals(expectedName, record.getLoggerName());
    }

    @Test
    public void testAllEnabled() {
        lmLogger.setLevel(org.jboss.logmanager.Level.ALL);
        testLevelEnabled(LogManager.getLogger(loggerName), Level.ALL);
        testLevelEnabled(LogManager.getFormatterLogger(loggerName), Level.ALL);
    }

    @Test
    public void testTraceEnabled() {
        lmLogger.setLevel(org.jboss.logmanager.Level.TRACE);
        testLevelEnabled(LogManager.getLogger(loggerName), Level.TRACE);
        testLevelEnabled(LogManager.getFormatterLogger(loggerName), Level.TRACE);
    }

    @Test
    public void testDebugEnabled() {
        lmLogger.setLevel(org.jboss.logmanager.Level.DEBUG);
        testLevelEnabled(LogManager.getLogger(loggerName), Level.DEBUG);
        testLevelEnabled(LogManager.getFormatterLogger(loggerName), Level.DEBUG);
    }

    @Test
    public void testInfoEnabled() {
        testLevelEnabled(LogManager.getFormatterLogger(loggerName), Level.INFO);
    }

    @Test
    public void testWarnEnabled() {
        testLevelEnabled(LogManager.getLogger(loggerName), Level.WARN);
        testLevelEnabled(LogManager.getFormatterLogger(loggerName), Level.WARN);
    }

    @Test
    public void testErrorEnabled() {
        testLevelEnabled(LogManager.getLogger(loggerName), Level.ERROR);
        testLevelEnabled(LogManager.getFormatterLogger(loggerName), Level.ERROR);
    }

    @Test
    public void testFatalEnabled() {
        testLevelEnabled(LogManager.getLogger(loggerName), Level.FATAL);
        testLevelEnabled(LogManager.getFormatterLogger(loggerName), Level.FATAL);
    }

    @Test
    public void testOffEnabled() {
        testLevelEnabled(LogManager.getLogger(loggerName), Level.OFF);
        testLevelEnabled(LogManager.getFormatterLogger(loggerName), Level.OFF);
    }

    private void testLevelEnabled(final Logger logger, final Level level) {
        final String msg = String.format("Expected level %s to be enabled on logger %s", level, logger);
        final String markerMsg = String.format("Expected level %s to be enabled on logger %s with marker %s", level, logger,
                marker);
        Assertions.assertTrue(logger.isEnabled(level), msg);
        Assertions.assertTrue(logger.isEnabled(level, marker), markerMsg);
        if (level.equals(Level.FATAL)) {
            Assertions.assertTrue(logger.isFatalEnabled(), msg);
            Assertions.assertTrue(logger.isFatalEnabled(marker), markerMsg);
        } else if (level.equals(Level.ERROR)) {
            Assertions.assertTrue(logger.isErrorEnabled(), msg);
            Assertions.assertTrue(logger.isErrorEnabled(marker), markerMsg);
        } else if (level.equals(Level.WARN)) {
            Assertions.assertTrue(logger.isWarnEnabled(), msg);
            Assertions.assertTrue(logger.isWarnEnabled(marker), markerMsg);
        } else if (level.equals(Level.INFO)) {
            Assertions.assertTrue(logger.isInfoEnabled(), msg);
            Assertions.assertTrue(logger.isInfoEnabled(marker), markerMsg);
        } else if (level.equals(Level.DEBUG)) {
            Assertions.assertTrue(logger.isDebugEnabled(), msg);
            Assertions.assertTrue(logger.isDebugEnabled(marker), markerMsg);
        } else if (level.equals(Level.TRACE)) {
            Assertions.assertTrue(logger.isTraceEnabled(), msg);
            Assertions.assertTrue(logger.isTraceEnabled(marker), markerMsg);
        }
    }

    private static String generatePrefix() {
        return "[" + UUID.randomUUID().toString() + "] ";
    }

    private static class TestMessageFactory extends AbstractMessageFactory implements MessageFactory2 {
        private final String prefix;

        private TestMessageFactory(final String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Message newMessage(final CharSequence charSequence) {
            return newMessage(String.valueOf(charSequence));
        }

        /*
         * (non-Javadoc)
         *
         * @see org.apache.logging.log4j.message.MessageFactory#newMessage(java.lang.Object)
         */
        @Override
        public Message newMessage(final Object message) {
            return newMessage(String.valueOf(message));
        }

        /*
         * (non-Javadoc)
         *
         * @see org.apache.logging.log4j.message.MessageFactory#newMessage(java.lang.String)
         */
        @Override
        public Message newMessage(final String message) {
            return new TestMessage(prefix, null, message);
        }

        @Override
        public Message newMessage(final String message, final Object... params) {
            return new TestMessage(prefix, null, message, params);
        }
    }

    private static class TestMessage implements Message {
        private final String format;
        private final Object[] params;
        private final Throwable cause;

        TestMessage(final String prefix, final Throwable cause, final String format, final Object... params) {
            this.format = prefix == null ? "" : prefix + format;
            this.params = params == null ? new Object[0] : Arrays.copyOf(params, params.length);
            this.cause = cause;
        }

        @Override
        public String getFormattedMessage() {
            if (format == null) {
                return null;
            }
            if (cause == null) {
                return String.format(format, params);
            }
            final StringWriter writer = new StringWriter();
            writer.append(String.format(format, params));
            writer.write(System.lineSeparator());
            cause.printStackTrace(new PrintWriter(writer));
            return writer.toString();
        }

        @Override
        public String getFormat() {
            return format;
        }

        @Override
        public Object[] getParameters() {
            return params;
        }

        @Override
        public Throwable getThrowable() {
            return cause;
        }
    }
}
