/*
 * Copyright (C) 2012 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.maven.ext.io.server;

import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

@UtilityClass
public final class PortFinder
{
    private static final Random RANDOM = new Random();

    private static AtomicInteger COUNTER = new AtomicInteger(1);

    // Sourced from https://github.com/Commonjava/http-testserver/blob/master/src/main/java/org/commonjava/test/http/util/PortFinder.java
    public static <T> T findPortFor( final int maxTries, PortConsumer<T> consumer )
    {
        for ( int i = 0; i < maxTries; i++ )
        {
            final int port = 1024 + COUNTER.incrementAndGet() + ( Math.abs( RANDOM.nextInt() ) % 30000 );
            T result = null;
            try
            {
                return consumer.call( port );
            }
            catch ( final IOException e )
            {
            }
        }

        throw new IllegalStateException( "Cannot find open port after " + maxTries + " attempts." );
    }

    public interface PortConsumer<T>
    {
        T call(int port) throws IOException;
    }
}
