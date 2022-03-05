import java.io.*;
import java.net.Socket;

public class Handler implements Runnable{
    Socket socket;
    private Thread https;
    BufferedReader reader;
    BufferedWriter writer;
    String request;

    public Handler(Socket socket) throws IOException {
        this.socket = socket;
        this.socket.setSoTimeout(3000);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

    }

    @Override
    public void run() {
        try {
            request = reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
