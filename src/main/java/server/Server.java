package server;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Server {
    private final List<String> VALID_PATH;
    private final int COUNT_OF_THREAD;
    private Map<Integer, Boolean> isListen;



    public Server(List<String> VALID_PATH, int countOfThread) {
        this.VALID_PATH = VALID_PATH;
        this.COUNT_OF_THREAD = countOfThread;
        // используем потокобезопасную МАПу, так как будем использовать из разных потоков
        isListen = new ConcurrentHashMap<>();
        System.out.println("Сервер запущен!");
    }

    public void listen(int port) {
        if (!isListen.containsKey(port) && (isListen.get(port) == null || !isListen.get(port))){
            final ExecutorService CONNECTION_POOL = Executors.newFixedThreadPool(COUNT_OF_THREAD);
            try {
                // создаём серверный сокет на порту полученном из настроек
                ServerSocket serverSocket = new ServerSocket(port);
                System.out.println("Слушаем порт: " + port);
                isListen.put(port,true);
                // запускаем цикл прослушивания в отдельном потоке, чтобы можно было остановить сервер
                Thread thread = new Thread(() -> {
                    // Запускаем бесконечный цикл, в котором будем ждать подключений
                    // и отрабатывать эти подключения в отдельных потоках
                    while (isListen.get(port)) {
                        try {
                            Socket socket = serverSocket.accept();
                            CONNECTION_POOL.execute(new ClientHandler(socket, VALID_PATH));
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
        }else {
            System.out.println("ВНИМАНИЕ! Указанный порт " + port + " уже прослушивается");
        }
    }

    public void close(int port) {
        if (isListen.containsKey(port) && isListen.get(port)){
            isListen.put(port,false);
            try {
                Socket socket = new Socket("localhost", port);
                var outputMessage = new ObjectOutputStream(socket.getOutputStream());
                outputMessage.writeObject("close");
                outputMessage.close();
                socket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            System.out.println("ВНИМАНИЕ! Указанный порт " + port + " не прослушивается");
        }

    }
}
