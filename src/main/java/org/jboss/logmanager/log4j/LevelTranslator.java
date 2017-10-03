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

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.logging.log4j.Level;

/**
 * A utility to translate levels.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class LevelTranslator {
    private final Map<java.util.logging.Level, Level> julToLog4jLevels = new TreeMap<>(new Comparator<java.util.logging.Level>() {
        @Override
        public int compare(final java.util.logging.Level o1, final java.util.logging.Level o2) {
            return Integer.compare(o1.intValue(), o2.intValue());
        }
    });
    private final ConcurrentMap<Level, java.util.logging.Level> log4jToJulLevels = new ConcurrentHashMap<>();

    private static class Holder {
        static final LevelTranslator INSTANCE = new LevelTranslator();
    }

    private LevelTranslator() {
        initLevels(julToLog4jLevels, log4jToJulLevels);
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
        java.util.logging.Level result = log4jToJulLevels.get(level);
        if (result == null) {
            // Determine closest level
            final int log4jLevel = level.intLevel();
            result = java.util.logging.Level.ALL;
            for (final java.util.logging.Level current : julToLog4jLevels.keySet()) {
                if (current.intValue() > log4jLevel) {
                    break;
                }
                result = current;
            }
            log4jToJulLevels.putIfAbsent(level, result);
        }
        return result;
    }

    /**
     * Translates a {@linkplain java.util.logging.Level JUL level} to a {@linkplain Level log4j level}.
     *
     * @param level the JUL level
     *
     * @return the log4j level
     */
    Level translateLevel(final java.util.logging.Level level) {
        // TODO (jrp) this won't account for SEVERE, WARNING, FINE, FINEST, etc.
        final Level result = julToLog4jLevels.get(level);
        return result == null ? Level.INFO : result;
    }

    private static void initLevels(final Map<java.util.logging.Level, Level> julToLog4jLevels, final Map<Level, java.util.logging.Level> log4jToJulLevels) {
        addLevel(julToLog4jLevels, log4jToJulLevels, org.jboss.logmanager.Level.ALL, Level.ALL);
        addLevel(julToLog4jLevels, log4jToJulLevels, org.jboss.logmanager.Level.TRACE, Level.TRACE);
        addLevel(julToLog4jLevels, log4jToJulLevels, org.jboss.logmanager.Level.DEBUG, Level.DEBUG);
        addLevel(julToLog4jLevels, log4jToJulLevels, org.jboss.logmanager.Level.INFO, Level.INFO);
        addLevel(julToLog4jLevels, log4jToJulLevels, org.jboss.logmanager.Level.WARN, Level.WARN);
        addLevel(julToLog4jLevels, log4jToJulLevels, org.jboss.logmanager.Level.ERROR, Level.ERROR);
        addLevel(julToLog4jLevels, log4jToJulLevels, org.jboss.logmanager.Level.FATAL, Level.FATAL);
        addLevel(julToLog4jLevels, log4jToJulLevels, org.jboss.logmanager.Level.OFF, Level.OFF);

        // TODO (jrp) minor note it looks like log4j levels use a reverse int level

        // Add JUL levels
        addLevel(julToLog4jLevels, log4jToJulLevels, java.util.logging.Level.FINE, Level.DEBUG);
        // TODO (jrp) are FINER, FINEST and CONFIG right?
        addLevel(julToLog4jLevels, log4jToJulLevels, java.util.logging.Level.FINER, Level.TRACE);
        addLevel(julToLog4jLevels, log4jToJulLevels, java.util.logging.Level.FINEST, Level.TRACE);
        addLevel(julToLog4jLevels, log4jToJulLevels, java.util.logging.Level.CONFIG, Level.TRACE);
        addLevel(julToLog4jLevels, log4jToJulLevels, java.util.logging.Level.WARNING, Level.WARN);
        addLevel(julToLog4jLevels, log4jToJulLevels, java.util.logging.Level.SEVERE, Level.FATAL);
    }

    private static void addLevel(final Map<java.util.logging.Level, Level> julToLog4jLevels, final Map<Level, java.util.logging.Level> log4jToJulLevels,
                                 final java.util.logging.Level julLevel, final Level log4jLevel) {
        julToLog4jLevels.put(julLevel, log4jLevel);
        log4jToJulLevels.put(log4jLevel, julLevel);
    }
}
