/*
 * Copyright © 2016-2018 European Support Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openecomp.sdc.logging.slf4j;

import static org.openecomp.sdc.logging.slf4j.SLF4JLoggerWrapper.AuditField.BEGIN_TIMESTAMP;
import static org.openecomp.sdc.logging.slf4j.SLF4JLoggerWrapper.AuditField.CLIENT_IP_ADDRESS;
import static org.openecomp.sdc.logging.slf4j.SLF4JLoggerWrapper.AuditField.ELAPSED_TIME;
import static org.openecomp.sdc.logging.slf4j.SLF4JLoggerWrapper.AuditField.END_TIMESTAMP;
import static org.openecomp.sdc.logging.slf4j.SLF4JLoggerWrapper.AuditField.RESPONSE_CODE;
import static org.openecomp.sdc.logging.slf4j.SLF4JLoggerWrapper.AuditField.RESPONSE_DESCRIPTION;
import static org.openecomp.sdc.logging.slf4j.SLF4JLoggerWrapper.AuditField.STATUS_CODE;

import java.text.Format;
import java.text.SimpleDateFormat;
import org.openecomp.sdc.logging.api.AuditData;
import org.openecomp.sdc.logging.api.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * @author evitaliy
 * @since 08 Jan 18
 */
class SLF4JLoggerWrapper implements Logger {

    //The specified format presents time in UTC formatted per ISO 8601, as required by ONAP logging guidelines
    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

    private static final String PREFIX = "";

    enum AuditField {

        BEGIN_TIMESTAMP(PREFIX + "BeginTimestamp"),
        END_TIMESTAMP(PREFIX + "EndTimestamp"),
        ELAPSED_TIME(PREFIX + "ElapsedTime"),
        STATUS_CODE(PREFIX + "StatusCode"),
        RESPONSE_CODE(PREFIX + "ResponseCode"),
        RESPONSE_DESCRIPTION(PREFIX + "ResponseDescription"),
        CLIENT_IP_ADDRESS(PREFIX + "ClientIpAddress");

        private final String key;

        AuditField(String key) {
            this.key = key;
        }

        public String asKey() {
            return key;
        }
    }

    private final org.slf4j.Logger logger;

    SLF4JLoggerWrapper(org.slf4j.Logger delegate) {
        this.logger = delegate;
    }

    // May cause http://www.slf4j.org/codes.html#loggerNameMismatch
    SLF4JLoggerWrapper(Class<?> clazz) {
        this(LoggerFactory.getLogger(clazz));
    }

    SLF4JLoggerWrapper(String className) {
        this(LoggerFactory.getLogger(className));
    }

    @Override
    public String getName() {
        return logger.getName();
    }

    @Override
    public boolean isMetricsEnabled() {
        return logger.isInfoEnabled(Markers.METRICS);
    }

    @Override
    public void metrics(String msg) {
        logger.info(Markers.METRICS, msg);
    }

    @Override
    public void metrics(String msg, Object arg) {
        logger.info(Markers.METRICS, msg, arg);
    }

    @Override
    public void metrics(String msg, Object arg1, Object arg2) {
        logger.info(Markers.METRICS, msg, arg1, arg2);
    }

    @Override
    public void metrics(String msg, Object... arguments) {
        logger.info(Markers.METRICS, msg, arguments);
    }

    @Override
    public void metrics(String msg, Throwable t) {
        logger.info(Markers.METRICS, msg, t);
    }

    @Override
    public boolean isAuditEnabled() {
        return logger.isInfoEnabled(Markers.AUDIT);
    }

    @Override
    public void audit(AuditData data) {

        if (data == null) {
            return; // not failing if null
        }

        putTimes(data);
        putIfNotNull(RESPONSE_CODE.key, data.getResponseCode());
        putIfNotNull(RESPONSE_DESCRIPTION.key, data.getResponseDescription());
        putIfNotNull(CLIENT_IP_ADDRESS.key, data.getClientIpAddress());

        if (data.getStatusCode() != null) {
            MDC.put(STATUS_CODE.key, data.getStatusCode().name());
        }

        try {
            logger.info(Markers.AUDIT, "");
        } finally {
            for (AuditField f : AuditField.values()) {
                MDC.remove(f.key);
            }
        }
    }

    private void putIfNotNull(String key, String value) {
        if (value != null) {
            MDC.put(key, value);
        }
    }

    private void putTimes(AuditData data) {
        // SimpleDateFormat is not thread-safe and cannot be a constant
        Format dateTimeFormat = new SimpleDateFormat(DATE_TIME_PATTERN);
        MDC.put(BEGIN_TIMESTAMP.key, dateTimeFormat.format(data.getStartTime()));
        MDC.put(END_TIMESTAMP.key, dateTimeFormat.format(data.getEndTime()));
        MDC.put(ELAPSED_TIME.key, String.valueOf(data.getEndTime() - data.getStartTime()));
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public void debug(String msg) {
        logger.debug(msg);
    }

    @Override
    public void debug(String format, Object arg) {
        logger.debug(format, arg);
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        logger.debug(format, arg1, arg2);
    }

    @Override
    public void debug(String format, Object... arguments) {
        logger.debug(format, arguments);
    }

    @Override
    public void debug(String msg, Throwable t) {
        logger.debug(msg, t);
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    @Override
    public void info(String msg) {
        logger.info(msg);
    }

    @Override
    public void info(String format, Object arg) {
        logger.info(format, arg);
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        logger.info(format, arg1, arg2);
    }

    @Override
    public void info(String format, Object... arguments) {
        logger.info(format, arguments);
    }

    @Override
    public void info(String msg, Throwable t) {
        logger.info(msg, t);
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    @Override
    public void warn(String msg) {
        logger.warn(msg);
    }

    @Override
    public void warn(String format, Object arg) {
        logger.warn(format, arg);
    }

    @Override
    public void warn(String format, Object... arguments) {
        logger.warn(format, arguments);
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        logger.warn(format, arg1, arg2);
    }

    @Override
    public void warn(String msg, Throwable t) {
        logger.warn(msg, t);
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    @Override
    public void error(String msg) {
        logger.error(msg);
    }

    @Override
    public void error(String format, Object arg) {
        logger.error(format, arg);
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        logger.error(format, arg1, arg2);
    }

    @Override
    public void error(String format, Object... arguments) {
        logger.error(format, arguments);
    }

    @Override
    public void error(String msg, Throwable t) {
        logger.error(msg, t);
    }
}
