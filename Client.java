
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import database.Database;
import login.CredentialAsker;
import login.Registrator;

public class Client {
	public static void main(String args[]) {
		if(args.length != 0) {
			System.out.println("Client takes no arguments");
			return;
		}
		String username = CredentialAsker.requestUsername();
		String password = CredentialAsker.requestPassword();
		try {
			Registrator registrator = new Registrator("https://sigarra.up.pt/feup/pt/mob_val_geral.autentica?");
			if(registrator.register(username, password)) {
				System.out.println("Login successful");
				try {
					Database db = new Database("database.db");
					if(db.open() != null) {
						try {
							db.insertUser(username);
							db.deleteUser(username);
						} catch (SQLException e) {
							//TODO User already exists. Should we print something?
							e.printStackTrace();
						}
					} else {
						System.out.println("Database wasnt open");
					}
					db.close();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
					System.out.println("Error: Could not create database");
					return;
				} catch (SQLException e1) {
					e1.printStackTrace();
					System.out.println("Error: Database may not be open");
				}
			} else {
				System.out.println("Failed to login");
				System.out.println("Server response: " + registrator.getServerMessage());
			}
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}
}
