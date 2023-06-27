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
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.spi.LoggerContext;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.status.StatusListener;
import org.apache.logging.log4j.status.StatusLogger;
import org.jboss.logmanager.ExtLogRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class StatusLoggerTestCase extends AbstractTestCase {
    private TestQueueHandler handler;
    private org.jboss.logmanager.Logger lmLogger;
    private StatusLogger statusLogger;

    @BeforeEach
    public void setup() {
        lmLogger = org.jboss.logmanager.Logger.getLogger("");
        final TestQueueHandler handler = new TestQueueHandler();
        lmLogger.addHandler(handler);
        this.handler = handler;
        // Required to initialize the JBossStatusListener
        LogManager.getContext();
        statusLogger = StatusLogger.getLogger();
        statusLogger.setLevel(Level.WARN);
        statusLogger.updateListenerLevel(Level.WARN);
        statusLogger.clear();
    }

    @AfterEach
    public void tearDown() {
        handler.close();
        lmLogger.removeHandler(handler);
        statusLogger.clear();
    }

    @Test
    public void testListenerAttached() {
        boolean found = false;
        for (StatusListener listener : statusLogger.getListeners()) {
            if (listener.getClass().equals(JBossStatusListener.class)) {
                found = true;
                break;
            }
        }
        Assertions.assertTrue(found,
                "Expected to find " + JBossStatusListener.class.getName() + " registered: " + statusLogger.getListeners());
    }

    @Test
    public void testError() {
        // Log an error which should show up on the handler
        statusLogger.error("Test status message");
        checkEmpty(false);
        final ExtLogRecord record = handler.poll();
        Assertions.assertNotNull(record);
        Assertions.assertEquals("Test status message", record.getFormattedMessage());
    }

    @Test
    public void testLevelChange() {
        // Log a warning message which should be ignored
        statusLogger.info("Test info message 1");
        checkEmpty(true);

        // Set the level to warn and log another message
        //statusLogger.setLevel(Level.INFO);
        statusLogger.updateListenerLevel(Level.INFO);
        statusLogger.info("Test info message 2");
        checkEmpty(false);
        final List<StatusData> statusData = statusLogger.getStatusData();
        Assertions.assertEquals("Test info message 2", statusData.get(0).getMessage().getFormattedMessage());
    }

    @Test
    public void testConfiguration() throws Exception {
        final URI config = LoggerContextTestCase.class.getResource("/log4j2.xml").toURI();
        final LoggerContext loggerContext = LogManager.getContext(LoggerContextTestCase.class.getClassLoader(), true, config);
        Assertions.assertNotNull(loggerContext);
        // The status logger should contain a message
        checkEmpty(false);
        final List<StatusData> statusData = statusLogger.getStatusData();
        final String foundMsg = statusData.get(0).getMessage().getFormattedMessage();
        Assertions.assertTrue(foundMsg.contains(config.toString()),
                String.format("Expected the log message to contain %s. Found %s", config, foundMsg));
    }

    private void checkEmpty(final boolean expectEmpty) {
        if (statusLogger.getStatusData().isEmpty() != expectEmpty) {
            final StringBuilder msg = new StringBuilder("Expect the data to ");
            if (expectEmpty) {
                msg.append("be empty, found:")
                        .append(System.lineSeparator());
                final Iterator<StatusData> iter = statusLogger.getStatusData().iterator();
                while (iter.hasNext()) {
                    msg.append(iter.next().getFormattedStatus());
                    if (iter.hasNext()) {
                        msg.append(System.lineSeparator());
                    }
                }
            } else {
                msg.append("not be empty");
            }
            Assertions.fail(msg.toString());
        }
    }
}
