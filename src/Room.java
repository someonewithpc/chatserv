import java.util.TreeSet;

/**
 * Room class
 */
class
Room
{
        private final TreeSet<User> users;
        private final String name;

        /**
         * Constructor
         *
         * @param name
         */
        public Room (String name)
        {
                this.users = new TreeSet<>();
                this.name = name;
        }

        /**
         * @return TreeSet<User>
         */
        public TreeSet<User>
        get_users ()
        {
                return this.users;
        }

        /**
         * @param user
         */
        public void
        join_user (User user)
        {
                this.users.add(user);
        }

        /**
         * @param user
         */
        public void
        left_user (User user)
        {
                this.users.remove(user);
        }

        /**
         * @return String
         */
        public String
        get_name ()
        {
                return this.name;
        }
}
