/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.core.appender;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.RollingRandomAccessFileManager;
import org.apache.logging.log4j.core.appender.rolling.RollingFileManager;
import org.apache.logging.log4j.core.appender.rolling.RolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.TriggeringPolicy;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttr;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.helpers.Booleans;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.net.Advertiser;

/**
 * An appender that writes to random access files and can roll over at
 * intervals.
 *
 * @param <T> The {@link Layout}'s {@link Serializable} type.
 */
@Plugin(name = "FastRollingFile", category = "Core", elementType = "appender", printObject = true)
public final class RollingRandomAccessFileAppender<T extends Serializable> extends AbstractOutputStreamAppender<T> {

    private final String fileName;
    private final String filePattern;
    private Object advertisement;
    private final Advertiser advertiser;

    private RollingRandomAccessFileAppender(final String name, final Layout<T> layout,
            final Filter filter, final RollingFileManager manager, final String fileName,
            final String filePattern, final boolean ignoreExceptions,
            final boolean immediateFlush, final Advertiser advertiser) {
        super(name, layout, filter, ignoreExceptions, immediateFlush, manager);
        if (advertiser != null) {
            final Map<String, String> configuration = new HashMap<String, String>(
                    layout.getContentFormat());
            configuration.put("contentType", layout.getContentType());
            configuration.put("name", name);
            advertisement = advertiser.advertise(configuration);
        }
        this.fileName = fileName;
        this.filePattern = filePattern;
        this.advertiser = advertiser;
    }

    @Override
    public void stop() {
        super.stop();
        if (advertiser != null) {
            advertiser.unadvertise(advertisement);
        }
    }

    /**
     * Write the log entry rolling over the file when required.
     *
     * @param event The LogEvent.
     */
    @Override
    public void append(final LogEvent event) {
        final RollingRandomAccessFileManager manager = (RollingRandomAccessFileManager) getManager();
        manager.checkRollover(event);

        // Leverage the nice batching behaviour of async Loggers/Appenders:
        // we can signal the file manager that it needs to flush the buffer
        // to disk at the end of a batch.
        // From a user's point of view, this means that all log events are
        // _always_ available in the log file, without incurring the overhead
        // of immediateFlush=true.
        manager.setEndOfBatch(event.isEndOfBatch());
        super.append(event);
    }

    /**
     * Returns the File name for the Appender.
     *
     * @return The file name.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Returns the file pattern used when rolling over.
     *
     * @return The file pattern.
     */
    public String getFilePattern() {
        return filePattern;
    }

    /**
     * Create a RollingRandomAccessFileAppender.
     *
     * @param fileName The name of the file that is actively written to.
     *            (required).
     * @param filePattern The pattern of the file name to use on rollover.
     *            (required).
     * @param append If true, events are appended to the file. If false, the
     *            file is overwritten when opened. Defaults to "true"
     * @param name The name of the Appender (required).
     * @param immediateFlush When true, events are immediately flushed. Defaults
     *            to "true".
     * @param policy The triggering policy. (required).
     * @param strategy The rollover strategy. Defaults to
     *            DefaultRolloverStrategy.
     * @param layout The layout to use (defaults to the default PatternLayout).
     * @param filter The Filter or null.
     * @param ignore If {@code "true"} (default) exceptions encountered when appending events are logged; otherwise
     *               they are propagated to the caller.
     * @param advertise "true" if the appender configuration should be
     *            advertised, "false" otherwise.
     * @param advertiseURI The advertised URI which can be used to retrieve the
     *            file contents.
     * @param config The Configuration.
     * @param <S> The {@link Layout}'s {@link Serializable} type.
     * @return A RollingRandomAccessFileAppender.
     */
    @PluginFactory
    public static <S extends Serializable> RollingRandomAccessFileAppender<S> createAppender(
            @PluginAttr("fileName") final String fileName,
            @PluginAttr("filePattern") final String filePattern,
            @PluginAttr("append") final String append,
            @PluginAttr("name") final String name,
            @PluginAttr("immediateFlush") final String immediateFlush,
            @PluginElement("policy") final TriggeringPolicy policy,
            @PluginElement("strategy") RolloverStrategy strategy,
            @PluginElement("layout") Layout<S> layout,
            @PluginElement("filter") final Filter filter,
            @PluginAttr("ignoreExceptions") final String ignore,
            @PluginAttr("advertise") final String advertise,
            @PluginAttr("advertiseURI") final String advertiseURI,
            @PluginConfiguration final Configuration config) {

        final boolean isAppend = Booleans.parseBoolean(append, true);
        final boolean ignoreExceptions = Booleans.parseBoolean(ignore, true);
        final boolean isFlush = Booleans.parseBoolean(immediateFlush, true);
        final boolean isAdvertise = Boolean.parseBoolean(advertise);

        if (name == null) {
            LOGGER.error("No name provided for FileAppender");
            return null;
        }

        if (fileName == null) {
            LOGGER.error("No filename was provided for FileAppender with name "
                    + name);
            return null;
        }

        if (filePattern == null) {
            LOGGER.error("No filename pattern provided for FileAppender with name "
                    + name);
            return null;
        }

        if (policy == null) {
            LOGGER.error("A TriggeringPolicy must be provided");
            return null;
        }

        if (strategy == null) {
            strategy = DefaultRolloverStrategy.createStrategy(null, null,
                    "true", config);
        }

        if (layout == null) {
            @SuppressWarnings({ "unchecked", "UnnecessaryLocalVariable" })
            final
            Layout<S> l = (Layout<S>) PatternLayout.createLayout(null, null, null, null, null);
            layout = l;
        }


        final RollingRandomAccessFileManager manager = RollingRandomAccessFileManager.getRollingRandomAccessFileManager(fileName, filePattern,
            isAppend, isFlush, policy, strategy, advertiseURI, layout);
        if (manager == null) {
            return null;
        }

        return new RollingRandomAccessFileAppender<S>(name, layout, filter, manager,
                fileName, filePattern, ignoreExceptions, isFlush,
                isAdvertise ? config.getAdvertiser() : null);
    }
}