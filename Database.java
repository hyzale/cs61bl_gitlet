package gitlet;

import java.io.File;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Paths;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class Database {
    private static Database db = new Database();
    // public Commit head;
    public Branch currentBranch;
    // filename -> Blob
    public Map<String, Blob> stagedFiles;
    // filename -> BlobID
    public Map<String, String> stagedBlobIDs;
    public List<String> removedFiles;
    // blobID -> Blob
    public Map<String, Blob> blobs;
    // commitID -> Commit
    public Map<String, Commit> commits;
    // branchName -> Branch
    public Map<String, Branch> branches;
    File workingDirectory = new File(".gitlet");
    private Database() {
        commits = new HashMap<>();
        blobs = new HashMap<>();
        branches = new HashMap<>();
        stagedFiles = new HashMap<>();
        if (workingDirectory.exists()) {
            // head = (Commit) readObjectFromFile(".gitlet/head");
            currentBranch = (Branch) readObjectFromFile(".gitlet/currentBranch");
            stagedBlobIDs = (HashMap<String, String>) readObjectFromFile(".gitlet/stagedBlobIDs");
            if (stagedBlobIDs == null) {
                stagedBlobIDs = new HashMap<>();
            }
            removedFiles = (ArrayList<String>) readObjectFromFile(".gitlet/removedFiles");
            if (removedFiles == null) {
                removedFiles = new ArrayList<>();
            }
            bulkReadToMap(".gitlet/branches", branches);
            bulkReadToMap(".gitlet/commits", commits);
            bulkReadToMap(".gitlet/blobs", blobs);

            // link branch -> commit by id
            branches.forEach((bname, b) -> {
                if (b.getCommitID() != null) {
                    b.setHeadCommit(commits.get(b.getCommitID()));
                }
            });
            // link commit -> blob by id
            commits.forEach((cname, c) -> {
                Arrays.stream(c.getBlobIDs()).forEach(bid -> c.addToBlobs(blobs.get(bid)));
            });
            // link staged -> blob by id
            stagedBlobIDs.forEach((fname, bid) -> {
                stagedFiles.put(fname, blobs.get(bid));
            });
        }
    }

    public static Database connect() {
        return db;
    }

    // Do not call following if not in test environment.
    public static Database connectToTest() {
        return (new Database()).connect();
    }

    public static void resetForTest() {
        db = new Database();
    }

    public static void add(Object o) {
        if (o instanceof Commit) {
            db.commits.put(((Commit) o).getCommitID(), (Commit) o);
        }
        if (o instanceof Blob) {
            db.blobs.put(((Blob) o).getBlobID(), (Blob) o);
            db.stagedBlobIDs.put(((Blob) o).getFilename(), ((Blob) o).getBlobID());
        }
        if (o instanceof Branch) {
            db.branches.put(((Branch) o).getBranchName(), (Branch) o);
        }
    }

    /* The helper of writing object to file*/
    public static void writeObjectToFile(Object theThingToWrite,
                                         String fileName) throws IOException {
        File outFile = new File(fileName);
        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(outFile));
            out.writeObject(theThingToWrite);
            out.close();
        } catch (IOException excp) {
            throw excp;
        }
    }

    /* The helper of reading file, caller should cast*/
    public static Object readObjectFromFile(String fileName) {
        Object obj;
        File inFile = new File(fileName);
        try {
            ObjectInputStream inp = new ObjectInputStream(new FileInputStream(inFile));
            obj = inp.readObject();
            inp.close();
        } catch (IOException | ClassNotFoundException excp) {
            obj = null;
        }
        return obj;
    }

    private void bulkReadToMap(String directory, Map res) {
        Arrays.stream(new File(directory).list()).forEach((f) -> {
            res.put(f, readObjectFromFile(Paths.get(directory, f).toString()));
        });
    }

    public boolean isStaged(Blob b) {
        for (Blob ch : stagedFiles.values()) {
            if (ch.getBlobID().equals(b.getBlobID())) {
                return true;
            }
        }
        return false;
    }

    public void removeStaged(String filename) {
        stagedFiles.remove(filename);
    }

    private void bulkWriteFromMap(String directory, Map source) throws Exception {
        if (source != null) {
            for (Object s: source.keySet()) {
                Database.writeObjectToFile(source.get(s), directory + s);
            }
        }
    }

    public void saveOnClose() throws Exception {
        // Database.writeObjectToFile(head, ".gitlet/head");
        stagedBlobIDs = new HashMap<>();
        stagedFiles.keySet().stream().forEach((f) ->
                stagedBlobIDs.put(f, stagedFiles.get(f).getBlobID()));
        Database.writeObjectToFile(currentBranch, ".gitlet/currentBranch");
        Database.writeObjectToFile(stagedBlobIDs, ".gitlet/stagedBlobIDs");
        Database.writeObjectToFile(removedFiles, ".gitlet/removedFiles");
        bulkWriteFromMap(".gitlet/commits/", commits);
        bulkWriteFromMap(".gitlet/blobs/", blobs);
        branches.put(currentBranch.getBranchName(), currentBranch);
        bulkWriteFromMap(".gitlet/branches/", branches);
    }

    public void setBranch(Branch target) {
        currentBranch = target;
    }

    public Commit getHead() {
        return db.currentBranch.getHeadCommit();
    }
}
