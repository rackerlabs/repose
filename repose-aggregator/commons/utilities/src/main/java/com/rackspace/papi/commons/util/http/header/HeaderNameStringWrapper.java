package com.rackspace.papi.commons.util.http.header;

public class HeaderNameStringWrapper {
    private String headerName;

    public HeaderNameStringWrapper(String headerName) {
        this.headerName = headerName;
    }

    public String getName() {
        return headerName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        HeaderNameStringWrapper that = (HeaderNameStringWrapper) o;

        if(headerName != null) {
            if (!headerName.equalsIgnoreCase(that.headerName)) {
                return false;
            }
        } else {
            if(that.headerName != null) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        return headerName != null ? headerName.toLowerCase().hashCode() : 0;
    }

    @Override
    public String toString() {
        return "HeaderNameStringWrapper{" +
                "headerName='" + headerName + '\'' +
                '}';
    }
}
