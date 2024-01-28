package server;

import server.request.Request;
import server.request.RequestLine;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.Map;


public class ClientHandler implements Runnable {
    private final Socket SOCKET;
    private final Map<String, Map<String, Handler>> HANDLERS_MAP;

    public ClientHandler(Socket socket, Map<String, Map<String, Handler>> handlersMap) {
        this.SOCKET = socket;
        this.HANDLERS_MAP = handlersMap;
    }

    // Парсер Request
    private Request getRequestFromBufferedInputStream(BufferedInputStream bis) {
        try {
            // читаем весь запрос в байтах
            var buffer = new byte[bis.available()];
            bis.read(buffer);

            // выполняем поиск requestLine

            // определим разделитель /r/n
            final byte[] requestLineSeparator = new byte[]{'\r', '\n'};
            // найдем окончание requestLine (если его нет, то вернем null вместо значения)
            final int indexEndRequestLine = findIndex(buffer, requestLineSeparator, 0);
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
            final int indexEndHeaders = findIndex(
                    buffer,
                    headersSeparator,
                    indexStartHeaders
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
            if (indexStartBody == buffer.length) {
                return new Request(requestLine, headers);
            }
            bis.reset();
            bis.skip(indexStartBody);
            return new Request(requestLine, headers, bis);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Метод поиска Индекса
    private int findIndex(byte[] array, byte[] target, int start) {
        outer:
        for (int i = start; i < array.length - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
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
                if (
                        HANDLERS_MAP.containsKey(requestMethod) &&
                                HANDLERS_MAP.get(requestMethod).containsKey(requestURL)
                ) {
                    Handler handler = HANDLERS_MAP.get(requestMethod).get(requestURL);
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
