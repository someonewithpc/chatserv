import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;

/**
 * This is the main class for the Chat Client
 */
public class
ChatClient
{
        // Variáveis relacionadas com a interface gráfica --- * NÃO MODIFICAR *
        JFrame frame = new JFrame("Chat Client");
        private JTextField chatBox = new JTextField();
        private JTextArea chatArea = new JTextArea();
        // --- Fim das variáveis relacionadas com a interface gráfica

        // Se for necessário adicionar variáveis ao objecto ChatClient, devem
        // ser colocadas aqui

        // Socket info
        private SocketChannel client_socket;

        // Decoder and enconder for transmitting text
        private final Charset charset = StandardCharsets.UTF_8;
        private final CharsetEncoder encoder = charset.newEncoder();

        /**
         * Método a usar para acrescentar uma string à caixa de texto
         * * NÃO MODIFICAR *
         *
         * @param message
         */
        public void
        printMessage (final String message)
        {
                chatArea.append(message);
        }

        /**
         * Constructor
         *
         * @param server server_host
         * @param port server_port
         * @throws IOException
         */
        public ChatClient (String server, int port)
        throws IOException
        {
                // Inicialização da interface gráfica --- * NÃO MODIFICAR *
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                JPanel panel = new JPanel();
                panel.setLayout(new BorderLayout());
                panel.add(chatBox);
                frame.setLayout(new BorderLayout());
                frame.add(panel, BorderLayout.SOUTH);
                frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);
                frame.setSize(500, 300);
                frame.setVisible(true);
                chatArea.setEditable(false);
                chatBox.setEditable(true);
                chatBox.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                                try {
                                        newMessage(chatBox.getText());
                                } catch (IOException ex) {
                                } finally {
                                        chatBox.setText("");
                                }
                        }
                });
                frame.addWindowListener(new WindowAdapter() {
                        public void windowOpened(WindowEvent e) {
                                chatBox.requestFocus();
                        }
                });
                // --- Fim da inicialização da interface gráfica

                // Se for necessário adicionar código de inicialização ao
                // construtor, deve ser colocado aqui

                // Socket information
                try {
                        client_socket = SocketChannel.open();
                        client_socket.configureBlocking(true);
                        client_socket.connect(new InetSocketAddress(server, port));
                } catch (IOException ex) {
                        // Fail graciously if the server is offline.
                }
        }

        /**
         * Instancia o ChatClient e arranca-o invocando o seu método run()
         * * NÃO MODIFICAR *
         *
         * @param args
         * @throws IOException
         */
        public static void
        main (String[] args)
        throws IOException
        {
                ChatClient client = new ChatClient(args[0], Integer.parseInt(args[1]));
                client.run();
        }

        /**
         * Método invocado sempre que o utilizador insere uma mensagem
         * na caixa de entrada
         *
         * @param message
         * @throws IOException
         */
        public void
        newMessage (String message)
        throws IOException
        {
                // PREENCHER AQUI com código que envia a mensagem ao servidor

                client_socket.write(encoder.encode(CharBuffer.wrap(message + "\n")));
        }

        /**
         * Método principal do objecto
         *
         * @throws IOException
         */
        private void
        run ()
        throws IOException
        {
                // PREENCHER AQUI
                try
                {
                        while (!client_socket.finishConnect())
                        {;} /* Deliberately blank. */
                }
                catch (IOException ce)
                {
                        System.err.println("Unable to connect to the server...");
                        System.exit(0);
                        return;
                }

                BufferedReader input_reader =
                        new BufferedReader(new InputStreamReader(client_socket.socket().getInputStream()));

                // Listen loop
                while (true)
                {
                        String message = input_reader.readLine();

                        if (message == null)
                        {
                                break;
                        }

                        System.out.println("RECEIVED: " + message);
                        message = message.trim();

                        printMessage(Message.parse_string(message).toString(true));
                        int len = chatArea.getDocument().getLength();  // Length of chat text in lines
                        chatArea.setCaretPosition(len);                // Scroll to bottom
                }

                System.out.println("Terminated.");
                client_socket.close();
                System.exit(0);
        }
}
