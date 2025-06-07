package server;

import common.Artwork;
import common.Request;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static final int PORT = 12345;
    private static List<Artwork> artworks = Collections.synchronizedList(new ArrayList<>());
    private static int currentId = 1;
    private static Map<String, Long> activeConnections = new ConcurrentHashMap<>();
    private static final long TIMEOUT = 30000; // 30 секунд

    public static void main(String[] args) throws IOException {
        System.out.println("Сервер запущен...");

        // Запускаем поток для очистки неактивных подключений
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(10000);
                    long currentTime = System.currentTimeMillis();
                    activeConnections.entrySet().removeIf(entry ->
                            currentTime - entry.getValue() > TIMEOUT
                    );
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(new ClientHandler(socket)).start();
            }
        }
    }

    static class ClientHandler implements Runnable {
        private ObjectInputStream in;
        private ObjectOutputStream out;
        private Socket socket;
        private String clientId;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                while (!socket.isClosed()) {
                    Request request = (Request) in.readObject();
                    if (request == null) {
                        System.out.println("Клиент отключился");
                        break;  // Выходим из цикла, если получили null
                    }

                    // Обновляем время последней активности
                    if (request.getArtwork() != null) {
                        clientId = request.getArtwork().getUserType();
                        activeConnections.put(clientId, System.currentTimeMillis());
                    }

                    switch (request.getOperation()) {
                        case GET_ALL:
                            Request response = new Request(Request.Operation.GET_ALL);
                            response.setArtworks(new ArrayList<>(artworks));
                            out.writeObject(response);
                            break;

                        case ADD:
                            Artwork newArt = request.getArtwork();
                            newArt = new Artwork(
                                    currentId++,
                                    newArt.getTitle(),
                                    newArt.getAuthor(),
                                    newArt.getType(),
                                    newArt.getYear(),
                                    newArt.getImageBase64(),
                                    newArt.getUserType()
                            );
                            artworks.add(newArt);
                            out.writeObject(new Request(Request.Operation.ADD));
                            break;

                        case GET_ACTIVE_USERS:
                            Request usersResponse = new Request(Request.Operation.GET_ACTIVE_USERS);
                            usersResponse.setUserTypes(new HashSet<>(activeConnections.keySet()));
                            out.writeObject(usersResponse);
                            break;

                        case DELETE:
                            int idDel = request.getArtwork().getId();
                            artworks.removeIf(a -> a.getId() == idDel);
                            out.writeObject(new Request(Request.Operation.DELETE));
                            break;

                        case UPDATE:
                            Artwork updated = request.getArtwork();
                            artworks.replaceAll(a -> a.getId() == updated.getId() ? updated : a);
                            out.writeObject(new Request(Request.Operation.UPDATE));
                            break;
                    }
                    out.flush();
                }
            } catch (Exception e) {
                System.err.println("Ошибка обработки клиента: " + e.getMessage());
            } finally {
                if (clientId != null) {
                    activeConnections.remove(clientId);
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    System.err.println("Ошибка закрытия сокета: " + e.getMessage());
                }
            }
        }
    }

    public static Set<String> getActiveUsers() {
        return activeConnections.keySet();
    }
}