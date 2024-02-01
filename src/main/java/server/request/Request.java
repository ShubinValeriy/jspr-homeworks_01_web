package server.request;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import server.tools.Tools;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class Request implements RequestContext {
    private final List<String> CONTENT_TYPE = List.of(
            "application/x-www-form-urlencoded",
            "multipart/form-data",
            "text/plain"
    );
    private final RequestLine requestLine;
    private final String headers;
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
    public List<NameValuePair> getQueryParam(String name) {
        return listQueries.stream().
                filter(s -> s.getName().equals(name)).
                collect(Collectors.toList());
    }

    public List<NameValuePair> getQueryParams() {
        return listQueries;
    }

    //Парсинг requestLine для получения параметров из Query
    private List<NameValuePair> parsRequestLine(RequestLine requestLine) {
        try {
            URIBuilder uriBuilder = new URIBuilder(requestLine.getRequestURL());
            List<NameValuePair> listQueries = uriBuilder.getQueryParams();
            return listQueries;
        } catch (
                URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }


    // Функциональность обработки тела, оформленного в виде x-www-form-url-encoded запроса для
    // получения параметров, переданных в теле запроса

    public List<NameValuePair> getPostParams() {
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
            return postParams;
        }
        return null;
    }

    public List<NameValuePair> getPostParam(String name) {
        return getPostParams().stream().
                filter(s -> s.getName().equals(name)).
                collect(Collectors.toList());
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


    // Функциональность поддерживающая парсинг multipart-запросов
    public List<NameValuePair> getParts() {
        // проверка есть ли в наше request что парсить
        if (ServletFileUpload.isMultipartContent(this)) {
            // Создаем фабрику для элементов файлов на диске
            DiskFileItemFactory factory = new DiskFileItemFactory();
            // Сконфигурируем репозиторий для безопасного временного хранения файлов
            String pathTemp = "src/main/resources/temp";
            factory.setRepository(
                    new File(pathTemp));
            factory.setSizeThreshold(
                    DiskFileItemFactory.DEFAULT_SIZE_THRESHOLD);
            factory.setFileCleaningTracker(null);

            // Создаем экземпляр ServletFileUpload, который затем используется для разбора запроса
            ServletFileUpload upload = new ServletFileUpload(factory);

            try {
                // Разбираем запрос и извлекаем из него список элементов FileItem
                List<FileItem> items = upload.parseRequest(this);
                Iterator<FileItem> iterator = items.iterator();
                List<NameValuePair> nameValuePairs = new ArrayList<>();
                while (iterator.hasNext()) {
                    FileItem item = iterator.next();
                    // Проверяем является ли текущий FileItem формой
                    NameValuePair nvp;
                    if (item.isFormField()) {
                        nvp = new NameValuePair() {
                            @Override
                            public String getName() {
                                return item.getFieldName();
                            }

                            @Override
                            public String getValue() {
                                return item.getString();
                            }

                            @Override
                            public String toString() {
                                return getName() + " = " + getValue();
                            }
                        };
                    } else {
                        nvp = new NameValuePair() {
                            @Override
                            public String getName() {
                                return item.getFieldName();
                            }

                            @Override
                            public String getValue() {
                                return item.getName();
                            }

                            @Override
                            public String toString() {
                                return getName() + " = " + getValue();
                            }
                        };
                        // Загрузим полученный файл
                        File file = new File(pathTemp + "/" + item.getName());
                        item.write(file);
                    }
                    nameValuePairs.add(nvp);
                }
                return nameValuePairs;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    public List<NameValuePair> getPart(String name) {
        return getParts().stream().
                filter(s -> s.getName().equals(name)).
                collect(Collectors.toList());
    }

    @Override
    public String getCharacterEncoding() {
        return "UTF-8";
    }

    @Override
    public String getContentType() {
        final var contentType = Tools.extractHeader(
                Arrays.asList(headers.split("\r\n")),
                "Content-Type"
        );
        if (contentType.isPresent()) {
            return contentType.get();
        }
        return null;
    }

    @Override
    public int getContentLength() {
        if (getBody() == null) {
            return -1;
        }
        return getBody().length;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(getBody());
    }
}
