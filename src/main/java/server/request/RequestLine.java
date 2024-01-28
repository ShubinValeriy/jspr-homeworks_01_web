package server.request;


public class RequestLine {
    private String requestMethod;
    private String requestURL;
    private String versionHTTP;

    public RequestLine(String requestMethod, String requestURL, String versionHTTP) {
        this.requestMethod = requestMethod;
        this.requestURL = requestURL;
        this.versionHTTP = versionHTTP;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public String getRequestURL() {
        return requestURL;
    }

    public String getVersionHTTP() {
        return versionHTTP;
    }

    public String toString() {
        return " метод: " + requestMethod + "\n URL " + requestURL;
    }
}
