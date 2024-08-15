package gitlet;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;

import static gitlet.Utils.*;

/**
 * Represents a gitlet repository.
 *  @author Liana Kong and Mischa Mikami
 *  */
public class Repository implements Serializable {
    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** Stores all methods created */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    /** Stores all branches created */
    public static final File BRANCH_DIR = join(GITLET_DIR, "branch");
    /** Points at the current branch */
    public static final File CURR_BRANCH = new File(GITLET_DIR, "currBranch");
    /** Points at head commit */
    public static final File HEAD = new File(GITLET_DIR, "head");
    /** Stores all commits made */
    public static final File COMMIT_DIR = join(GITLET_DIR, "commit");
    /** Stores the file contents (blobs) that are tracked by commits */
    public static final File BLOB_DIR = join(GITLET_DIR, "blob");
    /** Stages all files for removal and addition in the next commit */
    public static final File STAGING_DIR = join(GITLET_DIR, "stage");
    private final StagingArea stagingArea = new StagingArea();

    public Repository() {
        stagingArea.load();
    }

    /** Initializes Gitlet */
    public void init() {
        try {
            if (GITLET_DIR.exists()) {
                throw new GitletException("A Gitlet version-control system already exists in the current directory.");
            }
            GITLET_DIR.mkdir();
            COMMIT_DIR.mkdir();
            STAGING_DIR.mkdir();
            BLOB_DIR.mkdir();
            BRANCH_DIR.mkdir();
            commit("initial commit");
        } catch (GitletException e) {
            System.out.println(e.getMessage());
        }
    }

    /** Adds any changed file contents to the staging directory so that they can be committed */
    public void add(String fileName) {
        Map<String, String> stagedForAddition = stagingArea.getStagedForAddition();
        Map<String, String> stagedForRemoval = stagingArea.getStagedForRemoval();
        Map<String, String> currCommitBlobs = Objects.requireNonNull(getCurrCommit()).getBlobMap();
        try {
            File file = new File(CWD, fileName);
            if (file.exists()) {
                String fileContents = Utils.readContentsAsString(file); // gets contents of file as string
                String prevCommitBlobID = currCommitBlobs.get(fileName); // check if file changed from curr commit
                if (!fileContents.equals(prevCommitBlobID)) { // if file is new or modified
                    stagedForAddition.put(fileName, fileContents); // stage it for addition
                    stagedForRemoval.remove(fileName);
                } else { // if file is unchanged and currently staged
                    stagedForAddition.remove(fileName);
                    stagedForRemoval.remove(fileName);
                }
                stagingArea.save();
            } else {
                throw new GitletException("File does not exist.");
            }
        } catch (GitletException e) {
            System.out.println(e.getMessage());
        }
    }

    /** Removes specific files in the next commit and adds this file ID to the staging directory */
    public void remove(String fileName) {
        Map<String, String> stagedForAddition = stagingArea.getStagedForAddition();
        Map<String, String> stagedForRemoval = stagingArea.getStagedForRemoval();
        Map<String, String> currCommitBlobs = Objects.requireNonNull(getCurrCommit()).getBlobMap();
        try {
            if (stagedForAddition.containsKey(fileName)) { // unstage file if it is currently staged for addition
                stagedForAddition.remove(fileName);
            } else if (currCommitBlobs.containsKey(fileName)) {
                File file = new File(CWD, fileName);
                if (file.exists()) {
                    file.delete();
                }
                String blobId = currCommitBlobs.get(fileName);
                stagedForRemoval.put(fileName, blobId);
            } else {
                throw new GitletException("No reason to remove the file.");
            }
            stagingArea.save();
        } catch (GitletException e) {
            System.out.println(e.getMessage());
        }
    }

    /** Updates the saved file contents (blobs) that have been removed or added & returns tracked files */
    private Map<String, String> updatedBlobs() {
        Map<String, String> blobMap = new HashMap<>(); // maps Commit IDs to Blob IDs
        if (getCurrCommit() != null) { // put all blobs from the previous commit into the new commit
            blobMap.putAll(getCurrCommit().getBlobMap());
        }
        blobMap.putAll(stagingArea.getStagedForAddition());
        for (String fileName : stagingArea.getStagedForRemoval().keySet()) {
            blobMap.remove(fileName);
        }
        return blobMap;
    }

    private Commit getCurrCommit() {
        if (getHead() != null) {
            File currCommit = new File(COMMIT_DIR, getHead());
            return Utils.readObject(currCommit, Commit.class);
        }
        return null;
    }

    private String getHead() {
        if (HEAD.exists()) {
            return Utils.readContentsAsString(HEAD);
        }
        return null;
    }

    /** Commit any changes made to the file contents and save it to the commit directory
     * for merge, add what happens if parent2 isn't null */
    public void commit(String message) {
        try {
            if (message.isEmpty()) {
                throw new GitletException("Please enter a commit message.");
            } else if (message.equals("initial commit")) {
                String timestamps = "Thu Jan 1 00:00:00 1970 -0800";
                Commit committing = new Commit(message, timestamps, updatedBlobs(), null, null);
                committing.updateCommits(committing);
                updateBranch(committing);
            } else if (stagingArea.getStagedForAddition().isEmpty() && stagingArea.getStagedForRemoval().isEmpty()) {
                throw new GitletException("No changes added to the commit.");
            } else if (!message.contains("merge")) {
                Commit committing = new Commit(message, realDate(), updatedBlobs(), getHead(), null);
                committing.updateCommits(committing);
                updateBranch(committing);
            } else {
                int endLength = message.length() - (" into ").length() - Math.toIntExact(CURR_BRANCH.length());
                String otherBranchName = message.substring(("Merged ").length(), endLength);
                Commit committing = new Commit(message, realDate(), updatedBlobs(), getHead(), otherBranchName);
                committing.updateCommits(committing);
                updateBranch(committing);
            }
        } catch (GitletException e) {
            System.out.println(e.getMessage());
        }
    }

    /** Updates the branch to point at the most recent commit */
    public void updateBranch(Commit committing) {
        if (committing.getMessage().equals("initial commit")) {
            branch("main");
            Utils.writeContents(CURR_BRANCH, "main");
        }
        File newBranchFile = new File(BRANCH_DIR, Utils.readContentsAsString(CURR_BRANCH));
        Utils.writeContents(newBranchFile, committing.getId(committing));
    }

    /** Displays the current timestamp of a commit, with a newly formatted date */
    private String realDate() {
        SimpleDateFormat dateSetUp = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");
        dateSetUp.setTimeZone(TimeZone.getTimeZone("PST"));
        Date timestamps = new Date();
        return dateSetUp.format(timestamps);
    }

    /** Logs all the commits made: starts at current commit and moves backward through all parent commits */
    public void log() {
        String headCommitId = getHead();
        Commit currCommit = getCurrCommit();
        while (currCommit != null) {
            System.out.println("===");
            System.out.println("commit " + headCommitId);
            if (currCommit.getParent2() != null) {
                String first = currCommit.getParent1().substring(0, 7);
                String second = currCommit.getParent2().substring(0, 7);
                System.out.print("Merge: " + first + " " + second);
            }
            System.out.println("Date: " + currCommit.getTimestamp());
            System.out.println(currCommit.getMessage() + "\n");
            String parentCommit = currCommit.getParent1();
            if (parentCommit == null) {
                break;
            }
            currCommit = accessCommit(parentCommit);
            headCommitId = parentCommit;
        }
    }

    /** Gets the commit object from a specified SHA1-ID (identifier for name of commits) */
    private Commit accessCommit(String commitId) {
        if (commitId == null || commitId.isEmpty()) {
            return null;
        }
        int idLength = commitId.length();
        if (idLength < UID_LENGTH) {
            for (String id : Objects.requireNonNull(plainFilenamesIn(COMMIT_DIR))) {
                if (id.startsWith(commitId)) { // checks each commitID to find correct full one
                    commitId = id; // once found, reassigns shortened commitID to its full ID
                    break;
                }
            }
        }
        File thisFile = new File(COMMIT_DIR, commitId);
        if (!thisFile.exists()) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        return Utils.readObject(thisFile, Commit.class);
    }


    /** Displays information about every commit made, in no particular order
     * for merge, in the nested if statement, only the first 7 shaIDs are printed */
    public void globalLog() {
        for (String fileId : Objects.requireNonNull(Utils.plainFilenamesIn(COMMIT_DIR))) {
            Commit currCommit = accessCommit(fileId);
            System.out.println("===");
            System.out.println("commit " + fileId);
            if (Objects.requireNonNull(currCommit).getParent2() != null) {
                String first = currCommit.getParent1().substring(0, 7);
                String second = currCommit.getParent2().substring(0, 7);
                System.out.print("Merge: " + first + " " + second);
            }
            System.out.println("Date: " + currCommit.getTimestamp());
            System.out.println(currCommit.getMessage() + "\n");
        }
    }

    /** Finds all commit IDs that match a given commit message */
    public void find(String message) {
        List<String> anyMatches = new ArrayList<>();
        try {
            for (String fileId : Objects.requireNonNull(Utils.plainFilenamesIn(COMMIT_DIR))) {
                if (message.equals(Objects.requireNonNull(accessCommit(fileId)).getMessage())) {
                    System.out.println(fileId);
                    anyMatches.add(fileId);
                }
            }
            if (anyMatches.isEmpty()) {
                throw new GitletException("Found no commit with that message.");
            }
        } catch (GitletException e) {
            System.out.println(e.getMessage());
        }
    }

    public void status() {
        System.out.println("=== Branches ===");
        Set<String> sortedBranches = new TreeSet<>(Objects.requireNonNull(plainFilenamesIn(BRANCH_DIR)));
        for (String branch : sortedBranches) {
            if (branch.equals(Utils.readContentsAsString(CURR_BRANCH))) {
                System.out.println("*" + branch);
            } else {
                System.out.println(branch);
            }
        }
        System.out.println("\n" + "=== Staged Files ===");
        Map<String, String> stagedForAddition = stagingArea.getStagedForAddition();
        Set<String> sortedAddition = new TreeSet<>(stagedForAddition.keySet());
        for (String file : sortedAddition) {
            System.out.println(file);
        }
        System.out.println("\n" + "=== Removed Files ===");
        Map<String, String> stagedForRemoval = stagingArea.getStagedForRemoval();
        Set<String> sortedRemoval = new TreeSet<>(stagedForRemoval.keySet());
        for (String file : sortedRemoval) {
            System.out.println(file);
        }
        System.out.println("\n" + "=== Modifications Not Staged For Commit ===");
        System.out.println("\n" + "=== Untracked Files ===");
    }

    /** Case 1: Restore the head commit */
    public void restore(String fileName) {
        try {
            Commit headCommit = getCurrCommit();
            Map<String, String> headBlobMap = Objects.requireNonNull(headCommit).getBlobMap();
            if (!headBlobMap.containsKey(fileName)) {
                throw new GitletException("File does not exist in that commit.");
            }
            restoreFile(headCommit, fileName);
        } catch (GitletException e) {
            System.out.println(e.getMessage());
        }
    }

    /** Case 2: Restore a specified commit based on its Sha1-ID */
    public void restore(String commitID, String fileName) {
        try {
            Commit currCommit = Objects.requireNonNull(accessCommit(commitID));
            if (!currCommit.getBlobMap().containsKey(fileName)) {
                throw new GitletException("File does not exist in that commit.");
            }
            restoreFile(currCommit, fileName);
        } catch (GitletException e) {
            System.out.println(e.getMessage());
        }
    }

    /** Restores a file from specified commit object */
    private void restoreFile(Commit commit, String fileName) {
        Map<String, String> commitBlobs = commit.getBlobMap(); // access files/blobIDs from this commit
        String blobContents = commitBlobs.get(fileName); // retrieve correct blobID for specified file
        File file = new File(CWD, fileName); // add file to CWD
        Utils.writeContents(file, blobContents); // write blobContents to this file in CWD
        stagingArea.getStagedForAddition().remove(fileName);
        stagingArea.getStagedForRemoval().remove(fileName);
        stagingArea.save();
    }

    /** Creates a new branch and points it at the given head */
    public void branch(String branchName) {
        File newBranch = new File(BRANCH_DIR, branchName);
        try {
            if (newBranch.exists()) {
                throw new GitletException("A branch with that name already exists.");
            }
            Utils.writeContents(newBranch, getHead());
        } catch (GitletException e) {
            System.out.println(e.getMessage());
        }
    }

    /** If the file is not in the staging directory or currently tracked, add it to an ArrayList of untracked files */
    private ArrayList<String> getUntrackedFiles() {
        ArrayList<String> untrackedFilesMap = new ArrayList<>();
        for (String fileName : Objects.requireNonNull(plainFilenamesIn(CWD))) {
            boolean stagedForAdd = stagingArea.getStagedForAddition().containsKey(fileName);
            boolean stagedForRemoval = stagingArea.getStagedForRemoval().containsKey(fileName);
            boolean trackedFile = updatedBlobs().containsKey(fileName);
            if (!stagedForAdd && !stagedForRemoval && !trackedFile) {
                untrackedFilesMap.add(fileName);
            }
        }
        return untrackedFilesMap;
    }

    /** Switches to another branch, overwrites CWD files as needed, updates head, and updates branch */
    public void switching(String branchName) {
        try {
            if (!getUntrackedFiles().isEmpty()) {
                throw new GitletException("There is an untracked file in the way;"
                        + " delete it, or add and commit it first.");
            } else if (branchName.equals(Utils.readContentsAsString(CURR_BRANCH))) {
                throw new GitletException("No need to switch to the current branch.");
            } else {
                File newBranch = new File(BRANCH_DIR, branchName);
                if (!newBranch.exists()) {
                    throw new GitletException("No such branch exists.");
                } else {
                    Commit newBranchCommit = accessCommit(Utils.readContentsAsString(newBranch));
                    updateCWD(Objects.requireNonNull(newBranchCommit));
                    removeDifferentTrackedFiles(Objects.requireNonNull(getCurrCommit()), newBranchCommit);
                    Utils.writeContents(CURR_BRANCH, branchName); // switch branch
                    Utils.writeContents(Repository.HEAD, Utils.readContentsAsString(newBranch));
                    accessCommit(Objects.requireNonNull(getHead()));
                    stagingArea.clearStage();
                    stagingArea.save();
                }
            }
        } catch (GitletException e) {
            System.out.println(e.getMessage());
        }
    }

    /** Updates the CWD based on the files from the most recent commit in the new branch */
    private void updateCWD(Commit newBranchCommit) {
        for (String fileName : newBranchCommit.getBlobMap().keySet()) {
            restoreFile(newBranchCommit, fileName);
        }
    }

    /** Removes tracked files from the previous commit that are not in any commits from the new branch */
    private void removeDifferentTrackedFiles(Commit currBranch, Commit newBranchCommit) {
        for (String fileName : currBranch.getBlobMap().keySet()) {
            if (!newBranchCommit.getBlobMap().containsKey(fileName)) {
                Utils.restrictedDelete(fileName);
            }
        }
    }

    /** Removes the branch without deleting any commits made in the branch */
    public void rmBranch(String branchName) {
        String currBranch = Utils.readContentsAsString(CURR_BRANCH);
        File branchToDelete = new File(BRANCH_DIR, branchName);
        try {
            if (!branchToDelete.exists()) {
                throw new GitletException("A branch with that name does not exist.");
            } else if (branchName.equals(currBranch)) {
                throw new GitletException("Cannot remove the current branch.");
            } else {
                branchToDelete.delete();
            }
        } catch (GitletException e) {
            System.out.println(e.getMessage());
        }
    }


    public void reset(String commitId) {
        try {
            if (!getUntrackedFiles().isEmpty()) {
                throw new GitletException("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
            }
            Commit resetCommit = accessCommit(commitId);
            updateCWD(Objects.requireNonNull(resetCommit));
            // removeDifferentTrackedFiles(Objects.requireNonNull(getCurrCommit()), resetCommit);
            for (String fileName : Objects.requireNonNull(plainFilenamesIn(CWD))) {
                if (!resetCommit.getBlobMap().containsKey(fileName)) {
                    Utils.restrictedDelete(fileName);
                }
            }
            Utils.writeContents(HEAD, commitId);
            Utils.writeContents(new File(BRANCH_DIR, Utils.readContentsAsString(CURR_BRANCH)), commitId);
            stagingArea.clearStage();
            stagingArea.save();
        } catch (GitletException e) {
            System.out.println(e.getMessage());
        }
    }

    private Commit accessBranchCommitId(String branchName) {
        File newBranch = new File(BRANCH_DIR, branchName);
        return accessCommit(Utils.readContentsAsString(newBranch));
    }

    public void merge(String branchName) {
        try {
            File otherBranch = new File(BRANCH_DIR, branchName);
            if (!getUntrackedFiles().isEmpty()) {
                throw new GitletException("There is an untracked file in the way;"
                        + " delete it, or add and commit it first.");
            } else if (!stagingArea.getStagedForAddition().isEmpty() || !stagingArea.getStagedForRemoval().isEmpty()) {
                throw new GitletException("You have uncommitted changes.");
            } else if (!otherBranch.exists()) {
                throw new GitletException("A branch with that name does not exist.");
            } else if (branchName.equals(Utils.readContentsAsString(CURR_BRANCH))) {
                throw new GitletException("Cannot merge a branch with itself.");
            } else if (Objects.requireNonNull(findSplit(branchName)).getMessage().
                    equals(accessBranchCommitId(branchName).getMessage())) {
                throw new GitletException("Given branch is an ancestor of the current branch.");
            } else if (Objects.requireNonNull(findSplit(branchName)).getMessage().
                    equals(Objects.requireNonNull(getCurrCommit()).getMessage())) {
                switching(branchName);
                throw new GitletException("Current branch fast-forwarded.");
            }

            HashSet<String> allFiles = getAllFiles(branchName);
            boolean conflict = false;
            for (String file : allFiles) {
                String splitVersion = Objects.requireNonNull(findSplit(branchName)).getBlobMap().get(file);
                String headVersion = Objects.requireNonNull(getCurrCommit()).getBlobMap().get(file);
                String branchVersion = accessBranchCommitId(branchName).getBlobMap().get(file);

                if (splitVersion == null) { // case 4 & 5
                    notInSplitCase(file, headVersion, branchVersion);
                } else if (headVersion == null || branchVersion == null) { // case 6 & 7
                    unmodifiedCase(file, splitVersion, headVersion, branchVersion);
                } else if (splitVersion.equals(headVersion) || splitVersion.equals(branchVersion)) { // case 1 & 2
                    modifiedCase(file, splitVersion, headVersion, branchVersion);
                } else if (branchVersion.equals(headVersion)) { // case 3a
                    modifiedCase(file, splitVersion, headVersion, branchVersion);
                } else {
                    conflict = true;
                    mergeConflict(file, headVersion, branchVersion); // case 3b
                }
            }
            if (conflict) {
                throw new GitletException("Encountered a merge conflict.");
            } else {
                commit("Merged " + branchName + " into " + Utils.readContentsAsString(CURR_BRANCH) + ".");
            }
        } catch (GitletException e) {
            System.out.println(e.getMessage());
        }
    }

    private Commit findSplit(String branchName) {
        ArrayList<String> allIds = new ArrayList<>();
        LinkedList<Commit> toVisit = new LinkedList<>(); // add all commits and its parents into a linked list
        if (getCurrCommit() != null) {
            toVisit.add(getCurrCommit());
        }
        if (accessBranchCommitId(branchName) != null) {
            toVisit.add(accessBranchCommitId(branchName));
        }

        while (!toVisit.isEmpty()) {
            Commit firstCommitInList = toVisit.getFirst(); // access first commit in linked list
            if (allIds.contains(firstCommitInList.getId(firstCommitInList))) {
                return firstCommitInList;
            }
            allIds.add(firstCommitInList.getId(firstCommitInList));
            if (firstCommitInList.getParent1() != null) {
                toVisit.add(accessCommit(firstCommitInList.getParent1()));
            }
            if (firstCommitInList.getParent2() != null) {
                toVisit.add(accessCommit(firstCommitInList.getParent2()));
            }
            toVisit.removeFirst(); // move onto the next commit in linked list
        }
        return null;
    }

    private HashSet<String> getAllFiles(String branch) {
        Commit branchCommit = accessBranchCommitId(branch);
        Commit headCommit = getCurrCommit();
        Commit splitPoint = findSplit(branch);
        HashSet<String> allFiles = new HashSet<>(branchCommit.getBlobMap().keySet());
        allFiles.addAll(Objects.requireNonNull(headCommit).getBlobMap().keySet());
        allFiles.addAll(Objects.requireNonNull(splitPoint).getBlobMap().keySet());
        return allFiles;
    }

    private void notInSplitCase(String file, String headVersion, String branchVersion) {
        if (branchVersion != null && headVersion == null) {
            File newFile = new File(CWD, file);
            Utils.writeContents(newFile, branchVersion);
            add(file);
        } // if head != null: do nothing
    }

    private void modifiedCase(String file, String splitVersion, String headVersion, String branchVersion) {
        if (splitVersion.equals(headVersion) && branchVersion != null) {
            File newFile = new File(CWD, file);
            Utils.writeContents(newFile, branchVersion);
            add(file);
        } // if split == branch: do nothing
    }

    private void unmodifiedCase(String file, String splitVersion, String headVersion, String branchVersion) {
        if (splitVersion.equals(headVersion) && branchVersion == null) {
            remove(file);
        } // if split = branch: do nothing
    }

    private void mergeConflict(String file, String headVersion, String branchVersion) {
        File conflictFile = new File(CWD, file);
        String conflictMessage = "<<<<<<< HEAD" + "\n"
                + Objects.requireNonNullElse(headVersion, "") + "=======" + "\n"
                + Objects.requireNonNullElse(branchVersion, "") + ">>>>>>>";
        Utils.writeContents(conflictFile, conflictMessage);
        add(file);
    }
}
