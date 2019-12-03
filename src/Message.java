import java.util.regex.Pattern;

/**
 * Type of the Message
 */
enum MessageType
{
        OK, ERROR, MESSAGE, NEW_NICKNAME, JOINED, LEFT, BYE, PRIVATE
}

/**
 * Message class
 */
public class
Message
{
        // Regex for command process
        private static final String REGEX_OK = "OK";
        private static final String REGEX_ERROR = "ERROR";
        private static final String REGEX_MESSAGE = "MESSAGE .+ .*";
        private static final String REGEX_NEW_NICKNAME = "NEWNICK .+ .+";
        private static final String REGEX_JOINED = "JOINED .+";
        private static final String REGEX_LEFT = "LEFT .+";
        private static final String REGEX_BYE = "BYE";
        private static final String REGEX_PRIVATE = "PRIVATE .+ .+";
        private final MessageType type;
        // There are no getters for these two attributes, refer to toString
        private final String token1;
        private final String token2;


        /**
         * Constructor
         *
         * @param type
         */
        public Message (MessageType type)
        {
                this.type = type;
                this.token1 = "";
                this.token2 = "";
        }

        /**
         * Constructor
         *
         * @param type
         * @param token1
         */
        public Message (MessageType type, String token1)
        {
                this.type = type;
                this.token1 = token1;
                this.token2 = "";
        }

        /**
         * Constructor
         *
         * @param type
         * @param token1
         * @param token2
         */
        public Message (MessageType type, String token1, String token2)
        {
                this.type = type;
                this.token1 = token1;
                this.token2 = token2;
        }

        /**
         * Message from String
         * ONLY TO BE USED BY CHAT CLIENT (that is, command process and not
         * message process as happens in the server)
         *
         * @param text
         * @return Message
         */
        public static Message
        parse_string (String text)
        {
                MessageType type;
                String token1 = "";
                String token2 = "";

                if (Pattern.matches(REGEX_OK, text))
                {
                        type = MessageType.OK;
                }
                else if (Pattern.matches(REGEX_ERROR, text))
                {
                        type = MessageType.ERROR;
                }
                else if (Pattern.matches(REGEX_MESSAGE, text))
                {
                        type = MessageType.MESSAGE;
                        int receiver_begin = text.indexOf(" ")+1,
                                receiver_end = text.indexOf(" ", receiver_begin);
                        token1 = text.substring(receiver_begin, receiver_end); // Emitter
                        token2 = text.substring(receiver_end+1); // Message
                }
                else if (Pattern.matches(REGEX_NEW_NICKNAME, text))
                {
                        type = MessageType.NEW_NICKNAME;
                        token1 = text.split(" ")[1];
                        token2 = text.split(" ")[2];
                }
                else if (Pattern.matches(REGEX_JOINED, text))
                {
                        type = MessageType.JOINED;
                        token1 = text.split(" ")[1];
                }
                else if (Pattern.matches(REGEX_LEFT, text))
                {
                        type = MessageType.LEFT;
                        token1 = text.split(" ")[1];
                }
                else if (Pattern.matches(REGEX_BYE, text))
                {
                        type = MessageType.BYE;
                }
                else if (Pattern.matches(REGEX_PRIVATE, text))
                {
                        type = MessageType.PRIVATE;
                        // Delimiters
                        int receiver_begin = text.indexOf(" ")+1,
                            receiver_end = text.indexOf(" ", receiver_begin);
                        token1 = text.substring(receiver_begin, receiver_end); // Emitter
                        token2 = text.substring(receiver_end+1); // Message
                }
                else
                {
                        type = MessageType.ERROR;
                }

                return new Message(type, token1, token2);
        }

        /**
         * @return MessageType
         */
        public MessageType
        get_type ()
        {
                return this.type;
        }

        /**
         * @return String
         */
        @Override
        public String
        toString ()
        {
                return this.toString(false);
        }

        /**
         * @param pretty
         * @return String
         */
        public String
        toString (boolean pretty)
        {
                String output = "";

                switch (this.type)
                {
                        case OK:
                                if (pretty)
                                {
                                        output = "Command successful.";
                                } else
                                {
                                        output = "OK";
                                }
                                break;
                        case ERROR:
                                if (pretty)
                                {
                                        output = "Error. " + this.token1;
                                }
                                else
                                {
                                        output = "ERROR";
                                }
                                break;
                        case MESSAGE:
                                if (pretty)
                                {
                                        output = this.token1 + ": " + this.token2;
                                } else
                                {
                                        output = "MESSAGE " + this.token1 + " " + this.token2;
                                }
                                break;
                        case NEW_NICKNAME:
                                if (pretty)
                                {
                                        output = this.token1 + " is now known as " + this.token2;
                                } else
                                {
                                        output = "NEWNICK " + this.token1 + " " + this.token2;
                                }
                                break;
                        case JOINED:
                                if (pretty)
                                {
                                        output = this.token1 + " has joined the room.";
                                } else
                                {
                                        output = "JOINED " + this.token1;
                                }
                                break;
                        case LEFT:
                                if (pretty)
                                {
                                        output = this.token1 + " left the room.";
                                } else
                                {
                                        output = "LEFT " + this.token1;
                                }
                                break;
                        case BYE:
                                if (pretty)
                                {
                                        output = "Disconnected...";
                                } else
                                {
                                        output = "BYE";
                                }
                                break;
                        case PRIVATE:
                                if (pretty)
                                {
                                        output = "<" + this.token1 + ">: " + this.token2;
                                } else
                                {
                                        output = "PRIVATE " + this.token1 + " " + this.token2;
                                }
                                break;
                }

                return output + "\n";
        }
}
