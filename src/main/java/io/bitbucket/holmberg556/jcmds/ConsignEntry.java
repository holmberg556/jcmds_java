package io.bitbucket.holmberg556.jcmds;

import java.util.ArrayList;

public class ConsignEntry implements java.io.Serializable {

    private static final long serialVersionUID = 1L;
    public Digest content_sig; // TODO
    public Digest dep_sig;
    public long mtime;
    public ArrayList<Include> includes;
    
    ConsignEntry() {
        this.content_sig = Digest.undefined;
        this.dep_sig = Digest.undefined;
    }

}
