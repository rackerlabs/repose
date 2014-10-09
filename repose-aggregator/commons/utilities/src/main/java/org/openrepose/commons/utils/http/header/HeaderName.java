package org.openrepose.commons.utils.http.header;

public class HeaderName {
    private String name;

    private HeaderName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        HeaderName that = (HeaderName) o;

        if(name != null) {
            if (!name.equalsIgnoreCase(that.name)) {
                return false;
            }
        } else {
            if(that.name != null) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        return name != null ? name.toLowerCase().hashCode() : 0;
    }

    @Override
    public String toString() {
        return "HeaderName{" +
                "name='" + name + '\'' +
                '}';
    }

    public static HeaderName wrap(String name) {
        return new HeaderName(name);
    }
}
