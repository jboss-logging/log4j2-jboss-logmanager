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

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Level;

/**
 * A utility to translate levels.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class LevelTranslator {
    private static final Level DEFAULT_LOG4J_LEVEL = Level.DEBUG;
    private static final org.jboss.logmanager.Level DEFAULT_LEVEL = org.jboss.logmanager.Level.DEBUG;
    private final Map<Integer, Level> julToLog4j = new HashMap<>();
    private final Map<Integer, java.util.logging.Level> log4jToJul = new HashMap<>();

    private static class Holder {
        static final LevelTranslator INSTANCE = new LevelTranslator();
    }

    private LevelTranslator() {
        // Add JUL levels
        julToLog4j.put(java.util.logging.Level.FINEST.intValue(), Level.TRACE);
        // This has a intValue() of 700 which is really between INFO and DEBUG, we'll default to DEBUG
        julToLog4j.put(java.util.logging.Level.CONFIG.intValue(), Level.DEBUG);

        // Note these should be added last to override any values that match
        julToLog4j.put(org.jboss.logmanager.Level.ALL.intValue(), Level.ALL);
        julToLog4j.put(org.jboss.logmanager.Level.TRACE.intValue(), Level.TRACE);
        julToLog4j.put(org.jboss.logmanager.Level.DEBUG.intValue(), Level.DEBUG);
        julToLog4j.put(org.jboss.logmanager.Level.INFO.intValue(), Level.INFO);
        julToLog4j.put(org.jboss.logmanager.Level.WARN.intValue(), Level.WARN);
        julToLog4j.put(org.jboss.logmanager.Level.ERROR.intValue(), Level.ERROR);
        julToLog4j.put(org.jboss.logmanager.Level.FATAL.intValue(), Level.FATAL);
        julToLog4j.put(org.jboss.logmanager.Level.OFF.intValue(), Level.OFF);

        log4jToJul.put(Level.ALL.intLevel(), org.jboss.logmanager.Level.ALL);
        log4jToJul.put(Level.TRACE.intLevel(), org.jboss.logmanager.Level.TRACE);
        log4jToJul.put(Level.DEBUG.intLevel(), org.jboss.logmanager.Level.DEBUG);
        log4jToJul.put(Level.INFO.intLevel(), org.jboss.logmanager.Level.INFO);
        log4jToJul.put(Level.WARN.intLevel(), org.jboss.logmanager.Level.WARN);
        log4jToJul.put(Level.ERROR.intLevel(), org.jboss.logmanager.Level.ERROR);
        log4jToJul.put(Level.FATAL.intLevel(), org.jboss.logmanager.Level.FATAL);
        log4jToJul.put(Level.OFF.intLevel(), org.jboss.logmanager.Level.OFF);
    }

    /**
     * Returns an instance of the level translator.
     *
     * @return an instance
     */
    static LevelTranslator getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * Translates a {@linkplain Level log4j level} to a {@linkplain java.util.logging.Level JUL level}.
     *
     * @param level the log4j level
     *
     * @return the closest match of a JUL level
     */
    java.util.logging.Level translateLevel(final Level level) {
        final java.util.logging.Level result = level == null ? null : log4jToJul.get(level.intLevel());
        return result == null ? DEFAULT_LEVEL : result;
    }

    /**
     * Translates a {@linkplain java.util.logging.Level JUL level} to a {@linkplain Level log4j level}.
     *
     * @param level the JUL level
     *
     * @return the log4j level
     */
    Level translateLevel(final java.util.logging.Level level) {
        return level == null ? DEFAULT_LOG4J_LEVEL : translateLevel(level.intValue());
    }

    /**
     * Translates a {@linkplain java.util.logging.Level#intValue()}  JUL level} to a {@linkplain Level log4j level}.
     *
     * @param level the JUL level int value
     *
     * @return the log4j level
     */
    Level translateLevel(final int level) {
        final Level result = julToLog4j.get(level);
        return result == null ? DEFAULT_LOG4J_LEVEL : result;
    }
}
