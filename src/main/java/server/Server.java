package server;

import java.io.IOException;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Server {
    private final int COUNT_OF_THREAD;
    private Map<Integer, Boolean> isListen;
    private Map<String, Map<String, Handler>> handlersMap;


    public Server(int countOfThread) {
        this.COUNT_OF_THREAD = countOfThread;
        // используем потокобезопасные МАПы, так как будем использовать из разных потоков
        isListen = new ConcurrentHashMap<>();
        handlersMap = new ConcurrentHashMap<>();
        System.out.println("Сервер запущен!");
    }

    public void listen(int port) {
        if (!isListen.containsKey(port) && (isListen.get(port) == null || !isListen.get(port))) {
            final ExecutorService CONNECTION_POOL = Executors.newFixedThreadPool(COUNT_OF_THREAD);
            try {
                // создаём серверный сокет на порту полученном из настроек
                ServerSocket serverSocket = new ServerSocket(port);
                System.out.println("Слушаем порт: " + port);
                isListen.put(port, true);
                // запускаем цикл прослушивания в отдельном потоке, чтобы можно было остановить сервер
                Thread thread = new Thread(() -> {
                    // Запускаем бесконечный цикл, в котором будем ждать подключений
                    // и отрабатывать эти подключения в отдельных потоках
                    while (isListen.get(port)) {
                        try {
                            Socket socket = serverSocket.accept();
                            CONNECTION_POOL.execute(new ClientHandler(socket, handlersMap));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    // закрываем соединение, пишем уведомление и закрываем пулл потоков обработки
                    try {
                        serverSocket.close();
                        System.out.println("Порт " + port + " больше не прослушивается");
                        CONNECTION_POOL.shutdown();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                thread.start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            System.out.println("ВНИМАНИЕ! Указанный порт " + port + " уже прослушивается");
        }
    }

    public void close(int port) {
        if (isListen.containsKey(port) && isListen.get(port)) {
            isListen.put(port, false);
            try {
                Socket socket = new Socket("localhost", port);
                socket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            System.out.println("ВНИМАНИЕ! Указанный порт " + port + " не прослушивается");
        }

    }

    public void addHandler(String requestMethod, String requestURL, Handler handler) {
        if (!handlersMap.containsKey(requestMethod)) {
            handlersMap.put(requestMethod, new ConcurrentHashMap<>());
            handlersMap.get(requestMethod).put(requestURL, handler);
        } else {
            handlersMap.get(requestMethod).put(requestURL, handler);
        }
    }
}
