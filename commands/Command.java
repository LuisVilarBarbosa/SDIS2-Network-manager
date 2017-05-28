package commands;

import communication.Message;
import communication.Multicast;
import database.Database;
import files.TransmitFile;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.ClientInfoStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Command implements Serializable {
    public static final String SEND_FILE = "SEND_FILE";
    public static final String SEND_COMMAND = "SEND_COMMAND";
    public static final String SEND_COMMAND_ACK = "SEND_COMMAND_ACK";
    public static final String PORT = "PORT";
    public static final String PORT_ACK = "PORT_ACK";
    public static final String TCP = "TCP";
    public static final String TCP_ACK = "TCP_ACK";
    public static final String HTTP = "HTTP";
    public static final String HTTPS = "HTTPS";
    public static final String FTP = "FTP";
    public static final String LIST_USERS = "LIST_USERS";
    public static final String CHANGE_PERMISSIONS = "CHANGE_PERMISSIONS";
    public static final String LIST_PEERS = "LIST_PEERS";
    private static final String EXIT = "EXIT";
    private static final String powershell = "powershell.exe";
    private static final String[] portsHTTP = {"80", "8080", "8008"};
    private static final String[] portsHTTPS = {"443"};
    private static final String[] portsFTP = {"20", "21"};
	private static final String notEnoughPermissions = "You don't have enough permissions";
    private String command;
    private ArrayList<String> args = new ArrayList<String>();
    private Multicast multicast;

    public Command(Multicast mc, String command, String... args) {
        this.command = command;
        this.multicast = mc;
        for (String arg : args) {
            this.args.add(arg);
        }
    }

    public String getCommand() {
        return command;
    }

    public ArrayList<String> getArgs() {
        return args;
    }

    public BigDecimal[] getPeers(int startIndex) {
        List<String> peersTemp = args.subList(startIndex, args.size());
        BigDecimal[] peers = new BigDecimal[peersTemp.size()];
        for(int i = 0; i < peersTemp.size(); i++) {
            peers[i] = new BigDecimal(peersTemp.get(i));
        }
        return peers;
    }

    public boolean execute(Database db, boolean isAdmin) throws Exception {
        if(command.equals(SEND_FILE)) {
            try {
                TransmitFile.sendFile(multicast, args.get(0), getPeers(1));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else if(command.equals(SEND_COMMAND)) {
        	if(!isAdmin) {
        		System.out.println(notEnoughPermissions);
        		return true;
        	}
            BigDecimal[] peers = {};
            if(args.get(0).equals("-windows") || args.get(0).equals("-linux")) {
                if(args.size() > 2)
                    peers = getPeers(2);
            }
            else if(args.size() > 1)
                    peers = getPeers(1);

            Message msg = new Message(SEND_COMMAND, multicast.getThisPeer(), args, peers);
            multicast.send(msg);
        }
        else if(command.equals(PORT)) {
        	if(!isAdmin) {
        		System.out.println(notEnoughPermissions);
        		return true;
        	}
            String option = args.get(0);
            String port = args.get(1);
            String[] args = {option, port};
            BigDecimal[] peers = getPeers(2);

            Message msg = new Message(PORT, multicast.getThisPeer(), args, peers);
            multicast.send(msg);
        }
        else if(command.equals(TCP) || command.equals(HTTP) || command.equals(HTTPS) || command.equals(FTP)) {
            if(!isAdmin) {
                System.out.println(notEnoughPermissions);
                return true;
            }
            String option = args.get(0);
            BigDecimal[] peers = getPeers(1);

            Message msg = new Message(command, multicast.getThisPeer(), option, peers);

            multicast.send(msg);
        }
        else if(command.equalsIgnoreCase(LIST_USERS)) {
        	try {
        		ResultSet rs = db.getAllUsers();
	    		String status = "";
	        	System.out.format("\n%-20s  %s\n", "Users", "Permissions");
	        	System.out.println("------------------------");
	        	while(!rs.isAfterLast()) {
	        		if(rs.getBoolean("isAdmin")) {
	        			status = "ADMIN";
	        		} else {
	        			status = "REGULAR";
	        		}
	        		System.out.format("%-20s= %s\n", rs.getString("username"), status);
	        		rs.next();
	        	}
        	} catch(SQLException e) {
        		e.printStackTrace();
        		System.out.println("RIP");
        	}
        }
        else if(command.equalsIgnoreCase(CHANGE_PERMISSIONS)) {
        	if(!isAdmin) {
        		System.out.println(notEnoughPermissions);
        		return true;
        	}
        	String username = args.get(0);
        	String permission = args.get(1);
        	boolean isAdministrator = false;
        	
        	if(!permission.equalsIgnoreCase("regular") && !permission.equalsIgnoreCase("admin")){
        		System.out.println("Not a valid type of user");
        		return true;
        	} else if(db.searchUser(username).isAfterLast()) {
        		System.out.println("User not existant");
        		return true;
        	} else {
        		if(permission.equalsIgnoreCase("regular")) {
        			isAdministrator = false;
        		} else if(permission.equalsIgnoreCase("admin")) {
        			isAdministrator = true;
        		} else {
        			System.out.println("Error occured");
        		}
        		
        		db.updateUser(username, isAdministrator);
        		
        		//TODO Atualizar as outras dbs
        		return true;
        	}
        }
        else if(command.equalsIgnoreCase(LIST_PEERS)){
            multicast.showConnectedPeers();
        }
        else if(command.equalsIgnoreCase(EXIT)) {
            return false;
        }
        return true;
    }

    public static void executeTCP(Multicast mc, Message message) throws Exception {
        String option = (String)message.getBody();
        ArrayList<String> args = new ArrayList<>();
        if(System.getProperty("os.name").contains("Windows")) {
            args.add(powershell);
            if (option.contains("disable"))
                args.add("start-process powershell -ArgumentList '-noprofile Disable-NetAdapterBinding -Name * -ComponentID ms_tcpip' -verb RunAs");
            else if(option.contains("enable"))
                args.add("start-process powershell -ArgumentList '-noprofile Enable-NetAdapterBinding -Name * -ComponentID ms_tcpip' -verb RunAs");
            String[] allArgs = new String[args.size()];
            allArgs = args.toArray(allArgs);
            ExecuteCommand ec = new ExecuteCommand(allArgs);
            ec.run();
            CommandResponse cr = new CommandResponse(ec.getOutputStreamLines(), ec.getErrorStreamLines());
            Message response = new Message(TCP_ACK, mc.getThisPeer(), cr, message.getSender().getId());
            mc.send(response);
        }
    }

    public static void executeCommand(Multicast mc, Message message) throws Exception {
        String os = System.getProperty("os.name");
        ArrayList<String> body = null;
        if(message.getBody() instanceof  ArrayList<?>)
            body = ((ArrayList<String>) message.getBody());
        String firstArg = body.get(0);
        String[] cmdTemp = new String[3];
        cmdTemp[0] = "powershell.exe";
        cmdTemp[1] = "-command";
        if ((firstArg.contains("windows") && os.contains("Windows")) || ((!firstArg.contains("windows") && !firstArg.contains("Linux")) && os.contains("Windows")))
        {
            cmdTemp[2] = body.get(1);
            executeAndSend(mc, message.getSender().getId(), SEND_COMMAND_ACK, true, cmdTemp);
        } else if((firstArg.contains("linux") && os.contains("Linux")) || ((!firstArg.contains("windows") && !firstArg.contains("Linux")) && os.contains("Linux")))
        {
            String[] splitted = body.get(1).split(" ");
            executeAndSend(mc, message.getSender().getId(), SEND_COMMAND_ACK, true, splitted);
        }


    }

    public static void executePort(Multicast multicast, Message message) throws Exception {
        String os = System.getProperty("os.name");
        String[] info = (String[]) message.getBody();
        String option = info[0];
        String port = info[1];
        if (option.contains("disable"))
            blockPort(multicast, port, message.getSender().getId(), os);
        else if (option.contains("enable"))
            allowPort(multicast, port, message.getSender().getId(), os);
}

    public static void blockPort(Multicast multicast,String port, BigDecimal senderID, String os) throws IOException, InterruptedException {
        String disable = null, disable2 = null, disable3= null;
        if(os.contains("Windows")) {
            disable = "New-NetFirewallRule -DisplayName \'Disabling Port Outbound" + port + "\' -Action Block -Direction Outbound -DynamicTarget Any -EdgeTraversalPolicy Block -Profile Any -Protocol tcp -RemotePort " + port;
            disable2 = "New-NetFirewallRule -DisplayName \'Disabling Port Inbound" + port + "\' -Action Block -Direction Inbound -DynamicTarget Any -EdgeTraversalPolicy Block -Profile Any -Protocol tcp -RemotePort " + port;
            String[] argsWindows = {"powershell.exe", "-command", disable};
            executeAndSend(multicast, senderID, PORT_ACK, true, argsWindows);
            argsWindows[2] = disable2;
            executeAndSend(multicast, senderID, PORT_ACK, true, argsWindows);
        }
        else if(os.contains("Linux")){
            disable = "/sbin/iptables -A OUTNPUT -p tcp --destination-port " + port + " -j DROP";
            disable2 = "/sbin/iptables -A INPUT -p tcp --destination-port " + port + " -j DROP";
            disable3 = "/sbin/iptables-save";
            String[] argsLinux = disable.split(" ");
            executeAndSend(multicast, senderID, PORT_ACK, true, argsLinux);
            argsLinux = disable2.split(" ");
            executeAndSend(multicast, senderID, PORT_ACK, true, argsLinux);
            argsLinux = disable3.split(" ");
            executeAndSend(multicast, senderID, PORT_ACK, true, argsLinux);
        }
    }

    public static void allowPort(Multicast multicast,String port, BigDecimal senderID, String os) throws IOException, InterruptedException {
        String enable = null, enable2 = null, enable3 = null;
        if(os.contains("Windows")) {
            enable = "Remove-NetFirewallRule -DisplayName \'Disabling Port Outbound" + port + "\'";
            enable2 = "Remove-NetFirewallRule -DisplayName \'Disabling Port Inbound" + port + "\'";
            String[] args = {"powershell.exe", "-command", enable};
            executeAndSend(multicast, senderID, PORT_ACK, true, args);
            args[2] = enable2;
            executeAndSend(multicast, senderID, PORT_ACK, true, args);
        }
        else if(os.contains("Linux")){
            enable = "/sbin/iptables -D OUTNPUT -p tcp --destination-port " + port + " -j DROP";
            enable2 = "/sbin/iptables -D INPUT -p tcp --destination-port " + port + " -j DROP";
            enable3 = "/sbin/iptables-save";
            String[] argsLinux = {enable};

            executeAndSend(multicast, senderID, PORT_ACK, true, argsLinux);
            argsLinux = enable2.split(" ");
            executeAndSend(multicast, senderID, PORT_ACK, true, argsLinux);
            argsLinux = enable3.split(" ");
            executeAndSend(multicast, senderID, PORT_ACK, true, argsLinux);
        }
    }

    public static void executeAndSend(Multicast multicast,BigDecimal senderID, String operation, boolean output, String... command) throws IOException, InterruptedException {
        ExecuteCommand ec = new ExecuteCommand(command);
        if(output)
            ec.setStoreOutput(true);
        ec.run();
        CommandResponse cr = new CommandResponse(ec.getOutputStreamLines(), ec.getErrorStreamLines());
        Message response = new Message(operation, multicast.getThisPeer(), cr, senderID);
        multicast.send(response);
    }

    public static void executeProtocol(Multicast multicast, Message message) throws IOException, InterruptedException {
        String os = System.getProperty("os.name");
        String option = (String)message.getBody();
        String operation = message.getOperation();
        String[] ports = null;
        if(operation.equals(HTTP))
            ports = portsHTTP;
        else if (operation.equals(HTTPS))
            ports = portsHTTPS;
        else if(operation.equals(FTP))
            ports = portsFTP;
        if(ports != null)
            changeProtocolStatus(multicast, ports, option, message, os);
    }

    public static void changeProtocolStatus(Multicast multicast, String[] ports, String option, Message message, String os) throws IOException, InterruptedException {
        for (String port : ports) {
            if (option.contains("disable"))
                blockPort(multicast, port, message.getSender().getId(), os);
            else if (option.contains("enable"))
                allowPort(multicast, port, message.getSender().getId(), os);
        }
    }
}
