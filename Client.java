
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
	
	public static boolean start() {
		String username = CredentialAsker.requestUsername();
		String password = CredentialAsker.requestPassword();
		try {
			Registrator registrator = new Registrator("https://sigarra.up.pt/feup/pt/mob_val_geral.autentica?");
			if(registrator.register(username, password)) {
				System.out.println("Login successful");
				try {
					db = new Database("database.db");
					return db.open() != null;
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
					return false;
				} catch (IOException e) {
					System.out.println("Error: Could not create database");
					return false;
				} catch (SQLException e1) {
					System.out.println("Error: Database may not be open");
					return false;
				}
			} else {
				System.out.println("Failed to login");
				System.out.println("Server response: " + registrator.getServerMessage());
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
			e.printStackTrace();
			return;
		}
	}
}
