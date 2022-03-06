public class main {

    public static void main(String[] args) {
        Proxy proxy = null;
        try {
            proxy = new Proxy(8080);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        proxy.listen();
        //listen for connections
    }
}
