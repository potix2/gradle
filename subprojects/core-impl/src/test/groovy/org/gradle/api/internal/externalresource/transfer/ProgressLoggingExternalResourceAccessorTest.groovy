/*
 * Copyright 2012 the original author or authors.
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

package org.gradle.api.internal.externalresource.transfer

import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream
import org.gradle.api.internal.externalresource.ExternalResource
import org.gradle.logging.ProgressLogger
import org.gradle.logging.ProgressLoggerFactory
import spock.lang.Specification
import spock.lang.Unroll

class ProgressLoggingExternalResourceAccessorTest extends Specification {

    ExternalResourceAccessor accessor = Mock()
    ProgressLoggerFactory progressLoggerFactory = Mock();
    def progressLoggerAccessor = new ProgressLoggingExternalResourceAccessor(accessor, progressLoggerFactory)
    ProgressLogger progressLogger = Mock()
    ExternalResource externalResource = Mock()

    @Unroll
    def "delegates #method to delegate resource accessor"() {
        when:
        progressLoggerAccessor."$method"("location")
        then:
        1 * accessor."$method"("location")
        where:
        method << ['getMetaData', 'getResource', 'getResourceSha1']
    }

    def "getResource returns null when delegate returns null"() {
        setup:
        accessor.getResource("location") >> null
        when:
        def loadedResource = progressLoggerAccessor.getResource("location")
        then:
        loadedResource == null
    }

    def "getResource wraps loaded Resource from delegate in ProgressLoggingExternalResource"() {
        setup:
        accessor.getResource("location") >> externalResource
        when:
        def loadedResource = progressLoggerAccessor.getResource("location")
        then:
        loadedResource != null
        loadedResource instanceof ProgressLoggingExternalResourceAccessor.ProgressLoggingExternalResource
    }


    def "ProgressLoggingExternalResource.writeTo wraps delegate call in progress logger and logs progress"() {
        setup:
        accessor.getResource("location") >> externalResource
        externalResource.getName() >> "test resource"
        externalResource.getContentLength() >> 64 * 1024
        externalResource.writeTo(_) >> { OutputStream stream -> stream.write(new byte[10],0,10) }
        when:
        def processLoggableResource = progressLoggerAccessor.getResource("location")
        and:
        processLoggableResource.writeTo(new ByteOutputStream())
        then:
        1 * progressLoggerFactory.newOperation(_) >> progressLogger
        1 * progressLogger.started()
        1 * progressLogger.progress("10 B/64 KB downloaded")
        1 * progressLogger.completed()
    }

    @Unroll
    def "ProgressLoggingExternalResource delegates #method to delegate ExternalResource"() {
        when:
        accessor.getResource("location") >> externalResource
        def plExternalResource = progressLoggerAccessor.getResource("location")
        and:
        plExternalResource."$method"()
        then:
        1 * externalResource."$method"()
        where:
        method << ['close', 'getMetaData', 'getName', 'getLastModified', 'getContentLength', 'isLocal', 'openStream']
    }

    @Unroll
    def "ProgressLoggingExternalResource  #method to delegate ExternalResource"() {
        when:
        accessor.getResource("location") >> externalResource
        def plExternalResource = progressLoggerAccessor.getResource("location")
        and:
        plExternalResource."$method"()
        then:
        1 * externalResource."$method"()
        where:
        method << ['close', 'getMetaData', 'getName', 'getLastModified', 'getContentLength', 'isLocal', 'openStream']
    }
}