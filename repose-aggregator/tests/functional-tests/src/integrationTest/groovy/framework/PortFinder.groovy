/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
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
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */

package framework

import groovy.util.logging.Log4j
import org.springframework.util.SocketUtils

@Log4j
@Singleton
class PortFinder {
    final int DEFAULT_START_PORT = 10000
    final int DEFAULT_WAIT_TIME_MS = 100

    int maxPort = SocketUtils.PORT_RANGE_MAX
    int sleepTime = DEFAULT_WAIT_TIME_MS
    int currentPort = DEFAULT_START_PORT

    synchronized int getNextOpenPort() {
        // todo: consider using new ServerSocket(0).getLocalPort() instead of all of this, but then you would be getting
        // todo: ports in the range where ports are frequently handed out for such things.  It may not be safe enough to
        // todo: count on them still being available between the time we confirm they're good in this method and the
        // todo: time Repose actually uses it.  ¯\_(ツ)_/¯
        while (currentPort <= maxPort) {
            try {
                log.debug "Checking if port $currentPort is available..."
                new Socket("localhost", currentPort)
            } catch (ConnectException e) {
                log.debug "...port $currentPort is available."
                currentPort++
                return currentPort - 1
            } catch (IOException e) {
                log.info "...port $currentPort does not seem to be available."
                log.trace "Exception details: ", e
            } catch (Exception e) {
                log.warn "Got an exception: $e"
                throw e
            }

            log.trace "Taking a nap before trying the next port..."
            Thread.sleep(sleepTime)
            currentPort++
        }

        throw new SocketException("Ran out of ports")
    }
}
