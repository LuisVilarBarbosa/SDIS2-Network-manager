import commands.Command;
import communication.Multicast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NetworkManager {

    public static void main(String args[]) {
        Multicast m;
        if (args.length != 4 && args.length != 6) {
            System.out.println("Usage:");
            System.out.println("NetworkManager new_group <local host port> <public host name> <public host port>");
            System.out.println("NetworkManager join_group <local host port> <public host name> <public host port> <another host name> <another host port>");
            return;
        } else if(Client.start()){
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
        } else {
        	System.out.println("Could not open database");
        	return;
        }

        NetworkManager n = new NetworkManager();

        //n.startMenu();
        n.receiveCommand(m);
    }

    private void receiveCommand(Multicast m) {
        while(true) {
            System.out.println("Write 'list' to view the availale commands");
            Scanner s = new Scanner(System.in);

            String readCommand = s.nextLine();

            final String regex = "([A-Z_]+\\s((-windows|-linux)\\s)?\"\\w+(\\s\\w+)*\"(\\s[0-9]+)*|LIST|list)";
            final Pattern pattern = Pattern.compile(regex);
            final Matcher matcher = pattern.matcher(readCommand);
            if(!matcher.matches())
                System.out.println("INVALID COMMAND");
            else if(readCommand.toLowerCase().contains("list")){
                System.out.println("AVAILABLE COMMANDS:");
                System.out.println("   SEND_FILE <file path> [<peer_id>...]\n" +
                        "   SEND_COMMAND [-windows|-linux] \"<command>\" [<peer_id>...]\n" +
                        "   PORT <-enable|-disable> <port number> [<peer_id>...]\n" +
                        "   TCP <-enable|-disable> [<peer_id>...]\n" +
                        "   HTTP <-enable|-disable> [<peer_id>...]\n" +
                        "   FTP <-enable|-disable> [<peer_id>...]\n" +
                        "   EXIT\n");
            }
            else {
                String[] commandsTemp = readCommand.split("\"");

                ArrayList<String> finalCommands = new ArrayList<>();
                for (int i = 0; i < commandsTemp.length; i++) {
                    if (i != 1) {
                        String[] tmp = commandsTemp[i].split(" ");
                        for (int j = 0; j < tmp.length; j++) {
                            if (!tmp[j].isEmpty())
                                finalCommands.add(tmp[j]);
                        }
                    } else {
                        finalCommands.add(commandsTemp[i]);
                    }
                }

                String[] finalCommandsArr = new String[finalCommands.size()];
                finalCommandsArr = finalCommands.toArray(finalCommandsArr);
                String[] args = Arrays.copyOfRange(finalCommandsArr, 1, finalCommandsArr.length);
                Command c = new Command(m, finalCommands.get(0), args);
                c.execute();
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
