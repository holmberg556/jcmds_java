package io.bitbucket.holmberg556.jcmds;

import java.util.ArrayList;
import java.util.HashMap;

public class ProgramPath {
    ArrayList<Dir> m_dir_arr = new ArrayList<>();
    HashMap<String,File> m_file_by_cmdname = new HashMap<>();

    static HashMap<String,ProgramPath> s_obj_by_path = new HashMap<>();

    ProgramPath(String program_path) {
        for (String dir : program_path.split(":")) {
            Dir d = Dir.s_curr_dir.lookup_or_fs_dir(dir);
            if (d != null) {
                m_dir_arr.add(d);
            }
        }
    }

    static void teardown() {
        s_obj_by_path = new HashMap<>();
    }

    static ProgramPath find(String path) {
        ProgramPath obj = s_obj_by_path.get(path);
        if (obj == null) {
            obj = new ProgramPath(path);
            s_obj_by_path.put(path, obj);
        }
        return obj;
    }

    File find_program(String cmdname) {
        if (m_file_by_cmdname.containsKey(cmdname)) {
            return m_file_by_cmdname.get(cmdname);
        }
        if (! cmdname.contains("/")) {
            // simple name
            for (Dir dir : m_dir_arr) {
                File f = dir.lookup_or_fs_file(cmdname);
                if (f != null) {
                    m_file_by_cmdname.put(cmdname, f);
                    return f;
                }
            }
        }
        else {
            // relative path
            File f = Dir.s_curr_dir.lookup_or_fs_file(cmdname);
            if (f != null) {
                m_file_by_cmdname.put(cmdname, f);
                return f;
            }
        }
        m_file_by_cmdname.put(cmdname, null);
        return null;
    }

}
