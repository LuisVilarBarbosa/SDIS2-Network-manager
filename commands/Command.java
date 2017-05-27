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
        if(command.equals("SEND_FILE")) {
            try {
                TransmitFile.sendFile(multicast, args.get(0), getPeers(1));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else if(command.equals("SEND_COMMAND")) {
            BigDecimal[] peers = {};
            if(args.get(0).equals("-windows") || args.get(0).equals("-linux")) {
                if(args.size() > 2)
                    peers = getPeers(2);
            }
            else {
                if(args.size() > 1)
                    peers = getPeers(1);
            }

            Message msg = new Message("SendCommand", multicast.getThisPeer(), args, peers);
            multicast.send(msg);
        }
        else if(command.equals("PORT")) {
            String option = args.get(0);
            String port = args.get(1);
            String[] args = {option, port};
            BigDecimal[] peers = getPeers(2);

            Message msg = new Message("PORT", multicast.getThisPeer(), args, peers);
            multicast.send(msg);
        }
        else if(command.equals("TCP")) {
            String option = args.get(0);
            BigDecimal[] peers = getPeers(1);

            Message msg = new Message("TCP", multicast.getThisPeer(), option, peers);
            multicast.send(msg);
        }
        else if(command.equals("HTTP")) {
            String option = args.get(0);

            BigDecimal[] peers = getPeers(1);

            Message msg = new Message("HTTP", multicast.getThisPeer(), this, peers);
            multicast.send(msg);
        }
        else if(command.equals("FTP")) {
            String option = args.get(0);

            BigDecimal[] peers = getPeers(1);

            Message msg = new Message("FTP", multicast.getThisPeer(), this, peers);
            multicast.send(msg);
        }
        else if(command.equals("EXIT"))
            return false;
        return true;
    }

    public static void executeTCP(Multicast mc, Message message) throws Exception {
        String option = (String)message.getBody();
        ArrayList<String> args = new ArrayList<>();
        if(System.getProperty("os.name").contains("Windows")) {
            args.add("powershell.exe");
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
            Message response = new Message("TCPAck", mc.getThisPeer(), cr, message.getSender().getId());
            mc.send(response);
        }
    }

    public static void executeCommand(Multicast mc, Message message) throws Exception {
        String os = System.getProperty("os.name");
        ArrayList<String> body = null;
        if(message.getBody() instanceof  ArrayList<?>)
            body = ((ArrayList<String>) message.getBody());
        String firstArg = body.get(0);
        ExecuteCommand ec = null;
        if ((firstArg.contains("windows") && os.contains("Windows"))
                || (firstArg.contains("linux") && os.contains("Linux"))) {
            String[] cmdTemp = body.get(1).split(" ");
            ec = new ExecuteCommand(cmdTemp);
        } else if (!firstArg.contains("windows") && !firstArg.contains("Linux")) {
            String[] cmdTemp = body.get(0).split(" ");
            ec = new ExecuteCommand(cmdTemp);
        }
        if (ec != null) {
            ec.setStoreOutput(true);
            ec.run();
            CommandResponse cr = new CommandResponse(ec.getOutputStreamLines(), ec.getErrorStreamLines());
            Message response = new Message("SendCommandAck", mc.getThisPeer(), cr, message.getSender().getId());
            mc.send(response);
        }
    }

    public static void executePort(Multicast multicast, Message message) throws Exception {
        String os = System.getProperty("os.name");
        String[] info = (String[])message.getBody();
        String option = info[0];
        String port = info[1];
        ArrayList<String> args = new ArrayList<>();
        if(os.contains("Windows"))
        {
            args.add("powershell.exe");
            if(option.contains("disable")){
                args.add("New-NetFirewallRule -DisplayName \"Disabling Port "+ port + "\" -Action Block -Direction Outbound -DynamicTarget Any -EdgeTraversalPolicy Block -Profile Any -Protocol tcp -RemotePort " + port + " -Verb RunAs");
            }
            else if(option.contains("enable")){
                args.add("New-NetFirewallRule -DisplayName \"Enabling Port "+ port + "\" -Action Allow -Direction Outbound -DynamicTarget Any -EdgeTraversalPolicy Block -Profile Any -Protocol tcp -RemotePort " + port + " -Verb RunAs");

            }
        }
        String[] allArgs = new String[args.size()];
        allArgs = args.toArray(allArgs);
        ExecuteCommand ec = new ExecuteCommand(allArgs);
        ec.run();
        CommandResponse cr = new CommandResponse(ec.getOutputStreamLines(), ec.getErrorStreamLines());
        Message response = new Message("PORTAck", multicast.getThisPeer(), cr, message.getSender().getId());
        multicast.send(response);
    }
}
