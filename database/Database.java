package database;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Database {
	private static final String delim = ",";
	private static final String createUserTableSQL =	
			"CREATE TABLE users ("
			+ "id INTEGER PRIMARY KEY AUTOINCREMENT,"
			+ "username TEXT UNIQUE NOT NULL," 	//TODO unique??
			+ "isAdmin BOOLEAN NOT NULL);";
	private static final String insertUserSQL = 
			"INSERT INTO users (username, isAdmin) VALUES (?, ?);";
	private final String dbPath;
	private Connection dbConnection;
	
	public Database(String dbPath) throws IOException, ClassNotFoundException, SQLException {
		this.dbPath = dbPath;
		File f = new File("./" + this.dbPath);
		if(!f.exists()) {
			System.out.println("Database not found. Creating a new one...");
			f.createNewFile();
			this.init();
		}
	}
	
	private void init() throws ClassNotFoundException, SQLException {
		this.open();
		PreparedStatement stmt = this.dbConnection.prepareStatement(createUserTableSQL);
		stmt.execute();
	}
	
	public Connection open() throws ClassNotFoundException, SQLException {
		Class.forName("org.sqlite.JDBC");
		String dbURL = "jdbc:sqlite:" + this.dbPath;
		this.dbConnection = DriverManager.getConnection(dbURL);
		return this.dbConnection;
	}
	
	public void close() throws SQLException {
		this.dbConnection.close();
	}
	
	public boolean insertUser(String username) throws SQLException {
		PreparedStatement stmt = this.dbConnection.prepareStatement(insertUserSQL);
		stmt.setString(1, username);
		stmt.setBoolean(2, false);
		return stmt.execute();
	}
	
	//TODO 	OU usamos SQL diretamente
	//		OU usamos preparedStatements, e depois temos de dar set aos "?"
	//VER: http://docs.oracle.com/javase/tutorial/jdbc/basics/prepared.html
	
	//Para correr java -classpath ".:sqlite-jdbc-3.16.1.jar" Client
	
	
}
