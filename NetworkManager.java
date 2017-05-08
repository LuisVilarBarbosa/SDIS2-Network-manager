import java.util.Scanner;

public class NetworkManager {

    public static void main(String args[]) {
        if(args.length != 0)
            System.out.println("Indicated arguments will be ignored.");

        NetworkManager n = new NetworkManager();

        n.startMenu();
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
        options();
    }

    private void register() {

    }

    private void options() {
        StringBuilder sb = new StringBuilder("Options:\n");
        sb.append("1-File/folder transfer\n");
        sb.append("2-File/folder consulting\n");
        sb.append("3-Enable/disable networking services\n");
        sb.append("4-Remote execution of commands\n");
        System.out.println(sb);
        int option = getResponse(1,4);
        switch (option) {
            case 1:
                break;
            case 2:
                break;
            case 3:
                break;
            case 4:
                break;
        }
    }
}
