package gitlet;


/* Branch class that represent the Branch of the Gitlet system */

import java.io.Serializable;
import java.util.stream.Collectors;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.NoSuchElementException;

public class Branch implements Iterable<Commit>, Serializable {
    private String branchName;
    private Commit headCommit;
    private String commitID;

    public Branch(String name, Commit head) {
        branchName = name;
        headCommit = head;
        commitID = headCommit != null ? headCommit.getCommitID() : null;
        Database.add(this);
    }

    public String getCommitID() {
        return commitID;
    }

    // only used by database
    public void setHeadCommit(Commit c) {
        headCommit = c;
        commitID = c.getCommitID();
    }

    //creates new branch with name - points to current head node.
    //BEFORE EVER CALL BRANCH - CODE SHOULD BE RUNNING WITH A DEFAULT BRANCH CALLED MASTER.
    public Branch(Branch parent, String name) {
        branchName = name;
        headCommit = parent.getHeadCommit();
        commitID = headCommit != null ? headCommit.getCommitID() : null;
        Database.add(this);
    }

    public Commit getHeadCommit() {
        return headCommit;
    }

    public String getBranchName() {
        return branchName;
    }

    public boolean tracksFile(String filename) {
        return headCommit != null && headCommit.tracksFile(filename);
    }

    @Override
    public Iterator<Commit> iterator() {
        return new BranchIterator();
    }

    class BranchIterator implements java.util.Iterator<Commit> {
        Commit cur;

        BranchIterator() {
            cur = headCommit;
        }

        @Override
        public Commit next() {
            Commit res = cur;
            if (cur == null) {
                throw new NoSuchElementException();
            }
            cur = cur.getParent();
            return res;
        }

        @Override
        public boolean hasNext() {
            return cur != null;
        }
    }

    public Blob getFile(String fname) {
        Commit c = headCommit;
        while (c != null && !c.getBlobs().containsKey(fname)) {
            c = c.getParent();
        }
        return c == null ? null : c.getBlobs().get(fname);
    }

    public Set getTrackedFilenames() {
        return headCommit.getTrackedFilenames();
    }

    @Override
    public boolean equals(Object obj) {
        return ((Branch) obj).getBranchName().equals(branchName);
    }

    public static boolean contains(Branch b) {
        Database db = Database.connect();
        List l = db.branches.keySet().stream()
                .map(name -> db.branches.get(name))
                .collect(Collectors.toList());
        for (int i = 0; i < l.size(); i++) {
            if (b.branchName.equals(l.get(i))) {
                return true;
            }
        }
        return false;
    }

    public Blob getLatestBlob(String fileName) {
        if (headCommit != null) {
            return headCommit.getLatestBlob(fileName);
        }
        return null;
    }

    public Commit findCommonAncestor(Branch branch) {
        Set<Commit> ancestors = new HashSet<>();
        for (Commit c : this) {
            ancestors.add(c);
        }
        for (Commit c : branch) {
            if (ancestors.stream().anyMatch((an) -> an.equals(c))) {
                return c;
            }
        }
        return null;
    }

    public Set<String> deletedComparedTo(Commit ancestor) {
        return headCommit.deletedComparedTo(ancestor);
    }
    public Map<String, Blob> blobsComparedTo(Commit ancestor) {
        return headCommit.blobsComparedTo(ancestor);
    }
}
