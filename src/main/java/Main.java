
import server.Handler;
import server.Server;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;


public class Main {
    public static void main(String[] args) throws InterruptedException {
        // Задание № 1
//        var validPath = List.of("/index.html",
//                "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js",
//                "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");
//        int port = 9999;
//        int port2 = 1111;
//        Server server = new Server(validPath, 64);
//        server.listen(port);
//        server.listen(port2);
//        // даем поработать 30 секунд и закрываем
//        TimeUnit.SECONDS.sleep(30);
//        server.close(port);
//        server.close(port2);


        // Задание № 2
        final var server = new Server(64);
        // код инициализации сервера (из вашего предыдущего ДЗ)

        // добавление хендлеров (обработчиков)
        Handler handler = (request, responseStream) -> {
            try {
                // определяем директорию запрашиваемого файла
                final var filePath = Path.of(
                        ".",
                        "public",
                        request.getRequestLine().getRequestURL());
                // определяем тип запрашиваемого файла
                final var mimeType = Files.probeContentType(filePath);
                // определяем тип запрашиваемого файла
                final var length = Files.size(filePath);
                responseStream.write((
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: " + mimeType + "\r\n" +
                                "Content-Length: " + length + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                Files.copy(filePath, responseStream);
                responseStream.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };

        Handler handlerForClassic = (request, responseStream) -> {
            try {
                // определяем директорию запрашиваемого файла
                final var filePath = Path.of(
                        ".",
                        "public",
                        request.getRequestLine().getRequestURL());
                // определяем тип запрашиваемого файла
                final var mimeType = Files.probeContentType(filePath);
                final var template = Files.readString(filePath);
                final var content = template.replace(
                        "{time}",
                        LocalDateTime.now().toString()
                ).getBytes();
                responseStream.write((
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: " + mimeType + "\r\n" +
                                "Content-Length: " + content.length + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                responseStream.write(content);
                responseStream.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };

        Handler handlerForPrimitive = (request, responseStream) -> {
            try {
                responseStream.write((
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Length: " + 0 + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                responseStream.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };

        server.addHandler("GET", "/spring.svg", handler);
        server.addHandler("GET", "/styles.css", handler);
        server.addHandler("GET", "/spring.png", handler);
        server.addHandler("GET", "/app.js", handler);
        server.addHandler("GET", "/resources.html", handler);
        server.addHandler("GET", "/classic.html", handlerForClassic);
        server.addHandler("GET", "/", handlerForPrimitive);

        server.listen(9999);
        // даем поработать 30 секунд и закрываем
        TimeUnit.SECONDS.sleep(30);
        server.close(9999);

    }
}
