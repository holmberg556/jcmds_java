package io.bitbucket.holmberg556.jcmds;

import java.util.ArrayList;

public class MainSimple {

    static ArrayList<String> targets = new ArrayList<>();
    
    public static void main(String[] args) {

        ArgParse p = new ArgParse("simpleMain");
        p.flag(null, "debug", "write debug output", (value) -> {
            Opts.debug = value;
        });
        p.flag("v", "verbose", "be more verbose", (value) -> {
            Opts.verbose = value;
        });
        p.flag(null, "trace", "write trace output", (value) -> {
            Opts.trace = value;
        });
        p.integer("j", "parallel", "run in parallel", (value) -> {
            Opts.parallel = value;
        });
        p.args("targets", "*", "targets to build", (value) -> {
            targets = value;
        });
        p.parse(args);

        if (Opts.debug) {
            System.out.printf("This is jcons ... %s\n", targets);
        }

        Cons cons = new Cons();
        cons.Command("ccc", "bbb", "cp bbb ccc", false);
        cons.Command("bbb", "aaa", "cp aaa bbb", false);

        cons.Command("prog.o", "prog.c", "gcc -c prog.c", true);

        ArrayList<File> tgts = new ArrayList<>();
        ArrayList<Entry> entries = new ArrayList<>();
        Dir.lookup_targets(targets, entries, tgts);

        Engine engine = new Engine(tgts);
        engine.Run();

        for (Entry entry : entries) {
            if (entry instanceof Dir) {
                Dir d = (Dir) entry;
                if (! d.dirty) {
                    System.out.printf("jcons: up-to-date: %s\n", d.path());
                }
            }
        }

        Dir.terminate();
        if (Opts.debug) System.out.printf("Exiting jcons ...\n");
    }

}
