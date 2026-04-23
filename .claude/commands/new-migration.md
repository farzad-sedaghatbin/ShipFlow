Create a new Flyway migration file. Description: $ARGUMENTS

Steps:
1. Scan `backend/src/main/resources/db/migration/` for files matching today's date (`V{YYYY}_{MM}_{DD}_*`) to determine the next sequence number (0001 if none today).
2. Build the filename: `V{YYYY}_{MM}_{DD}_{sequence}__{description}.sql`
   - Description: derive from $ARGUMENTS — lowercase, words joined by underscores.
   - Example: `V2026_04_22_0001__add_user_notification_preferences.sql`
3. Write the SQL file with:
   - A comment header explaining the migration purpose.
   - PostgreSQL-compatible DDL. **Avoid H2-incompatible syntax**: no `jsonb`, no `CREATE INDEX CONCURRENTLY`, no `SERIAL` (use `BIGINT GENERATED ALWAYS AS IDENTITY` instead). Safe types: `BIGINT`, `VARCHAR`, `TEXT`, `BOOLEAN`, `TIMESTAMP WITH TIME ZONE`, `DECIMAL`.
   - Indexes named `idx_{table}_{columns}`. Foreign keys named `fk_{table}_{referenced_table}`.
   - If existing rows need data migration, include an `UPDATE` statement after DDL.
4. After creating the file, remind: restart the backend so Flyway picks it up: `cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`

**Never edit an existing migration file.**

Show the planned filename and SQL before writing, then create the file.
