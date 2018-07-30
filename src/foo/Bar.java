package foo;

import java.util.ArrayList;
import java.util.Collections;

public class Bar {

    public interface CallNext {
        void call();
    }

    static class Abase {
        public CallNext next;
    }

    static class A extends Abase {
        String name;

        A(String name) {
            System.out.printf("creating an A(%s)\n", name);
            this.name = name;
            next = this::xxx;
        }

        void xxx() {
            System.out.printf("a.xxx called for %s\n", this.name);
            next = this::yyy;
        }

        void yyy() {
            System.out.printf("a.yyy called for %s\n", this.name);

            // next = this::zzz;
            next = () -> zzz();
        }

        void zzz() {
            System.out.printf("a.zzz called for %s\n", this.name);
            next = null;
        }

    }

    public static void main(String[] args) {
        System.out.println("### Testing method references ...");

        java.io.File f = new java.io.File(args[1]);
        System.out.println(f.isFile());

        A a1 = new A("first");
        A a2 = new A("second");

        while (a1.next != null) {
            a1.next.call();
        }

        while (a2.next != null) {
            a2.next.call();
        }

        ArrayList<A> arr = new ArrayList<A>();
        arr.add(a1);
        arr.add(a2);
        Collections.sort(arr, (x, y) -> 0);
    }

}
