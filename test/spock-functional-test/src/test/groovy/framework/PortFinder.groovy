package framework

import groovy.util.logging.Log4j;

/**
 *
 * @author richard-sartor
 */
@Log4j
public class PortFinder {

    def _basePort = 10000
    def _currentPort = null

    def getNextOpenPort(start=null) {

        if (start != null) {
            _currentPort = start
        } else if (_currentPort == null) {
            _currentPort = _basePort
        }

        while (_currentPort < 65536) {
            try {
                def url = String.format("http://localhost:%d/", _currentPort)
                log.debug "Trying " + url
                url.toURL().getText()
            } catch (java.net.ConnectException e) {
                log.debug "Didn't connect, using this one"
                _currentPort++
                return _currentPort - 1
            } catch (SocketException e) {
                // ignore the exception
                log.debug "Got a SocketException: " + e.toString();
            } catch (Exception e) {
                log.debug "Got an Exception: " + e.toString();
                throw e
            }

            Thread.sleep(1000)
            log.debug "Connected"

            _currentPort++
        }

        throw new RuntimeException("Ran out of ports")
    }
}