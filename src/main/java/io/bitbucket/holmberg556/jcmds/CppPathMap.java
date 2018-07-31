package io.bitbucket.holmberg556.jcmds;

import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class CppPathMap {

    static HashMap<String,CppPathMap> s_arr_to_obj = new HashMap<>();

    ArrayList<Dir> m_cpp_path;
    ArrayList<Dir> m_cpp_path_unique;
    HashSet<Dir> m_cpp_path_set;
    HashMap<String, File> m_file_to_rfile = new HashMap<>();

    HashMap<File,Digest> m_file_to_result = new HashMap<>();
    HashMap<Dir, HashMap<Include, File>> by_dir = new HashMap<>();

    public CppPathMap(ArrayList<Dir> cpp_path) {
        m_cpp_path = cpp_path;
        m_cpp_path_unique = new ArrayList<>();
        m_cpp_path_set = new HashSet<>();
        HashSet<Dir> seen = new HashSet<>();
        for (Dir d : m_cpp_path) {
            if (!seen.contains(d)) {
                seen.add(d);
                m_cpp_path_unique.add(d);
                m_cpp_path_set.add(d);
            }
        }
    }

    static HashMap<String, Dir> dirByName = new HashMap<>();
    static HashMap<String, HashSet<Dir>> dirsByName = new HashMap<>();
    
    static void cacheFilesystemInfo() {
        // System.out.printf("cacheFilesystemInfo ............. size=%d%n",
        // s_arr_to_obj.size());
        HashSet<Dir> seenDir = new HashSet<>();
        for (CppPathMap pm : s_arr_to_obj.values()) {
            for (Dir d : pm.m_cpp_path_unique) {
                if (!seenDir.contains(d)) {
                    seenDir.add(d);
                    cacheDirInfo(d);
                } else {

                }
            }
        }
        // System.out.printf("#elements = %d%n", dirByName.size());
    }

    static void cacheDirInfo(Dir d) {
        //System.out.printf("d = %s%n", d.path());
        try (DirectoryStream<Path> stream = Files
                .newDirectoryStream(Paths.get(d.path()))) {
            for (Path file : stream) {
                String entry = file.getFileName().toString();
                if (dirByName.containsKey(entry)) {
                    HashSet<Dir> dirs = dirsByName.get(entry);
                    if (dirs == null) {
                        dirs = new HashSet<>();
                        dirs.add(dirByName.get(entry));
                        dirsByName.put(entry,  dirs);
                    }
                    dirs.add(d);
                } else {
                    dirByName.put(entry, d);
                }
            }
        } catch (IOException | DirectoryIteratorException x) {
            // leave empty HashSet
        }
    }

    ArrayList<File> find_node_includes(File node) {
        Dir d = node.parent();
        HashMap<Include, File> by_dir1 = by_dir.get(d);
        if (by_dir1 == null) {
            by_dir1 = new HashMap<>();
            by_dir.put(d, by_dir1);
        }
        ArrayList<File> incs = new ArrayList<>();
        if (Opts.debug)
            System.out.printf("*********** db_includes: %s%n", node.db_includes());
        for (Include include : node.db_includes()) {
            File inc = by_dir1.get(include);
            if (inc == File.null_sentinel)
                continue;
            if (include.quotes) {
                inc = d.lookup_or_fs_file(include.file);
            }
            if (inc == null) {
                inc = m_file_to_rfile.get(include.file);
                if (inc == null) {
                    if (!m_file_to_rfile.containsKey(include.file)) {
                        inc = find_include(include.file);
                        m_file_to_rfile.put(include.file, inc);
                    }
                }
            }
            if (inc != null && inc != node) {
                incs.add(inc);
            }
            if (inc == null) {
                by_dir1.put(include, File.null_sentinel);
            } else {
                by_dir1.put(include, inc);
            }
        }
        return incs;
    }

    File find_include(String file) {
        // calculate new value and cache it
        Dir dir = dirByName.get(file);
        if (dir != null) {
            // System.out.printf(">>>>>> dirByName: %s%n", file);
            HashSet<Dir> dirset = dirsByName.get(file);
            if (dirset == null && m_cpp_path_set.contains(dir)) {
                // System.out.printf(">>>>>> just in: %s%n", dir.path());
                return dir.lookup_or_fs_file(file);
            } else if (dirset != null) {
                // System.out.printf(">>>>>> several found: %s%n", dirset);
                File f = null;
                for (Dir d : m_cpp_path_unique) {
                    if (dirset.contains(d)) {
                        f = d.lookup_or_fs_file(file);
                        if (f != null) {
                            // System.out.printf(">>>>>> found: %s%n",
                            // f.path());
                            return f;
                        }
                    }
                }
                // System.out.printf(">>>>>> not found: %s%n", f);
                return f;
            }
        }
        // System.out.printf(">>>>>> fallback to normal handling of: %s%n",
        // file);
        File f = null;
        for (Dir d : m_cpp_path_unique) {
            f = d.lookup_or_fs_file(file);
            if (f != null) break;
        }
        return f;
    }


    public void put_result(File file, Digest digest) {
        m_file_to_result.put(file, digest);
    }

    public Digest get_result(File file) {
        Digest digest = m_file_to_result.get(file);
        if (digest == null) {
            System.out.printf("INTERNAL ERROR - node_calc_md5 %s%n", file.path());
            System.exit(1);
        }
        return digest;
    }

    Digest has_result(File file) {
        return m_file_to_result.get(file);
    }

    public static CppPathMap find(ArrayList<Dir> cpp_path) {
        StringBuilder idx_builder = new StringBuilder();
        for (Dir d : cpp_path) {
            idx_builder.append(d.path());
            idx_builder.append(" ");
        }
        String idx = idx_builder.toString();
        CppPathMap p = s_arr_to_obj.get(idx);
        if (p != null) {
            return p;
        }
        else {
            CppPathMap obj = new CppPathMap(cpp_path);
            s_arr_to_obj.put(idx, obj);
            return obj;
        }
    }


}
