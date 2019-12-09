import java.util.regex.Matcher;
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
        public static final String REGEX_NICKNAME = "([^ .]+)";
        public static final String REGEX_ROOM_NAME = "([^ .]+)";
        public static final String REGEX_TEXT = "(.*)";
        // Regex for command process
        private static final String REGEX_CMD_OK = "OK";
        private static final String REGEX_CMD_ERROR = "ERROR";
        private static final String REGEX_CMD_MESSAGE = "MESSAGE " + REGEX_NICKNAME + " " + REGEX_TEXT;
        private static final String REGEX_CMD_NEW_NICKNAME = "NEWNICK " + REGEX_NICKNAME + " " + REGEX_NICKNAME;
        private static final String REGEX_CMD_JOINED = "JOINED " + REGEX_NICKNAME;
        private static final String REGEX_CMD_LEFT = "LEFT " + REGEX_NICKNAME;
        private static final String REGEX_CMD_BYE = "BYE";
        private static final String REGEX_CMD_PRIVATE = "PRIVATE " + REGEX_NICKNAME + " " + REGEX_TEXT;
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
                Matcher tokens;
                String token1 = "";
                String token2 = "";

                if (Pattern.matches(REGEX_CMD_OK, text))
                {
                        type = MessageType.OK;
                }
                else if (Pattern.matches(REGEX_CMD_ERROR, text))
                {
                        type = MessageType.ERROR;
                }
                else if ((tokens = Pattern.compile(REGEX_CMD_MESSAGE).matcher(text)).find())
                {
                        type = MessageType.MESSAGE;
                        token1 = tokens.group(1); // Emitter
                        token2 = tokens.group(2); // Message
                }
                else if ((tokens = Pattern.compile(REGEX_CMD_NEW_NICKNAME).matcher(text)).find())
                {
                        type = MessageType.NEW_NICKNAME;
                        token1 = tokens.group(1); // Old nickname
                        token2 = tokens.group(2); // New nickname
                }
                else if ((tokens = Pattern.compile(REGEX_CMD_JOINED).matcher(text)).find())
                {
                        type = MessageType.JOINED;
                        token1 = tokens.group(1);
                }
                else if ((tokens = Pattern.compile(REGEX_CMD_LEFT).matcher(text)).find())
                {
                        type = MessageType.LEFT;
                        token1 = tokens.group(1);
                }
                else if (Pattern.matches(REGEX_CMD_BYE, text))
                {
                        type = MessageType.BYE;
                }
                else  if ((tokens = Pattern.compile(REGEX_CMD_PRIVATE).matcher(text)).find())
                {
                        type = MessageType.PRIVATE;
                        token1 = tokens.group(1); // Emitter
                        token2 = tokens.group(2); // Message
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
