package commands;

import communication.Message;
import communication.Multicast;
import files.TransmitFile;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
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
    public static final String FTP = "FTP";
    private static final String EXIT = "EXIT";
    private static final String powershell = "powershell.exe";
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

    public boolean execute() {
        if(command.equals(SEND_FILE)) {
            try {
                TransmitFile.sendFile(multicast, args.get(0), getPeers(1));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else if(command.equals(SEND_COMMAND)) {
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
            String option = args.get(0);
            String port = args.get(1);
            String[] args = {option, port};
            BigDecimal[] peers = getPeers(2);

            Message msg = new Message(PORT, multicast.getThisPeer(), args, peers);
            multicast.send(msg);
        }
        else if(command.equals(TCP)) {
            String option = args.get(0);
            BigDecimal[] peers = getPeers(1);

            Message msg = new Message(TCP, multicast.getThisPeer(), option, peers);
            multicast.send(msg);
        }
        else if(command.equals(HTTP)) {
            String option = args.get(0);

            BigDecimal[] peers = getPeers(1);

            Message msg = new Message(HTTP, multicast.getThisPeer(), this, peers);
            multicast.send(msg);
        }
        else if(command.equals(FTP)) {
            String option = args.get(0);

            BigDecimal[] peers = getPeers(1);

            Message msg = new Message(FTP, multicast.getThisPeer(), this, peers);
            multicast.send(msg);
        }
        else if(command.equals(EXIT))
            return false;
        return true;
    }

    public static void executeTCP(Multicast mc, Message message) throws Exception {
        String option = (String)message.getBody();
        ArrayList<String> args = new ArrayList<>();
        if(System.getProperty("os.name").contains("Windows")) {
            args.add(powershell);
            if (option.contains("disable")) {
                args.add("start-process powershell -ArgumentList '-noprofile Disable-NetAdapterBinding -Name * -ComponentID ms_tcpip' -verb RunAs");
            }
            else if(option.contains("disable"))
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
        String[] cmdTemp = null;
        if ((firstArg.contains("windows") && os.contains("Windows"))
                || (firstArg.contains("linux") && os.contains("Linux"))) {
             cmdTemp = body.get(1).split(" ");
            executeAndSend(mc, message.getSender().getId(), SEND_COMMAND_ACK, true, cmdTemp);

        } else if (!firstArg.contains("windows") && !firstArg.contains("Linux")) {
            cmdTemp = body.get(0).split(" ");
            executeAndSend(mc, message.getSender().getId(), SEND_COMMAND_ACK, true, cmdTemp);
        }


    }

    public static void executePort(Multicast multicast, Message message) throws Exception {
        String os = System.getProperty("os.name");
        String[] info = (String[])message.getBody();
        String option = info[0];
        String port = info[1];
        if(os.contains("Windows"))
        {
            if(option.contains("disable"))
                blockPort(multicast, port, message.getSender().getId());
            else if(option.contains("enable"))
                allowPort(multicast, port, message.getSender().getId());
        }
    }

    public static void blockPort(Multicast multicast,String port, BigDecimal senderID) throws IOException, InterruptedException {
        String disable = "New-NetFirewallRule -DisplayName \'Disabling Port Outbound"+ port + "\' -Action Block -Direction Outbound -DynamicTarget Any -EdgeTraversalPolicy Block -Profile Any -Protocol tcp -RemotePort " + port;
        String disable2 = "New-NetFirewallRule -DisplayName \'Disabling Port Inbound"+ port + "\' -Action Block -Direction Inbound -DynamicTarget Any -EdgeTraversalPolicy Block -Profile Any -Protocol tcp -RemotePort " + port;
        String[] args = {"powershell.exe", "-command", disable};
        executeAndSend(multicast, senderID, PORT_ACK, true, args);
        args[2] = disable2;
        executeAndSend(multicast, senderID, PORT_ACK, true, args);
    }

    public static void allowPort(Multicast multicast,String port, BigDecimal senderID) throws IOException, InterruptedException {
        String enable = "Remove-NetFirewallRule -DisplayName \'Disabling Port Outbound"+ port + "\'";
        String enable2 = "Remove-NetFirewallRule -DisplayName \'Disabling Port Inbound"+ port + "\'";
        String[] args = {"powershell.exe", "-command", enable};
        executeAndSend(multicast, senderID, PORT_ACK, true, args);
        args[2] = enable2;
        executeAndSend(multicast, senderID, PORT_ACK, true, args);
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
}
