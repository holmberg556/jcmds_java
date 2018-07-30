package io.bitbucket.holmberg556.jcmds;

import java.util.ArrayList;
import java.util.Arrays;

class ArgParse {

    interface SetFlag {
        void Set(boolean value);
    }

    interface SetInt {
        void Set(int value);
    }

    interface SetStr {
        void Set(String value);
    }

    interface SetStrList {
        void Set(ArrayList<String> value);
    }

    abstract class OptBase {
        String shrt;
        String lng;
        String help;
        String meta;

        abstract boolean parse();

        @Override
        public String toString() {
            return "OptBase [shrt=" + shrt + ", lng=" + lng + ", help=" + help
                    + "]";
        }

        String getValue(String optstr, String delim) {
            if (argv[optind].equals(optstr)) {
                optind++;
                if (optind == argv.length) {
                    System.out.printf("ERROR: no option argument: %s\n",
                            argv[optind - 1]);
                    System.exit(1);
                }
                return argv[optind];
            }
            if (argv[optind].startsWith(optstr + delim)) {
                return argv[optind]
                        .substring(optstr.length() + delim.length());
            }
            return null;
        }

        String getValue1() {
            String value = null;
            if (lng != null) {
                value = getValue("--" + lng, "=");
            }
            if (shrt != null && value == null) {
                value = getValue("-" + shrt, "");
            }
            return value;
        }

        String usage_text() {
            String res = (shrt == null ? "--" + lng : "-" + shrt);
            res += _arg_extra();
            return "[" + res + "]";
    }

        String _arg_extra() {
            return (_with_arg() ? " " + _arg_name() : "");
        }

        String names() {
            String extra = (_with_arg() ? " " + _arg_name() : "");
            if (shrt == null) {
                return "--" + lng + extra;
            }
            else {
                return "-" + shrt + extra + ", --" + lng + extra;
            }
        }

//    void init() {}
        boolean _with_arg() { return false; }
        void _set(String value) {}

//    string show() {
//        if (sname.length != 0 && name.length != 0) {
//            return sname ~ "/" ~ name;
//        }
//        else {
//            return sname.length == 0 ? name : sname;
//        }
//    }

        String _arg_name() {
            if (meta != null) return meta;
            String res = lng;
            res = res.replaceFirst("^-*", "");
            res = res.replaceAll("-", "_");
            res = res.toUpperCase();
            return res;
        }
    }

    class OptFlag extends OptBase {
        SetFlag f;

        boolean parse() {
            if (lng != null && argv[optind].equals("--" + lng)) {
                f.Set(true);
                return true;
            }
            if (shrt != null && argv[optind].equals("-" + shrt)) {
                f.Set(true);
                return true;
            }
            return false;
        }
    }

    class OptInt extends OptBase {
        SetInt f;

        boolean _with_arg() { return true; }

        boolean parse() {
            String value = getValue1();
            if (value != null) {
                int i = Integer.parseInt(value);
                f.Set(i);
                return true;
            }
            return false;
        }
    }

    class OptStr extends OptBase {
        SetStr f;

        boolean _with_arg() { return true; }

        boolean parse() {
            String value = getValue1();
            if (value != null) {
                f.Set(value);
                return true;
            }
            return false;
        }

    }

    abstract class ArgBase {
        String name;
        String help;

        abstract boolean parse();
    }
    
    class ArgStrList extends ArgBase {
        SetStrList f;

        boolean parse() {
            ArrayList<String> res = new ArrayList<>();
            for (int i=optind; i<argv.length; i++) {
                res.add(argv[i]);
            }
            f.Set(res);
            return true;
        }
    }
    
    ArrayList<OptBase> opts = new ArrayList<>();
    ArrayList<ArgBase> arguments = new ArrayList<>();
    String prog;
    String[] argv;
    int optind = 0;

    ArgParse(String prog) {
        this.prog = prog;
    }

    void flag(String shrt, String lng, String help, SetFlag f) {
        OptFlag o = new OptFlag();
        o.shrt = shrt;
        o.lng = lng;
        o.help = help;
        o.f = f;
        opts.add(o);
    }

    void integer(String shrt, String lng, String help, SetInt f) {
        OptInt o = new OptInt();
        o.shrt = shrt;
        o.lng = lng;
        o.help = help;
        o.f = f;
        opts.add(o);
    }

    void integer(String shrt, String lng, String help, String meta, SetInt f) {
        OptInt o = new OptInt();
        o.shrt = shrt;
        o.lng = lng;
        o.help = help;
        o.meta = meta;
        o.f = f;
        opts.add(o);
    }

    void string(String shrt, String lng, String help, SetStr f) {
        OptStr o = new OptStr();
        o.shrt = shrt;
        o.lng = lng;
        o.help = help;
        o.f = f;
        opts.add(o);
    }

    void args(String name, String many, String help, SetStrList f) {
        ArgStrList o = new ArgStrList();
        o.name = name;
        o.help = help;
        o.f = f;
        arguments.add(o);
    }

    boolean findOpt() {
        for (OptBase opt : opts) {
            if (opt.parse())
                return true;
        }
        return false;
    }

    void parse(String[] argv_) {
        argv = argv_;
        for (optind = 0; optind < argv.length; optind++) {
            if (argv[optind].equals("--")) {
                optind++;
                break;
            }
            if (!argv[optind].startsWith("-")) {
                break;
            }
            boolean found = findOpt();
            if (!found) {
                System.out.printf("ERROR: unknown option: '%s'\n",
                        argv[optind]);
                System.exit(1);
            }
        }
        for (ArgBase arg : arguments) {
            arg.parse();
        }
    }

    ArrayList<String> args_OLD() {
        ArrayList<String> res = new ArrayList<>();
        for (int i = optind; i < argv.length; i++) {
            res.add(argv[i]);
        }
        return res;
    }

    void print_usage_synopsis() {
        System.out.printf("xxx = %s\n", Arrays.toString(argv));
        String leading = "usage: " + prog;
        String indent = leading.replaceAll(".", " ");

        System.out.print(leading);
        int off = leading.length();

        for (OptBase opt : opts) {
            String str = opt.usage_text();
            if (off + 1 + str.length() > 79) {
                System.out.println();
                System.out.print(indent);
                off = leading.length();
            }
            System.out.print(" " + str);
            off += 1 + str.length();
        }
        System.out.println();

        if (! arguments.isEmpty()) {
            for (ArgBase arg : arguments) {
                System.out.print(indent);
                System.out.print(" [" + arg.name + " [" + arg.name + " ... ]]");
                System.out.println();
            }
            System.out.println();

            System.out.println("positional arguments:");
            for (ArgBase arg : arguments) {
                System.out.printf("  %-13s %s\n", arg.name, arg.help);
            }
            System.out.println();
        }
    }

    void print_usage() {
        print_usage_synopsis();

//        if (mArgsName.length > 0) {
//            writeln();
//            writeln("positional arguments:");
//            writeln("  ", mArgsName, "           ", mArgsHelp);
//            writeln();
//        }

        System.out.printf("optional arguments:\n");
        System.out.printf("  -h, --help            show this help message and exit\n");
        for (OptBase opt : opts) {
            String str = "  " + opt.names();
            int wanted = 22;
            while (str.length() < wanted) {
                str += " ";
            }
            if (str.length() > wanted) {
                System.out.println(str);
                System.out.printf("%" + wanted + "s  %s\n", "  ", opt.help);
            }
            else {
                System.out.printf("%s  %s\n", str, opt.help);
            }
        }
    }

}
