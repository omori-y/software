import java.io.*;
import java.net.*;
import java.util.*;

public class OthelloServer {
    private static final int PORT = 6000;
    private ServerSocket serverSocket;
    private List<OthelloGameRoom> rooms = new ArrayList<>();

    public OthelloServer() throws IOException {
        serverSocket = new ServerSocket(PORT);
        System.out.println("サーバー起動");

        while (true) {
            Socket socket = serverSocket.accept();
            System.out.println("新しいクライアント接続: " + socket.getInetAddress());

            boolean joined = false;
            for (OthelloGameRoom room : rooms) {
                if (!room.isFull()) {
                    room.addPlayer(socket);
                    joined = true;
                    break;
                }
            }

            if (!joined) {
                OthelloGameRoom newRoom = new OthelloGameRoom();
                newRoom.addPlayer(socket);
                rooms.add(newRoom);
                new Thread(newRoom).start();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        new OthelloServer();
    }
}
