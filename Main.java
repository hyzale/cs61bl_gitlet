package gitlet;

/* Driver class for Gitlet, the tiny stupid version-control system.
   @author
*/
public class Main {
    /* Usage: java gitlet.Main ARGS, where ARGS contains
       <COMMAND> <OPERAND> .... */

    private static void checkOperands(String[] args, int count) {
        if (args.length != count) {
            Output.wrongOprends();
            System.exit(0);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            Output.noInput();
            return;
        }
        Database db = Database.connect();
        switch (args[0]) {
            case "init":
                checkOperands(args, 1);
                Controller.init();
                break;
            case "branch":
                checkOperands(args, 2);
                Controller.branch(args[1]);
                break;
            case "commit":
                if (args.length == 1 || args[1].isEmpty()) {
                    Output.noCommitMessage();
                    return;
                } else {
                    Controller.commit(args[1]);
                }
                checkOperands(args, 2);
                break;
            case "add":
                checkOperands(args, 2);
                Controller.add(args[1]);
                break;
            case "status":
                checkOperands(args, 1);
                Controller.status();
                break;
            case "rm-branch":
                checkOperands(args, 2);
                Controller.rmBranch(args[1]);
                break;
            case "checkout":
                if (args.length == 3 && args[1].equals("--")) {
                    Controller.checkoutToHead(args[2]);
                } else if (args.length == 4 && args[2].equals("--")) {
                    Controller.checkoutToCommit(args[1], args[3]);
                } else if (args.length == 2) {
                    Controller.checkoutToBranch(args[1]);
                } else {
                    checkOperands(args, -1);
                }
                break;
            case "log":
                checkOperands(args, 1);
                Controller.log();
                break;
            case "global-log":
                checkOperands(args, 1);
                Controller.globalLog();
                break;
            case "reset":
                checkOperands(args, 2);
                Controller.reset(args[1]);
                break;
            case "merge":
                checkOperands(args, 2);
                Controller.merge(args[1]);
                break;
            case "rm":
                checkOperands(args, 2);
                Controller.rm(args[1]);
                break;
            case "find":
                checkOperands(args, 2);
                Controller.find(args[1]);
                break;
            // Below is the default, when any of the above is not matched
            default:
                Output.wrongCommand();
        }


        db.saveOnClose();
    }
}
