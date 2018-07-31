package io.bitbucket.holmberg556.jcmds;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.regex.Pattern;

class Jcmds {

    static boolean opt_version;
    static boolean opt_verbose;
    static boolean opt_list_targets;
    static boolean opt_list_commands;
    static ArrayList<String> opt_args;
    static ArrayList<String> cmds_files = new ArrayList<>();

    static String g_CHDIR;
    static ArrayList<String> g_INPUT = new ArrayList<>();
    static ArrayList<String> g_OUTPUT = new ArrayList<>();
    static ArrayList<String> g_DEPEND = new ArrayList<>();
    static ArrayList<String> g_EXE_DEPEND = new ArrayList<>();
    static String g_CACHEDIR;

    
    static class Foo {
        ArrayList<String> get_strings() {
            ArrayList<String> x = new ArrayList<>();
            return  x;
        }
        
        void fff() {
            ArrayList<String> x = null;
            if ((x = get_strings()) != null) {
                System.out.println("hello");
            }
        }
    }

    static class ConsCache {
        HashMap<String, Cons> mIncsConsMap = new HashMap<>();

        Cons get(ArrayList<String> incs) {
            String idx = String.join(" ", incs);
            Cons cons = mIncsConsMap.get(idx);
            if (cons == null) {
                cons = new Cons(); // TODO: change
                cons.set_cpp_path(incs);
                mIncsConsMap.put(idx, cons);
            }
            return cons;
        }
    }

    // ----------------------------------------------------------------------

    static class CmdReader {
        ArrayList<String> mArgs;
        String mProg;

        CmdReader(String[] args) {
            mArgs = new ArrayList<>();
            for (String arg : args) {
                mArgs.add(arg);
            }
            mProg = new java.io.File(mArgs.get(0)).getName();
        }

        void process(String cmdline, ConsCache cons_cache, String subdir) {
            ArrayList<String> infiles = null;
            String infile = null;
            String outfile = null;
            ArrayList<String> incs = new ArrayList<>();

            Cmd cmd = null;

            String used_cmdline = (subdir.equals(".") ?
                    cmdline :
                        "cd " + subdir + " && " + cmdline);

            //--------------------
            if (g_INPUT.size() > 0 && g_OUTPUT.size() > 0) {
                Cons cons = cons_cache.get(incs);
                cmd = cons.Command(g_OUTPUT, g_INPUT, used_cmdline, false);
            }

            //--------------------
            else if (g_INPUT.size() > 0 && g_EXE_DEPEND.size() > 0
                    && cmdline.equals(":")) {
                Cons cons = cons_cache.get(incs);
                for (String tgt : g_EXE_DEPEND) {
                    for (String src : g_INPUT) {
                        cons.ExeDepends(tgt, src);
                    }
                }
            }

            //--------------------
            else if (g_INPUT.size() > 0 && g_DEPEND.size() > 0
                    && cmdline.equals(":")) {
                Cons cons = cons_cache.get(incs);
                for (String tgt : g_DEPEND) {
                    for (String src : g_INPUT) {
                        cons.Depends(tgt, src);
                    }
                }
            }

            //--------------------
            else if ((is_cmd("gcc") || is_cmd("g++") || is_cmd("c++")) && opt_c() &&
                    (infile = src()) != null && (outfile = opt_o()) != null) {
                ArrayList<String> args = mArgs;
                ArrayList<String> digest_cmdline_arr = new ArrayList<>();
                digest_cmdline_arr.add(mProg);
                for (int i=0, n=args.size(); i<n; i++) {
                    if (args.get(i).equals("-I") && n-i >= 2) {
                        incs.add(args.get(i+1));
                        i += 1;
                    }
                    else if (args.get(i).startsWith("-I")) {
                        incs.add(args.get(i).substring(2));
                    }
                    else {
                        digest_cmdline_arr.add(args.get(0));
                    }
                }

                String digest_cmdline = String.join(" ", digest_cmdline_arr);
                Cons cons = cons_cache.get(incs);
                cmd = cons.Command(outfile, infile, used_cmdline, digest_cmdline, true);
            }

            //--------------------
            else if ((is_cmd("gcc") || is_cmd("g++") || is_cmd("c++"))
                    && !opt_c() &&
                    (infiles = objs()) != null &&
                    add_archives(infiles) &&
                    (outfile = opt_o()) != null) {

                Cons cons = new Cons();
                cmd = cons.Command(outfile, infiles, used_cmdline);
            }

            //--------------------
            else if (is_cmd("ar") && ar_option(mArgs.get(1)) && nargs() >= 4) {
                outfile = mArgs.get(2);
                infiles = new ArrayList<>();
                for (int i = 3; i < mArgs.size(); i++) {
                    infiles.add(mArgs.get(i));
                }

                Cons cons = new Cons();
                cmd = cons.Command(outfile, infiles, used_cmdline);
            }

            //--------------------
            else if (is_cmd("cp") && nargs() == 3 && !is_option(mArgs.get(1))) {
                infile = mArgs.get(1);
                outfile = mArgs.get(2);

                Cons cons = new Cons();
                cmd = cons.Command(outfile, infile, used_cmdline);
            }

            //--------------------
            else if ((infile = has_input()) != null
                    && (outfile = has_output()) != null) {
                Cons cons = new Cons();
                cmd = cons.Command(outfile, infile, used_cmdline);
            }

            //--------------------
            else {
               System.out.printf("                                   ------ Unknown command: %s%n", used_cmdline);
               System.exit(1);
            }

            if (g_CACHEDIR != null) {
                cmd.set_cachedir(g_CACHEDIR);
            }
        }

        boolean is_option(String arg) {
            return arg.length() > 1 && arg.startsWith("-");
        }

        int nargs() {
            return mArgs.size();
        }

        boolean is_cmd(String name) {
            return mProg.equals(name) || mProg.endsWith("-" + name);
        }

        boolean opt_c() {
            return mArgs.indexOf("-c") != -1;
        }

        String opt_o() {
            int i = mArgs.indexOf("-o");
            if (i == -1) return null;
            if (i == mArgs.size() - 1) return null;
            return mArgs.get(i+1);
        }

        boolean ar_option(String arg) {
            return arg.equals("rc") || arg.equals("cq") || arg.equals("qc");
        }

        String src() {
            String res = null;
            for (String arg : mArgs) {
                if (arg.endsWith(".c") || arg.endsWith(".cpp") || arg.endsWith(".cc")) {
                    if (res != null) return null; // more than one
                    res = arg;
                }
            }
            return res;
        }

        ArrayList<String> objs() {
            ArrayList<String> files = null;
            for (String arg : mArgs) {
                if (arg.endsWith(".o")) {
                    if (files == null) {
                        files = new ArrayList<>();
                    }
                    files.add(arg);
                }
            }
            return files;
        }

        boolean add_archives(ArrayList<String> files) {
            for (String arg : mArgs) {
                if (arg.endsWith(".a")) {
                    files.add(arg);
                }
            }
            return true;
        }

        String has_input() {
            String res = null;
            for (int i = 0; i < mArgs.size(); i++) {
                if (mArgs.get(i).equals("<") && i < mArgs.size() - 1) {
                    if (res != null)
                        return null;
                    res = mArgs.get(i + 1);
                }
            }
            return res;
        }

        String has_output() {
            String res = null;
            for (int i = 0; i < mArgs.size(); i++) {
                if (mArgs.get(i).equals(">") && i < mArgs.size() - 1) {
                    if (res != null)
                        return null;
                    res = mArgs.get(i + 1);
                }
            }
            return res;
        }
    }

    // ----------------------------------------------------------------------


// ----------------------------------------------------------------------

    static Pattern spaces_re = Pattern.compile(" +");

    static void read_commands(InputStreamReader ir, HashMap<String,ConsCache> cons_cache_by_dir,
            String subdir) {
        Cons.push_dir(subdir);

        String cwd = ".";
        try (BufferedReader r = new BufferedReader(ir)) {
            String line = null;
            while ((line = r.readLine()) != null) {
                String line2 = line.trim();
                if (line2.isEmpty()) continue;
                String[] args = spaces_re.split(line2.trim());

                if (args[0].equals("#") && args.length == 3
                        && args[1].equals("CHDIR:")) {
                    g_CHDIR = args[2];
                    continue;
                }
                if (line.startsWith("# INPUT: ")) {
                    g_INPUT.add(line.substring(9));
                    continue;
                }
                else if (line.startsWith("# OUTPUT: ")) {
                    g_OUTPUT.add(line.substring(10));
                    continue;
                }
                else if (line.startsWith("# DEPEND: ")) {
                    g_DEPEND.add(line.substring(10));
                    continue;
                }
                else if (line.startsWith("# EXE_DEPEND: ")) {
                    g_EXE_DEPEND.add(line.substring(14));
                    continue;
                }
                else if (line.startsWith("# CACHEDIR: ")) {
                    g_CACHEDIR = line.substring(12);
                    continue;
                }

                if (line.startsWith("#")) continue;

                if (args[0].equals("cd") && args.length > 3 && args[2].equals("&&")) {
                    cwd = args[1];
                    args = Arrays.copyOfRange(args, 3, args.length);
                }

                CmdReader cmd_reader = new CmdReader(args);

                if (! cons_cache_by_dir.containsKey(cwd)) {
                    cons_cache_by_dir.put(cwd, new ConsCache());
                }

                if (! cwd.equals(".")) {
                    Cons.push_dir(cwd);
                    cmd_reader.process(line, cons_cache_by_dir.get(cwd), subdir);
                    Cons.pop_dir();
                }
                else if (g_CHDIR == null || g_CHDIR.isEmpty()) {
                    cmd_reader.process(line, cons_cache_by_dir.get(cwd), subdir);
                }
                else {
                    Cons.push_dir(g_CHDIR);
                    String line_with_chdir = "cd " + g_CHDIR + " && " + line;
                    cmd_reader.process(line_with_chdir, cons_cache_by_dir.get(cwd), subdir);
                    Cons.pop_dir();
                }

                cwd = ".";
                g_CHDIR = null;
                g_INPUT.clear();
                g_OUTPUT.clear();
                g_DEPEND.clear();
                g_EXE_DEPEND.clear();
                g_CACHEDIR = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        Cons.pop_dir();
    }

//----------------------------------------------------------------------

    static ArrayList<String> targets = null;

    static void parse_arguments(String[] argv) {
        ArgParse p = new ArgParse("jcmds");
        p.flag("h", "help", "print this help and exit", (value) -> {
            Opts.help = value;
        });
        p.flag("q", "quiet", "be more quiet", (value) -> {
            Opts.quiet = value;
        });
        p.flag(null, "debug", "debug output", (value) -> {
            Opts.debug = value;
        });
        p.flag(null, "trace", "trace output", (value) -> {
            Opts.trace = value;
        });
        p.integer("j", "parallel", "build in parallel", "N", (value) -> {
            Opts.parallel = value;
        });
        p.flag(null, "ignore-includes", "no C/C++ header files", (value) -> {
                    Opts.ignore_includes = value;
                });
        p.flag("k", "keep-going", "continue after errors", (value) -> {
            Opts.keep_going = value;
        });
        p.flag("B", "always-make", "always build targets", (value) -> {
            Opts.build_all = value;
        });
        p.flag("r", "remove", "remove targets", (value) -> {
            Opts.remove = value;
        });
        p.flag(null, "accept-existing-target",
                "make updating an existing target a nop", (value) -> {
                Opts.accept_existing_target = true;
        });
        p.flag(null, "dont-trust-mtime",
                "always consult files for content digest", (value) -> {
                    Opts.trust_mtime = !value;
        });
        p.string("f", "file", "name of *.cmds file", (value) -> {
            cmds_files.add(value);
        });
        p.flag("v", "verbose", "be more verbose", (value) -> {
            Opts.verbose=value;
        });
        p.flag(null, "version", "show version", (value) -> {
            Opts.version=value;
        });
        p.string(null, "cache-dir", "name of cache directory", (value) -> {
            Opts.cachedir = value;
        });
        p.flag(null, "cache-force", "copy existing files into cache",
                (value) -> {
            Opts.cache_force=value;
        });
        p.flag("p", "list-targets", "list known targets", (value) -> {
            Opts.list_targets = value;
        });
        p.flag(null, "list-commands", "list known commands", (value) -> {
            Opts.list_commands = value;
        });
        p.flag(null, "log-states", "log state machine", (value) -> {
            Opts.log_states = value;
        });
        p.args("target", "*", "targets to build", (value) -> {
            targets = value;
        });
        p.parse(argv);

        if (Opts.help) {
            p.print_usage();
            System.exit(0);
        }
        
        if (opt_version) {
            System.out.printf("jcons-cmds version 0.20%n");
            System.out.printf("%n");
            System.out.printf("Copyright 2002-2018 Johan Holmberg <holmberg556@gmail.com>.%n");
            System.out.printf("This is non-free software. There is NO warranty; not even%n");
            System.out.printf("for MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.%n");
            System.exit(0);
        }
    }

//----------------------------------------------------------------------

//int lookup_targets(String[] targets, ref Rentry entries[], ref Rfile[] tgts) {
//    if (targets.size() == 0) {
//        targets ~= ".";
//    }
//    int errors = 0;
//    for (target; targets) {
//        Rentry tgt = filesystem.cwd.lookup_entry(target);
//        if (tgt is null) {
//            writeln("jcons: error: don't know how to build '", target, "'");
//            errors += 1;
//            continue;
//        }
//        if (auto f = cast(Rfile)tgt) {
//            tgts ~= f;
//            f.m_top_target = true;
//            entries ~= f;
//        }
//        else  if (auto d = cast(Rdir)tgt) {
//            d.append_files_under(tgts);
//            entries ~= d;
//        }
//        else {
//            writeln("jcons: error: internal error");
//            exit(1);
//        }
//    }
//    return errors;
//}

//----------------------------------------------------------------------

     public static void main(String[] argv) {
        parse_arguments(argv);

//         cons.initialize();
//         setup_signals();
//
        if (Opts.cachedir != null) {
            Cache.set_cachedir(Opts.cachedir);
        }

        HashMap<String,HashMap<String,ConsCache>> cons_cache_by_dir2 = new HashMap<>();

        if (cmds_files.isEmpty()) {
            if (! cons_cache_by_dir2.containsKey(".")) {
                cons_cache_by_dir2.put(".", new HashMap<>());
            }
            InputStreamReader r = new InputStreamReader(System.in);
            read_commands(r, cons_cache_by_dir2.get("."), ".");
        }
        else {
            for (String fname : cmds_files) {
                try (FileReader r = new FileReader(fname)) {
                    if (Opts.verbose) {
                        System.out.printf("+++ reading command file '%s'%n", fname);
                    }
                    String dir = new java.io.File(fname).getParent();
                    if (dir == null)
                        dir = ".";
                    if (! cons_cache_by_dir2.containsKey(dir)) {
                        cons_cache_by_dir2.put(dir, new HashMap<>());
                    }
                    read_commands(r, cons_cache_by_dir2.get(dir), dir);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    System.exit(1);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        }

        ArrayList<File> tgts = new ArrayList<>();
        ArrayList<Entry> entries = new ArrayList<>();
        int errors = Dir.lookup_targets(targets, entries, tgts);
        if (errors > 0) {
            System.exit(1);
        }
        if (Opts.list_targets) {
            for (Entry tgt : tgts) {
                System.out.printf("%s%n", tgt.path());
            }
            System.exit(0);
        }

        if (Opts.list_commands) {
            System.out.printf("tgts ============ %s%n", tgts);
            Cmd.show_cmds();
            System.exit(0);
        }
        if (Opts.remove) {
            remove_targets(tgts, entries);
            System.exit(0);
        }
        
        Engine engine = new Engine(tgts);
        engine.Run();
        Dir.terminate();

        ArrayList<String> paths = new ArrayList<>();
        for (Builder.BaseFun fun : Builder.s_active_funs.keySet()) {
            if (fun.m_caller != null) {
                if (fun.m_caller instanceof Builder.FUN_update_tgt) {
                    Builder.FUN_update_tgt update_fun = (Builder.FUN_update_tgt) fun.m_caller;
                    paths.add(update_fun.tgt.path());
                }
            }
        }
        if (paths.size() > 0) {
            Collections.sort(paths);
            System.out.printf("jcons: error: circular dependency for '%s'%n",
                    String.join(",", paths));
            System.exit(1);
        }

        
        
        for (Entry entry : entries) {
            if (entry instanceof Dir) {
                Dir d = (Dir) entry;
                if (!d.dirty) {
                    System.out.printf("jcons: up-to-date: %s%n", d.path());
                }
            }
        }

        Dir.terminate();
        if (Opts.debug)
            System.out.printf("Exiting jcons ...%n");

//
//    auto engine = new Engine(tgts);
//    engine.run();
//    scope(exit) filesystem.terminate();
//
//    if (Engine.got_sigint) {
//        writeln("jcons: *** got interrupt, terminating");
//        return 1;
//    }
//
//
//    for (entry; entries) {
//        if (auto d = cast(Rdir)entry) {
//            if (! entry.dirty) {
//                writeln("jcons: up-to-date: ", entry.path);
//            }
//        }
//    }
//
        System.exit(Engine.g_nerrors == 0 ? 0 : 1);
//}
//
//
//extern (C) nothrow @nogc @system void handle_sigint_signal(int sig) {
//    Engine.got_sigint = true;
//}
//
//void setup_signals() {
//    auto f = signal(SIGINT, &handle_sigint_signal);
//}

     }

     static void remove_targets(ArrayList<File> tgts, ArrayList<Entry> entries) {
         for (File tgt : tgts) {
             if (tgt.file_exist_FORCED()) {
                 new java.io.File(tgt.path()).delete();
                 System.out.printf("Removed %s%n", tgt.path());
                 tgt.st_invalid_error();
             }
         }
         for (Entry entry : entries) {
             if (entry instanceof Dir) {
                 Dir d = (Dir) entry;
                 if (! entry.dirty) {
                     System.out.printf("jcons: already removed: %s%n", entry.path());
                 }
             }
         }
     }
}
