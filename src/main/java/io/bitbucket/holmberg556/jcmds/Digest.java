package io.bitbucket.holmberg556.jcmds;

import java.util.Arrays;
import java.util.Formatter;

public class Digest implements Comparable<Digest>, java.io.Serializable {

    private static final long serialVersionUID = 1L;
    public static final Digest undefined;
    public static final Digest invalid;
    
    static {
        undefined = new Digest();
        undefined.bytes = new byte[] { 0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0 };
        invalid = new Digest();
        undefined.bytes = new byte[] { 1,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0 };
    }

    public byte[] bytes;

    public String toString() {
        return "Digest[" + this.hex() + "]";
    }
    
    public String hex() {
        try(Formatter formatter = new Formatter()) {
            for (byte b : bytes) {
                formatter.format("%02x", b);
            }
            return formatter.toString();
        }
    }

    
    public boolean invalid_p() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(bytes);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Digest other = (Digest) obj;
        if (!Arrays.equals(bytes, other.bytes))
            return false;
        return true;
    }

    @Override
    public int compareTo(Digest o) {
        int n1 = this.bytes.length;
        int n2 = o.bytes.length;
        for (int i=0; i<n1 && i<n2; i++) {
            if (this.bytes[i] < o.bytes[i]) return -1;
            if (this.bytes[i] > o.bytes[i]) return 1;            
        }
        if (n1 < n2) return -1;
        if (n1 > n2) return 1;
        return 0;
    }

}
