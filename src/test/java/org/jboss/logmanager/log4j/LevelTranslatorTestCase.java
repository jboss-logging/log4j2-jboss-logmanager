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

import org.apache.logging.log4j.Level;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class LevelTranslatorTestCase {

    private final LevelTranslator levelTranslator = LevelTranslator.getInstance();

    @Test
    public void testOff() {
        testLevel(Level.OFF, java.util.logging.Level.OFF);
    }

    @Test
    public void testFatal() {
        testLevel(Level.FATAL, org.jboss.logmanager.Level.FATAL);
    }

    @Test
    public void testError() {
        testLevel(Level.ERROR, org.jboss.logmanager.Level.ERROR);
        testLevel(Level.ERROR, java.util.logging.Level.SEVERE, org.jboss.logmanager.Level.ERROR);
    }

    @Test
    public void testWarn() {
        testLevel(Level.WARN, org.jboss.logmanager.Level.WARN);
        testLevel(Level.WARN, java.util.logging.Level.WARNING, org.jboss.logmanager.Level.WARN);
    }

    @Test
    public void testInfo() {
        testLevel(Level.INFO, org.jboss.logmanager.Level.INFO);
        testLevel(Level.INFO, java.util.logging.Level.INFO);
    }

    @Test
    public void testDebug() {
        testLevel(Level.DEBUG, org.jboss.logmanager.Level.DEBUG);
        testLevel(Level.DEBUG, java.util.logging.Level.FINE, org.jboss.logmanager.Level.DEBUG);
        testLevel(Level.DEBUG, java.util.logging.Level.CONFIG, org.jboss.logmanager.Level.DEBUG);
    }

    @Test
    public void testTrace() {
        testLevel(Level.TRACE, org.jboss.logmanager.Level.TRACE);
        testLevel(Level.TRACE, java.util.logging.Level.FINER, org.jboss.logmanager.Level.TRACE);
        testLevel(Level.TRACE, java.util.logging.Level.FINEST, org.jboss.logmanager.Level.TRACE);
    }

    @Test
    public void testAll() {
        testLevel(Level.ALL, java.util.logging.Level.ALL);
    }

    @Test
    public void testNull() {
        Assert.assertEquals("Expected null log4j level to map to INFO",
                org.jboss.logmanager.Level.DEBUG, levelTranslator.translateLevel((Level) null));
        Assert.assertEquals("Expected null JUL level to map to INFO",
                Level.DEBUG, levelTranslator.translateLevel((java.util.logging.Level) null));
        Assert.assertEquals("Expected a -1 effective level to map to INFO",
                Level.DEBUG, levelTranslator.translateLevel(-1));
    }

    private void testLevel(final Level log4jLevel, final java.util.logging.Level julLevel) {
        testLevel(log4jLevel, julLevel, julLevel);
    }

    private void testLevel(final Level log4jLevel, final java.util.logging.Level julLevel, final java.util.logging.Level expectedJulLevel) {
        Assert.assertEquals(String.format("Expected log4j level %s to equal JUL level %s", log4jLevel, julLevel),
                log4jLevel, levelTranslator.translateLevel(julLevel));
        Assert.assertEquals(String.format("Expected JUL level %s to equal log4j level %s", julLevel, log4jLevel),
                expectedJulLevel, levelTranslator.translateLevel(log4jLevel));
    }
}
