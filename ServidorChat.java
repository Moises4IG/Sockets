import java.io.*;
import java.net.*;
import java.util.*;

public class ServidorChat {
    private static final int PUERTO = 6789;
    private static Set<ClienteHandler> clientHandlers = Collections.synchronizedSet(new HashSet<>());

    public static void main(String[] args) throws Exception {
        System.out.println("El servidor de chat está corriendo...");
        ServerSocket listener = new ServerSocket(PUERTO, 0, InetAddress.getByName("0.0.0.0"));

        try {
            while (true) {
                Socket socket = listener.accept();
                ClienteHandler handler = new ClienteHandler(socket);
                clientHandlers.add(handler);
                handler.start();
            }
        } finally {
            listener.close();
        }
    }

    private static class ClienteHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String nombreUsuario;

        public ClienteHandler(Socket socket) {
            this.socket = socket;
            System.out.println("Cliente conectado: " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                nombreUsuario = in.readLine().trim();
                if (!nombreUsuario.isEmpty()) {
                    broadcast("NUEVO_USUARIO " + nombreUsuario);
                }

                String input;
                while ((input = in.readLine()) != null) {
                    broadcast(nombreUsuario + ": " + input);
                }
            } catch (SocketException e) {
                System.out.println(nombreUsuario + " se ha desconectado abruptamente.");
            } catch (IOException e) {
                System.out.println("Error en ClienteHandler: " + e.getMessage());
                e.printStackTrace();
            } finally {
                if (nombreUsuario != null && !nombreUsuario.isEmpty()) {
                    System.out.println(nombreUsuario + " se ha desconectado.");
                    broadcast("USUARIO_DESCONECTADO " + nombreUsuario);
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    System.out.println("Error al cerrar el socket: " + e.getMessage());
                    e.printStackTrace();
                }
                clientHandlers.remove(this);
            }
        }

        private void broadcast(String message) {
            for (ClienteHandler handler : clientHandlers) {
                handler.out.println(message);
            }
        }
    }
}
