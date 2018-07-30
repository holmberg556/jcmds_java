package io.bitbucket.holmberg556.jcmds;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

class Command {
    
    static class Result {
        int pid;
        int status;

        Result(int pid, int status) {
            this.pid = pid;
            this.status = status;
        }
    }

    static class Input {
        int pid;
        String command;

        Input(int pid, String command) {
            this.pid = pid;
            this.command = command;
        }
    }

    private ArrayBlockingQueue<Input> qin;
    private ArrayBlockingQueue<Result> qout;
    private ExecutorService exec;
    private ArrayList<MyRunnable> runnables;

    static class MyRunnable implements Runnable {
        ArrayBlockingQueue<Input> q;
        ArrayBlockingQueue<Result> qout;
        int i;

        MyRunnable(int i, ArrayBlockingQueue<Input> q, ArrayBlockingQueue<Result> qout) {
            this.i = i;
            this.q = q;
            this.qout = qout;
        }
        public void run() {
            for (;;) {
                Input input = null;
                try {
                    input = q.take();
                    if (input.command.isEmpty()) {
                        return;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
                int status = run_command(input);
                if (Opts.debug)
                    System.out.printf("        command in [%d]: %s ---> %d\n",
                        i, input.command, status);
                try {
                    qout.put(new Result(input.pid, status));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        }
        private static int run_command(Input input) {
            ProcessBuilder pb = new ProcessBuilder("sh", "-c", input.command);
            pb.inheritIO();
            Process p = null;
            int status = 1;
            try {
                p = pb.start();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                System.exit(1);
            }
            try {
                status = p.waitFor();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                System.exit(1);
            }
            return status;
        }
    }

    public Command(int n) {
        this.qin = new ArrayBlockingQueue<>(n);
        this.qout = new ArrayBlockingQueue<>(n);
        this.exec = Executors.newFixedThreadPool(n);

        this.runnables = new ArrayList<>();
        for (int i=0; i<n; i++) {
            MyRunnable r = new MyRunnable(i, qin, qout);
            exec.submit(r);
            runnables.add(r);
        }
    }

    int latestPid = 100;

    public int start(String command, boolean noop) {
        latestPid += 1;
        try {
            this.qin.put(new Input(latestPid, command));
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return latestPid;
    }

    public Result waitForOne(boolean blocking) {
        Result res = null;
        if (blocking) {
            try {
                res = this.qout.take();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                System.exit(1);
            }
        }
        else {
            res = this.qout.poll();
        }
        return res;
    }

    public void finish() {
        for (int i=0; i<runnables.size(); i++) {
            try {
                qin.put(new Input(0, ""));
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
        exec.shutdown();
        try {
            exec.awaitTermination(3650, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        int nmax = 5;
        Command mycommands = new Command(nmax);
        int nrunning = 0;
        boolean progress = true;
        try(Scanner scanner = new Scanner(System.in)) {
            while ( nrunning > 0 || scanner.hasNextLine()) {
                System.out.printf("### nrunning=%d, hasNextLine=%s\n",
                        nrunning, scanner.hasNextLine());
                if (nrunning > 0) {
                    boolean blocking =
                        (! progress || nrunning >= nmax);
                    Result res = mycommands.waitForOne(blocking);
                    if (res != null && res.pid != 0) {
                        System.out.printf("process finished: %d, status: %d\n",
                                res.pid, res.status);
                        nrunning -= 1;
                    }
                }
                progress = false;
                if (scanner.hasNextLine()) {
                    progress = true;
                    String command = scanner.nextLine();
                    mycommands.start(command, false);
                    nrunning += 1;
                }
            }
        }
        mycommands.finish();
        System.out.println("no more input ...\n");
    }
}
