package io.bitbucket.holmberg556.jcmds;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.bitbucket.holmberg556.jcmds.Builder.BaseFun;

public class File extends Entry {

    enum Status {
        UNKNOWN,
        OK,
        ERROR
    }

    enum Color {
        WHITE,
        BLACK,
        GREY
    }

    @Override
    public String toString() {
        return "File[path=" + path() + "]";
    }

    Status build_ok = Status.UNKNOWN;
    long m_mtime;
    boolean m_mtime_set = false;
    boolean m_file_exist;
    boolean m_file_exist_set = false;
    Dir m_tgtfile = null;
    ArrayList<File> extra_deps = null;
    ArrayList<File> m_exe_deps = null;
    boolean m_top_target = false;
    Cmd cmd = null;
    BaseFun m_waiting_sem = null;

    Digest wanted_digest;

    Color color;

    static File null_sentinel = new File("null sentinel", null);

    File(String name, Dir parent) {
        super(name, parent);
    }

    Dir parent() {
        return raw_parent;
    }

    // ----------------------------------------------------------------------

    ConsignEntry _consign_get_entry() {
        return raw_parent.consign().get(name);
    }

    void _consign_remove_entry() {
        raw_parent.consign().remove(name);
    }

    void _consign_set_dirty() {
        raw_parent.consign().dirty = true;
    }

    // --------------------------------------------------
    // --------------------------------------------------
    // Methods for managing the .consign information ...

    // ----------------------------------------------------------------------
    // Return the "raw" content_sig value.
    // It will always exist when this function is called.

    Digest db_content_sig() {
        ConsignEntry consign_entry = _consign_get_entry();
        return consign_entry.content_sig;
    }


    // ----------------------------------------------------------------------
    // Return dep_sig if it is valid.
    // Called when we consider rebuilding the file.

    Digest db_dep_sig() {
        //System.out.printf("--> db_dep_sig: ???%n");
        ConsignEntry consign_entry = _consign_get_entry();
        if (consign_entry.dep_sig.invalid_p()) {
            //System.out.printf("--> db_dep_sig: %s, INVALID%n", this.path());
            return Digest.invalid;
        }

        long mtime = file_mtime();
        if (mtime == consign_entry.mtime && Opts.trust_mtime) {
            return consign_entry.dep_sig;
        }
        Digest content_sig = get_file_digest(path());
        //System.out.printf("--> db_dep_sig: %s, content_sig: %s <-> %s%n",
        //        this.path(), content_sig, consign_entry.content_sig);
        if (content_sig.equals(consign_entry.content_sig)) {
            consign_entry.mtime = mtime;
            _consign_set_dirty();
            //System.out.printf("--> db_dep_sig: %s, %s%n", this.path(), consign_entry.dep_sig);
            return consign_entry.dep_sig;
        }
        consign_entry.includes = null;
        _consign_set_dirty();
        //System.out.printf("--> db_dep_sig: %s, UNDEFINED%n", this.path());
        return Digest.undefined;
    }

    // ----------------------------------------------------------------------
    // The file exists and is a "source".
    // Make sure it has an accurate .consign entry.

    void st_source() {
        ConsignEntry consign_entry = _consign_get_entry();
        long mtime = file_mtime();
        if (!(mtime == consign_entry.mtime && Opts.trust_mtime)) {
            consign_entry.mtime = mtime;
            consign_entry.content_sig = get_file_digest(path());
            consign_entry.includes = null;
            _consign_set_dirty();
        }
        if (consign_entry.dep_sig != consign_entry.content_sig) {
            consign_entry.dep_sig = consign_entry.content_sig;
            _consign_set_dirty();
        }
        this.build_ok = Status.OK;
    }

    // ----------------------------------------------------------------------
    // The file is invalid: the file does not exist, or a build step failed.
    // Forget about the file.

    void st_invalid_error() {
        set_dirty();
        _consign_remove_entry();
        _consign_set_dirty();
        this.build_ok = Status.ERROR;
    }

    // ----------------------------------------------------------------------
    // Propagate the fact that there has been an error "upstream".
    // No change of .consign info.

    void st_propagate_error() {
        set_dirty();
        this.build_ok = Status.ERROR;
    }

    // ----------------------------------------------------------------------
    // The file is already up-to-date.
    // No change of .consign info.

    void st_propagate_ok() {
        this.build_ok = Status.OK;
    }

    // ----------------------------------------------------------------------
    // The file has been updated by running a command.
    // The new 'dep_sig' is stored, and 'mtime' and 'content_sig' are updated.

    void st_updated(Digest dep_sig) {
        set_dirty();
        ConsignEntry consign_entry = _consign_get_entry();
        _consign_set_dirty();
        consign_entry.mtime = file_mtime_FORCED();
        consign_entry.content_sig = get_file_digest(path());
        consign_entry.dep_sig = dep_sig;

        consign_entry.includes = null;
        if (Opts.debug) {
            System.out.printf("---> st_updated(%s, %s%n", this.path(), consign_entry.dep_sig);
        }
        this.build_ok = Status.OK;
    }

    // ----------------------------------------------------------------------

    boolean st_ok_p() {
        return this.build_ok == Status.OK;
    }

    // ----------------------------------------------------------------------

    boolean file_exist() {
        if (!m_file_exist_set) {
            set_file_exist_and_mtime(path());
            m_file_exist_set = true;
            m_mtime_set = m_file_exist;
        }
        return m_file_exist;
    }

    // ----------------------------------------------------------------------

    boolean file_exist_FORCED() {
        // like 'file_exist', but with forced call
        set_file_exist_and_mtime(path());
        m_file_exist_set = true;
        m_mtime_set = m_file_exist;

        return m_file_exist;
    }

    // ----------------------------------------------------------------------

    long file_mtime() {
        if (!m_mtime_set) {
            get_file_mtime(path());
            m_mtime_set = true;
        }
        return m_mtime;
    }

    // ----------------------------------------------------------------------

    long file_mtime_FORCED() {
        // like 'file_mtime', but with forced call
        m_mtime = get_file_mtime(path());
        m_mtime_set = true;

        return m_mtime;
    }

    // ----------------------------------------------------------------------
    // Called when file exists and has current info in .consign.
    // We may either get value from .consign or compute it now.

    ArrayList<Include> db_includes() {
        ConsignEntry consign_entry = _consign_get_entry();
        if (consign_entry.includes == null) {
            consign_entry.includes = new ArrayList<Include>();
            calc_includes(consign_entry.includes);
            _consign_set_dirty();
        }
        return consign_entry.includes;
    }

    // ----------------------------------------------------------------------

    static Pattern include_re = Pattern.compile("^\\s*#\\s*include\\s+([<\"])(.*?)[>\"]$");
    static Pattern end_re = Pattern.compile("^#end\\b");
    
    void calc_includes(ArrayList<Include> includes) {
        String path = this.path();
        try (FileReader fr = new FileReader(path);
                BufferedReader r = new BufferedReader(fr)) {
            String line;
            while ((line = r.readLine()) != null) {
                if (! line.contains("#")) continue;
                Matcher m = include_re.matcher(line);
                if (m.matches()) {
                    String quotes = m.group(1);
                    String filename = m.group(2);
                    // System.out.printf("INCLUDE: %s --- %s%n", quotes,
                    // filename);
                    includes.add(new Include(quotes.charAt(0) == '"',filename));
                }
                m = end_re.matcher(line);
                if (m.matches()) break;
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.exit(1);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.exit(1);
        }
    }

    boolean is_source() {
        return cmd == null;
    }
    //
    // //----------------------------------------------------------------------
    //
    // override void print_tree(int level) {
    // for (int i=0; i<level; ++i) { write(" "); }
    // writeln("\"", name, "\"");

    // }
    //
    // }


    void set_file_exist_and_mtime(String path) {
        java.io.File f = new java.io.File(path);
        m_mtime = f.lastModified();
        m_file_exist = m_mtime != 0;
    }

    static long get_file_mtime(String path) {
        java.io.File f = new java.io.File(path);
        long mtime = f.lastModified();
        if (mtime == 0) {
            throw new RuntimeException("nonexisting file");
        }
        return mtime;
    }

    static byte[] static_buf = new byte[16384];

    static Digest get_file_digest(String path) {
        Md5 md5 = new Md5();
        try (FileInputStream f = new FileInputStream(path)) {
            for (;;) {
                int n = 0;
                try {
                    n = f.read(static_buf);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
                if (n == -1) {
                    break;
                }
                md5.append(static_buf, n);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return md5.digest();
    }


}
