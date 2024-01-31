package server.request;


import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import server.tools.Tools;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Request {
    private final List<String> CONTENT_TYPE = List.of(
            "application/x-www-form-urlencoded",
            "multipart/form-data",
            "text/plain"
    );
    private final RequestLine requestLine;
    private final String headers;
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
            return listQueries.stream().
                    filter(s -> s.getName().equals(name)).
                    collect(Collectors.toList());
        } catch (
                URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public List<NameValuePair> getQueryParams() {
        return getQueryParam("");
    }

    // Функциональность обработки тела, оформленного в виде x-www-form-url-encoded запроса для
    // получения параметров, переданных в теле запроса

    public List<NameValuePair> getPostParam(String name) {
        // проверяем, что тело запроса есть
        if (!requestLine.getRequestMethod().equals("POST") || getBody() == null) {
            return null;
        }
        // проверяем, что "Content-Type" - application/x-www-form-urlencoded
        final var contentType = Tools.extractHeader(
                Arrays.asList(headers.split("\r\n")),
                "Content-Type"
        );
        if (contentType.isPresent() && contentType.get().equals(CONTENT_TYPE.get(0))) {
            List<NameValuePair> postParams = getNameValuePairsFromStringBody(new String(getBody()));
            if (name.isEmpty()) {
                return postParams;
            }
            return postParams.stream().
                    filter(s -> s.getName().equals(name)).
                    collect(Collectors.toList());
        }
        return null;
    }

    public List<NameValuePair> getPostParams() {
        return getPostParam("");
    }

    private List<NameValuePair> getNameValuePairsFromStringBody(String stringBody) {
        String[] params = stringBody.split("&");
        List<NameValuePair> nameValuePairs = new ArrayList<>();
        for (String param : params) {
            String[] component = param.split("=");
            NameValuePair nvp = new NameValuePair() {
                @Override
                public String getName() {
                    return component[0];
                }

                @Override
                public String getValue() {
                    return component[1];
                }

                @Override
                public String toString() {
                    return getName() + " = " + getValue();
                }
            };
            nameValuePairs.add(nvp);
        }
        return nameValuePairs;
    }
}
