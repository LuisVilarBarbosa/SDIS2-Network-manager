import communication.Command;
import communication.Multicast;

import java.util.Arrays;
import java.util.Scanner;
import java.util.StringTokenizer;

public class NetworkManager {

    public static void main(String args[]) {
        Multicast m;
        if (args.length != 4 && args.length != 6) {
            System.out.println("Usage:");
            System.out.println("NetworkManager new_group <local host port> <public host name> <public host port>");
            System.out.println("NetworkManager join_group <local host port> <public host name> <public host port> <another host name> <another host port>");
            return;
        } else {

            String mode = args[0];
            int localHostPort = Integer.parseInt(args[1]);
            String publicHostName = args[2];
            int publicHostPort = Integer.parseInt(args[3]);
            if (args.length == 4) {
                if (!mode.equals("new_group")) {
                    System.out.println("Invalid arguments.");
                    return;
                }
                m = new Multicast(localHostPort, publicHostName, publicHostPort);
            } else {
                String anotherHostName = args[4];
                int anotherHostPort = Integer.parseInt(args[5]);
                if (!mode.equals("join_group")) {
                    System.out.println("Invalid arguments.");
                    return;
                }
                try {
                    m = new Multicast(localHostPort, publicHostName, publicHostPort, anotherHostName, anotherHostPort);
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }

        }

        NetworkManager n = new NetworkManager();

        n.startMenu();
        n.receiveCommand(m);
    }

    private void receiveCommand(Multicast m) {
        while(true) {
            System.out.println("Write 'list' to view the availale commands");
            Scanner s = new Scanner(System.in);
            String cmd = s.nextLine();
            String[] commands = cmd.split(" ");
            System.out.println(commands.length);
            String[] args = Arrays.copyOfRange(commands, 1, commands.length);
            Command c = new Command(m, commands[0], args);
            try {
                if(!c.execute()) {
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private int getResponse(int min, int max) {
        System.out.print("Option: ");
        Scanner scanner = new Scanner(System.in);
        Integer value = null;
        do {
            try {
             value=Integer.parseInt(scanner.next());
            }catch(NumberFormatException e) {}
            if (value == null || value < min || value > max)
                System.out.println("Invalid value." );
        } while (value == null || value < min || value > max);
        return value;
    }

    private void startMenu() {
        System.out.println("1-Login");
        System.out.println("2-Register");
        int option = getResponse(1,2);
        switch (option) {
            case 1:
            login();
            break;
            case 2:
                register();
                break;
        }
    }

    private void login() {
        System.out.println("Email: ");
        System.out.println("Password: ");
    }

    private void register() {

    }


}
