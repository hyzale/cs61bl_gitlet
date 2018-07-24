package gitlet;

import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class UnitTest {


    public static void tearDown() throws Exception {
        File cwd = new File(".gitlet");
        if (cwd.exists()) {
            Utils.delete(cwd);
        }
        Database.resetForTest();
    }

    public static void setup() throws Exception {
        tearDown();
        Main.main(new String[]{"init"});
        Database.resetForTest();

    }

    @Test
    public void testCommit() throws Exception {
        setup();
        File testFile = new File("hahahahhahha.txt");
        testFile.createNewFile();
        Main.main(new String[]{"add", "hahahahhahha.txt"});
        //Main.main(new String[]{"status"});
        Main.main(new String[]{"commit", "snjdjsk"});
        //Main.main(new String[]{"status"});
        Database testDb = Database.connectToTest();
        System.out.println(testDb.getHead().getCommitID());
        assert (testDb.getHead().tracksFile("hahahahhahha.txt"));
        testFile = new File("hahha.txt");
        testFile.createNewFile();
        assert (testDb.getHead().tracksFile("hahahahhahha.txt"));
        assertFalse(testDb.getHead().tracksFile("hahha.txt"));
        Main.main(new String[]{"add", "hahha.txt"});
        Main.main(new String[]{"commit", "snjdjsk"});

        testDb = Database.connectToTest();
        System.out.println(testDb.getHead().getParent().getCommitID());
        System.out.println(testDb.getHead().getCommitID());
        assert (testDb.getHead().tracksFile("hahahahhahha.txt"));
        assert (testDb.getHead().tracksFile("hahha.txt"));

        System.out.println(testDb.getHead().getTime());
        assertEquals(3, testDb.commits.size());

    }

    @Test
    public void testReset() throws Exception {
        setup();
        setup();
        File testFile = new File("haha.txt");
        FileWriter fileWriter = new FileWriter(testFile);
        testFile.createNewFile();

        fileWriter.write("This is asdasdasdasdasd!");
        fileWriter.flush();

        Main.main(new String[]{"add", "haha.txt"});
        Main.main(new String[]{"commit", "test checkout"});

        System.out.println("Committed blobID: " + new Blob("haha.txt").getBlobID());

        fileWriter.write("This is asdasdasdasdasd!");
        fileWriter.flush();
        fileWriter.close();

        System.out.println("Modified blobID: " + new Blob("haha.txt").getBlobID());
        Database testDb = Database.connectToTest();
        assertNotEquals(new Blob("haha.txt").getBlobID(),
                testDb.getHead().getLatestBlob("haha.txt").getBlobID());


        Main.main(new String[]{"reset", testDb.getHead().getCommitID()});

        System.out.println("Restored to blobID: " + new Blob("haha.txt").getBlobID());
        testDb = Database.connectToTest();
        assertEquals(new Blob("haha.txt").getBlobID(),
                testDb.getHead().getLatestBlob("haha.txt").getBlobID());
    }

    @Test
    public void testCommitNoMessage() throws Exception {
        setup();
        //Main.main(new String[]{"status"});
        Main.main(new String[]{"commit"});

    }

    @Test
    public void testLog() throws Exception {
        setup();
        testCommit();
        Main.main(new String[]{"log"});
    }

    @Test
    public void testGlobalLog() throws Exception {
        setup();
        testCommit();
        Main.main(new String[]{"global-log"});
    }

    @Test
    public void testCheckout() throws Exception {
        setup();
        File testFile = new File("haha.txt");
        FileWriter fileWriter = new FileWriter(testFile);
        testFile.createNewFile();

        fileWriter.write("This is asdasdasdasdasd!");
        fileWriter.flush();

        Main.main(new String[]{"add", "haha.txt"});
        Main.main(new String[]{"commit", "test checkout"});

        System.out.println("Committed blobID: " + new Blob("haha.txt").getBlobID());

        fileWriter.write("This is asdasdasdasdasd!");
        fileWriter.flush();
        fileWriter.close();

        System.out.println("Modified blobID: " + new Blob("haha.txt").getBlobID());
        Database testDb = Database.connectToTest();
        assertNotEquals(new Blob("haha.txt").getBlobID(),
                testDb.getHead().getLatestBlob("haha.txt").getBlobID());

        Main.main(new String[]{"checkout", "--", "haha.txt"});

        System.out.println("Restored to blobID: " + new Blob("haha.txt").getBlobID());
        testDb = Database.connectToTest();
        assertEquals(new Blob("haha.txt").getBlobID(),
                testDb.getHead().getLatestBlob("haha.txt").getBlobID());
    }

    @Test
    public void testFailingBranch() throws Exception {
        setup();
        Main.main(new String[]{"branch", "master"});
    }


    @Test
    public void testSuccessfulBranch() throws Exception {
        setup();
        Main.main(new String[]{"branch", "master2333"});
    }

    @Test
    public void testMergeCases() throws Exception {
        testSuccessfulBranch();

        Main.main(new String[]{"checkout", "master2333"});
        // on 23333
        File testFile = new File("haha.txt");
        FileWriter fileWriter = new FileWriter(testFile);
        testFile.createNewFile();

        fileWriter.write("This is asdasdasdasdasd!");
        fileWriter.flush();

        Main.main(new String[]{"add", "haha.txt"});
        Main.main(new String[]{"commit", "test merge"});


        Main.main(new String[]{"checkout", "master"});


        Main.main(new String[]{"checkout", "master2333"});
        Main.main(new String[]{"merge", "master"});
        // ff
        Main.main(new String[]{"checkout", "master"});
        Main.main(new String[]{"merge", "master"});
        // same
        Main.main(new String[]{"merge", "master2333"});
        // not ff
    }
    @Test
    public void testFailingInit() throws Exception {
        File cwd = new File(".gitlet");
        if (!cwd.exists()) {
            cwd.mkdir();
        }
        Main.main(new String[]{"init"});
    }

    @Test
    public void testInit() throws Exception {
        tearDown();
        // run gitlet init
        Main.main(new String[]{"init"});

        // gitlet dead after init.

        // statically check if files are created alright:
        assertEquals(1, new File(".gitlet/branches").list().length);
        assertEquals(1, new File(".gitlet/commits").list().length);
        assertEquals(0, new File(".gitlet/blobs").list().length);

        // gitlet restarted, db reloaded
        Database testDb = Database.connectToTest();
        // check currentBranch
        assertEquals(testDb.currentBranch.getBranchName(), "master");
        // check branches
        assertEquals(testDb.branches.keySet().size(), 1);
        assert (testDb.branches.containsKey("master"));
        assert (testDb.branches.get("master").getHeadCommit() != null);
        // check if commit tree is empty
        assertEquals(1, testDb.commits.keySet().size());
    }

    @Test
    public void addFile() throws Exception {
        setup();
        Main.main(new String[]{"add", "README.md"});

        Database testDb = Database.connectToTest();
        assertEquals(true, testDb.stagedFiles.containsKey("README.md"));
        assertEquals(new Blob("README.md").getBlobID(),
                testDb.stagedFiles.get("README.md").getBlobID());
    }

    @Test
    public void newCommitWithBlobs() throws Exception {
        Commit a = new Commit((Commit) null, "bbb");
        // java.lang.Thread.sleep(500);
        Commit b = new Commit(a, "bbb");
        Map<String, Blob> mapp = new HashMap<>();
        Blob bb = new Blob("gitlet/Main.java");
        mapp.put("aaa", bb);
        Commit c = new Commit(b, "bbb", mapp);
        assertNotEquals(a.getCommitID(), b.getCommitID());
        assertEquals(c.getBlobIDs()[0], bb.getBlobID());
    }

    @Test
    public void newBlob() throws Exception {
        Blob bb = new Blob("./gitlet/UnitTest.java");
        assert (bb.data.length > 0);
        System.out.println(bb.getBlobID());

    }

    @Test
    public void getTime() {
    }

    @Test
    public void getParentID() {
    }

    @Test
    public void rmBranchNotExist() throws Exception {
        setup();
        Main.main(new String[]{"rm-branch", "ahjh"});

    }


    @Test
    public void newCommitInMain() throws Exception {
        setup();
        Main.main(new String[]{"commit", "ahjh"});
    }

    @Test
    public void getStatus() throws Exception {
        setup();
        Main.main(new String[]{"branch", "spell-check-my-ami"});
        Main.main(new String[]{"branch", "qqq"});
        Main.main(new String[]{"branch", "transfer"});
        Main.main(new String[]{"status"});
    }

    @Test
    public void blankStatus() throws Exception {
        setup();
        Main.main(new String[]{"status"});
    }

    @Test
    public void tryToAdd() throws Exception {
        setup();
        Database testDb = Database.connectToTest();
        assertEquals(0, testDb.stagedFiles.size());
        Main.main(new String[]{"add", "README.md"});
        testDb = Database.connectToTest();
        assertEquals(1, testDb.stagedFiles.size());
        assertEquals(testDb.stagedFiles.get("README.md"), new Blob("README.md"));


        System.out.println(testDb.stagedFiles.get("README.md").getBlobID());
        System.out.println(new Blob("README.md").getBlobID());
    }

    @Test
    public void addingRemovalOfStaged() throws Exception {
        setup();
        Database testDb = Database.connectToTest();
        assertEquals(0, testDb.stagedFiles.size());
        File testFile = new File("hahahahhahha.txt");
        testFile.createNewFile();

        Main.main(new String[]{"add", "hahahahhahha.txt"});
        testFile.delete();

        Main.main(new String[]{"status"});

        Main.main(new String[]{"add", "hahahahhahha.txt"});
        Main.main(new String[]{"status"});

        /*
        testDb = Database.connectToTest();
        assertEquals(1, testDb.stagedFiles.size());
        assertEquals(testDb.stagedFiles.get("hahahahhahha.txt"), new Blob("hahahahhahha.txt"));


        System.out.println(testDb.stagedFiles.get("hahahahhahha.txt").getBlobID());
        System.out.println(new Blob("hahahahhahha.txt").getBlobID());*/
    }


    @Test
    public void addingRemovalOfCommited() throws Exception {
        // todo
    }
}
