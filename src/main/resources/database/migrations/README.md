# Creating database migrations

To create a database migration, first create a migration script containing the change you want to perform. This script should be located in this directory (`src/main/resources/database/migrations`) and use a file name in the format `yyyy-MM-dd_<name>.sql` where `<name>` is a short name/description of the database migration. An example of such a file name would be `1970-01-01_create-the-world.sql`.

After creating the database script, verify that it works using the `/db-admin migrate <your script>` command.

Finally, add your change to the `src/main/resources/database/schema.sql` file.
