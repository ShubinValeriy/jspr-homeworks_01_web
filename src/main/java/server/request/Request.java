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
    private List<NameValuePair> listQueries;

    public Request(RequestLine requestLine, String headers, byte[] body) {
        this.requestLine = requestLine;
        this.headers = headers;
        this.body = body;
        this.listQueries = parsRequestLine(requestLine);
    }

    public Request(RequestLine requestLine, String headers) {
        this.requestLine = requestLine;
        this.headers = headers;
        this.listQueries = parsRequestLine(requestLine);
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
    public List<NameValuePair> getQueryParams() {
        return listQueries;
    }

    public List<NameValuePair> getQueryParam(String name) {
        return listQueries.stream().
                filter(s -> s.getName().equals(name)).
                collect(Collectors.toList());
    }

    //Парсинг requestLine для получения параметров из Query
    private List<NameValuePair> parsRequestLine(RequestLine requestLine) {
        try {
            URIBuilder uriBuilder = new URIBuilder(requestLine.getRequestURL());
            return uriBuilder.getQueryParams();
        } catch (
                URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }


}
