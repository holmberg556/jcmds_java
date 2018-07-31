package io.bitbucket.holmberg556.jcmds;

public class UseArgParse {

    static boolean opt_verbose = false;
    static int opt_parallel = 1;
    static String opt_output = null;

    public static void main(String[] args) {
        ArgParse p = new ArgParse("foo");
        p.flag("v", "verbose", "be more verbose", (value) -> {
            opt_verbose = value;
        });
        p.integer("j", "parallel", "run in parallel", (value) -> {
            opt_parallel = value;
        });
        p.string("o", "output", "output file", (value) -> {
            opt_output = value;
        });
        p.parse(args);
        System.out.printf("verbose = %s%n", opt_verbose);
        System.out.printf("parallel = %d%n", opt_parallel);
        System.out.printf("output = %s%n", opt_output);
    }

}
