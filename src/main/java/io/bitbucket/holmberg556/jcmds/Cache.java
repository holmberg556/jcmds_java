package io.bitbucket.holmberg556.jcmds;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Cache {

    static Cache g_build_cache = null;
    
    static void set_cachedir(String cachedir) {
        Dir d = Cons.s_conscript_dir != null ? Cons.s_conscript_dir : Dir.s_curr_dir;
        g_build_cache = new Cache(d, cachedir);
    }

    Dir m_cache_dir;

    Cache(Dir d, String cache_dir) {
        m_cache_dir = d.find_dir(cache_dir);
    }
    
    public void put(Digest dep_sig, File file) {
        String dep_sig_str = dep_sig.hex();
        Digest xxx = dep_sig;
        String prefix = dep_sig_str.substring(0, 2);
        Dir cache_dir = m_cache_dir.find_dir(prefix);
        boolean ok = cache_dir.mkdir_p();
        if (!ok) return;

        File cache_file = cache_dir.find_file(dep_sig_str);
        if (! cache_file.file_exist()) {
            ok = link_or_copy(file.path(), cache_file.path());
            if (!ok) return;
            cache_file.st_updated(dep_sig);
        }
    }

    boolean link_or_copy(String source, String target) {
        Path tgt = Paths.get(target);
        Path src = Paths.get(source);
        try {
            Files.createLink(tgt, src);
            return true;
        } catch (IOException e1) {
            // fall through
        }

        try {
            Files.copy(src, tgt);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public boolean get(Digest dep_sig, File file) {
        String dep_sig_str = dep_sig.hex();
        String prefix = dep_sig_str.substring(0, 2);
        Dir cache_dir = m_cache_dir.find_dir(prefix);
        boolean ok = cache_dir.mkdir_p();
        if (!ok) return false;

        File cache_file = cache_dir.find_file(dep_sig_str);
        if (! cache_file.file_exist()) {
            return false;
        }
        if (!cache_file.db_dep_sig().equals(dep_sig)) {
            new java.io.File(cache_file.path()).delete();
            return false;
        }
        ok = link_or_copy(cache_file.path(), file.path());
        if (!ok) {
            new java.io.File(file.path()).delete();
            return false;
        }
        file.st_updated(dep_sig);
        System.out.printf("Updated from cache: %s%n", file.path());
        return true;
    }

}
