import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is the main class for the Chat Server
 */
public class
ChatServer
{
        // A pre-allocated buffer for the received data
        private static final ByteBuffer buffer = ByteBuffer.allocate(16384);

        // Decoder and encoder for transmitting text
        private static final Charset charset = StandardCharsets.UTF_8;
        private static final CharsetDecoder decoder = charset.newDecoder();
        private static final CharsetEncoder encoder = charset.newEncoder();

        // Regex for message process
        private static final String REGEX_MSG_NEW_NICKNAME = "nick " + Message.REGEX_NICKNAME;
        private static final String REGEX_MSG_JOIN = "join " + Message.REGEX_ROOM_NAME;
        private static final String REGEX_MSG_LEAVE = "leave";
        private static final String REGEX_MSG_BYE = "bye";
        private static final String REGEX_MSG_PRIVATE = "priv " + Message.REGEX_NICKNAME + " " + Message.REGEX_TEXT;

        // Users info
        private static final HashMap<String, User> nickname_user = new HashMap<>();
        private static final HashMap<String, Room> nickname_room = new HashMap<>();

        /**
         * Run the server
         *
         * @param args
         */
        public static void
        main (String[] args)
        {
                // Port to listen
                int server_port = Integer.parseInt(args[0]);

                try
                {
                        // Instead of creating a ServerSocket, create a ServerSocketChannel
                        ServerSocketChannel ssc = ServerSocketChannel.open();

                        // Set it to non-blocking, so we can use select
                        ssc.configureBlocking(false);

                        // Get the Socket connected to this channel, and bind
                        // it to the listening port
                        ServerSocket ss = ssc.socket();
                        InetSocketAddress isa = new InetSocketAddress(server_port);
                        ss.bind(isa);

                        // Create a new Selector for selecting
                        Selector selector = Selector.open();

                        // Register the ServerSocketChannel, so we can listen
                        // for incoming connections
                        ssc.register(selector, SelectionKey.OP_ACCEPT);
                        System.out.println("Listening on port " + server_port);

                        while (true)
                        {
                                // See if we've had any activity -- either an incoming connection,
                                // or incoming data on an existing connection
                                int num = selector.select();

                                // If we don't have any activity, loop around and wait again
                                if (num == 0)
                                {
                                        continue;
                                }

                                // Get the keys corresponding to the activity that has been
                                // detected, and process them one by one
                                Set<SelectionKey> keys = selector.selectedKeys();
                                for (SelectionKey key : keys)
                                {
                                        // Get a key representing one of bits of I/O activity
                                        // What kind of activity is it?
                                        if (key.isAcceptable())
                                        {
                                                // It's an incoming connection.  Register this socket with
                                                // the Selector so we can listen for input on it
                                                Socket s = ss.accept();
                                                System.out.println("Got connection from " + s + ".");

                                                // Make sure to make it non-blocking, so we can use a selector
                                                // on it.
                                                SocketChannel sc = s.getChannel();
                                                sc.configureBlocking(false);

                                                // Register it with the selector, for reading
                                                sc.register(selector, SelectionKey.OP_READ, new User(sc));
                                        }
                                        else if (key.isReadable())
                                        {
                                                SocketChannel sc = null;

                                                try
                                                {
                                                        // It's incoming data on a connection -- process it
                                                        sc = (SocketChannel) key.channel();
                                                        boolean ok = processInput(key, sc);

                                                        // If the connection is dead, remove it from the selector
                                                        // and close it
                                                        if (!ok)
                                                        {
                                                                key.cancel();
                                                                close_client(key, sc);
                                                        }
                                                }
                                                catch (IOException ie)
                                                {
                                                        // On exception, remove this channel from the selector
                                                        key.cancel();
                                                        close_client(key, sc);
                                                }
                                        }
                                }

                                // We remove the selected keys, because we've dealt with them.
                                keys.clear();
                        }
                }
                catch (IOException ie)
                {
                        System.err.println(ie.getMessage());
                }
        }

        /**
         * Close a connection with a client
         *
         * @param key
         * @param sc
         */
        private static void
        close_client (SelectionKey key, SocketChannel sc)
        {
                User sender = (User)key.attachment();
                if (sender.get_state() == State.INSIDE)
                {
                        Room room = sender.get_room();
                        room.left_user(sender);
                        TreeSet<User> user_list = room.get_users();

                        for (User user : user_list)
                        {
                                try
                                {
                                        send_left_message(user, sender.get_nickname());
                                }
                                catch (IOException ie)
                                {
                                        System.err.println("Error sending left message: " + ie);
                                }
                        }

                        if (user_list.size() == 0)
                        {
                                nickname_room.remove(room.get_name());
                        }
                }

                nickname_user.remove(sender.get_nickname());
                key.cancel();

                Socket s = sc.socket();
                try
                {
                        System.out.println("Closing connection to " + s);
                        sc.close();
                }
                catch (IOException ie)
                {
                        System.err.println("Error closing socket " + s + ": " + ie);
                }
        }

        /**
         * Read a message from the socket and process it
         *
         * @param key
         * @param sc
         * @return boolean false if connection closed, true otherwise
         * @throws IOException
         */
        private static boolean
        processInput (SelectionKey key, SocketChannel sc)
        throws IOException
        {
                // Read the message to the buffer
                buffer.clear();
                sc.read(buffer);
                buffer.flip();

                // If no data, close the connection
                if (buffer.limit() == 0)
                {
                        return false;
                }

                // Decode and add to buffer
                User sender = (User)key.attachment();
                sender.add_to_buffer(decoder.decode(buffer).toString());
                String instructions = sender.get_buffer().toString();

                // // Print byte representation of incoming package
                // for (final char c : instructions.toCharArray()) {
                //         System.out.print((int) c + " ");
                // }
                // System.out.println();

                // We only want to process full instructions
                if (!instructions.endsWith("\n"))
                {
                        return true;
                }

                for (String instruction : instructions.split("\n"))
                {
                        Matcher tokens;
                        if (instruction.startsWith("/"))
                        {
                                String cmd = instruction.substring(1).trim();

                                if ((tokens = Pattern.compile(REGEX_MSG_NEW_NICKNAME).matcher(cmd)).find())
                                {
                                        send_nickname_command(sender, tokens.group(1));
                                }
                                else if ((tokens = Pattern.compile(REGEX_MSG_JOIN).matcher(cmd)).find())
                                {
                                        send_join_command(sender, tokens.group(1));
                                }
                                else if (Pattern.matches(REGEX_MSG_LEAVE, cmd))
                                {
                                        send_leave_command(sender);
                                }
                                else if (Pattern.matches(REGEX_MSG_BYE, cmd))
                                {
                                        send_bye_command(key, sender);
                                }
                                else if ((tokens = Pattern.compile(REGEX_MSG_PRIVATE).matcher(cmd)).find())
                                {
                                        send_private_command(
                                            sender,
                                            // Emitter
                                            tokens.group(1),
                                            // Message
                                            tokens.group(2)
                                        );
                                }
                                else if (cmd.startsWith("/"))
                                {
                                        send_public_message(sender, cmd);
                                }
                                else
                                {
                                        send_error_message(sender, "Unknown command.");
                                }

                        }
                        else if (instruction.length() != 0)
                        {
                                send_public_message(sender, instruction.trim());
                        }


                        sender.advance_buffer(instruction.length() + 1);
                }
                return true;
        }

        /**
         * Helper function to send a message
         *
         * @param sc
         * @param message
         * @throws IOException
         */
        private static void
        send_message (SocketChannel sc, Message message)
        throws IOException
        {
                sc.write(encoder.encode(CharBuffer.wrap(message.toString())));
        }

        /**
         * Helper function to send a message
         *
         * @param receiver
         * @param sender
         * @param message_value
         * @throws IOException
         */
        private static void
        send_message (User receiver, String sender, String message_value)
        throws IOException
        {
                Message message = new Message(MessageType.MESSAGE, sender, message_value);
                send_message(receiver.get_socket(), message);
        }

        /**
         * Send error message
         *
         * @param receiver
         * @param error_message
         * @throws IOException
         */
        private static void
        send_error_message (User receiver, String error_message)
        throws IOException
        {
                Message message = new Message(MessageType.ERROR, error_message);
                send_message(receiver.get_socket(), message);
        }

        /**
         * Send ok message
         *
         * @param receiver
         * @throws IOException
         */
        private static void
        send_ok_message (User receiver)
        throws IOException
        {
                Message message = new Message(MessageType.OK);
                send_message(receiver.get_socket(), message);
        }

        /**
         * Send newnick message
         *
         * @param receiver
         * @param old_nickname
         * @param new_nickname
         * @throws IOException
         */
        private static void
        send_nickname_message (User receiver, String old_nickname, String new_nickname)
        throws IOException
        {
                Message message = new Message(MessageType.NEW_NICKNAME, old_nickname, new_nickname);
                send_message(receiver.get_socket(), message);
        }

        /**
         * Send joined message
         *
         * @param receiver
         * @param join_nickname
         * @throws IOException
         */
        private static void
        send_joined_message (User receiver, String join_nickname)
        throws IOException
        {
                Message message = new Message(MessageType.JOINED, join_nickname);
                send_message(receiver.get_socket(), message);
        }

        /**
         * Send left message
         *
         * @param receiver
         * @param left_nickname
         * @throws IOException
         */
        private static void
        send_left_message (User receiver, String left_nickname)
        throws IOException
        {
                Message message = new Message(MessageType.LEFT, left_nickname);
                send_message(receiver.get_socket(), message);
        }

        /**
         * Send bye message
         *
         * @param receiver
         * @throws IOException
         */
        private static void
        send_bye_message (User receiver)
        throws IOException
        {
                Message message = new Message(MessageType.BYE);
                send_message(receiver.get_socket(), message);
        }

        /**
         * Send private message
         *
         * @param receiver
         * @param sender
         * @param message_value
         * @throws IOException
         */
        private static void
        send_private_message (User receiver, String sender, String message_value)
        throws IOException
        {
                Message message = new Message(MessageType.PRIVATE, sender, message_value);
                send_message(receiver.get_socket(), message);
        }

        /**
         * Send simple message
         *
         * @param sender
         * @param message_value
         * @throws IOException
         */
        private static void
        send_public_message (User sender, String message_value)
        throws IOException
        {
                if (sender.get_state() == State.INSIDE)
                {
                        Room sender_room = sender.get_room();
                        TreeSet<User> user_list = sender_room.get_users();

                        for (User user : user_list)
                        {
                                send_message(user, sender.get_nickname(), message_value);
                        }
                }
                else
                {
                        send_error_message(sender, "You are not in a room.");
                }
        }

        /**
         * Send nick command
         *
         * @param sender
         * @param nick
         * @throws IOException
         */
        private static void
        send_nickname_command (User sender, String nick)
        throws IOException
        {
                if (
                        // Allow to change to the current nick
                        // Only compare if the user already has a nickname
                        ((sender.get_state() != State.INIT)
                         && sender.get_nickname().equals(nick))
                        || // Don't allow to set a nickname already in use
                        !nickname_user.containsKey(nick)
                   )
                {
                        if (sender.get_state() == State.INIT)
                        {
                                sender.set_state(State.OUTSIDE);
                        }

                        if (sender.get_state() == State.INSIDE)
                        {
                                Room sender_room = sender.get_room();
                                TreeSet<User> user_list = sender_room.get_users();

                                for (User user : user_list)
                                {
                                        if (user != sender)
                                        {
                                                send_nickname_message(user, sender.get_nickname(), nick);
                                        }
                                }
                        }

                        nickname_user.remove(sender.get_nickname());
                        nickname_user.put(nick, sender);
                        send_ok_message(sender);
                        sender.set_nickname(nick);
                }
                else
                {
                        send_error_message(sender, "There already is a user with nick " + nick);
                }
        }

        /**
         * Send join command
         *
         * @param sender
         * @param room_name
         * @throws IOException
         */
        private static void
        send_join_command (User sender, String room_name)
        throws IOException
        {
                if (sender.get_state() == State.INIT)
                {
                        send_error_message(sender, "You don't have a nickname.");
                }
                /*else if (sender.get_room().get_name().equals(room_name))
                {
                        send_error_message(sender, "You already are in that room.");
                }*/
                else
                {
                        // If already in a room, leave it first
                        if (sender.get_state() == State.INSIDE)
                        {
                                send_leave_command(sender);
                        }

                        // If room doesn't exist
                        if (!nickname_room.containsKey(room_name))
                        {
                                nickname_room.put(room_name, new Room(room_name));
                        }

                        // Notify
                        Room new_room = nickname_room.get(room_name);
                        TreeSet<User> current_room_user_list = new_room.get_users();
                        new_room.join_user(sender);

                        for (User user : current_room_user_list)
                        {
                                send_joined_message(user, sender.get_nickname());
                        }

                        send_ok_message(sender);
                        sender.set_room(new_room);
                        sender.set_state(State.INSIDE);
                }
        }

        /**
         * Send leave command
         *
         * @param sender
         * @throws IOException
         */
        private static void
        send_leave_command (User sender)
        throws IOException
        {
                if (sender.get_state() != State.INSIDE)
                {
                        send_error_message(sender, "You are not in a room.");
                }
                else
                {
                        Room room = sender.get_room();
                        room.left_user(sender);
                        TreeSet<User> user_list = room.get_users();

                        for (User user : user_list)
                        {
                                send_left_message(user, sender.get_nickname());
                        }

                        if (user_list.size() == 0)
                        {
                                nickname_room.remove(room.get_name());
                        }

                        send_ok_message(sender);
                        sender.set_state(State.OUTSIDE);
                }
        }


        /**
         * Send bye command
         *
         * @param key
         * @param sender
         * @throws IOException
         */
        private static void
        send_bye_command (SelectionKey key, User sender)
        throws IOException
        {
                send_bye_message(sender);
                close_client(key, sender.get_socket());
        }

        /**
         * Send private message
         *
         * @param sender
         * @param receiver
         * @param message_value
         * @throws IOException
         */
        private static void
        send_private_command (User sender, String receiver, String message_value)
        throws IOException
        {
                if (sender.get_state() == State.INIT)
                {
                        send_error_message(sender, "You don't have a nickname.");
                }
                else
                {
                        if (nickname_user.containsKey(receiver))
                        {
                                send_ok_message(sender);
                                send_private_message(nickname_user.get(receiver), sender.get_nickname(), message_value);
                        }
                        else
                        {
                                send_error_message(sender, receiver + ": No such nickname online.");
                        }
                }
        }
}
