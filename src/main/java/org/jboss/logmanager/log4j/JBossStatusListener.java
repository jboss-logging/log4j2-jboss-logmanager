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

import java.io.IOException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.status.StatusListener;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.jboss.logmanager.Logger;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
// TODO (jrp) it's possible we could just get rid of this in favor of StatusConsoleListener
class JBossStatusListener implements StatusListener {
    // TODO (jrp) should be writing directly to System.out? Seems wrong though.
    private final Logger logger = Logger.getLogger("org.jboss.logmanager.log4j.status");
    private final LevelTranslator levelTranslator = LevelTranslator.getInstance();
    private final Level level;

    JBossStatusListener() {
        // TODO (jrp) we may need to look at other properties files, this defaults to log4j.component.properties
        final PropertiesUtil properties = PropertiesUtil.getProperties();
        // TODO (jrp) may need to look at other properties, e.g. Log4jDefaultStatusLevel
        final String stringLevel = properties.getStringProperty("log4j2.StatusLogger.level", "WARN");
        Level level = Level.getLevel(stringLevel);
        if (level == null) {
            level = Level.WARN;
        }
        this.level = level;
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
    public void close() throws IOException {
        // Nothing to do here
    }

}
