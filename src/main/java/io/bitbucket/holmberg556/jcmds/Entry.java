package io.bitbucket.holmberg556.jcmds;

public class Entry {

    String name;
    String cached_path;
    Dir raw_parent;
    boolean dirty;


    public Entry(String name, Dir parent) {
        this.name = name;
        this.cached_path = null;
        this.raw_parent = parent;
        this.dirty = false;
    }

    // Constructor for "root" directory
    public Entry(String name) {
        this.name = name;
        this.cached_path = name;
        this.raw_parent = null;
        this.dirty = false;
    }

    // Return the path of a file/directory.
    // The path may be absolute/relative depending on where in the filesystem
    // the file/directory is located.
    String path() {
        if (cached_path == null) {
            cached_path = raw_parent.path();
            if (cached_path.equals(".")) {
                cached_path = name;
            }
            else {
                if (! cached_path.endsWith("/")) {
                    cached_path += "/";
                }
                cached_path += name;
            }
        }
        return cached_path;
    }
    //
    // void print_tree(int level) {}
    //
    // // Return the absolute path of a file/directory.
    // String abspath() {
    //       if (raw_parent is null) {
    //           return name;
    //       }
    //       else {
    //           String path = raw_parent.abspath;
    //           if (path[$-1] != '/') {
    //               path ~= '/';
    //           }
    //           path ~= name;
    //           return path;
    //       }
    // }
    //
    // String path_prefix() {
    //       String parent_path = raw_parent.path;
    //       if (parent_path == ".") {
    //           return "";
    //       }
    //       else if (parent_path[$-1] == '/') {
    //           return parent_path;
    //       }
    //       else {
    //           return parent_path ~ "/";
    //       }
    // }


    // Set dirty flag and propagate upwards in tree.
    void set_dirty() {
        if (dirty) {
            return;
        }
        dirty = true;
        for (Dir dir = raw_parent; dir != null; dir = dir.raw_parent) {
            if (dir.dirty) break;
            dir.dirty = true;
        }
    }
}
