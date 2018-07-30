package io.bitbucket.holmberg556.jcmds;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Formatter;

class Md5 {

    MessageDigest md;

    Md5() {
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    static Digest digest(String string) {
        Md5 m = new Md5();
        m.append(string);
        return m.digest();
    }

    static String hexdigest(String string) {
        Md5 m = new Md5();
        m.append(string);
        return m.hexdigest();
    }

    static Digest calc_digest(ArrayList<Digest> digests) {
        Md5 m = new Md5();
        m.append(digests);
        return m.digest();
    }

    void append(ArrayList<Digest> digests) {
        for (Digest digest: digests) {
            md.update(digest.bytes);
        }
    }

    void append(String str) {
        md.update(str.getBytes());
    }

    Digest digest() {
        Digest digest = new Digest();
        digest.bytes = md.digest();
        return digest;
    }

    String hexdigest() {
        try(Formatter formatter = new Formatter()) {
            for (byte b : md.digest()) {
                formatter.format("%02x", b);
            }
            return formatter.toString();
        }
    }

    void append(Digest digest) {
        //System.out.println(digest.hex());
        //System.out.println(digest.bytes.length);
        md.update(digest.bytes);

    }

    void append(int i) {
        append(Integer.toString(i));
    }

    void append(byte[] buf, int n) {
        md.update(buf,  0, n);
    }

}
