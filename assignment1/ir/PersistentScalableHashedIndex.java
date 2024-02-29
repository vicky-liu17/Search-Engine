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

    public void startMerging() {
        mergingStatusList.put(dictFileList.get(dictFileList.size() - 1), false);
        FileMerger m = new FileMerger();
        Thread t = new Thread(m);
        t.start();
        threads.add(t);
    }

    public long writeTempFiles(
            RandomAccessFile dataFile, RandomAccessFile dictionaryFile, String text, long position) {
        int dataSize = writeData(dataFile, text, position);
        position += dataSize;
        long hashCode = calculateHash(text.split(" ")[0]);
        while (entryExists(dictionaryFile, hashCode))
            hashCode = (hashCode + 1) % TABLESIZE;
        Entry newEntry = new Entry(position, dataSize);
        writeEntry(newEntry, position, dictionaryFile);

        return position;
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
            writeIndex(docInfoFileList.get(docInfoFileList.size() - 1));
            startMerging();
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
        System.err.println("Writing index to disk...");
        writeIndex(docInfoFileList.get(docInfoFileList.size() - 1));
        startMerging();
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

    class FileMerger implements Runnable {
        String dictFile_1;
        String dictFile_2;
        String dataFile_1;
        String dataFile_2;
        String docInfoFile_1;
        String docInfoFile_2;

        public FileMerger() {
        }

        public void run() {
            startMergingFile();
        }

        public void startMergingFile() {
            int i = 0;
            while (i < dictFileList.size() - 1) {
                String dictFile1 = dictFileList.get(i);
                String dictFile2 = dictFileList.get(i + 1);

                if (!mergingStatusList.get(dictFile1) && !mergingStatusList.get(dictFile2)) {
                    mergingStatusList.put(dictFile1, true);
                    mergingStatusList.put(dictFile2, true);

                    dictFile_1 = dictFile1;
                    dataFile_1 = dataFileList.get(i);
                    docInfoFile_1 = docInfoFileList.get(i);

                    dictFile_2 = dictFile2;
                    dataFile_2 = dataFileList.get(i + 1);
                    docInfoFile_2 = docInfoFileList.get(i + 1);

                    mergeTwoIndexes();
                    break;
                }
                i++;
            }
        }

        public void mergeTwoIndexes() {
            System.out.println(dataFile_1 + " and " + dataFile_2 + " is in the merging process.");

            String mergeDictFileName = INDEXDIR + "/" + "merging_" + dictFile_1.split("/")[2] + "_"
                    + dictFile_2.split("/")[2];
            String mergeDataFileName = INDEXDIR + "/" + "merging_" + dataFile_1.split("/")[2] + "_"
                    + dataFile_2.split("/")[2];

            // Merge data and dictionary
            try (BufferedReader bufferedReader_1 = new BufferedReader(new FileReader(dataFile_1));
                    BufferedReader bufferedReader_2 = new BufferedReader(new FileReader(dataFile_2));
                    RandomAccessFile dict = new RandomAccessFile(mergeDictFileName, "rw");
                    RandomAccessFile data = new RandomAccessFile(mergeDataFileName, "rw")) {

                long tempFilePointer = 0;
                String line1 = bufferedReader_1.readLine();
                String line2 = bufferedReader_2.readLine();

                while (line1 != null && line2 != null) {
                    String dataString;
                    String[] arr1 = line1.split(" ");
                    String[] arr2 = line2.split(" ");
                    String term1 = arr1[0];
                    String term2 = arr2[0];

                    if (term1.compareTo(term2) <= 0) {
                        dataString = term1.compareTo(term2) == 0 ? line1 + "," + arr2[1] : line1;
                        line1 = bufferedReader_1.readLine();
                    } else {
                        dataString = line2;
                        line2 = bufferedReader_2.readLine();
                    }
                    dataString += "\n";
                    tempFilePointer = writeTempFiles(data, dict, dataString, tempFilePointer);
                }

                String line;
                BufferedReader bufferedReader = line1 != null ? bufferedReader_1 : bufferedReader_2;
                while ((line = bufferedReader.readLine()) != null) {
                    String dataString = line + "\n";
                    tempFilePointer = writeTempFiles(data, dict, dataString, tempFilePointer);
                }

                bufferedReader_1.close();
                bufferedReader_2.close();
                bufferedReader.close();
                dict.close();
                data.close();

                deleteFile(dictFile_1);
                deleteFile(dataFile_1);
                deleteFile(dictFile_2);
                deleteFile(dataFile_2);

                renameFile(mergeDictFileName, dictFile_1);
                renameFile(mergeDataFileName, dataFile_1);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Merge docInfo files
            // The 'true' parameter in FileWriter(docInfoFile_1, true) indicates that the
            // FileWriter
            // should append the new content to the end of the existing content in the file,
            // rather than overwriting the existing content.

            try (
                    BufferedReader br = new BufferedReader(new FileReader(docInfoFile_2));
                    BufferedWriter bw = new BufferedWriter(new FileWriter(docInfoFile_1, true))) {
                String line;
                while ((line = br.readLine()) != null) {
                    bw.write(line);
                    bw.newLine();
                }
                // Remove second file
                deleteFile(docInfoFile_2);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Remove the second file from memory
            int indexToRemove = dictFileList.indexOf(dictFile_2);
            if (indexToRemove != -1) {
                dictFileList.remove(indexToRemove);
                dataFileList.remove(indexToRemove);
                docInfoFileList.remove(indexToRemove);
            }

            mergingStatusList.put(dictFile_1, false);
            System.err.println("Finished!");

            // Repeat and merge again if possible
            startMergingFile();
        }

        public void deleteFile(String filePath) {
            File fileToDelete = new File(filePath);
            String name = fileToDelete.getName();

            if (fileToDelete.exists()) {
                if (fileToDelete.delete()) {
                    System.err.println(name + " is deleted successfully");
                } else {
                    System.err.println("Failed to delete " + name);
                }
            } else {
                System.err.println(name + " does not exist");
            }
        }

        public void renameFile(String filePath1, String filePath2) {
            File sourceFile = new File(filePath1);
            File destFile = new File(filePath2);
            String sourceName = sourceFile.getName();
            String destName = destFile.getName();

            if (sourceFile.renameTo(destFile)) {
                System.out.println("Successfully Renamed " + sourceName + " to " + destName);
            } else {
                System.out.println("Failed to rename " + sourceName);
                // Retry renaming after waiting for a short period
                try {
                    Thread.sleep(1000);
                    if (sourceFile.renameTo(destFile)) {
                        System.out.println("Successfully Renamed " + sourceName + " to " + destName);
                    } else {
                        System.out.println("Failed to rename " + sourceName + " even after retrying");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}