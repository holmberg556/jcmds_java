package io.bitbucket.holmberg556.jcmds;

import java.util.ArrayList;
import java.util.Stack;

public class Cons {

    public LibPathMap libpath_map() {
        // TODO Auto-generated method stub
        return null;
    }

    static Dir s_conscript_dir;

    static {
        Dir.initialize();
        s_conscript_dir = Dir.s_curr_dir;
    }

    static Stack<Dir> conscript_dir_stack = new Stack<>();

    static void push_dir(String path) {
        conscript_dir_stack.push(s_conscript_dir);
        Dir d = s_conscript_dir.find_dir(path);
        s_conscript_dir = d;
    }

    static void pop_dir() {
        s_conscript_dir = conscript_dir_stack.pop();
    }

    ProgramPath m_program_path;

    Env m_env;
    Dir m_conscript_dir;

    boolean m_obj_ext_set;
    String m_obj_ext;

    boolean m_exe_ext_set;
    String m_exe_ext;

    boolean m_lib_ext_set;
    String m_lib_ext;

    String m_libs_arg_string;
    String[] m_libs_arg_array;

    String[] m_libs;
    boolean m_libs_set;

    String[] m_libpath;
    boolean m_libpath_set;

    Dir[] m_libpath_dirs;
    boolean m_libpath_dirs_set;

    LibPathMap m_libpath_map;

    ArrayList<Dir> m_cpp_path_arg = new ArrayList<>();
    CppPathMap m_cpp_path;
    Cache m_build_cache;

    //----------------------------------------------------------------------

    Cons() {
        m_env = new Env();

        //     Settings::init_once();

        //     m_conscript_dir = s_conscript_dir;
        //     m_env = new VarEnv();

        //     m_cpp_path = null;
        //     m_build_cache = null;
        //     m_obj_ext_set = false;
        //     m_exe_ext_set = false;
        //     m_lib_ext_set = false;

        m_libs_set = false;
        m_libpath_set = false;
        m_libpath_dirs_set = false;
        m_libpath_map = null;

        //     if (Ros::is_windows() || Ros::fake_windows()) {
        //       Settings::apply("windows", m_env);
        //     }
        //     else {
        //       Settings::apply("linux", m_env);
        //     }

        //     if (Rglobal::scons) {
        //       // rcons-python
        //       _scons_defaults(m_env);
        //       m_env.set( "CC_CMD",     "$CCCOM" );
        //       m_env.set( "CXX_CMD",    "$CXXCOM" );

        //       m_env.set( "SOURCES",    "%INPUT" );
        //       m_env.set( "TARGET",     "%OUTPUT" );

        //     }

        //     s_all_rcons.push_back(this);
    }

    //----------------------------------------------------------------------

    void connect_tgt(File tgt, Cmd cmd) {
        Cmd existing_cmd = tgt.cmd;
        if (existing_cmd != null) {
            System.out.printf("jcons: error: same target twice: '%s'\n",
                    tgt.path());
            System.exit(1);
        }

        cmd.tgts.add(tgt);
        tgt.cmd = cmd;
        if (tgt.extra_deps != null) {
            if (cmd.extra_deps != null) {
                // merge sets
                cmd.extra_deps.addAll(tgt.extra_deps);
                tgt.extra_deps = null;
            }
            else {
                // move set
                cmd.extra_deps = tgt.extra_deps;
                tgt.extra_deps = null;
            }
        }
    }

    //----------------------------------------------------------------------

    void connect_src(File src, Cmd cmd) {
        cmd.srcs.add(src);
    }

  //----------------------------------------------------------------------

    Cmd Command(String tgt, String src, String command, String digest_command, boolean cpp_scanner) {
        File t = s_conscript_dir.find_file(_expand(tgt));
        File s = s_conscript_dir.find_file(_expand(src));
        Cmd cmd = new Cmd(this);
        connect_src(s, cmd);
        connect_tgt(t, cmd);
        cmd.m_digest_cmdline = digest_command;
        cmd.m_cmdline = command;
        cmd.uses_cpp = cpp_scanner && !Opts.ignore_includes;
        return cmd;
    }

  //----------------------------------------------------------------------

    private String _expand(String str) {
        // TODO implement
        return str;
    }

    Cmd Command(String tgt, String src, String command, boolean cpp_scanner) {
        File t = s_conscript_dir.find_file(_expand(tgt));
        File s = s_conscript_dir.find_file(_expand(src));
        Cmd cmd = new Cmd(this);
        connect_src(s, cmd);
        connect_tgt(t, cmd);
        cmd.m_cmdline = command;
        cmd.uses_cpp = cpp_scanner && !Opts.ignore_includes;
        return cmd;
    }

    //----------------------------------------------------------------------

    Cmd Command(ArrayList<String> tgts, ArrayList<String> srcs, String command, boolean cpp_scanner) {
        Cmd cmd = new Cmd(this);
        for (String tgt : tgts) {
            File t = s_conscript_dir.find_file(_expand(tgt));
            connect_tgt(t, cmd);
        }
        for (String src : srcs) {
            File s = s_conscript_dir.find_file(_expand(src));
            connect_src(s, cmd);
        }
        cmd.m_cmdline = command;
        cmd.uses_cpp = cpp_scanner && !Opts.ignore_includes;
        return cmd;
    }

    //----------------------------------------------------------------------

    Cmd Command(String tgt, String src, String cmd) {
        ArrayList<String> tgts = new ArrayList<>();
        tgts.add(tgt);
        ArrayList<String> srcs = new ArrayList<>();
        srcs.add(src);
        return Command(tgts, srcs, cmd, false);
    }

    // ----------------------------------------------------------------------

    Cmd Command(String tgt, ArrayList<String> srcs, String cmd) {
        ArrayList<String> tgts = new ArrayList<>();
        tgts.add(tgt);
        return Command(tgts, srcs, cmd, false);
    }

    //----------------------------------------------------------------------

    Cmd Command(ArrayList<String> tgts, String src, String cmd) {
        ArrayList<String> srcs = new ArrayList<>();
        srcs.add(src);
        return Command(tgts, srcs, cmd, false);
    }

    void Depends(String tgt, String src) {
        File t = s_conscript_dir.find_file(_expand(tgt));
        File s = s_conscript_dir.find_file(_expand(src));

        if (t.cmd != null) {
            if (t.cmd.extra_deps == null)
                t.cmd.extra_deps = new ArrayList<>();
            t.cmd.extra_deps.add(s);
        }
        else {
            if (t.extra_deps == null)
                t.extra_deps = new ArrayList<>();
            t.extra_deps.add(s);
        }
    }

    //----------------------------------------------------------------------

    void ExeDepends(String tgt, String src) {
        File t = s_conscript_dir.find_file(_expand(tgt));
        File s = s_conscript_dir.find_file(_expand(src));
        if (t.m_exe_deps == null)
            t.m_exe_deps = new ArrayList<>();
        t.m_exe_deps.add(s);
    }

    //----------------------------------------------------------------------

    public ProgramPath program_path() {
        if (m_program_path == null) {
            String path = System.getenv("PATH");
            if (path == null) {
                System.out.println("ERROR: PATH not set");
                System.exit(1);
            }
            m_program_path = ProgramPath.find(path);
        }
        return m_program_path;
    }

    CppPathMap cpp_path() {
        if (m_cpp_path == null) {
            m_cpp_path = CppPathMap.find(m_cpp_path_arg);
        }
        return m_cpp_path;
    }

    //----------------------------------------------------------------------

    void set_cpp_path(ArrayList<String> dirs) {
        m_cpp_path_arg = new ArrayList<>();
        for (String dir : dirs) {
            Dir d = s_conscript_dir.find_dir(dir);
            if (d.tgtdir != null) {
                m_cpp_path_arg.add(d.tgtdir);
            }
            m_cpp_path_arg.add(d);
        }
        cpp_path();
        //this.env_update_cpp_path();
    }


}
