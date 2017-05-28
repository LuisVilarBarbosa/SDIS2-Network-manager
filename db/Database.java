package db;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Todas as operações que retornarem ResultSets, já devolvem esse set a apontar para o 1º elemento.
 * Caso não seja encontrado nenhum resultado, tentar aceder ao ResultSet pode dar problemas e lançar
 * excepções. Há duas maneiras de lidar com isso: <p />
 *  <ol>
 *  <li> Verificar se o cursor dentro do ResultSet já passou o último elemento -> <strong>ResultSet.isAfterLast()</strong>
 *  <li> Fazer <strong>catch</strong> duma SQLException quando se tenta aceder a um elemento do ResultSet
 *  (que pode ser inexistente, daí lançar a excepção)
 *  </ol>
 * 
 * @author ZeCarlosCoutinho
 *
 */
public class Database {
	private static final String createUserTableSQL =	
			"CREATE TABLE users ("
			+ "id INTEGER PRIMARY KEY AUTOINCREMENT,"
			+ "username TEXT UNIQUE NOT NULL," 	
			+ "isAdmin BOOLEAN NOT NULL);";
	private static final String insertUserSQL = 
			"INSERT INTO users (username, isAdmin) VALUES (?, ?);";
	private static final String deleteUserSQL =
			"DELETE FROM users "
			+ "WHERE username LIKE ? ;";
	private static final String updateUserSQL =
			"UPDATE users "
			+ "SET isAdmin = ? "
			+ "WHERE username LIKE ? ;";
	private static final String selectUserSQL =
			"SELECT * FROM users "
			+ "WHERE username LIKE ?;";
	private static final String selectAllUsersSQL =
			"SELECT * FROM users;";
	
	private static final String createFileTableSQL =
			"CREATE TABLE files ("
			+ "id INTEGER PRIMARY KEY AUTOINCREMENT,"
			+ "path TEXT UNIQUE NOT NULL,"
			+ "date DATE NOT NULL,"
			+ "user_id INTEGER,"
			+ "FOREIGN KEY(user_id) REFERENCES users(id));";
	private static final String insertFileSQL = 
			"INSERT INTO files (path, date, user_id) VALUES (?, ?, ?);";
	private static final String deleteFileSQL =
			"DELETE FROM files "
			+ "WHERE path LIKE ? AND user_id = ?;";
	private static final String selectFileSQL =
			"SELECT * FROM files "
			+ "WHERE path LIKE ?;";
	private static final String selectFileOfUserSQL =
			"SELECT * FROM files "
			+ "WHERE path LIKE ? AND user_id = ?";
	private static final String selectUserFilesSQL =
			"SELECT * FROM files "
			+ "WHERE user_id = ?;";
	
	private final String dbPath;
	private Connection dbConnection;
	public static final String superuser1 = "up201404293";
	public static final String superuser2 = "up201405729";
	public static final String superuser3 = "up201403745";
	public static final String superuser4 = "up201403074";
	
	public Database(String dbPath) throws IOException, ClassNotFoundException, SQLException {
		File dir = new File(dbPath);
		if(!dir.exists()) {
			dir.mkdir();
			System.out.println("Database folder not found. Creating a new folder...");
		}
		this.dbPath = dbPath + "/database.db";
		File f = new File(this.dbPath);
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
		stmt = this.dbConnection.prepareStatement(createFileTableSQL);
		stmt.execute();
		insertUser(superuser1, true);
		insertUser(superuser2, true);
		insertUser(superuser3, true);
		insertUser(superuser4, true);
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
		boolean b = insertUser(username, false);
		return b;
	}
	
	public boolean insertUser(String username, boolean isAdmin) throws SQLException {
		ResultSet rs = searchUser(username);
		if(rs.isAfterLast()) {
			PreparedStatement stmt = this.dbConnection.prepareStatement(insertUserSQL);
			stmt.setString(1, username);
			stmt.setBoolean(2, isAdmin);
			return stmt.execute();
		}
		//In case user already exists
		return false;
	}
	
	public boolean deleteUser(String username) throws SQLException {
		//Deletes user files
		ResultSet userFiles = searchFiles(username);
		while(!userFiles.isAfterLast()) {
			deleteFile(userFiles.getString("path"), username);
			userFiles.next();
		}
		userFiles.close();
		
		PreparedStatement stmt = this.dbConnection.prepareStatement(deleteUserSQL);
		stmt.setString(1, username);
		return stmt.execute();
	}
	
	public boolean updateUser(String username, boolean isAdmin) throws SQLException {
		PreparedStatement stmt = this.dbConnection.prepareStatement(updateUserSQL);
		stmt.setBoolean(1, isAdmin);
		stmt.setString(2, username);
		return stmt.execute();
	}
	
	/**
	 * @param username
	 * @return ResultSet pointing to the first row of results
	 * @throws SQLException
	 */
	public ResultSet searchUser(String username) throws SQLException {
		PreparedStatement stmt = this.dbConnection.prepareStatement(selectUserSQL);
		stmt.setString(1, username);
		ResultSet rs = stmt.executeQuery();
		rs.next();
		return rs;
	}
	
	public ResultSet getAllUsers() throws SQLException {
		PreparedStatement stmt = this.dbConnection.prepareStatement(selectAllUsersSQL);
		ResultSet rs = stmt.executeQuery();
		rs.next();
		return rs;
	}
	
	public boolean insertFile(String path, Date date, String username) throws SQLException {
		ResultSet rs = searchUser(username);
		int user_id;
		try {
			user_id = rs.getInt("id");
		} catch (SQLException e) {
			//No user found
			return false;
		}
		rs.close();
		
		PreparedStatement stmt = this.dbConnection.prepareStatement(insertFileSQL);
		stmt.setString(1, path);
		stmt.setDate(2, date);
		stmt.setInt(3, user_id);
		return stmt.execute();
	}
	
	public boolean deleteFile(String path, String username) throws SQLException {
		ResultSet rs = searchUser(username);
		int user_id;
		try {
			user_id = rs.getInt("id");
		} catch (SQLException e) {
			//No user found
			return false;
		}
		rs.close();
		
		PreparedStatement stmt = this.dbConnection.prepareStatement(deleteFileSQL);
		stmt.setString(1, path);
		stmt.setInt(2, user_id);
		return stmt.execute();
	}
	
	public boolean updateFile(String path, Date date, String username) throws SQLException{
		boolean delete = this.deleteFile(path, username);
		Date data = new Date(System.currentTimeMillis());
		boolean insert = this.insertFile(path, data, username);
		return delete && insert;
	}
	
	public ResultSet searchFile(String path) throws SQLException {
		PreparedStatement stmt = this.dbConnection.prepareStatement(selectFileSQL);
		stmt.setString(1, path);
		ResultSet rs = stmt.executeQuery();
		rs.next();
		return rs;
	}
	
	public ResultSet searchFile(String path, String username) throws SQLException {
		ResultSet rs = searchUser(username);
		int user_id;
		try {
			user_id = rs.getInt("id");
		} catch(SQLException e) {
			//No user found
			return rs;
		}
		PreparedStatement stmt = this.dbConnection.prepareStatement(selectFileOfUserSQL);
		stmt.setString(1, path);
		stmt.setInt(2, user_id);
		rs = stmt.executeQuery();
		rs.next();
		return rs;
	}
	
	public ResultSet searchFiles(String username) throws SQLException {
		ResultSet rs = searchUser(username);
		int user_id;
		try {
			user_id = rs.getInt("id");
		} catch(SQLException e) {
			//No user found
			return rs;		}
		PreparedStatement stmt = this.dbConnection.prepareStatement(selectUserFilesSQL);
		stmt.setInt(1, user_id);
		rs = stmt.executeQuery();
		rs.next();
		return rs;
	}
	
	//TODO 	OU usamos SQL diretamente
	//		OU usamos preparedStatements, e depois temos de dar set aos "?"
	//VER: http://docs.oracle.com/javase/tutorial/jdbc/basics/prepared.html
	
	//Para correr java -classpath ".:sqlite-jdbc-3.16.1.jar" Client
	
	
}
