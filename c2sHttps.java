import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;

public class c2sHttps implements Runnable {
    InputStream p2c;
    OutputStream p2s;

    public c2sHttps(InputStream p2c, OutputStream p2s){
        this.p2c = p2c;
        this.p2s = p2s;
    }


    @Override
    public void run(){
        try {
            // Read byte by byte from client and send directly to server
            byte[] buffer = new byte[4096];
            int in;
            do {
                in = p2c.read(buffer);
                if (in > 0) {
                    p2s.write(buffer, 0, in);
                    if (p2c.available() < 1) {
                        p2s.flush();
                    }
                }
            } while (in >= 0);
        }
        catch (SocketTimeoutException ste) {
            System.out.println("timeout");
        }
        catch (IOException e) {
            System.out.println("Proxy to client HTTPS read timed out");
            e.printStackTrace();
        }
    }
}
