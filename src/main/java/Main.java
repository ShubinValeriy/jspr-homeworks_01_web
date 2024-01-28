import server.Server;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        var validPath = List.of("/index.html",
                "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js",
                "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");
        int port = 9999;
        int port2 = 1111;
        Server server = new Server(validPath, 64);
        server.listen(port);
        server.listen(port2);
        // даем поработать 30 секунд и закрываем
        TimeUnit.SECONDS.sleep(30);
        server.close(port);
        server.close(port2);
    }
}
