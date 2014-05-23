package features.filters.translation.httpx.parser;

/**
 * @author fran
 */
public final class RequestParserFactory {

    private RequestParserFactory() {
    }

    public static Parser newInstance() {
        return new HttpRequestParser();
    }
}
