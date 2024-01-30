package server.request;


import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URIBuilder;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Request {
    private RequestLine requestLine;
    private String headers;
    private byte[] body;

    public Request(RequestLine requestLine, String headers, byte[] body) {
        this.requestLine = requestLine;
        this.headers = headers;
        this.body = body;
    }

    public Request(RequestLine requestLine, String headers) {
        this.requestLine = requestLine;
        this.headers = headers;
    }

    public RequestLine getRequestLine() {
        return requestLine;
    }

    public String getHeaders() {
        return headers;
    }

    public byte[] getBody() {
        return body;
    }

    public String toString() {
        return " requestLine " + requestLine + "\n headers " + headers;
    }


    // функциональность обработки параметров запроса для получения параметров из Query String
    public List<NameValuePair> getQueryParam(String name) {
        try {
            URIBuilder uriBuilder = new URIBuilder(requestLine.getRequestURL());
            List<NameValuePair> listQueries = uriBuilder.getQueryParams();
            if (name.isEmpty()) {
                return listQueries;
            }
            List<NameValuePair> listQueriesForName = listQueries.stream().
                    filter(s -> s.getName().equals(name)).
                    collect(Collectors.toList());
            return listQueriesForName;
        } catch (
                URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public List<NameValuePair> getQueryParams() {
        return getQueryParam("");
    }


}
