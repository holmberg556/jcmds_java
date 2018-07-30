package io.bitbucket.holmberg556.jcmds;

import java.io.IOException;
import java.util.HashMap;

public class ZUnusedCommand {
    
    private static int processNr = 0;
    private static HashMap<Integer,Process> processByNr;
    private static HashMap<Process,Integer> processById;

    public static int start(String command) {
        ProcessBuilder pb = new ProcessBuilder("sh", "-c", command);
        pb.inheritIO();
        Process p = null;
        try {
            p = pb.start();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.exit(1);
        }
        processNr  += 1;
        processByNr.put(processNr, p);
        processById.put(p, processNr);
        return processNr;
    }


//    public static int waitForOne() {
//        try {
//            p.waitFor();
//        } catch (InterruptedException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//            System.exit(1);
//        }
//    }

}
