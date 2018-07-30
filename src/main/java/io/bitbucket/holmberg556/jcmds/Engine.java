package io.bitbucket.holmberg556.jcmds;

import java.util.ArrayList;
import java.util.HashMap;

import io.bitbucket.holmberg556.jcmds.Builder.BaseFun;
import io.bitbucket.holmberg556.jcmds.Builder.FUN_update_tgt;

public class Engine {

    static int g_nerrors = 0;

    public static void add_error() {
        g_nerrors += 1;
    }

    public void set_runnable(BaseFun fun) {
        this.m_ready.push(fun);
    }

    static class ExecuteContext {
        BaseFun fun;
        boolean noop;
        Cmd cmd; // for 'set_exitstsatus'
    }

    static class QueueRun {
        ArrayList<BaseFun> arr = new ArrayList<BaseFun>();
        int n = 0;

        boolean empty() {
            return n == 0;
        }

        BaseFun pop() {
            n -= 1;
            BaseFun res = arr.get(n);
            arr.set(n, null);
            return res;
        }

        void push(BaseFun f) {
            if (n < arr.size()) {
                arr.set(n, f);
            }
            else {
                arr.add(f);
            }
            n += 1;
        }

        public void show() {
            for (int i=0; i<n; i++) {
                System.out.printf("ready[%d]: %s\n", i, arr.get(i));
            }

        }
    }

    static boolean got_sigint;
    public static Engine instance;

    QueueRun m_ready = new QueueRun();
    HashMap<Integer, ExecuteContext> m_executing = new HashMap<>();
    private Command mycommands;

    Engine(ArrayList<File> tgts) {
        instance = this;
        m_ready.push(new Builder.FUN_top_level(tgts));
    }

    boolean should_terminate_p() {
        return got_sigint || g_nerrors > 0 && !Opts.keep_going;
    }

    void fun_call_next(BaseFun fun) {
        fun.sem_set(1);
        fun.call_next_method();
        if (fun.finished_p()) {
            return_to_caller(fun);
        }
        else {
            _queue_new_funs();
            fun.sem_release();
        }
    }

    void return_to_caller(BaseFun f) {
        if (f.m_caller != null) {
            if (! f.status_ok) f.m_caller.status_ok = false;
            f.m_caller.sem_release();
            f.m_caller = null;
        }
    }

    void _queue_new_funs() {
        int n = Builder.mm_created_fun_arr.size();
        for (int i=n-1; i>=0; i--) {
            m_ready.push(Builder.mm_created_fun_arr.get(i));
        }
    }

    void Run() {
        CppPathMap.cacheFilesystemInfo();
        this.mycommands = new Command(Opts.parallel);
        boolean terminate_warning_given = false;
        boolean progress = true;
        while (m_executing.size() > 0 || ! m_ready.empty()) {
            if (Opts.debug) {
                System.out.println("LOOP---------------------------------");
                m_ready.show();
            }
            if (m_executing.size() > 0) {
                if (should_terminate_p() && ! terminate_warning_given) {
                    terminate_warning_given = true;
                    System.out.printf("jcons: *** waiting for commands to finish ...\n");
                }
                boolean blocking =
                    (! progress || m_executing.size() >= Opts.parallel);
                Command.Result res = mycommands.waitForOne(blocking);
                if (res != null && res.pid != 0) {
                    ExecuteContext context = m_executing.get(res.pid);
                    if (context == null) {
                        throw new RuntimeException("internal error: unknown pid");
                    }
                    context.cmd.set_exitstatus(res.status);
                    BaseFun sem_fun = context.fun;
                    m_executing.remove(res.pid);
                    sem_fun.sem_release();
                    if (should_terminate_p()) {
                        sem_fun.m_finish_after_cmd = true;
                    }
                    if ((res.status & 127) != 0) {
                        // TODO: when to run this code ... ???
                        // sem_fun.signalled_command(res.status);
                    }
                }
                // always fall through
            }
            progress = false;
            if (! m_ready.empty()) {
                progress = true;
                BaseFun fun = m_ready.pop();
                if (should_terminate_p() && ! fun.m_finish_after_cmd) {
                    // unwind call stack
                    fun.release_semaphores();
                    return_to_caller(fun);
                }
                else {
                    // normal case
                    fun_call_next(fun);
                }
            }
        }
        mycommands.finish();
    }

    public void execute_cmd(FUN_update_tgt fun, String cmdline, boolean noop, Cmd cmd) {
        //System.out.printf("EXECUTE: %s\n", cmdline);
        int pid = this.mycommands.start(cmdline, noop);
        ExecuteContext context = new ExecuteContext();
        context.fun = fun;
        context.cmd = cmd;
        m_executing.put(pid, context);
        fun.sem_acquire();
    }

}
