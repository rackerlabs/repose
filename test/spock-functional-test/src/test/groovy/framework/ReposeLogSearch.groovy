package framework


class ReposeLogSearch {

    String logFileLocation;

    ReposeLogSearch(String logFileLocation) {
        this.logFileLocation = logFileLocation
    }

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
}
