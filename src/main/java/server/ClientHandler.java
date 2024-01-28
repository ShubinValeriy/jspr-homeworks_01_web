package server;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;


public class ClientHandler implements Runnable {
    private final Socket SOCKET;
    private final List<String> VALID_PATH;

    public ClientHandler(Socket socket, List<String> validPaths) {
        this.SOCKET = socket;
        this.VALID_PATH = validPaths;
    }

    @Override
    public void run() {
        try {
            final var in = new BufferedReader(new InputStreamReader(SOCKET.getInputStream()));
            final var out = new BufferedOutputStream(SOCKET.getOutputStream());
            // Зачитываем  request line которая должна быть вида: GET /path HTTP/1.1
            final var requestLine = in.readLine();
            // Разбиваем полученную строку на элементы используя разделитель " "
            final var parts = requestLine.split(" ");

            // Проверяем состоит ли request line из необходимого количества элементов?
            if (parts.length != 3) {
                // если в заголовке меньше необходимого количества элементов, то отправим 400 ошибку
                out.write((
                        "HTTP/1.1 400 Bad Request\r\n" +
                                "Content-Length: 0\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                out.flush();
            } else if (!VALID_PATH.contains(parts[1])) {
                // проверяем есть ли адрес полученный в запросе в списке валидных директорий
                // если запрошенного адреса нет, то отправим 404 ошибку
                out.write((
                        "HTTP/1.1 404 Not Found\r\n" +
                                "Content-Length: 0\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                out.flush();
            } else {
                // определяем директорию запрашиваемого файла
                final var filePath = Path.of(".", "public", parts[1]);
                // определяем тип запрашиваемого файла
                final var mimeType = Files.probeContentType(filePath);
                // определяем тип запрашиваемого файла
                final var length = Files.size(filePath);

                // отработка запроса файла classic.html
                if (parts[1].equals("/classic.html")) {
                    final var template = Files.readString(filePath);
                    final var content = template.replace(
                            "{time}",
                            LocalDateTime.now().toString()
                    ).getBytes();
                    out.write((
                            "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: " + mimeType + "\r\n" +
                                    "Content-Length: " + content.length + "\r\n" +
                                    "Connection: close\r\n" +
                                    "\r\n"
                    ).getBytes());
                    out.write(content);
                    out.flush();
                } else {
                    // отработка остальных файлов
                    out.write((
                            "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: " + mimeType + "\r\n" +
                                    "Content-Length: " + length + "\r\n" +
                                    "Connection: close\r\n" +
                                    "\r\n"
                    ).getBytes());
                    Files.copy(filePath, out);
                    out.flush();
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
