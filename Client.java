
import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;

import database.Database;
import login.CredentialAsker;
import login.Registrator;

public class Client {
	public static Database db;
	public static boolean admin;
	public static boolean loggedIn;
	
	public static boolean start(String dbFolderPath) {
		String username = "";
		String password = "";
		if(CredentialAsker.requestAuthentication()) {
			username = CredentialAsker.requestUsername();
			password = CredentialAsker.requestPassword();
			loggedIn = true;
		} else {
			loggedIn = false;
		}
		
		try {
			
			Registrator registrator;
			boolean loginSucessful = true;
			if(loggedIn) {
				registrator = new Registrator("https://sigarra.up.pt/feup/pt/mob_val_geral.autentica?");
				loginSucessful = registrator.register(username, password);
				if(loginSucessful) {
					System.out.println("Login successful");
				} else {
					System.out.println("Failed to login");
					System.out.println("Server response: " + registrator.getServerMessage());
					return false;
				}
			}
			if(loginSucessful) {
				try {
					db = new Database("database/" + dbFolderPath);
					if(loggedIn) {
						if(db.open() != null) {
							ResultSet rs = db.searchUser(username);
							if(!rs.isAfterLast()) {
								admin = rs.getBoolean("isAdmin");
							} else {
								System.out.println("User not present in database. Creating entry...");
								db.insertUser(username);
								admin = false;
							}
							System.out.println("Welcome!");
							return true;
	
						} else {
							return false;
						}
					} else {
						admin = false;
						System.out.println("Welcome!");
						return true;
					}
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
					return false;
				} catch (IOException e) {
					e.printStackTrace();
					System.out.println("Error: Could not create database");
					return false;
				} catch (SQLException e1) {
					e1.printStackTrace();
					System.out.println("Error: Database may not be open");
					return false;
				}
			} else {
				System.out.println("Some error ocurred. Exiting...");
				return false;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public static void stop() {
		try {
			db.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
	}
}
