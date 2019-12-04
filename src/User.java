import java.nio.channels.SocketChannel;

/**
 * User state
 */
enum State
{
        INIT, OUTSIDE, INSIDE
}

/**
 * User class
 */
public class
User
implements Comparable<User>
{
        private String nickname = "";
        private State state;
        private Room room;
        private final SocketChannel socket;
        private String buffer = "";

        /**
         * Constructor
         *
         * @param socket
         */
        public User (SocketChannel socket)
        {
                this.state = State.INIT;
                this.socket = socket;
        }

        /**
         * Comparator
         *
         * @param other
         * @return int
         */
        @Override
        public int
        compareTo (User other)
        {
                return this.nickname.compareTo(other.nickname);
        }


        /**
         * @return String
         */
        public String
        get_nickname ()
        {
                return this.nickname;
        }

        /**
         * @param nickname
         */
        public void
        set_nickname (String nickname)
        {
                this.nickname = nickname;
        }

        /**
         * @return State
         */
        public State
        get_state ()
        {
                return this.state;
        }

        /**
         * @param state
         */
        public void
        set_state (State state)
        {
                this.state = state;
        }

        /**
         * @return Room
         */
        public Room
        get_room ()
        {
                return this.room;
        }

        /**
         * @param room
         */
        public void
        set_room (Room room)
        {
                this.room = room;
        }

        /**
         * @return SocketChannel
         */
        public SocketChannel
        get_socket ()
        {
                return this.socket;
        }

        public void
        add_to_buffer(String cmd)
        {
                this.buffer += cmd;
        }

        public String
        get_buffer()
        {
                return this.buffer;
        }

        public void
        advance_buffer(int idx)
        {
                this.buffer = this.buffer.substring(idx);
        }
}
