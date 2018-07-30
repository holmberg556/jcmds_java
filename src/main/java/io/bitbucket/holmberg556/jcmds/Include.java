package io.bitbucket.holmberg556.jcmds;

class Include implements java.io.Serializable {
    boolean quotes;
    String file;

    static final long serialVersionUID = 1L;

    Include(boolean quotes, String filename) {
        this.quotes = quotes;
        this.file = filename;
    }

    @Override
    public String toString() {
        return "Include[quotes=" + quotes + ", file=" + file + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((file == null) ? 0 : file.hashCode());
        result = prime * result + (quotes ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Include other = (Include) obj;
        if (file == null) {
            if (other.file != null)
                return false;
        } else if (!file.equals(other.file))
            return false;
        if (quotes != other.quotes)
            return false;
        return true;
    }
    
}
