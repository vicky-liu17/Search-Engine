/*  
*   This file is part of the computer assignment for the
*   Information Retrieval course at KTH.
* 
*   Johan Boye, KTH, 2018
*/

package ir;

import java.io.*;
import java.util.*;
import java.nio.charset.*;

/*
*   Implements an inverted index as a hashtable on disk.
*   
*   Both the words (the dictionary) and the data (the postings list) are
*   stored in RandomAccessFiles that permit fast (almost constant-time)
*   disk seeks. 
*
*   When words are read and indexed, they are first put in an ordinary,
*   main-memory HashMap. When all words are read, the index is committed
*   to disk.
*/
public class PersistentScalableHashedIndex extends PersistentHashedIndex {

    /** The dictionary hash table on disk can fit this many entries. */
    public static final long TABLESIZE = 3_500_100L;
    private static final int TOKEN_LIMIT = 10_000_000;

    private int totalTokens = 0;
    private int tokenSeperateCounter = 0;
    private int lastDocId = -1;
    private int numOfTempFiles = 0;
    private ArrayList<String> dictFileList = new ArrayList<>();
    private ArrayList<String> dataFileList = new ArrayList<>();
    private ArrayList<String> docInfoFileList = new ArrayList<>();
    private HashMap<String, Boolean> mergingStatusList = new HashMap<>();
    private ArrayList<Thread> threads = new ArrayList<>();

    // ==================================================================

    /**
     * Constructor. Opens the dictionary file and the data file.
     * If these files don't exist, they will be created.
     */
    public PersistentScalableHashedIndex() {
        createFiles();
        try {
            readDocInfo();
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createFiles() {
        String dictFilename = INDEXDIR + "/"
                + (numOfTempFiles == 0 ? DICTIONARY_FNAME : DICTIONARY_FNAME + numOfTempFiles);
        String dataFilename = INDEXDIR + "/"
                + (numOfTempFiles == 0 ? DATA_FNAME : DATA_FNAME + numOfTempFiles);
        String docInfoFileName = INDEXDIR + "/"
                + (numOfTempFiles == 0 ? DOCINFO_FNAME : DOCINFO_FNAME + numOfTempFiles);
        dictFileList.add(dictFilename);
        dataFileList.add(dataFilename);
        docInfoFileList.add(docInfoFileName);
        mergingStatusList.put(dictFileList.get(dictFileList.size() - 1), true);
        try {
            if (numOfTempFiles > 0) {
                dictionaryFile.close();
                dataFile.close();
            }
            // create the directory
            File indexDir = new File(INDEXDIR);
            if (!indexDir.exists()) {
                if (!indexDir.mkdirs()) {
                    System.err.println("Failed to create index directory.");
                    return;
                }
            }
            dictionaryFile = new RandomAccessFile(dictFileList.get(dictFileList.size() - 1), "rw");
            dataFile = new RandomAccessFile(dataFileList.get(dictFileList.size() - 1), "rw");
        } catch (IOException e) {
            e.printStackTrace();
        }
        numOfTempFiles++;
    }

    /**
     * Write the index to files.
     */
    public void writeIndex() {
        int collisions = 0;
        try {
            // Write the 'docNames' and 'docLengths' hash maps to a file
            writeDocInfo(docInfoFileList.get(docInfoFileList.size() - 1));

            // Write the dictionary and the postings list

            // Sort keyset
            List<String> keys = new ArrayList<>(index.keySet());
            Collections.sort(keys);
            int total_keys = keys.size();
            int counter = 0;
            for (String key : keys) {
                ++counter;
                if (counter % 1000 == 0) {
                    System.err.print("\r" + String.format("%d%%", (100 * counter) / total_keys));
                }

                // Find empty slot in dictionary
                long hash = calculateHash(key);
                while (entryExists(dictionaryFile, hash)) {
                    hash = (hash + 1) % TABLESIZE;
                    ++collisions;
                }

                // Write to dataFile
                String data = key + " " + index.get(key).toString() + "\n";
                int size = writeData(dataFile, data, free);

                // Write to dictionaryFile
                Entry entry = new Entry(free, size);
                writeEntry(entry, hash, dictionaryFile);

                free += size;
            }
            System.err.println("\r100%");
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.err.println(collisions + " collisions.");

        mergingStatusList.put(dictFileList.get(dictFileList.size() - 1), false);
        Merger m = new Merger();
        Thread t = new Thread(m);
        t.start();
        threads.add(t);
    }

    class Merger implements Runnable {
        String dictionaryFile1;
        String dictionaryFile2;
        String dataFile1;
        String dataFile2;
        String docInfoFile1;
        String docInfoFile2;

        public Merger() {
        }

        public void run() {
            findIndexesToMerge();
        }

        public void findIndexesToMerge() {
            for (int i = 0; i < dictFileList.size() - 1; ++i) {
                // Only next one is allowed to merge
                if (!mergingStatusList.get(dictFileList.get(i)) && !mergingStatusList.get(dictFileList.get(i + 1))) {
                    dictionaryFile1 = dictFileList.get(i);
                    dataFile1 = dataFileList.get(i);
                    docInfoFile1 = docInfoFileList.get(i);

                    dictionaryFile2 = dictFileList.get(i + 1);
                    dataFile2 = dataFileList.get(i + 1);
                    docInfoFile2 = docInfoFileList.get(i + 1);

                    mergingStatusList.put(dictionaryFile1, true);
                    mergingStatusList.put(dictionaryFile2, true);
                    mergeIndexes();
                    break;
                }
            }
        }

        public void mergeIndexes() {
            System.err.println("mergingStatusList indexes " + dataFile1 + " and " + dataFile2);

            // Merge dictionaries and data
            String merge_dict = INDEXDIR + "/" + "merger_" + dictionaryFile1.split("/")[2] + "_"
                    + dictionaryFile2.split("/")[2];
            String merge_data = INDEXDIR + "/" + "merger_" + dataFile1.split("/")[2] + "_" + dataFile2.split("/")[2];
            try {
                FileReader fr1 = new FileReader(dataFile1);
                FileReader fr2 = new FileReader(dataFile2);
                BufferedReader br1 = new BufferedReader(fr1);
                BufferedReader br2 = new BufferedReader(fr2);
                RandomAccessFile dict = new RandomAccessFile(merge_dict, "rw");
                RandomAccessFile data = new RandomAccessFile(merge_data, "rw");
                long local_free = 0;

                String line1 = br1.readLine();
                String line2 = br2.readLine();
                while (line1 != null && line2 != null) {
                    String[] arr1 = line1.split(" ");
                    String[] arr2 = line2.split(" ");
                    String term1 = arr1[0];
                    String term2 = arr2[0];

                    String dataString = "";
                    if (term1.compareTo(term2) == 0) {
                        // Terms are the same, merge
                        dataString = line1 + "," + arr2[1];
                        line1 = br1.readLine();
                        line2 = br2.readLine();
                    } else if (term1.compareTo(term2) < 0) {
                        // term1 comes before term2
                        dataString = line1;
                        line1 = br1.readLine();
                    } else if (term1.compareTo(term2) > 0) {
                        // term2 comes before term1
                        dataString = line2;
                        line2 = br2.readLine();
                    }
                    dataString += "\n";
                    local_free = writeTempFiles(data, dict, dataString, local_free);

                    int size = writeData(data, dataString, local_free);

                    local_free += size;

                }
                // Read remainder
                while ((line1 = br1.readLine()) != null) {
                    String dataString = line1 + "\n";
                    local_free = writeTempFiles(data, dict, dataString, local_free);
                }
                while ((line2 = br2.readLine()) != null) {
                    String dataString = line2 + "\n";
                    local_free = writeTempFiles(data, dict, dataString, local_free);
                }
                fr1.close();
                fr2.close();
                br1.close();
                br2.close();
                dict.close();
                data.close();

                // Remove both indexes' files from disk
                deleteFile(dictionaryFile1);
                deleteFile(dataFile1);
                deleteFile(dictionaryFile2);
                deleteFile(dataFile2);

                // Rename merged files to the first file's names
                renameFile(merge_dict, dictionaryFile1);
                renameFile(merge_data, dataFile1);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Merge docInfo files
            try {
                BufferedReader br = new BufferedReader(new FileReader(docInfoFile2));
                BufferedWriter bw = new BufferedWriter(new FileWriter(docInfoFile1, true));
                String line;
                while ((line = br.readLine()) != null) {
                    bw.write(line);
                    bw.newLine();
                }
                br.close();
                bw.close();

                // Remove second docInfo file
                deleteFile(docInfoFile2);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Remove the second file from memory
            for (int i = 0; i < dictFileList.size(); ++i) {
                if (dictFileList.get(i).equals(dictionaryFile2)) {
                    dictFileList.remove(i);
                    dataFileList.remove(i);
                    docInfoFileList.remove(i);
                    break;
                }
            }
            mergingStatusList.put(dictionaryFile1, false);
            System.err.println("Merge complete");

            // Repeat and merge again if possible
            findIndexesToMerge();
        }

        public void deleteFile(String filePath) {
            File f = new File(filePath);
            String name = f.getName();
            while (!f.delete()) {
                System.err.println("Failed to delete " + name + ", Trying again...");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.err.println("Deleted " + name);
        }

        public void renameFile(String filePath1, String filePath2) {
            File file1 = new File(filePath1);
            File file2 = new File(filePath2);
            String name1 = file1.getName();
            String name2 = file2.getName();
            while (!file1.renameTo(file2)) {
                System.out.println("Failed to rename " + name1 + " to " + name2 + ", Trying again...");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("Renamed " + name1 + " to " + name2);

        }
    }

    public long writeTempFiles(
            RandomAccessFile data, RandomAccessFile dict, String str, long ptr) {
        int size = writeData(data, str, ptr);
        ptr += size;
        long hash = calculateHash(str.split(" ")[0]);
        while (entryExists(dict, hash))
            hash = (hash + 1) % TABLESIZE;
        Entry entry = new Entry(ptr, size);
        writeEntry(entry, ptr, dict);

        return ptr;
    }

    // ==================================================================

    /**
     * Inserts this token in the main-memory hashtable.
     */
    public void insert(String token, int docID, int offset) {
        //
        // YOUR CODE HERE
        //

        totalTokens++;
        tokenSeperateCounter++;
        if (tokenSeperateCounter > TOKEN_LIMIT && lastDocId != docID) {
            System.out.println("Start to Create New Temp Files");
            writeIndex();
            docNames.clear();
            docLengths.clear();
            index.clear();
            free = 0;
            createFiles();
            tokenSeperateCounter = 0;
        }

        // Create a new PostingsEntry
        PostingsEntry entry = new PostingsEntry(docID, offset);
        // Check if the token already exists in the index
        if (index.containsKey(token)) {
            // If the token exists, get its postings list and add the new entry
            PostingsList postingsList = index.get(token);
            postingsList.insert(entry);
        } else {
            // If the token doesn't exist, create a new postings list and add the entry
            PostingsList postingsList = new PostingsList();
            postingsList.insert(entry);
            index.put(token, postingsList);
        }

        lastDocId = docID;
    }

    /**
     * Write index to file after indexing is done.
     */
    public void cleanup() {
        System.err.println(index.keySet().size() + " unique words at the present stage.");
        System.err.println(totalTokens + " unique words totally.");
        writeIndex();
        try {
            dictionaryFile.close();
            dataFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        threads.forEach(t -> {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });  

        try {
            dictionaryFile = new RandomAccessFile(dictFileList.get(0), "rw");
            dataFile = new RandomAccessFile(dataFileList.get(0), "rw");
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            readDocInfo();
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.err.println("done!");
    }
}