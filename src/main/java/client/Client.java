package client;

import common.Artwork;
import common.Request;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class Client implements AutoCloseable{
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;
    private static final int CONNECTION_TIMEOUT = 5000; // 5 секунд таймаут
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public Client() throws IOException {
        System.out.println("Подключение к серверу...");
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(SERVER_ADDRESS, SERVER_PORT), CONNECTION_TIMEOUT);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            System.out.println("Подключено к серверу");
        } catch (ConnectException e) {
            throw new IOException("Сервер не доступен. Пожалуйста, запустите сервер и попробуйте снова.", e);
        } catch (SocketTimeoutException e) {
            throw new IOException("Превышено время ожидания подключения к серверу.", e);
        } catch (IOException e) {
            throw new IOException("Ошибка подключения: " + e.getMessage(), e);
        }
    }

    public List<Artwork> getAllArtworks() throws Exception {
        if (socket.isClosed()) {
            throw new IOException("Соединение с сервером закрыто");
        }
        out.writeObject(new Request(Request.Operation.GET_ALL));
        Request response = (Request) in.readObject();
        if (response == null) {
            throw new IOException("Пустой ответ от сервера");
        }
        return response.getArtworks();
    }

    public Set<String> getActiveUsers() throws Exception {
        out.writeObject(new Request(Request.Operation.GET_ACTIVE_USERS));
        Request response = (Request) in.readObject();
        return new HashSet<>(response.getUserTypes());
    }

    public void addArtwork(Artwork artwork) throws Exception {
        System.out.println("Отправляю на сервер: " + artwork);
        out.reset(); // Сбрасываем поток, чтобы избежать ошибок сериализации
        out.writeObject(new Request(Request.Operation.ADD, artwork));
        out.flush();
        in.readObject(); // ожидаем подтверждение
        System.out.println("Произведение добавлено");
    }

    public void updateArtwork(Artwork artwork) throws Exception {
        out.reset();
        out.writeObject(new Request(Request.Operation.UPDATE, artwork));
        in.readObject(); // ожидаем подтверждение
    }

    public void deleteArtwork(Artwork artwork) throws Exception {
        out.reset();
        out.writeObject(new Request(Request.Operation.DELETE, artwork));
        in.readObject(); // ожидаем подтверждение
    }
    public void close() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            System.out.println("Соединение с сервером закрыто");
        } catch (IOException e) {
            System.err.println("Ошибка при закрытии соединения: " + e.getMessage());
        }
    }
}