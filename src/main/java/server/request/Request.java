package server.request;


import java.io.InputStream;

public class Request {
    private RequestLine requestLine;
    private String headers;
    private InputStream body;

    public Request(RequestLine requestLine, String headers, InputStream body) {
        this.requestLine = requestLine;
        this.headers = headers;
        this.body = body;
    }

    public Request(RequestLine requestLine, String headers) {
        this.requestLine = requestLine;
        this.headers = headers;
        this.body = InputStream.nullInputStream();
    }

    public RequestLine getRequestLine() {
        return requestLine;
    }

    public String getHeaders() {
        return headers;
    }

    public InputStream getBody() {
        return body;
    }

    public String toString() {
        return " requestLine " + requestLine + "\n headers " + headers;
    }
}
