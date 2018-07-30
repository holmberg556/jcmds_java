package io.bitbucket.holmberg556.jcmds;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FileTest {

    @Before
    public void setUp() throws Exception {
        Dir.initialize();
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testBasic() {
        assertTrue(true); // xxx
        File f;
        f = Dir.s_curr_dir.find_file("alpha.c");
        assertNotEquals(null, f);
        assertEquals("alpha.c", f.path());

        f = Dir.s_curr_dir.find_file("subdir/beta.c");
        assertNotEquals(null, f);
        assertEquals("subdir/beta.c", f.path());

        File f2 = Dir.s_curr_dir.find_file("subdir/beta.c");
        assertSame(f, f2);

        assertSame(f.raw_parent, f2.raw_parent);

        Dir d_subdir = Dir.s_curr_dir.find_dir("subdir");
        File f3 = d_subdir.find_file("beta.c");
        assertSame(f, f3);

    }

    @Test
    public void testLookup1() {
        // together with testLookup2 check that each test has a "clean" environment
        File f;
        f = Dir.s_curr_dir.lookup_or_fs_file("xxx.c");
        assertEquals(null, f);
        f = Dir.s_curr_dir.find_file("yyy.c");
        assertNotEquals(null, f);
    }

    @Test
    public void testLookup2() {
        File f;
        f = Dir.s_curr_dir.lookup_or_fs_file("yyy.c");
        assertEquals(null, f);
        f = Dir.s_curr_dir.find_file("xxx.c");
        assertNotEquals(null, f);
    }

    @Test
    public void testLookup() {
        assertTrue(true); // xxx
        File f;
        
        f = Dir.s_curr_dir.find_file("existing1.c");
        f = Dir.s_curr_dir.find_file("subdir1/existing2.c");

        f = Dir.s_curr_dir.lookup_or_fs_file("nonexisting.c");
        assertEquals(null, f);
        f = Dir.s_curr_dir.lookup_or_fs_file("existing1.c");
        assertNotEquals(null, f);

        try {
            f = Dir.s_curr_dir.lookup_or_fs_file("subdir1");
            fail("exception not thrown");
        }
        catch (RuntimeException exc) {
            assertEquals("not a file", exc.getMessage());
        }

        f = Dir.s_curr_dir.lookup_or_fs_file("subdir1/nonexisting.c");
        assertEquals(null, f);
        f = Dir.s_curr_dir.lookup_or_fs_file("subdir1/existing2.c");
        assertNotEquals(null, f);

        try {
            Dir.s_curr_dir.lookup_or_fs_dir("existing1.c");
            fail("exception not thrown");
        }
        catch (RuntimeException exc) {
            assertEquals("not a dir", exc.getMessage());
        }
    }

    @Test
    public void testDirAbove() {
        Dir d;
        d = Dir.s_curr_dir.find_dir(".");
        assertEquals(".", d.path());

        d = Dir.s_curr_dir.find_dir("..");
        String cwd = Dir.fs_getcwd();
        String cwd_up = (new java.io.File(cwd)).getParent();
        assertEquals(cwd_up, d.path());
    }

    @Test
    public void testMd5() {
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", Md5.digest("").hex());
        assertEquals("5d41402abc4b2a76b9719d911017c592", Md5.digest("hello").hex());

        Md5 m = new Md5();
        m.append(1234567890);
        assertEquals("e807f1fcf82d132f9bb018ca6738a19f", m.digest().hex());

        m = new Md5();
        m.append(1234567890);
        m.append("hello");
        Digest d = Md5.digest("");
        m.append(d);
        assertEquals("2494651ea359898de1ae46a4cc4d5e2d", m.digest().hex());
    }

    @Test
    public void testInclude() {
        Include stdio1 = new Include(false, "stdio.h");
        Include stdio2 = new Include(false, "stdio.h");
        Include not_stdio = new Include(false, "not_stdio.h");
        Include quote_stdio = new Include(true, "not_stdio.h");

        assertEquals(true, stdio1.equals(stdio2));
        assertEquals(true, stdio2.equals(stdio1));

        assertEquals(false, stdio1.equals(not_stdio));
        assertEquals(false, not_stdio.equals(stdio1));

        assertEquals(false, stdio1.equals(quote_stdio));
        assertEquals(false, quote_stdio.equals(stdio1));

    }

    @Test
    public void testDummy1() {
        assertEquals(11, 11);
        assertEquals(22, 22);
        assertEquals(33, 33);
    }

    @Test
    public void testDummy2() {
        assertEquals(11, 11);
        assertEquals(22, 22);
        assertEquals(33, 33);
    }
    @Test
    public void testDummy3() {
        assertEquals(11, 11);
        assertEquals(22, 22);
        assertEquals(33, 33);
    }

}
