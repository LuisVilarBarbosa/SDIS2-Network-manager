package communication;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

class TCP {

    public static void send(Socket socket, Object object) throws Exception {
        socket.setSoTimeout(3000); //3 sec
        OutputStream outputStream = socket.getOutputStream();
        PrintWriter prtWriter = new PrintWriter(outputStream, true); //True for flushing the buffer
        prtWriter.println(object);
        outputStream.close();
    }

    public static Object receive(Socket socket) throws IOException {
        socket.setSoTimeout(3000); //3 sec
        Object object = null;

        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
            object = objectInputStream.readObject();
            objectInputStream.close();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return object;
    }

}
