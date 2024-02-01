package server;

import server.request.Request;
import server.request.RequestLine;
import server.tools.Tools;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.Map;


public class ClientHandler implements Runnable {
    //УСТАНАВЛИВАЕМ ЛИМИТ на request line + заголовки
    private final int LIMIT = 4096;
    private final Socket SOCKET;
    private final Map<String, Map<String, Handler>> HANDLERS_MAP;

    public ClientHandler(Socket socket, Map<String, Map<String, Handler>> handlersMap) {
        this.SOCKET = socket;
        this.HANDLERS_MAP = handlersMap;
    }

    // Парсер Request
    private Request getRequestFromBufferedInputStream(BufferedInputStream bis) {
        try {

            //ограничиваем входящий поток данных нашим ЛИМИТОМ
            bis.mark(LIMIT);
            //зачитываем в буфер данные, ограниченные нашим ЛИМИТОМ
            var buffer = new byte[LIMIT];
            final var read = bis.read(buffer); // read - количество фактически прочтенных байтов

            // выполняем поиск requestLine

            // определим разделитель /r/n
            final byte[] requestLineSeparator = new byte[]{'\r', '\n'};
            // найдем окончание requestLine (если его нет, то вернем null вместо значения)
            final int indexEndRequestLine = Tools.findIndex(buffer, requestLineSeparator, 0, read);
            if (indexEndRequestLine == -1) {
                return null;
            }
            // считаем полученный requestLine
            final var requestLineArray = new String(Arrays.copyOf(buffer, indexEndRequestLine)).split(" ");
            // проверим наличие 3 обязательных элементов в requestLine (если их нет, то вернем null вместо значения)
            if (requestLineArray.length != 3) {
                return null;
            }

            // сформируем объект RequestLine

            //проверим корректно ли формируется path
            final var path = requestLineArray[1];
            if (!path.startsWith("/")) {
                return null;
            }
            RequestLine requestLine = new RequestLine(
                    requestLineArray[0],
                    requestLineArray[1],
                    requestLineArray[2]
            );

            // выполняем поиск заголовков

            // определим разделитель /r/n/r/n, которым должен закачиваться список заголовков
            final byte[] headersSeparator = new byte[]{'\r', '\n', '\r', '\n'};
            // найдем окончание HEADERS (если его нет, то вернем null вместо значения)
            int indexStartHeaders = indexEndRequestLine + requestLineSeparator.length;
            final int indexEndHeaders = Tools.findIndex(
                    buffer,
                    headersSeparator,
                    indexStartHeaders,
                    read
            );
            if (indexEndHeaders == -1) {
                return null;
            }
            final String headers = new String(Arrays.copyOfRange(
                    buffer,
                    indexStartHeaders,
                    indexEndHeaders
            ));

            // выполняем поиск тела запроса и формируем Request
            int indexStartBody = indexEndHeaders + headersSeparator.length;
            bis.reset();
            if (indexStartBody == bis.available()) {
                return new Request(requestLine, headers);
            }
            bis.skip(indexStartBody);

            // вычитываем Content-Length, чтобы прочитать body
            final var contentLength = Tools.extractHeader(
                    Arrays.asList(headers.split("\r\n")),
                    "Content-Length"
            );
            if (contentLength.isPresent()) {
                final var length = Integer.parseInt(contentLength.get());
                final var bodyBytes = bis.readNBytes(length);
                return new Request(requestLine, headers, bodyBytes);
            } else {
                return null;
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    // Bad request
    private void badRequest(BufferedOutputStream bos) {
        try {
            bos.write((
                    "HTTP/1.1 400 Bad Request\r\n" +
                            "Content-Length: 0\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            bos.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Not found
    private void notFound(BufferedOutputStream bos) {
        try {
            bos.write((
                    "HTTP/1.1 404 Not Found\r\n" +
                            "Content-Length: 0\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            bos.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void run() {
        try {
            final var in = new BufferedInputStream(SOCKET.getInputStream());
            final var out = new BufferedOutputStream(SOCKET.getOutputStream());
            // Парсим request
            Request request = getRequestFromBufferedInputStream(in);
            if (request == null) {
                badRequest(out);
            } else {
                String requestMethod = request.getRequestLine().getRequestMethod();
                String requestURL = request.getRequestLine().getRequestURL();
                int indexQuerySeparator = requestURL.indexOf('?');
                String requestPath = indexQuerySeparator > 0 ?
                        requestURL.substring(0, indexQuerySeparator) : requestURL;
                if (
                        HANDLERS_MAP.containsKey(requestMethod) &&
                                HANDLERS_MAP.get(requestMethod).containsKey(requestPath)
                ) {
                    Handler handler = HANDLERS_MAP.get(requestMethod).get(requestPath);
                    handler.handle(request, out);
                } else {
                    notFound(out);
                }
            }
            in.close();
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // закрываем соединение
        try {
            SOCKET.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
