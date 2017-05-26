package commands;

import communication.Message;
import communication.Multicast;
import files.TransmitFile;

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

    public boolean execute() throws Exception {
        if(command.equals("SEND_FILE")) {
            try {
                TransmitFile.sendFile(multicast, args.get(0), getPeers(1));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else if(command.equals("GET_FILE")) {
            String filepath = args.get(0);
            BigDecimal peerId = getPeers(1)[0];

            // Do something
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
            int port = Integer.parseInt(args.get(1));
            BigDecimal[] peers = getPeers(2);

            Message msg = new Message("PORT", multicast.getThisPeer(), this, peers);
            multicast.send(msg);
        }
        else if(command.equals("TCP")) {
            String option = args.get(0);

            BigDecimal[] peers = getPeers(1);

            Message msg = new Message("TCP", multicast.getThisPeer(), this, peers);
            multicast.send(msg);
        }
        else if(command.equals("UDP")) {
            String option = args.get(0);

            BigDecimal[] peers = getPeers(1);

            Message msg = new Message("UDP", multicast.getThisPeer(), this, peers);
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
}