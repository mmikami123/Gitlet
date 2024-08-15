package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class StagingArea implements Serializable {
    private Map<String, String> stagedForAddition;
    private Map<String, String> stagedForRemoval;
    private static final File STAGING_FILE = new File(Repository.STAGING_DIR, "stagingArea.txt");

    public StagingArea() {
        this.stagedForAddition = new HashMap<>();
        this.stagedForRemoval = new HashMap<>();
    }

    public Map<String, String> getStagedForAddition() {
        return this.stagedForAddition;
    }

    public Map<String, String> getStagedForRemoval() {
        return this.stagedForRemoval;
    }

    /**
     * Clear staging area before each commit
     */
    public void clearStage() {
        stagedForAddition.clear();
        stagedForRemoval.clear();
    }


    public void load() {
        if (STAGING_FILE.exists()) {
            StagingArea stage = Utils.readObject(STAGING_FILE, StagingArea.class);
            this.stagedForAddition = stage.getStagedForAddition();
            this.stagedForRemoval = stage.getStagedForRemoval();
        }
    }

    public void save() {
        Utils.writeObject(STAGING_FILE, this);
    }
}
