package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/** Represents a gitlet commit object.
 *  @author Liana Kong and Mischa Mikami
 */
public class Commit implements Serializable {

    private final String message;
    private final String timestamp;
    private final Map<String, String> blobMap;
    private final String parent1;
    private final String parent2;
    private final StagingArea stagingArea = new StagingArea();

    public Commit(String message, String timestamp, Map<String, String> blobMap, String parent1, String parent2) {
        this.message = message;
        this.timestamp = timestamp;
        this.blobMap = new HashMap<>(blobMap);
        this.parent1 = parent1;
        this.parent2 = parent2;
    }

    /** create new commit file and update which commit file the head points to */
    public void updateCommits(Commit committing) {
        File commitFile = new File(Repository.COMMIT_DIR, getId(committing));
        Utils.writeObject(commitFile, committing);
        Utils.writeContents(Repository.HEAD, getId(committing));
        stagingArea.clearStage();
        stagingArea.save();
    }

    public String getId(Commit commit) {
        Object commiting = Utils.serialize(commit);
        return Utils.sha1(commiting);
    }

    public String getMessage() {
        return this.message;
    }

    public String getTimestamp() {
        return this.timestamp;
    }

    public String getParent1() {
        return this.parent1;
    }

    public String getParent2() {
        return this.parent2;
    }

    public Map<String, String> getBlobMap() {
        return this.blobMap;
    }
}
