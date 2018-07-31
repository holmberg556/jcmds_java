package io.bitbucket.holmberg556.jcmds;

import java.util.ArrayList;

public class Cmd {
    
    public enum State {
        COOKING,
        DONE, RAW
    }

    public ArrayList<File> extra_deps;
    public ArrayList<File> srcs;
    public boolean uses_cpp;
    public ArrayList<File> tgts;
    public int m_exitstatus;
    public Cons m_cons;
    public State m_state;
    public Digest m_includes_digest;
    public boolean m_uses_libpath;
    public String m_digest_cmdline;
    public String m_cmdline;
    private boolean m_cmdname_file_set;
    private File m_cmdname_file;

    Cache m_build_cache;
    
    static ArrayList<Cmd> s_all_cmds = new ArrayList<>();

    Cmd(Cons cons) {
        this.m_cons = cons;
        this.srcs = new ArrayList<>();
        this.tgts = new ArrayList<>();
        this.m_state = State.RAW;
        s_all_cmds.add(this);
    }

    public void st_propagate_error() {
        for (File tgt : tgts) {
            tgt.st_propagate_error();
        }
     }

    public Cache build_cache() {
        return m_build_cache != null ? m_build_cache : Cache.g_build_cache;
    }

    public boolean all_tgts_exist() {
        for (File tgt : tgts) {
            if (! tgt.file_exist()) {
                return false;
            }
        }
        return true;
    }

    public String get_targets_str() {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (File tgt : tgts) {
            if (! first) {
                builder.append(" ");
            }
            builder.append(tgt.path());
            first = false;
        }
        return builder.toString();
    }

    public void append_sig(Md5 md5) {
        String local_cmdline = get_cmdline1();
        md5.append(local_cmdline);

        File f = find_program();
        if (f != null) {
            md5.append( f.path() );
            md5.append( f.db_content_sig() );

            if (f.m_exe_deps != null ) {
                for (File dep : f.m_exe_deps) {
                    md5.append( dep.path() );
                    md5.append( dep.db_content_sig() );
                }
            }
        }
    }

    public File find_program() {
        if (! m_cmdname_file_set) {
            String cmdline = get_cmdline2();
            int i = cmdline.indexOf(' ');
            String cmdname = (i == -1 ? cmdline : cmdline.substring(0, i));
            m_cmdname_file = m_cons.program_path().find_program(cmdname);
            m_cmdname_file_set = true;
        }
        return  m_cmdname_file;
    }

    public void set_exitstatus(int exitstatus) {
        m_exitstatus = exitstatus;
    }

    String get_cmdline1() {
        if (m_digest_cmdline == null) {
            return m_cmdline;
        }
        else {
            return m_digest_cmdline;
        }
    }

    String get_cmdline2() {
        return m_cmdline;
    }

    void set_cachedir(String cachedir) {
        Dir d = Cons.s_conscript_dir != null ? Cons.s_conscript_dir : Dir.s_curr_dir;
        m_build_cache = new Cache(d, cachedir);
    }
    
    static void show_cmds() {
        for (Cmd cmd : s_all_cmds) {
            System.out.println("-----------------------------------------------------------");
            System.out.printf("cmd: %s%n", cmd.m_cmdline);
            for (File src : cmd.srcs) {
                System.out.printf("    src: %s%n", src.path());
            }
            for (File tgt : cmd.tgts) {
                System.out.printf("    tgt: %s%n", tgt.path());
            }
        }
    }
}
