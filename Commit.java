package gitlet;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import java.util.Arrays;
import java.util.HashSet;

/* This class works with the commit action,
 * everthing needed to fulfill this action. */

public class Commit implements Serializable  {

    private Map<String, Blob> blobs;
    private String[] blobIDs;
    private String[] filesRemoved;
    private Commit parent;
    private String parentID;
    private String message;
    private Date time;
    private String commitID;

    // constructor only works when creating new Commit.
    // to retrieve old commit, use Database query methods.

    // init with blobs (copied from staging)
    public Commit(Commit parent, String message, Map<String, Blob> blobs, String[] filesRemoved) {
        this(parent, message, blobs);
        this.filesRemoved = filesRemoved;
    }

    // init with blobs (copied from staging)
    public Commit(Commit parent, String message, Map<String, Blob> blobs) {
        this(parent, message);
        this.blobs = new HashMap<>(blobs);
        this.blobIDs = blobs.values().stream().map((b) -> b.getBlobID()).toArray(String[]::new);
    }

    // init with existing Commit object, or null
    public Commit(Commit parent, String message) {
        this.message = message;
        this.time = new Date();
        this.parent = parent;
        if (parent != null) {
            this.parentID = parent.commitID;
        } else {
            // first commit in tree
            this.parentID = "";
        }
        this.commitID = Utils.sha1(parentID, message,
                Long.toString(java.lang.System.currentTimeMillis()));
        this.blobs = new HashMap<>();
        this.blobIDs = new String[0];
        this.filesRemoved = new String[0];
    }

    public Blob findBlob(String filename) {
        return blobs.get(filename);
    }

    public boolean inBlobs(String filename) {
        return blobs.keySet().contains(filename);
    }

    public String[] getRemovedFileList() {
        return filesRemoved;
    }
    public void addToBlobs(Blob b) {
        blobs.put(b.getFilename(), b);
    }

    // set of getter methods, need them for private class
    public String getMessage() {
        return message;
    }

    // init parent object if not done already
    public Commit getParent() {
        if (parentID != null && parent == null) {
            parent = Database.connect().commits.get(parentID);
        }
        return parent;
    }

    public String getTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
        return sdf.format(time);
    }


    public String getCommitID() {
        return commitID;
    }

    public String getParentID() {
        return this.parentID;
    }

    public String[] getBlobIDs() {
        return blobIDs;
    }

    public Map<String, Blob> getBlobs() {
        return blobs;
    }

    public boolean tracksFile(String filename) {
        return !Arrays.asList(this.filesRemoved).contains(filename)
                && (this.blobs.containsKey(filename)
                || this.parent != null && this.parent.tracksFile(filename));
    }

    public Set getTrackedFilenames() {
        Set<String> fnames = new HashSet();
        Commit p = this.getParent();
        if (p != null) {
            fnames.addAll(p.getTrackedFilenames());
        }
        fnames.addAll(getBlobs().keySet());
        fnames.removeAll(Arrays.asList(getRemovedFileList()));
        return fnames;
    }

    public Blob getLatestBlob(String fileName) {
        if (blobs.containsKey(fileName)) {
            return blobs.get(fileName);
        } else if (getParent() != null) {
            return getParent().getLatestBlob(fileName);
        }
        return null;
    }

    public boolean equals(Object c) {
        return ((Commit) c).getCommitID().equals(getCommitID());
    }

    public Set<String> deletedComparedTo(Commit ancestor) {
        if (ancestor.equals(this)) {
            return new HashSet<>();
        }
        if (this.getParent() != null) {
            Set<String> result = getParent().deletedComparedTo(ancestor);
            result.addAll(Arrays.asList(filesRemoved));
            result.removeAll(this.blobs.keySet());
            return result;
        } else {
            return new HashSet<>();
        }
    }
    public Map<String, Blob> blobsComparedTo(Commit ancestor) {
        if (ancestor.equals(this)) {
            return new HashMap<>();
        }
        if (this.getParent() != null) {
            Map<String, Blob> res = getParent().blobsComparedTo(ancestor);
            Arrays.asList(filesRemoved).forEach(fname -> res.remove(fname));
            blobs.keySet().forEach(fname -> res.put(fname, blobs.get(fname)));
            return res;
        } else {
            return new HashMap<>();
        }
    }

}

