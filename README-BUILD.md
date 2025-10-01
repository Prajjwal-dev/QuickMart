Quick steps to build & run locally

1) Ensure Java 17+ and Maven are installed and on PATH.
2) Set DB connection (optional env vars):
   - DB_URL (default jdbc:mysql://localhost:3306/quickmartdb)
   - DB_USER (default root)
   - DB_PASS (default empty)

Windows PowerShell example:

$env:DB_URL = "jdbc:mysql://localhost:3306/quickmartdb"
$env:DB_USER = "root"
$env:DB_PASS = ""

3) Build:

mvn -DskipTests package

4) Run from your IDE or via JavaFX plugin configured in pom.xml.

Troubleshooting:
- If you get "MySQL JDBC Driver not found" ensure the MySQL connector dependency is present in pom.xml and run `mvn package` to fetch it.
- If connection fails, try connecting with the same URL/user/pass using a MySQL client to verify credentials and DB existence.

Java runtime warnings:

If you see warnings like "A restricted method in java.lang.System has been called" when running JavaFX, add these VM options when running from your IDE (IntelliJ):

--enable-native-access=ALL-UNNAMED --enable-native-access=javafx.graphics

This suppresses restricted-access warnings for the JavaFX/webcam/native libraries used by the app.
