package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {
	private static final String delim = ",";
	private final String dbPath;
	private Connection dbConnection;
	
	public Database(String dbPath) {
		this.dbPath = dbPath;
	}
	
	public Connection open() throws ClassNotFoundException, SQLException {
		Class.forName("org.sqlite.JDBC");
		String dbURL = "jbdc:sqlite:" + this.dbPath;
		this.dbConnection = DriverManager.getConnection(dbURL);
		return this.dbConnection;
	}
	
	public void close() throws SQLException {
		this.dbConnection.close();
	}
	
	//TODO 	OU usamos SQL diretamente
	//		OU usamos preparedStatements, e depois temos de dar set aos "?"
	//VER: http://docs.oracle.com/javase/tutorial/jdbc/basics/prepared.html
}
