package login;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class CredentialAsker {
	
	public CredentialAsker() {
		
	}
	
	public static String requestUsername() {
		System.out.print("Username: ");
		
		BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
		String username = "";
		
		try {
			username = input.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return username;
	}
	
	public static String requestPassword() {
		System.out.print("Password: ");
		PasswordMasker masker = new PasswordMasker();
		Thread maskerThread = new Thread(masker);
		maskerThread.start();
		
		BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
		String password = "";
		
		try {
			password = input.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		masker.stop();
		
		return password;
	}
	
	public static boolean requestAuthentication() {
		System.out.print("Do you want to log in? (y/n)");
		BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
		String permission = "";
		
		try {
			permission = input.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if(permission.equals("y") || permission.equals("Y")) {
			return true;
		} else if(permission.equals("n") || permission.equals("N")) {
			return false;
		} else {
			System.out.println("Invalid input, try again");
			return requestAuthentication();
		}
	}
	
}
