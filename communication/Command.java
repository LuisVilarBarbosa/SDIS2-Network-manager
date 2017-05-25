package communication;

import files.TransmitFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Command {
    private String command;
    private ArrayList<String> args;
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
        List<String> peersTemp = args.subList(startIndex, args.size() - 1);
        BigDecimal[] peers = new BigDecimal[args.size()-2];
        for(int i = 0; i < args.size() - 2; i++) {
            peers[i] = new BigDecimal(peersTemp.get(i));
        }
        return peers;
    }

    public void execute() {
        if(command.equals("SEND_FILE")) {
            try {
                TransmitFile.sendFile(multicast, args.get(0), getPeers(1));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
