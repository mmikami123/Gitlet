package gitlet;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author Liana Kong and Mischa Mikami */
public class Main {
    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                throw new GitletException("Please enter a command.");
            }
            Repository repository = new Repository();
            String firstArg = args[0];
            switch (firstArg) {
                case "init":
                    if (args.length != 1) {
                        throw new GitletException("Incorrect operands.");
                    }
                    repository.init();
                    break;
                case "add":
                    validate(args, 2);
                    repository.add(args[1]);
                    break;
                case "rm":
                    validate(args, 2);
                    repository.remove(args[1]);
                    break;
                case "commit":
                    validate(args, 2);
                    repository.commit(args[1]);
                    break;
                case "log":
                    validate(args, 1);
                    repository.log();
                    break;
                case "global-log":
                    validate(args, 1);
                    repository.globalLog();
                    break;
                case "find":
                    validate(args, 2);
                    repository.find(args[1]);
                    break;
                case "status":
                    validate(args, 1);
                    repository.status();
                    break;
                case "restore":
                    validateRestore(args);
                    break;
                case "branch":
                    validate(args, 2);
                    repository.branch(args[1]);
                    break;
                case "switch":
                    validate(args, 2);
                    repository.switching(args[1]);
                    break;
                case "rm-branch":
                    validate(args, 2);
                    repository.rmBranch(args[1]);
                    break;
                case "reset":
                    validate(args, 2);
                    repository.reset(args[1]);
                    break;
                case "merge":
                    validate(args, 2);
                    repository.merge(args[1]);
                    break;
                default:
                    throw new GitletException("No command with that name exists.");
            }
        } catch (GitletException e) {
            System.out.println(e.getMessage());
            System.exit(0);
        }
    }

    /** Ensures number of arguments equals expected number of args and Gitlet repository is initialized */
    private static void validate(String[] args, int n) {
        try {
            if (args.length != n) {
                throw new GitletException("Incorrect operands.");
            }
            if (!Repository.GITLET_DIR.exists()) {
                throw new GitletException("Not in an initialized Gitlet directory.");
            }
        } catch (GitletException e) {
            System.out.println(e.getMessage());
            System.exit(0);
        }
    }

    /** Validates restore, based on its specific cases */
    private static void validateRestore(String[] args) {
        Repository repository = new Repository();
        try {
            if (!Repository.GITLET_DIR.exists()) {
                throw new GitletException("Not in an initialized Gitlet directory.");
            } else if (args.length == 3 && args[1].equals("--")) { // case 1
                repository.restore(args[2]);
            } else if (args.length == 4 && args[2].equals("--")) { // case 2
                repository.restore(args[1], args[3]);
            } else {
                throw new GitletException("Incorrect operands.");
            }
        } catch (GitletException e) {
            System.out.print(e.getMessage());
            System.exit(0);
        }
    }
}
