package com.rackspace.papi.commons.util.http.header;

public class HeaderNameMapKey {
    private String headerName;

    public HeaderNameMapKey(String headerName) {
        this.headerName = headerName;
    }

    public String getName() {
        return headerName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        HeaderNameMapKey that = (HeaderNameMapKey) o;

        if (headerName != null ? !headerName.equalsIgnoreCase(that.headerName) : that.headerName != null) { return false; }

        return true;
    }

    @Override
    public int hashCode() {
        return headerName != null ? headerName.toLowerCase().hashCode() : 0;
    }

    @Override
    public String toString() {
        return "HeaderNameMapKey{" +
                "headerName='" + headerName + '\'' +
                '}';
    }
}
