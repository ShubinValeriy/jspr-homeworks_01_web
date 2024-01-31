package server.tools;

import java.util.List;
import java.util.Optional;

public class Tools {

    // поиск конкретного заголовка из списка заголовков
    public static Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
    }

    // Методы поиска Индекса
    public static int findIndex(byte[] array, byte[] target, int start) {
        return findIndex(array, target, start, array.length);
    }

    public static int findIndex(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }


}
