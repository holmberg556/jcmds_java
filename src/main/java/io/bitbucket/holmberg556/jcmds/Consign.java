package io.bitbucket.holmberg556.jcmds;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

public class Consign implements java.io.Serializable {

    private static final long serialVersionUID = 1L;
    public boolean dirty;
    public HashMap<String,ConsignEntry> entries = new HashMap<>();

    public ConsignEntry get(String name) {
        ConsignEntry e = entries.get(name);
        if (e == null) {
            e = new ConsignEntry();
            entries.put(name, e);
        }
        return e;
    }

    public void remove(String name) {
        entries.remove(name);
    }
    
    public static Consign read_from_file(String consign_file) {
        java.io.File f = new java.io.File(consign_file);
        if (f.isFile()) {
            Consign obj = pickle_load(consign_file);
            obj.dirty = false;
            return obj;
        }
        else {
            return new Consign();
        }
    }

    static Consign pickle_load(String consign_file) {
        Consign consign = null;
        //System.out.printf("========> pickle_load %s\n", consign_file);
        try (FileInputStream f = new FileInputStream(consign_file);
                ObjectInputStream fo = new ObjectInputStream(f)) {
            try {
                consign = (Consign) fo.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                System.exit(1);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return consign;
    }
    
    void write_to_file(long curr_mtime, String consign_file) {
        for (ConsignEntry v : entries.values()) {
            if (v.mtime == curr_mtime) {
                // don't trust recent 'mtime' (within current "delta")
                v.mtime = 0;
            }
        }
        pickle_dump(consign_file);
    }


    void pickle_dump(String consign_file) {
        //System.out.printf("<======== pickle_dump %s\n", consign_file);
        try(FileOutputStream f = new FileOutputStream(consign_file);
                ObjectOutputStream fo = new ObjectOutputStream(f)) {
                fo.writeObject(this);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
