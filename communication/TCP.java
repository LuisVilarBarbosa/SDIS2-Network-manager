package communication;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;

class TCP {
    public static final int timeout = 3000;

    public static void send(Socket socket, Object object) throws Exception {
        socket.setSoTimeout(timeout);
        OutputStream outputStream = socket.getOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        objectOutputStream.writeObject(object);
    }

    public static Object receive(Socket socket) throws IOException {
        socket.setSoTimeout(timeout);
        Object object = null;

        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
            object = objectInputStream.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return object;
    }

}
