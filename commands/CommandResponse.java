package commands;

import java.io.Serializable;
import java.util.ArrayList;

public class CommandResponse implements Serializable {
    private ArrayList<String> outputStreamLines = new ArrayList<>();
    private ArrayList<String> errorStreamLines = new ArrayList<>();

    public CommandResponse(ArrayList<String> outputStreamLines, ArrayList<String> errorStreamLines) {
        this.outputStreamLines = outputStreamLines;
        this.errorStreamLines = errorStreamLines;
    }

    public ArrayList<String> getOutputStreamLines() {
        return outputStreamLines;
    }

    public ArrayList<String> getErrorStreamLines() {
        return errorStreamLines;
    }

    public void print() {
        if(!outputStreamLines.isEmpty()) {
            System.out.println("OUTPUT:");
            for(String outputLine : outputStreamLines) {
                System.out.println("    " + outputLine);
            }
        }
        if(!errorStreamLines.isEmpty()) {
            System.out.println("ERROR:");
            for(String errorLine : errorStreamLines) {
                System.out.println("    " + errorLine);
            }
        }
    }
}
