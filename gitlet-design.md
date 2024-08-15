# Gitlet Design Document

**Name**: Liana Kong and Mischa Mikami

## Classes and Data Structures
.gitlet  
├── commit (directory)  
│   ├── commit (initial file)  
│   └── commit (etc file)  
├── staging area (directory)  
│   ├── staging  
└── head (file)

### Class 1: Design Overview

#### Fields

1. Directory Structure:
- Commit: directory that contains serialized commit objects with varying sha1 IDs
  - Will store all the commits as fields in the Commit class so that they are serialized
  - Need to know String message, Date timestamp, Map<String, String> blobMap
    (for blob information), String parent1 (to restore), and String parent2 (to merge)
- Staging Area: serialized directory that contains a hash map
  - Hash Map: key (file names) and values (blob IDs)
  - Can be saved and loaded in between calls to add to staging area
- Head: file that updates the head that points at the commits
  - Contains commit file that is the current head

2. How we implemented each method:
- Init:
  - If .gitlet doesn't exit, make the directories/files specified and commit with the message initial commit
- Add:
  - 2 HashMaps: 
  - stagedForAddition (key: fileName & value: blobID, purpose: keeps track of files in staging area)
  - currCommitBlobs (key: fileName & value: blobID, purpose: shows blobs from current commit to compare if there any changes made to file)
  - Implementation:
  - compares blobIDs to check if file changed compared to curr commit
  - file is staged for addition if it is new or has been modified
  - if file is unchanged and currently staged, it is removed from staging area
- Commit:
  - If message or staging directory isn't empty, either perform an initial commit or a commit without merges
    with the help of a helper function
  - Helper function:
    - Update commit information
    - Save it into a new file
    - Change the head to point to this new commit
  - Blob Map: maps file names to blob IDs and returns the newly updated blobMap
- Restore:
  - If commit id == bad, throw error. if not, update the head to be pointing at the specified commit id
  - If a file is given and exists in the previous commit, update the head to be pointing at previous commit
- Log:
  - Print out all the commits made while the head is pointing to a commit that isn't the first one
    - When the head gets to the second to last commit, it exits the while loop and prints out the
      log for the initial commit
    - (Later) In the case of a merged commit, it will print out the first seven characters in the
      sha IDs for parent 1 and 2 after the printed commit id of the current commit

3. Branches:
- Default branch

### Class 2: Serialization and Persistence

#### Fields

1. Init
- Init: creates the directories for everything inside gitlet

2. Add (implements serialization in the staging area)
- Add: adds files that we have created or changed to the staging directory
- Compare blob info from current commit to blob info in CWD
- After each addition, we clear the staging directory (hash map)
- Staging area should save hash map in between calls to add more files
  - Should only clear once we commit files and signal staging area to clear

3. Commit (implements serialization)
- Commit: saves a snapshot of current code
- With each commit, update blob information
- With each commit, update parent information (head)

4. Restore
-

5. Log
- Log: creates a log of all of your commits
- Each log includes commit id, both parents if merged, date, and commit message

