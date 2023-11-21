import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.Map;

public class ClienteChatGUI {
    private JFrame frame;
    private JTextPane textPaneChat;
    private JTextField textFieldInput;
    private JButton sendButton;
    private PrintWriter out;
    private Socket socket;
    private StyledDocument doc;
    private Map<String, Color> userColors;
    private SimpleAttributeSet userStyle;
    private String nombreUsuario;

    public ClienteChatGUI() {
        userColors = new LinkedHashMap<>();
        userStyle = new SimpleAttributeSet();

        frame = new JFrame("Chat de Actividad Sockets");
        textPaneChat = new JTextPane();
        textPaneChat.setEditable(false);
        doc = textPaneChat.getStyledDocument();
        textFieldInput = new JTextField("Escriba aquí su nombre");
        textFieldInput.setForeground(Color.GRAY);
        textFieldInput.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (textFieldInput.getText().equals("Escriba aquí su nombre")) {
                    textFieldInput.setText("");
                    textFieldInput.setForeground(Color.BLACK);
                }
            }
        });
        sendButton = new JButton("Enviar");

        JPanel panelInferior = new JPanel(new BorderLayout());
        panelInferior.add(textFieldInput, BorderLayout.CENTER);
        panelInferior.add(sendButton, BorderLayout.EAST);

        frame.setLayout(new BorderLayout());
        frame.add(new JScrollPane(textPaneChat), BorderLayout.CENTER);
        frame.add(panelInferior, BorderLayout.SOUTH);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 600);
        frame.setVisible(true);

        establecerListeners();
        iniciarConexion();
    }

    private void establecerListeners() {
        ActionListener actionListener = e -> procesarEntradaUsuario();
        textFieldInput.addActionListener(actionListener);
        sendButton.addActionListener(actionListener);
    }

    private void procesarEntradaUsuario() {
        if (nombreUsuario == null) {
            nombreUsuario = textFieldInput.getText().toUpperCase();
            textFieldInput.setText("");
            out.println(nombreUsuario);
        } else {
            enviarMensaje();
        }
    }

    private void iniciarConexion() {
        try {
            socket = new Socket("172.100.2.180", 6789);
            out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            new Thread(() -> {
                try {
                    String mensajeDelServidor;
                    while ((mensajeDelServidor = in.readLine()) != null) {
                        appendToPane(mensajeDelServidor);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "No se puede conectar al servidor", "Error de conexión", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
    }

    private void enviarMensaje() {
        String mensaje = textFieldInput.getText().trim();
        if (!mensaje.isEmpty()) {
            out.println(mensaje);
            textFieldInput.setText("");
        }
    }

    private void appendToPane(String message) {
        SwingUtilities.invokeLater(() -> {
            try {
                if (message.startsWith("NUEVO_USUARIO") || message.startsWith("USUARIO_DESCONECTADO")) {
                    StyleConstants.setBold(userStyle, true);
                    doc.insertString(doc.getLength(), message + "\n", userStyle);
                } else {
                    int colonIndex = message.indexOf(":");
                    if (colonIndex > -1) {
                        String nombre = message.substring(0, colonIndex).trim();
                        String texto = message.substring(colonIndex + 1).trim();
                        Color color = userColors.computeIfAbsent(nombre, k -> getNextColor());
                        StyleConstants.setForeground(userStyle, color);
                        doc.insertString(doc.getLength(), nombre + ": ", userStyle);
                        doc.insertString(doc.getLength(), texto + "\n", null);
                    }
                }
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }

    private Color getNextColor() {
        return new Color(
            (int) (Math.random() * 256),
            (int) (Math.random() * 256),
            (int) (Math.random() * 256)
        );
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClienteChatGUI());
    }
}
