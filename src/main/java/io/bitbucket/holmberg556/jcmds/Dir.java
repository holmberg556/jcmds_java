package io.bitbucket.holmberg556.jcmds;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

public class Dir extends Entry {

    Object m_fs_entries;
    Consign cached_consign;
    Dir tgtdir;
    boolean m_cache_dir_p;
    boolean m_local_dir_p;
    HashMap<String, Entry> entries;

    static Dir s_curr_dir;
    static HashMap<String,Dir> s_tops;
    static ArrayList<Dir> s_dir_arr;


    Dir(String name, Dir parent) {
        super(name, parent);
        m_fs_entries = null;
        cached_consign = null;
        tgtdir = null;

        // TODO: verify that parent != null always is true,
        // and if so remove the tests below.
        m_cache_dir_p = (parent != null && parent.m_cache_dir_p);
        m_local_dir_p = (parent != null && parent.m_local_dir_p);

        entries = new HashMap<String,Entry>();
        entries.put(".", this);
        entries.put("..", (parent != null ? parent : this));
    }

    Dir(String name) {
        super(name);
        m_fs_entries = null;
        cached_consign = null;
        tgtdir = null;
        m_cache_dir_p = false;     // TODO: how is this used?
        m_local_dir_p = false;

        entries = new HashMap<String,Entry>();
        entries.put(".", this);
        entries.put("..", this);
    }

    String[] _split_path(String path) {
        if (path.isEmpty()) {
            throw new RuntimeException("empty path");
        }
        if (path.equals("/")) {
            return new String[]{ "" };
        }
        String[] parts = path.split("/");
        return parts;
    }


    File find_file(String path) {
        String[] parts = _split_path(path);
        String name = parts[parts.length-1];
        Dir d = (parts.length == 1) ? this : _find_dir(parts, 0, parts.length - 1);

        File f;
        if (! d.entries.containsKey(name)) {
            f = new File(name, d);
            d.entries.put(name, f);
        }
        else {
            Entry e = d.entries.get(name);
            if (! (e instanceof File)) {
                throw new RuntimeException("not file");
            }
            f = (File) e;
        }
        return f;
    }


    Dir find_dir(String path) {
        //System.out.printf("### find_dir: %s\n", path);
        String[] parts = _split_path(path);
        return _find_dir(parts,0, parts.length);
    }

    Dir _find_dir(String[] path, int start, int end) {
        Dir d = this; // xxx
        for (int i=start; i<end; i++) {
            String name = path[i];
            if (name.equals("#")) {
                d = s_curr_dir;
            }
            else if (name.isEmpty()) {
                d = s_tops.get("/");
            }
            else {
                Dir d2;
                if (! d.entries.containsKey(name)) {
                    d2 = new Dir(name, d);
                    d.entries.put(name, d2);
                }
                else {
                    Entry e = d.entries.get(name);
                    if (!(e instanceof Dir)) {
                        throw new RuntimeException("not dir");
                    }
                    d2 = (Dir) e;
                }
                d = d2;
            }
        }
        return d;
    }

    enum Color { WHITE, GREY, BLACK };

    //struct Include {
    // bool quotes;
    // String file;
    //}
    //
    ////======================================================================
    ////Information saved for each file
    //
    //class ConsignEntry {
    //
    // SysTime mtime;
    // Digest content_sig;
    // Digest dep_sig;
    // Include[] includes;
    //
    // this() {
    //     this.mtime = SysTime(0, UTC());
    // }
    //
    // const void toString(scope void delegate(const(char)[]) sink) {
    //     sink("ConsignEntry(");
    //     sink(to!String(mtime));
    //     sink(", ");
    //     sink(to!String(content_sig));
    //     sink(", ");
    //     sink(to!String(dep_sig));
    //     if (includes.length > 0) {
    //         sink(", [[");
    //         foreach (include; includes) {
    //             sink(to!String(include.quotes));
    //             sink(" ");
    //             sink(include.file);
    //             sink(", ");
    //         }
    //         sink("]] ");
    //     }
    //     sink(")");
    // }
    //}
    //
    ////======================================================================
    //
    ////======================================================================
    ////Rentry
    //
    //class Rentry {
    //
    //
    // this(String name, Dir parent) {
    //     this.name = name;
    //     this.cached_path = null;
    //     this.raw_parent = parent;
    //     this.dirty = false;
    // }
    //
    // // Constructor for "root" directory
    // this(String name) {
    //     this.name = name;
    //     this.cached_path = name;
    //     this.raw_parent = null;
    //     this.dirty = false;
    // }
    //
    // void toString(scope void delegate(const(char)[]) sink) {
    //     sink("Rentry[");
    //     sink(this.path);
    //     sink("]");
    // }
    //
    //
    // // Set dirty flag and propagate upwards in tree.
    // void set_dirty() {
    //     if (dirty) return;
    //     dirty = true;
    //     for (Dir dir = raw_parent; dir != null; dir = dir.raw_parent) {
    //         if (dir.dirty) break;
    //         dir.dirty = true;
    //     }
    // }
    //}
    //
    ////======================================================================
    //
    //Dir s_curr_dir;
    //Dir[] s_dir_arr;
    //Dir[String] s_tops;
    //
    static String s_dot_jcons_dir = ".jcons/java";
    //
    //@property Dir cwd() { return s_curr_dir; }
    //

    static String fs_getcwd() {
        return System.getProperty("user.dir");
    }

    //Initialize filesystem module.
    //The function can be called several times, and will then "reset"
    //the state. This can be useful for testing the code.

    static void initialize() {
        s_curr_dir = null;
        s_tops = new HashMap<String,Dir>(); // TODO

        s_dir_arr = new ArrayList<>();

        Dir top = new Dir("/");
        s_tops.put("/", top);
        s_curr_dir = top.find_dir(fs_getcwd());
        s_curr_dir.cached_path = ".";
        s_curr_dir.m_local_dir_p = true;

        if (! (fs_file_exist(s_dot_jcons_dir) && fs_isdir(s_dot_jcons_dir))) {
          mkdirRecurse(s_dot_jcons_dir);
        }
    }

    static void mkdirRecurse(String dir1) {
        new java.io.File(dir1).mkdirs();
    }

    // TODO: move to "Util" ???
    static boolean fs_file_exist(String path) {
        return new java.io.File(path).exists();
    }

    static boolean fs_isdir(String path) {
         return new java.io.File(path).isDirectory();
    }


    static {
        initialize();
    }

    //Flush information to .consign files.
    //Should be called before the process is terminated.
    static void terminate() {
     long curr_mtime = curr_time_filesystem();
     for (Dir d : s_dir_arr) {
         if (d.cached_consign.dirty) {
             d.cached_consign.write_to_file(curr_mtime, d._consign_path());
         }
     }
    }

    static long curr_time_filesystem() {
        String timestamp_file = s_dot_jcons_dir + "/.timestamp";
        try(PrintWriter o = new PrintWriter(new FileWriter(timestamp_file))) {
            o.printf("hello ...\n");
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return File.get_file_mtime(timestamp_file);
    }

    ////======================================================================
    ////Dir
    //
    //class Dir : Rentry {
    //
    // bool m_cache_dir_p;
    // bool m_local_dir_p;
    // Consign cached_consign;
    // Rentry[String] entries;
    // bool[String] m_fs_entries;
    // Dir tgtdir;
    //
    //
    // //----------------------------------------------------------------------
    //
    // Dir parent() {
    //     return raw_parent is null ? this : raw_parent;
    // }
    //
    // //----------------------------------------------------------------------
    //
    // void teardown() {
    //     s_dir_arr = [];
    //     foreach (k; s_tops.keys) s_tops.remove(k);
    // }
    //
    // //----------------------------------------------------------------------
    //
    // void init_out_of_source(String relpath, Dir tgtdir) {
    //     this.cached_path = relpath;
    //     this.tgtdir = tgtdir;
    // }
    //
    // //----------------------------------------------------------------------
    //
    // void top_print_tree() {
    //     foreach (k,v; s_tops) {
    //         v.print_tree(0);
    //     }
    // }
    //
    //----------------------------------------------------------------------

    String _consign_path() {
        if (m_cache_dir_p || (Opts.perl_cons && m_local_dir_p)) {
            String consign_path = this.path() + "/.rconsign";
            return consign_path;
        }
        else {
            String name = this.path();
            //
            // Transform a path with directory delimiters ('/') to a filename:
            //
            //   - use '=' as quote character
            //   - use '+' instead of '/'
            //
            name = name.replace("=", "==");
            name = name.replace("+", "=+");
            name = name.replace("/", "+");

            String consign_path = s_dot_jcons_dir + "/__" + name;
            return consign_path;
        }
    }

    //----------------------------------------------------------------------

    Consign consign() {
        if (cached_consign == null) {
            cached_consign = Consign.read_from_file(_consign_path());
            s_dir_arr.add(this);
        }
        return cached_consign;
    }

    //----------------------------------------------------------------------
    //
    // override void print_tree(int level) {
    //     for (int i=0; i<level; ++i) { write("    "); }
    //     writeln("'", name, "'");
    //     foreach (k,v; entries) {
    //         if (k == ".") continue;
    //         if (k == "..") continue;
    //         v.print_tree(level + 1);
    //     }
    // }
    //
    // //----------------------------------------------------------------------
    //
    //
    // unittest {
    //     assert( _split_path("/aaa") == ["", "aaa"] );
    //     assert( _split_path("/aaa/bbb") == ["", "aaa", "bbb"] );
    //
    //     assert( _split_path("aaa") == ["aaa"] );
    //     assert( _split_path("aaa/bbb") == ["aaa", "bbb"] );
    //     assert( _split_path("aaa/bbb/ccc") == ["aaa", "bbb", "ccc"] );
    // }
    //
    // //----------------------------------------------------------------------
    //
    //
    // //----------------------------------------------------------------------
    // unittest {
    //     initialize();
    //     assert(true);
    //     Dir d1  = s_curr_dir.find_dir("d1");
    //     Dir d1x = s_curr_dir.find_dir("d1");
    //     Dir d2  = s_curr_dir.find_dir("d2");
    //     Dir e1  = s_curr_dir.find_dir("d1/e1");
    //     Dir e2  = s_curr_dir.find_dir("d1/e2");
    //
    //     assert( d1.path() == "d1" );
    //     assert( d1x.path() == "d1" );
    //     assert( d1 is d1x );
    //     assert( d1 != d2 );
    //
    //     assert( d1 != e1 );
    //     assert( d1 is e1.parent() );
    //
    //     assert( d2.path() == "d2" );
    //
    //     assert( e1.path() == "d1/e1" );
    //     assert( e2.path() == "d1/e2" );
    //
    //     assert( d1.parent().path() == "." );
    //
    //     assert( d1.parent().parent().path() == dirName(getcwd()) );
    // }
    //
    //----------------------------------------------------------------------

    Dir lookup_or_fs_dir(String path) {
        String parts[] = _split_path(path);
        return _lookup_or_fs_dir(parts, 0, parts.length);
    }

    Dir _lookup_or_fs_dir(String[] parts, int begin, int end) {
        Dir d = this;
        for (int i=begin; i<end; i++) {
            String name = parts[i];
            if (name.isEmpty()) {
                d = s_tops.get("/");
            }
            else {
                Entry e = d.entries.get(name);
                if (! d.entries.containsKey(name)) {
                    String fspath = d.path() + "/" + name;
                    java.io.File fs = new java.io.File(fspath);
                    if (! fs.exists()) {
                        d.entries.put(name, null);
                        return null;
                    }
                    else if (fs.isDirectory()) {
                            Dir d2 = new Dir(name, d);
                            d.entries.put(name, d2);
                            d = d2;
                        }
                    else {
                        File f = new File(name, d);
                        d.entries.put(name,  f);
                        f.m_mtime = fs.lastModified();
                        f.m_mtime_set = true;
                        throw new RuntimeException("not a dir");
                    }
                }
                else if (e == null) {
                    return null;
                }
                else {
                    if (e instanceof Dir) {
                        d = (Dir) e;
                    }
                    else {
                        throw new RuntimeException("not a dir");
                    }
                }
            }
        }
        return d;
    }


    //----------------------------------------------------------------------
    // TODO: what to do if 'path' is a full filepath?

    File lookup_or_fs_file(String path) {
        String[] parts = _split_path(path);
        Dir d = _lookup_or_fs_dir(parts, 0, parts.length - 1);
        if (d  == null) {
            return null;
        }
        else {
            File f;
            String name = parts[parts.length - 1];
            Entry e = d.entries.get(name);
            if (! d.entries.containsKey(name)) {
                String fspath = d.path() + "/" + name;
                java.io.File fs = new java.io.File(fspath);
                if (! fs.exists()) {
                    d.entries.put(name,  null);
                    return null;
                }
                else if (fs.isFile()) {
                    f = new File(name, d);
                    d.entries.put(name,  f);
                }
                else {
                    Dir d2 = new Dir(name, d);
                    d.entries.put(name,  d2);
                    throw new RuntimeException("not a file");
                }
            }
            else if ( e == null) {
                return null;
            }
            else {
                if (e instanceof File) {
                    f = (File) e;
                }
                else {
                    throw new RuntimeException("not a file");
                }
            }
            return f;
        }
    }

    //----------------------------------------------------------------------

     Entry lookup_entry(String path) {
         String[] parts = _split_path(path);
         Entry e = this;
         for (String name : parts) {
             if (name.isEmpty()) {
                 e = s_tops.get("/"); // TODO: compare dlang, python
                 continue;
             }
             if (! (e instanceof Dir)) {
                 throw new RuntimeException("not dir");
             }
             Dir d = (Dir) e;
             e = d.entries.get(name);
             if (e == null) {
                 return null;
             }
         }
         return e;
     }

     //----------------------------------------------------------------------

    boolean mkdir_p() {
        java.io.File f = new java.io.File(this.path());
        if (f.isDirectory())
            return true;
        else
            return new java.io.File(this.path()).mkdirs();
    }

     //----------------------------------------------------------------------
    
     public void append_files_under(ArrayList<File> tgts) {
        // ArrayList<String> ks = new ArrayList<>();
        // ks.addAll(entries.keySet());
        // Collections.sort(ks);

        TreeSet<String> ks = new TreeSet<>();
        ks.addAll(entries.keySet());

        for (String k : ks) {
             Entry v = entries.get(k);
             if (k.equals(".")) continue;
             if (k.equals("..")) continue;
             if (v instanceof Dir) {
                 Dir d = (Dir) v;
                 d.append_files_under(tgts);
             }
             else if (v instanceof File) {
                 File f = (File) v;
                 if (f.cmd != null) {
                     tgts.add(f);
                 }
             }
             else {
                 System.out.printf("jcons: error: internal error\n");
                 System.exit(1);
             }
         }
     }

    static int lookup_targets(ArrayList<String> targets, ArrayList<Entry> entries, ArrayList<File> tgts) {
        if (targets.size() == 0) {
            targets.add(".");
        }
        int errors = 0;
        for (String target : targets) {
            Entry tgt = s_curr_dir.lookup_entry(target);
            if (tgt == null) {
                System.out.printf(
                        "jcons: error: don't know how to build '%s'\n", target);
                errors += 1;
                continue;
            }
            if (tgt instanceof File) {
                File f = (File) tgt;
                tgts.add(f);
                f.m_top_target = true;
                entries.add(f);
            }
            else if (tgt instanceof Dir) {
                Dir d = (Dir) tgt;
                d.append_files_under(tgts);
                entries.add(d);
            }
            else {
                System.out.printf("jcons: error: internal error\n");
                System.exit(1);
            }
        }
        return errors;
    }
}
