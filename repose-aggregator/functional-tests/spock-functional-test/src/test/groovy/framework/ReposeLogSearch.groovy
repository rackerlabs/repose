package framework

import java.nio.channels.FileChannel

/**
* Responsible for searching log file for an instance of Repose
*/
class ReposeLogSearch {

    String logFileLocation;

    ReposeLogSearch(String logFileLocation) {
        this.logFileLocation = logFileLocation
    }

    /**
     * Search the repose log file using the properties log locations specified
     * @param searchString is used to search log file for matches
     */
    public List<String> searchByString(String searchString) {
        File logFile=new File(logFileLocation);

        def foundMatches = []
        logFile.eachLine {

            ln -> if ( ln =~ searchString  ) {
                foundMatches << "${ln}"
            }
        }


        return foundMatches;

    }

    public List<String> printLog() {
        File logFile=new File(logFileLocation);
        logFile.eachLine {
            println it
        }
    }

    public def cleanLog(){
        new FileOutputStream(logFileLocation).getChannel().truncate(0).close();
    }

    public def deleteLog(){
        File logFile=new File(logFileLocation);
        logFile.delete()
    }
}
