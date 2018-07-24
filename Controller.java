package gitlet;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;



/* All commands inside this class. */

public class Controller {
    // all methods are psv (public, static, void) and throwable

    public static void status() throws Exception {
        try {
            Database db = Database.connect();
            List<String> branches = db.branches.keySet().stream()
                    .map(name -> (db.branches.get(name)
                            .equals(db.currentBranch) ? "*" : "") + name)
                    .collect(Collectors.toList());
            Collections.sort(branches);
            Output.printGitStatus(
                    branches,
                    new ArrayList(db.stagedFiles.keySet()),
                    db.removedFiles,
                    getDirtyFilesNamesWithStatus(),
                    getUntrackedFilesNames());
        } catch (NullPointerException e) {
            throw e;
        }
    }

    public static String[] getDirtyFilesNamesWithStatus() {
        File cwd = new File(".");
        Database db = Database.connect();
        Set<String> targets = new HashSet<>();
        targets.addAll(db.stagedFiles.keySet());
        targets.addAll(db.currentBranch.getTrackedFilenames());
        return targets.stream()
                .filter(fname ->
                        // 0. those who tracked || staged, and deleted
                        !(new File(fname).exists())
                                && (db.currentBranch.tracksFile(fname)
                                || db.stagedFiles.containsKey(fname))
                                // 1. those who tracked, and modified
                                || db.currentBranch.tracksFile(fname)
                                && !db.currentBranch.getFile(fname).equals(new Blob(fname))
                                // 2. those who staged, and modified
                                || db.stagedFiles.containsKey(fname)
                                && !db.stagedFiles.get(fname).equals(new Blob(fname)))
                .filter(fname -> (new File(fname).exists()) || !db.removedFiles.contains(fname))
                // 3. removal already staged
                .map(fname -> fname + " " + (new File(fname).exists() ? "(modified)" : "(deleted)"))
                .toArray(String[]::new);

    }

    public static String[] getUntrackedFilesNames() {
        File cwd = new File(".");
        Database db = Database.connect();
        return Arrays.stream(cwd.list())
                .filter(fname -> !new File(fname).isDirectory())
                // 1. ignore directory as per spec
                .filter((fname) -> !db.currentBranch.tracksFile(fname)
                        && !db.stagedFiles.containsKey(fname))
                // 2. if tracked by currentBranch
                .toArray(String[]::new);

    }

    public static void branch(String name) {
        try {
            Database db = Database.connect();
            if (db.branches.containsKey(name)) {
                throw new IllegalArgumentException();
            } else {
                Branch newBranch = new Branch(db.currentBranch, name);
                Database.add(newBranch);
            }
        } catch (IllegalArgumentException e) {
            Output.printBranchExistError();
        }
    }

    public static void add(String fileName) throws Exception {
        Database db = Database.connect();
        File file = new File(fileName);
        Blob newBlob = null;
        boolean deleted = !file.exists();
        if (!deleted) {
            newBlob = new Blob(fileName);
        }

        if (deleted && db.currentBranch.tracksFile(fileName)) {
            // 1. tracked and deleted
            db.removedFiles.add(fileName);
        } else if (deleted && db.stagedFiles.containsKey(fileName)) {
            // 2. staged and deleted
            db.stagedFiles.remove(fileName);
        } else if (deleted || file.isDirectory()) {
            // 3. deleted, ignore
            Output.fileDoesNotExistError();
        } else {
            if (db.currentBranch.tracksFile(fileName)
                    && newBlob.equals(db.currentBranch.getFile(fileName))) {
                // 4. tracked & identical
                db.removedFiles.removeIf((s) -> s.equals(fileName));
                return;
            }
            // 5. file exist, not tracked or tracked & different
            Database.add(newBlob);
            db.stagedFiles.put(fileName, newBlob);
            db.removedFiles.removeIf((s) -> s.equals(fileName));
        }
    }

    public static void rm(String fileName) throws Exception {
        //untrack the file - not to be tracked in next commit even if in current commit
        Database db = Database.connect();
        File file = new File(fileName);
        if (db.currentBranch.tracksFile(fileName)) {
            file.delete();
            // add(fileName);
            // ^ should work. why not?
            db.removedFiles.add(fileName);
        } else if (db.stagedFiles.containsKey(fileName)) {
            db.stagedFiles.remove(fileName);
        } else {
            Output.noReasonToRemoveFile();
        }


    }

    //skeleton code for init
    public static void init() throws Exception {
        try {
            File workingDirectory = new File(".gitlet");
            if (workingDirectory.exists()) {
                Output.printGitExistError();
                return;
            }

            workingDirectory.mkdir();
            Commit initialCommit = new Commit(null, "initial commit");
            Branch master = new Branch("master", initialCommit);

            Database.writeObjectToFile(null, ".gitlet/head");
            Database.writeObjectToFile(new HashMap<String, String>(), ".gitlet/stagedBlobIDs");
            Database.writeObjectToFile(new ArrayList<String>(), ".gitlet/removedFiles");

            new File(".gitlet/commits").mkdir();
            new File(".gitlet/blobs").mkdir();
            new File(".gitlet/branches").mkdir();

            Database.connect().currentBranch = master;
            Database.add(initialCommit);
            Database.add(master);
        } catch (NullPointerException e) {
            throw e;
        }
    }

    public static void commit(String msg) {
        Database db = Database.connect();

        if (db.stagedFiles.size() == 0 && db.removedFiles.size() == 0) {
            Output.noChangetoCommit();
        } else {
            Commit currCommit = new Commit(
                    db.currentBranch.getHeadCommit(),
                    msg,
                    new HashMap<>(db.stagedFiles),
                    db.removedFiles.stream().toArray(String[]::new));
            db.stagedFiles = new HashMap<>();
            db.stagedBlobIDs = new HashMap<>();
            db.removedFiles = new ArrayList<>();
            db.currentBranch.setHeadCommit(currCommit);
            Database.add(currCommit);
        }
    }

    public static void log() {
        //return the history: id + time + message for all contents!
        //take the map/collection of commits and traverse
        // through it and return those three variables for each commit
        Database db = Database.connect();
        Commit currCommit = db.currentBranch.getHeadCommit();
        while (currCommit != null) {
            Output.printCommit(currCommit);
            currCommit = currCommit.getParent();
        }
    }

    public static void globalLog() {
        //displays all commits ever made
        Database db = Database.connect();
        Collection<Commit> l = db.commits.values();
        Object[] c = l.toArray();
        for (int i = 0; i < c.length; i++) {
            Commit com = (Commit) c[i];
            Output.printCommit(com);
        }
    }

    //not restricted, also deals with commits not on current branch
    public static void find(String findmessage) {
        boolean p = false;
        Database db = Database.connect();
        Collection<Commit> l = db.commits.values();
        Object[] c = l.toArray();
        if (c == null) {
            Output.foundNoCommit();
        } else {
            for (int i = 0; i < c.length; i++) {
                Commit com = (Commit) c[i];
                if (com.getMessage().equals(findmessage)) {
                    System.out.println(com.getCommitID());
                    p = true;
                }
            }
            if (!p) {
                Output.foundNoCommit();
            }
        }
    }
//        l.stream()
//                .filter(e -> findmessage.equals(e.getMessage()))
//                .forEach(e -> System.out.println(e.getCommitID()));


    public static void checkoutToHead(String fileName) {
        //first form of checkout
        //takes head commit version of file aka front of branch
        //common failure cases
        Database db = Database.connect();
        if (!(db.currentBranch.tracksFile(fileName))) {
            Output.noFileInCommit();
            return;
        } else {
            Utils.writeContents(new File(fileName), db.currentBranch.getLatestBlob(fileName).data);
        }
    }

    public static void checkoutToCommit(String commitID, String fileName) {
        //second form of checkout
        Database db = Database.connect();
        String query = commitID;
        if (query.length() < 10) {
            List<String> result =
                    db.commits.keySet().stream()
                            .filter((cid) -> cid.startsWith(query))
                            .collect(Collectors.toList());
            if (result.size() > 0) {
                commitID = result.get(0);
            }
        }
        if (!db.commits.containsKey(commitID)) {
            Output.noCommitExist();
        } else if (!db.commits.get(commitID).tracksFile(fileName)) {
            Output.noFileInCommit();
        } else {
            Utils.writeContents(new File(fileName), db.commits
                    .get(commitID).getLatestBlob(fileName).data);
        }
    }

    public static void checkoutToBranch(String branchName) {
        Database db = Database.connect();
        if (!db.branches.containsKey(branchName)) {
            Output.noBranchExist();
        } else if (db.currentBranch.getBranchName().equals(branchName)) {
            Output.branchIsCurrentBranch();
        } else {
            Branch targetBranch = db.branches.get(branchName);
            Set<String> filesTrackedByTargetBranch = targetBranch.getTrackedFilenames();
            for (String fileName: filesTrackedByTargetBranch) {
                if (!db.currentBranch.tracksFile(fileName)
                        && new File(fileName).exists()
                        && !new Blob(fileName).equals(targetBranch.getLatestBlob(fileName))) {
                    Output.untrackedInCurrentBranch();
                    return;
                }
            }
            for (String fileName: filesTrackedByTargetBranch) {
                Blob fileContent = targetBranch.getLatestBlob(fileName);
                Utils.writeContents(new File(fileName), fileContent.data);
            }
            for (String fileName : new File(".").list()) {
                if (db.branches.values().stream().anyMatch(b -> !b.equals(targetBranch)
                        && b.tracksFile(fileName))
                        && !targetBranch.tracksFile(fileName)) {
                    new File(fileName).delete();
                }
            }
            db.currentBranch = targetBranch;
            db.stagedFiles.clear();
            db.stagedBlobIDs.clear();
        }
    }

    //deletes the pointers associated with the branch
    public static void rmBranch(String branchName) {
        Database db = Database.connect();
        if (!db.branches.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
        } else if (db.currentBranch.getBranchName().equals(branchName)) {
            throw new IllegalArgumentException("Cannot throw the current branch");
        }
        db.branches.remove(branchName);
    }

    public static void reset(String commitID) throws Exception {
        Database db = Database.connect();
        if (!db.commits.containsKey(commitID)) {
            Output.noCommitExist();
        } else {
            Commit targetCommit = db.commits.get(commitID);
            Set<String> fileList = targetCommit.getTrackedFilenames();
            for (String fileName : fileList) {
                if (!db.currentBranch.tracksFile(fileName)
                        && new File(fileName).exists()
                        && !new Blob(fileName).equals(targetCommit.getLatestBlob(fileName))) {
                    Output.untrackedInCurrentBranch();
                    return;
                }
            }
            for (String fileName : fileList) {
                Blob fileContent = targetCommit.getLatestBlob(fileName);
                File targetFile = new File(fileName);
                if (!targetFile.exists()) {
                    targetFile.createNewFile();
                }
                Utils.writeContents(new File(fileName), fileContent.data);
            }
            for (String fileName : new File(".").list()) {
                if (db.commits.values().stream()
                        .anyMatch(c -> !c.equals(targetCommit)
                                && c.tracksFile(fileName))
                        && !targetCommit.tracksFile(fileName)) {
                    new File(fileName).delete();
                }
            }
            db.currentBranch.setHeadCommit(targetCommit);
            db.stagedFiles.clear();
            db.stagedBlobIDs.clear();
        }
    }

    public static void merge(String targetBranchName) {
        Database db = Database.connect();
        if (!db.stagedFiles.isEmpty() || !db.removedFiles.isEmpty()) {
            Output.uncommitedChanges(); return;
        }
        if (getUntrackedFilesNames().length != 0) {
            Output.untrackedInCurrentBranch(); return;
        }
        Branch currentBranch = db.currentBranch;
        Branch targetBranch = db.branches.get(targetBranchName);
        if (targetBranch == null) {
            Output.branchWithNameNotExist(); return;
        } else if (currentBranch.equals(targetBranch)) {
            Output.mergeWithItself(); return;
        } else {
            Commit splitPoint = currentBranch.findCommonAncestor(targetBranch);
            if (splitPoint.equals(targetBranch.getHeadCommit())) {
                Output.splitPointIsCurrentBranch(); return;
            } else if (splitPoint.equals(currentBranch.getHeadCommit())) {
                Output.splitPointIsTargetBranch(); return;
            }
            Set<String> currDel = currentBranch.deletedComparedTo(splitPoint);
            Map<String, Blob> currBlob = currentBranch.blobsComparedTo(splitPoint);
            Set<String> tarDel = targetBranch.deletedComparedTo(splitPoint);
            Map<String, Blob> tarBlob = targetBranch.blobsComparedTo(splitPoint);
            List<String> fileToDelete = new ArrayList<String>();
            List<String> conflictFiles = new ArrayList<String>();
            Map<String, Blob> fileToWrite = new HashMap<>();
            for (String fName : tarBlob.keySet()) {
                if (!currBlob.containsKey(fName)) {
                    db.stagedFiles.put(fName, tarBlob.get(fName));
                    Utils.writeContents(new File(fName), tarBlob.get(fName).data);
                }
            }
            for (String fName : tarBlob.keySet()) {
                if (currDel.contains(fName)) {
                    fileToDelete.add(fName);
                }
            }
            for (String fName : currBlob.keySet()) {
                if (tarBlob.containsKey(fName)
                        && !tarBlob.get(fName).equals(currBlob.get(fName))
                        || currDel.contains(fName)) {
                    conflictFiles.add(fName);
                }
            }
            for (String fName: tarDel) {
                if (currBlob.containsKey(fName)) {
                    conflictFiles.add(fName);
                } else {
                    File f = new File(fName);
                    f.delete();
                    db.removedFiles.add(fName);
                }
            }
            for (String fName : fileToDelete) {
                File f = new File(fName); f.delete();
            }
            if (conflictFiles.size() > 0) {
                Output.mergeConflict();
                for (String faultyFile: conflictFiles) {
                    byte[] fileContent = "<<<<<<< HEAD\n".getBytes();
                    if (!currDel.contains(faultyFile)) {
                        fileContent = Utils.concat(fileContent,
                                currentBranch.getFile(faultyFile).data);
                    }
                    fileContent = Utils.concat(fileContent, "=======\n".getBytes());
                    if (!tarDel.contains(faultyFile)) {
                        fileContent = Utils.concat(fileContent,
                                targetBranch.getFile(faultyFile).data);
                    }
                    fileContent = Utils.concat(fileContent, ">>>>>>>\n".getBytes());
                    Utils.writeContents(new File(faultyFile), fileContent);
                } return;
            } else {
                commit("Merged " + currentBranch.getBranchName()
                        + " with " + targetBranch.getBranchName() + ".");
            }
        }
    }
}
