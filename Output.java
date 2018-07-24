package gitlet;


/* The only place that deal with output (only place that have "System.out.print") */

import java.util.Arrays;
import java.util.List;

public class Output {
    //failure case for init
    public static void printGitExistError() {
        System.out.println("A gitlet version-control system "
                + "already exists in the current directory.");
    }

    //failure case for add & remove
    public static void fileDoesNotExistError() {
        System.out.println("File does not exist");
    }

    public static void noReasonToRemoveFile() {
        System.out.println("No reason to remove the file.");
    }

    //failure case for branch
    public static void printBranchExistError() {
        System.out.println("A branch with that name already exists.");
    }

    //Error message for main commands
    public static void noInput() {
        System.out.print("Please enter a command.");
    }

    public static void wrongCommand() {
        System.out.print("No command with that name exists.");
    }

    public static void wrongOprends() {
        System.out.print("Incorrect operands.");
    }

    public static void noInitGitFound() {
        System.out.print("Not in an initialized gitlet directory.");
    }

    //failure case for commit
    public static void noCommitMessage() {
        System.out.println("Please enter a commit message.");
    }

    public static void noChangetoCommit() {
        System.out.println("No changes added to the commit.");
    }

    //failure case for checkout and rest
    public static void noFileInCommit() {
        System.out.println("File does not exist in that commit.");
    }

    public static void noCommitExist() {
        System.out.println("No commit with that id exists.");
    }
    public static void foundNoCommit() {
        System.out.println("Found no commit with that message.");
    }
    public static void noBranchExist() {
        System.out.println("No such branch exists.");
    }

    public static void branchIsCurrentBranch() {
        System.out.println("No need to checkout current branch.");
    }

    public static void untrackedInCurrentBranch() {
        System.out.println("There is an untracked file in the way; delete it or add it first.");
    }


    //Output for merge
    public static void uncommitedChanges() {
        System.out.println("You have uncommitted changes.");
    }
    public static void mergeWithItself() {
        System.out.println("Cannot merge a branch with itself.");
    }
    public static void branchWithNameNotExist() {
        System.out.println("A branch with that name does not exist.");
    }
    public static void splitPointIsTargetBranch() {
        System.out.println("Current branch fast-forwarded.");
    }
    public static void splitPointIsCurrentBranch() {
        System.out.println("Given branch is an ancestor of the current branch.");
    }
    public static void mergeConflict() {
        System.out.println("Encountered a merge conflict.");
    }
    //output for log and global log
    public static void printCommit(Commit currCommit) {
        System.out.println("===");
        System.out.println("Commit " + currCommit.getCommitID());
        System.out.println(currCommit.getTime());
        System.out.println(currCommit.getMessage());
        System.out.println();
    }

    public static void printGitStatus(
            List branches,
            List stagedFilenames,
            List removedFilenames,
            String[] dirtyFilenames,
            String[] untrackedFilenames
    ) {
        System.out.println("=== Branches ===");
        branches.forEach(System.out::println);
        System.out.println();
        System.out.println("=== Staged Files ===");
        stagedFilenames.forEach(System.out::println);
        System.out.println();
        System.out.println("=== Removed Files ===");
        removedFilenames.forEach(System.out::println);
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        Arrays.stream(dirtyFilenames).forEach(System.out::println);
        System.out.println();
        System.out.println("=== Untracked Files ===");
        Arrays.stream(untrackedFilenames).forEach(System.out::println);
        System.out.println();
    }
}
