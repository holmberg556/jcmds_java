package io.bitbucket.holmberg556.jcmds;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

class Builder {

    interface CallNext {
        void callNext();
    }

    enum Color {
        WHITE,
        BLACK,
        GREY
    }

    static class IncludesTree {
        HashMap<File, ArrayList<File>> deps = new HashMap<>();
        HashMap<File, ArrayList<File>> inverted_deps = new HashMap<>();
        ArrayList<File> finished = new ArrayList<>();
        HashSet<File> visited = new HashSet<>();

        void gen_inverted_deps() {
            for (HashMap.Entry<File,ArrayList<File>> e : deps.entrySet()) {
                File k = e.getKey();
                for (File v : e.getValue()) {
                    ArrayList<File> v2 = inverted_deps.get(v);
                    if (v2 != null) {
                        v2.add(k);
                    } else {
                        v2 = new ArrayList<>();
                        v2.add(k);
                        inverted_deps.put(v, v2);
                    }
                }
            }
        }

        void calculate_finished() {
            for(HashMap.Entry<File,ArrayList<File>> e : deps.entrySet()) {
                File k = e.getKey();
                k.color = Color.WHITE;
                for (File v : e.getValue()) {
                    v.color = Color.WHITE;
                }
            }
            // XXX finished = new ArrayList<File>();
            for (HashMap.Entry<File, ArrayList<File>> e : deps.entrySet()) {
                File k = e.getKey();
                if (k.color == Color.WHITE) {
                    _calculate_finished_dfs(k);
                }
            }
        }

        void _calculate_finished_dfs(File f) {
            f.color = Color.GREY;
            ArrayList<File> f2s = deps.get(f);
            if (f2s != null) {
                for (File f2 : f2s) {
                    if (f2.color == Color.WHITE) {
                        _calculate_finished_dfs(f2);
                    }
                }
            }
            f.color = Color.BLACK;
            finished.add(f);
        }

        void sccs_prepare() {
            for (File f : finished) {
                f.color = Color.WHITE;
            }
        }

        void sccs_dfs(File f, ArrayList<File> group_elements) {
            f.color = Color.GREY;
            group_elements.add(f);
            ArrayList<File> f2s = inverted_deps.get(f);
            if (f2s != null) {
                for (File f2 : f2s) {
                    if (f2.color == Color.WHITE) {
                        sccs_dfs(f2, group_elements);
                    }
                }
            }
            f.color = Color.BLACK;
        }
    }

    public static ArrayList<BaseFun> mm_created_fun_arr = new ArrayList<>();
    public static BaseFun mm_current_fun;
    public static HashMap<BaseFun, Boolean> s_active_funs = new HashMap<>();


    /**********************************************************************
     * Base class for all state machine classes.
     */
    static abstract class BaseFun {
        boolean m_finish_after_cmd;
        BaseFun m_caller;
        boolean status_ok;
        CallNext next;
        BaseFun m_next_waiting_sem;
        int m_sem_count;

        BaseFun() {
            status_ok = true;

            // collect all created funs
            mm_created_fun_arr.add(this);

            // connect caller and callee
            m_caller = mm_current_fun;
            if (m_caller != null) {
                m_caller.sem_acquire();
            }
            s_active_funs.put(this, true);
        }

        void maybe_FUN_update_tgt(File node) {
            if (node.build_ok == File.Status.UNKNOWN) {
                new FUN_update_tgt(node);
            }
            else {
                if (node.build_ok == File.Status.ERROR) this.status_ok = false;
            }
        }

        void sem_set(int count) {
            m_sem_count = count;
        }

        void sem_acquire() {
            m_sem_count += 1;
        }

        void sem_release() {
            m_sem_count -= 1;
            assert(m_sem_count >= 0);
            if (m_sem_count == 0) {
                Engine.instance.set_runnable(this);
            }
        }

        void release_semaphores() {}
        void signalled_command(int exitstatus) {}

        boolean finished_p() {
            return next == null;
        }

        void call_next_method() {
            mm_current_fun = this;
            mm_created_fun_arr.clear(); // reset for this call TODO: is this efficient?
            while (m_sem_count == 1 && next != null) {
                next.callNext();
            }
            if (next == null) {
                s_active_funs.remove(this);
            }
        }

        abstract String name();
        
        void trace_method() {
            StackTraceElement[] stackTrace = new Throwable().getStackTrace();
            String method = stackTrace[1].getMethodName();
            String klass = stackTrace[1].getClassName();
            klass = klass.replaceAll(".*\\$", "");
            String name = this.name();
            System.out.printf("        >>> %s.%s: %s\n", klass, method, name);
        }
    }

    // ======================================================================
    // FUN_get_includes_tree
    //
    // Collect the include tree of a file.
    // While traversing the tree, "update" files that can be built.
    // Each node is processed in parallel, so files that take a long time
    // to generate, will not stall the collecting in other parts of the
    // include tree.
    //

    static class FUN_get_includes_tree extends BaseFun {
        int m_level;
        CppPathMap cpp_path_map;
        File node;
        IncludesTree m_includes_tree;

        public FUN_get_includes_tree(CppPathMap cpp_path_map, File node,
                IncludesTree includes_tree, int level) {
            this.m_level = level;
            this.cpp_path_map = cpp_path_map;
            this.node = node;
            this.m_includes_tree = includes_tree;
            if (Opts.trace) trace_method();
            next = this::STATE_start;
        }

        @Override
        public String toString() {
            return String.format("FUN_get_includes_tree[%s]", node.name);
        }

        // @Override
        String name() {
            return String.format("[%s]", node.name);
        }

        void STATE_start() {
            if (Opts.trace) trace_method();
            maybe_FUN_update_tgt(node);
            next = this::STATE_node_updated;
        }

        void STATE_node_updated() {
            if (Opts.trace) trace_method();
            if (! node.st_ok_p()) {
                // stop after failure. Caller will check for this.
                status_ok = false;
                next = null;
                return;
            }

            for (File inc : cpp_path_map.find_node_includes(node)) {
                ArrayList<File> incs = m_includes_tree.deps.get(node);
                if (incs == null) {
                    incs = new ArrayList<>();
                    incs.add(inc);
                    m_includes_tree.deps.put(node, incs);
                } else {
                    incs.add(inc);
                }
                if (!m_includes_tree.visited.contains(inc)) {
                    m_includes_tree.visited.add(inc);
                    if (cpp_path_map.has_result(inc) == null) {
                        new FUN_get_includes_tree(cpp_path_map, inc, m_includes_tree,
                                m_level+1);
                    }
                }
            }
            next = this::STATE_finish;
        }

        //----------------------------------------------------------------------

        void STATE_finish() {
            if (Opts.trace) trace_method();
            next = null;
        }
    }

    // ======================================================================
    // FUN_includes_md5
    //
    // Calculate MD5 of include tree

    static class FUN_includes_md5 extends BaseFun {
        CppPathMap cpp_path_map;
        File node;
        IncludesTree m_includes_tree;

        FUN_includes_md5(CppPathMap cpp_path_map, File node) {
            this.cpp_path_map = cpp_path_map;
            this.node = node;
            this.m_includes_tree = new IncludesTree();
            if (Opts.trace) trace_method();
            next = this::STATE_start;
        }

        // @Override
        String name() {
            return String.format("[%s]", node.name);
        }

        //----------------------------------------------------------------------

        void STATE_start() {
            if (Opts.trace) trace_method();
            // XXX m_includes_tree.visited = new HashSet<>();

            if (! m_includes_tree.visited.contains(node)) {
                m_includes_tree.visited.add(node);
                if (cpp_path_map.has_result(node) == null) {
                    new FUN_get_includes_tree(cpp_path_map, node, m_includes_tree, 0);
                }
            }
            next = this::STATE_finish;
        }

        //----------------------------------------------------------------------

        void STATE_finish() {
            if (Opts.trace) trace_method();
            if (! status_ok) {
                next = null;
                return;
            }

            m_includes_tree.calculate_finished();
            m_includes_tree.gen_inverted_deps();

            if (m_includes_tree.finished.size() == 0) {
                m_includes_tree.finished.add(node);
            }

            m_includes_tree.sccs_prepare();
            Collections.reverse(m_includes_tree.finished);

            // Find strongly connected components. The second part of the
            // algorithm
            // visiting the nodes in the order given by the "finishing times"
            // from
            // the previous part.

            ArrayList<File> group_elements = new ArrayList<>();
            ArrayList<Integer> group_offset1 = new ArrayList<>();
            ArrayList<Integer> group_offset2 = new ArrayList<>();
            for (File it : m_includes_tree.finished) {
                if (it.color == Color.WHITE) {
                    int x1 = group_elements.size();
                    group_offset1.add(x1);
                    m_includes_tree.sccs_dfs(it, group_elements);
                    int x2 = group_elements.size();
                    group_offset2.add(x2);
                }
            }

            // Calculate MD5 for each of the "nodes". The value depend on the
            // recursively included files, so the order of traversal of
            // group_offset1/group_offset2 is significant.

            for (int i = group_offset1.size() - 1; i>=0; i--) {
                int j1 = group_offset1.get(i);
                int j2 = group_offset2.get(i);
                if (j2 == j1 + 1) {
                    // *one* file without cycles
                    File node = group_elements.get(j1);

                    if (cpp_path_map.has_result(node) == null) {
                        // no cached value
                        cpp_path_map.put_result(node, node_calc_md5(node));
                    }
                }
                else {
                    // a cycle with more than one file
                    Digest digest = node_calc_md5_cycle(group_elements, j1, j2);
                    for (int j=j1; j<j2; j++) {
                        cpp_path_map.put_result(group_elements.get(j), digest);
                    }
                }
            }
            next = null;
        }

        //-----------------------------------

        Digest node_calc_md5(File node)
        {
            ArrayList<File> deps = m_includes_tree.deps.get(node);
            if (deps == null || deps.size() == 0) {
                // no dependencies ==> use content
                Digest digest = node.db_content_sig();
                return digest;
            }

            ArrayList<Digest> digests = new ArrayList<Digest>();
            digests.add(node.db_content_sig());
            for (File node2 : deps) {
                digests.add(cpp_path_map.get_result(node2));
            }
            Collections.sort(digests); // TODO: needed in D?
            Digest digest = Md5.calc_digest(digests);
            return digest;
        }

        //-----------------------------------

        Digest node_calc_md5_cycle(ArrayList<File> cycle, int i1, int i2) {
            // set temporarily to make rest of code easier
            Digest tmp_digest = Md5.digest("temporary-md5-signature");
            for (int i=i1; i<i2; i++) {
                cpp_path_map.put_result(cycle.get(i), tmp_digest);
            }

            // collect common digest for all files in cycle
            ArrayList<Digest> digests = new ArrayList<Digest>();
            for (int i=i1; i<i2; i++) {
                digests.add(Md5.digest(cycle.get(i).path()));
                digests.add(node_calc_md5(cycle.get(i)));
            }
            Collections.sort(digests);
            Digest digest = Md5.calc_digest(digests);
            return digest;
        }

    }

    public static final int SIGINT = 0;


    // ======================================================================
    // FUN_update_tgt

    static class FUN_update_tgt extends BaseFun {
        File tgt;
        ArrayList<File> info;

        FUN_update_tgt(File tgt) {
            this.tgt = tgt;
            if (Opts.trace) trace_method();
            next = this::STATE_start;
        }

        @Override
        String name() {
            return tgt.path();
        }

        @Override
        public String toString() {
            return "UPDATE_TGT:" + tgt.path();
        }

        //----------------------------------------------------------------------

        void STATE_start() {
            if (Opts.trace) trace_method();
            if (tgt.is_source()) {
                if (tgt.file_exist()) {
                    if (tgt.m_top_target) {
                        System.out.printf("jcons: already up-to-date: '%s' (source file)\n",
                                tgt.path());
                    }
                    tgt.st_source();
                    STATE_finish();
                }
                else {
                    report_error("don't know how to build", tgt.path());
                    tgt.st_propagate_error();
                    STATE_finish();
                }
            }
            else {
                // 'tgt' not source
                Cmd cmd = tgt.cmd;
                if (cmd.m_state == Cmd.State.RAW) {
                    cmd.m_state = Cmd.State.COOKING;
                    for (File src : cmd.srcs) {
                        maybe_FUN_update_tgt(src);
                    }
                    if (cmd.extra_deps != null) {
                        for (File extra_dep : cmd.extra_deps) {
                            maybe_FUN_update_tgt(extra_dep);
                        }
                    }
                    if (cmd.m_uses_libpath) {
                        for (File fs_lib : cmd.m_cons.libpath_map().fs_libs()) {
                            maybe_FUN_update_tgt(fs_lib);
                        }
                    }
                    next = this::STATE_srcs_updated;
                }
                else if (cmd.m_state == Cmd.State.COOKING) {
                    this.sem_acquire();

                    // add 'this' to 'waiting sem' list of tgt
                    this.m_next_waiting_sem = tgt.m_waiting_sem;
                    tgt.m_waiting_sem = this;

                    next = this::STATE_updated_by_other;
                }
                else if (cmd.m_state == Cmd.State.DONE) {
                    status_ok = tgt.st_ok_p();
                    next = null;
                }
            }
        }

        //----------------------------------------------------------------------

        void STATE_updated_by_other() {
            if (Opts.trace) trace_method();
            status_ok = tgt.st_ok_p();
            next = null;
        }

        //----------------------------------------------------------------------

        void STATE_srcs_updated() {
            if (Opts.trace) trace_method();
            Cmd cmd = tgt.cmd;
            if (! status_ok) {
                cmd.st_propagate_error();
                STATE_finish();
                return;
            }
            if (cmd.uses_cpp) {
                CppPathMap cpp_path_map = cmd.m_cons.cpp_path();
                for (File src : cmd.srcs) {
                    ArrayList<File> incs = cpp_path_map.find_node_includes(src);
                    for (File inc : incs) {
                        if (cpp_path_map.has_result(inc) == null) {
                            new FUN_includes_md5(cpp_path_map, inc);
                        }
                        if (info == null) info = new ArrayList<>();
                        info.add(inc);
                    }
                }
            }

            File f = cmd.find_program();
            if (f != null) {
                maybe_FUN_update_tgt(f);
                if (f.m_exe_deps != null) {
                    for (File dep : f.m_exe_deps) {
                        maybe_FUN_update_tgt(dep);
                    }
                }
            }
            next = this::STATE_maybe_execute_cmd;
        }

        //----------------------------------------------------------------------
        // TODO: rename to a better name

        static int command_count = 0;

        void STATE_maybe_execute_cmd() {
            if (Opts.trace) trace_method();
            Cmd cmd = tgt.cmd;

            if (! status_ok) {
                cmd.st_propagate_error();
                STATE_finish();
                return;
            }

            if (cmd.uses_cpp) {
                Md5 md5 = new Md5();
                CppPathMap cpp_path_map = cmd.m_cons.cpp_path();
                if (info != null) {
                    for (File inc : info) {
                        md5.append( cpp_path_map.get_result(inc) );
                    }
                }
                cmd.m_includes_digest = md5.digest();
            }

            int tgt_nr = 0;
            for (File tgt : cmd.tgts) {
                tgt_nr++;
                tgt.wanted_digest = node_new_dep_sig(tgt, tgt_nr);
                //System.out.printf("WANTED DIGEST(%s) = %s\n", tgt.path(), tgt.wanted_digest);
            }
            boolean any_need_update = false;
            for (File tgt : cmd.tgts) {
                if (need_update_p(tgt)) {
                    any_need_update = true;
                }
            }
            if (any_need_update) {
                for (File tgt : cmd.tgts) {
                    if (! Opts.accept_existing_target) {
                        // OK to fail removing file
                        new java.io.File(tgt.path()).delete();
                    }
                }
                for (File tgt : cmd.tgts) {
                    try {
                        Files.createDirectories(Paths.get(tgt.parent().path()));
                    } catch (java.nio.file.FileAlreadyExistsException e) {
                        report_error("expected directory", tgt.parent().path());
                        cmd.st_propagate_error();
                        STATE_finish();
                        return;

                    } catch (IOException e) {
                        e.printStackTrace();
                        System.exit(1);;
                    }
                    //catch (FileException) { // TODO
                    //    report_error("expected directory", tgt.parent().path());
                    //    cmd.st_propagate_error();
                    //    STATE_finish();
                    //    return;
                    //}
                }
                Cache build_cache = cmd.build_cache();
                if (build_cache != null) {
                    boolean all_ok = true;
                    for (File tgt : cmd.tgts) {
                        all_ok = all_ok && build_cache.get(tgt.wanted_digest, tgt);
                    }
                    if (all_ok) {
                        // TODO: do anything more ???
                        STATE_finish();
                        return;
                    }
                }

                boolean noop = (Opts.accept_existing_target && cmd.all_tgts_exist());
                String cmdline = cmd.get_cmdline2();

                if (Opts.quiet) {
                    command_count++;
                    System.out.printf("[%d] %s\n", command_count, cmd.get_targets_str());
                    System.out.flush();
                }
                else {
                    System.out.println(cmdline);
                    System.out.flush();
                }

                Engine.instance.execute_cmd(this, cmdline, noop, cmd);
                next = this::STATE_cmd_executed;
            }
            else {
                // ! tgts_need_update
                for (File tgt : cmd.tgts) {
                    tgt.st_propagate_ok();
                    Cache build_cache = cmd.build_cache();
                    if (build_cache != null && Opts.cache_force) {
                        build_cache.put(tgt.db_dep_sig(), tgt);
                    }
                    if (tgt.m_top_target) {
                        System.out.printf("jcons: already up-to-date: '%s'\n", tgt.path());
                    }
                }
                STATE_finish();
            }
        }

        //----------------------------------------------------------------------

        void STATE_cmd_executed() {
            if (Opts.trace) trace_method();
            Cmd cmd = tgt.cmd;
            if (cmd.m_exitstatus != 0) {
                report_error("error building", tgt.path());
                cmd.st_propagate_error();
                STATE_finish();
                return;
            }

            String missing_names = "";
            for (File tgt : cmd.tgts) {
                if (! tgt.file_exist_FORCED()) {
                    if (! missing_names.isEmpty()) missing_names = missing_names + " ";
                    missing_names = missing_names + tgt.path();
                }
            }
            if (! missing_names.isEmpty()) {
                report_error("tgts not created", missing_names);
                cmd.st_propagate_error();
                STATE_finish();
                return;
            }

            // update tgt sigs
            for (File tgt : cmd.tgts) {
                Digest dep_sig = tgt.wanted_digest;
                tgt.st_updated(dep_sig);
                Cache build_cache = cmd.build_cache();
                if (build_cache != null) {
                    build_cache.put(dep_sig, tgt);
                }
            }
            STATE_finish();
        }

        //----------------------------------------------------------------------

        void STATE_finish() {
            if (Opts.trace) trace_method();
            release_semaphores();
            status_ok = tgt.st_ok_p();
            next = null;
        }

        //----------------------------------------------------------------------

        @Override
        void signalled_command(int exitstatus) {
            if ((exitstatus & 127) == SIGINT) {
                System.out.printf("jcons: *** [%s] interrupted\n", tgt.path());
            }
            else {
                System.out.printf("jcons: *** [%s] interrupted by %d\n",
                        tgt.path(), exitstatus);
            }
        }

        //-----------------------------------
        // Release other FUN_update_tgt objects also looking at this node.

        @Override
        void release_semaphores() {
            Cmd cmd = tgt.cmd;
            if (cmd != null) {
                if (cmd.m_state == Cmd.State.COOKING) {
                    for (File tgt : tgt.cmd.tgts) {
                        BaseFun waiting_fun = tgt.m_waiting_sem;
                        while (waiting_fun != null) {
                            waiting_fun.sem_release();
                            waiting_fun = waiting_fun.m_next_waiting_sem;
                        }
                        tgt.m_waiting_sem = null;
                    }
                }
                cmd.m_state = Cmd.State.DONE;
            }
        }

        //-----------------------------------
        // An error that may halt 'jcons', unless the -k option was given.

        void report_error(String err, String arg) {
            System.out.printf("jcons: error: %s '%s'\n", err, arg);
            Engine.add_error();
        }

        //-----------------------------------
        // Tell if a target file needs to be updated.

        static boolean need_update_p(File tgt) {
            if (Opts.build_all) {
                return true;
            }
            if (! tgt.file_exist()) {
                return true;
            }
            if (Opts.debug)
                System.out.printf("                    need_update_p: %s <-> %s\n",
                        tgt.db_dep_sig(), tgt.wanted_digest);
            if (tgt.db_dep_sig().equals(tgt.wanted_digest)) {
                return false;
            }
            else {
                return true;
            }
        }

        //-----------------------------------
        // Helper method to 'node_new_dep_sig'.
        // Returns the part of the digest all targets of a command have in
        // common.

        static Digest cmd_new_dep_sig(Cmd cmd) {
            // TODO: only do this once (when there are several targets)
            // TODO: maybe move 'md5' or whole method to 'Cmd' class.
            Md5 md5 = new Md5();
            for (File src : cmd.srcs) {
                md5.append(src.db_content_sig());
                md5.append(src.path());
            }
            if (cmd.extra_deps != null) {
                ArrayList<Digest> digests = new ArrayList<Digest>();
                for (File dep : cmd.extra_deps) {
                    digests.add(dep.db_content_sig());
                }
                Collections.sort(digests);
                for (Digest digest : digests) {
                    md5.append(digest);
                }
            }
            if (cmd.uses_cpp) {
                md5.append(cmd.m_includes_digest);
            }

            // TODO: uncomment
            // if (cmd.m_uses_libpath) {
            // file_arr_t & fs_libs = cmd.m_cons.libpath_map().fs_libs();
            // foreach (auto & it : fs_libs) {
            // md5.append(it.db_content_sig());
            // }
            // }

            cmd.append_sig(md5);
            return md5.digest();
        }

        //-----------------------------------
        // The digest a target file should have to be considered up-to-date.

        static Digest node_new_dep_sig(File tgt, int tgt_nr) {
            Digest dep_sig = cmd_new_dep_sig(tgt.cmd);
            Md5 md5 = new Md5();
            md5.append(dep_sig);
            md5.append(tgt_nr);
            md5.append(tgt.name);
            return md5.digest();
        }

    }

    // ======================================================================
    // FUN_top_level

    static public class FUN_top_level extends BaseFun {
        ArrayList<File> tgts;

        FUN_top_level(ArrayList<File> tgts) {
            this.tgts = tgts;
            if (Opts.trace) trace_method();
            next = this::STATE_start;
        }

        @Override
        String name() {
            return String.format("%s", tgts);
        }

        @Override
        public String toString() {
            return String.format("FUN_top_level[%s]", tgts);
        }

        void STATE_start() {
            if (Opts.trace) trace_method();
            for (File tgt : tgts) {
                maybe_FUN_update_tgt(tgt);
            }
            next = this::STATE_finish;
        }

        void STATE_finish() {
            if (Opts.trace) trace_method();
            next = null;
        }
    }
}

//======================================================================
// END
//======================================================================
