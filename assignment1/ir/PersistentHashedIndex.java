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
import java.time.Duration;
import java.time.Instant;

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
public class PersistentHashedIndex implements Index {

    /** The directory where the persistent index files are stored. */
    public static final String INDEXDIR = "./index";

    /** The dictionary file name */
    public static final String DICTIONARY_FNAME = "dictionary";

    /** The data file name */
    public static final String DATA_FNAME = "data";

    /** The terms file name */
    public static final String TERMS_FNAME = "terms";

    /** The doc info file name */
    public static final String DOCINFO_FNAME = "docInfo";

    /** The dictionary hash table on disk can fit this many entries. */
    public static final long TABLESIZE = 611953L;

    /** The dictionary hash table is stored in this file. */
    RandomAccessFile dictionaryFile;

    /** The data (the PostingsLists) are stored in this file. */
    RandomAccessFile dataFile;

    /** Pointer to the first free memory cell in the data file. */
    long free = 0L;

    /** The cache as a main-memory hash map. */
    HashMap<String, PostingsList> index = new HashMap<String, PostingsList>();

    // ===================================================================

    /**
     * A helper class representing one entry in the dictionary hashtable.
     */
    public class Entry {
        long ptr;
        int size;

        public Entry(long ptr, int size) {
            this.ptr = ptr;
            this.size = size;
        }
    }

    // ==================================================================

    /**
     * Constructor. Opens the dictionary file and the data file.
     * If these files don't exist, they will be created.
     */
    public PersistentHashedIndex() {
        try {
            // create the directory
            File indexDir = new File(INDEXDIR);
            if (!indexDir.exists()) {
                if (!indexDir.mkdirs()) {
                    System.err.println("Failed to create index directory.");
                    return;
                }
            }

            dictionaryFile = new RandomAccessFile(INDEXDIR + "/" + DICTIONARY_FNAME, "rw");
            dataFile = new RandomAccessFile(INDEXDIR + "/" + DATA_FNAME, "rw");
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            readDocInfo();
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes data to the data file at a specified place.
     *
     * @return The number of bytes written.
     */
    int writeData(RandomAccessFile dataFileToWrite, String dataString, long ptr) {
        try {
            dataFileToWrite.seek(ptr);
            byte[] data = dataString.getBytes();
            dataFileToWrite.write(data);
            return data.length;
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Reads data from the data file
     */
    String readData(long ptr, int size) {
        try {
            dataFile.seek(ptr);
            byte[] data = new byte[size];
            dataFile.readFully(data);
            return new String(data);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // ==================================================================
    //
    // Reading and writing to the dictionary file.

    /*
     * Writes an entry to the dictionary hash table file.
     *
     * @param entry The key of this entry is assumed to have a fixed length
     * 
     * @param ptr The place in the dictionary file to store the entry
     */
    void writeEntry(Entry entry, long ptr, RandomAccessFile dictFile) {
        try {

            // In this implementation, each entry consists of a long (8 bytes) for the
            // pointer and an int (4 bytes) for the size, totaling 12 bytes.
            ptr *= 12;
            dictFile.seek(ptr);
            dictFile.writeLong(entry.ptr);
            // Move the file pointer to 8 bytes ahead to get the entry.size(long (8 bytes))
            dictFile.seek(ptr + 8);
            dictFile.writeInt(entry.size);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads an entry from the dictionary file.
     *
     * @param ptr The place in the dictionary file where to start reading.
     */
    Entry readEntry(RandomAccessFile dictFile, long ptr) {
        try {
            // Multiplying the hash value by the entry size provides an offset that
            // accurately positions the read operation at the beginning of the data
            // associated with the hash value within the storage structure. This ensures
            // that the read operation retrieves the correct data, accounting for the fact
            // that each slot in the hash table may contain multiple entries, and each entry
            // has a specific size.
            ptr *= 12;
            dictFile.seek(ptr);
            long entry_ptr = dictFile.readLong();
            dictFile.seek(ptr + 8);
            int entry_size = dictFile.readInt();
            Entry entry = new Entry(entry_ptr, entry_size);
            return entry;
        } catch (IOException e) {
        }
        return null;
    }

    // ==================================================================

    /**
     * Writes the document names and document lengths to file.
     *
     * @throws IOException { exception_description }
     */
    public void writeDocInfo(String foutFileName) throws IOException {
        // create the directory
        File indexDir = new File(INDEXDIR);
        if (!indexDir.exists()) {
            if (!indexDir.mkdirs()) {
                System.err.println("Failed to create index directory.");
                return;
            }
        }

        FileOutputStream fout = new FileOutputStream(foutFileName);
        for (Map.Entry<Integer, String> entry : docNames.entrySet()) {
            Integer key = entry.getKey();
            String docInfoEntry = key + ";" + entry.getValue() + ";" + docLengths.get(key) + "\n";
            fout.write(docInfoEntry.getBytes());
        }
        fout.close();
    }

    /**
     * Reads the document names and document lengths from file, and
     * put them in the appropriate data structures.
     *
     * @throws IOException { exception_description }
     */
    public void readDocInfo() throws IOException {
        File file = new File(INDEXDIR + "/docInfo");
        FileReader freader = new FileReader(file);
        try (BufferedReader br = new BufferedReader(freader)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(";");
                docNames.put(new Integer(data[0]), data[1]);
                docLengths.put(new Integer(data[0]), new Integer(data[2]));
            }
        }
        freader.close();
    }

    /**
     * Write the index to files.
     */
    public void writeIndex() {
        int collisions = 0;
        try {
            // Write the 'docNames' and 'docLengths' hash maps to a file
            writeDocInfo(INDEXDIR + "/docInfo");

            // Write the dictionary and the postings list
            for (String key : index.keySet()) {
                // Find empty slot in dictionary
                long hashValue = calculateHash(key);
                while (entryExists(dictionaryFile, hashValue)) {
                    hashValue = (hashValue + 1) % TABLESIZE;
                    collisions++;
                }
                // Write to dataFile
                String data = key + " " + index.get(key).toString() + "\n";
                int size = writeData(dataFile, data, free);

                // Write to dictionaryFile
                Entry entry = new Entry(free, size);
                writeEntry(entry, hashValue, dictionaryFile);

                free += size;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.err.println(collisions + " collisions.");
    }

    // ==================================================================

    /**
     * Returns the postings for a specific term, or null
     * if the term is not in the index.
     */
    public PostingsList getPostings(String token) {
        // Calculate the hash value of the token
        long hashValue = calculateHash(token);
        Instant startTime = Instant.now();
        int collisions = 0;
        System.out.println(token);

        // Iterate through the hash table until an entry with the given token is found
        // or the end is reached
        while (entryExists(dictionaryFile, hashValue)) {
            // Read the entry at the calculated hash position
            Entry entry = readEntry(dictionaryFile, hashValue);

            // Read the data associated with the entry
            String[] data = readData(entry.ptr, entry.size).split(" ");
            System.out.println(data[0]);
            System.out.println(data[1]);

            // Check if the first element of the data array matches the token
            if (data[0].equals(token)) {
                Instant endTime = Instant.now();
                Duration elapsedTime = Duration.between(startTime, endTime);
                // If a match is found, return the associated postings list
                System.out.println("Collisions: " + collisions);
                System.out.println("Searching time for token " + token + ": " + elapsedTime.toMillis() + " ms");
                return new PostingsList(data[1].trim());
            }

            // If no match is found, move to the next slot in the hash table
            hashValue++;
            collisions++;
        }

        // If the loop completes without finding a match, return null
        return null;
    }

    /**
     * Inserts this token in the main-memory hashtable.
     */
    public void insert(String token, int docID, int offset) {
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
    }

    /**
     * Calculates the hash value for the given input string using a custom hash
     * algorithm.
     * 
     * @param input The input string for which the hash value is to be calculated.
     * @return The calculated hash value.
     */
    public long calculateHash(String input) {
        // Initialize the hash value to a prime number (7 is commonly used)
        long hashValue = 7;

        // Iterate through each character in the input string
        for (char character : input.toCharArray()) {
            // Update the hash value using a custom hash algorithm:
            // 1. Multiply the current hash value by a prime number (103 is commonly used)
            // 2. Add the ASCII value of the character to the hash value
            // 3. Take the modulo of TABLE_SIZE to keep the hash value within the range
            hashValue = (hashValue * 103 + character) % TABLESIZE;
        }

        // Return the calculated hash value
        return hashValue;
    }

    /**
     * Checks if an entry exists for the given hash value.
     * 
     * @param hashValue The hash value used to locate the entry.
     * @return True if an entry exists, false otherwise.
     */
    public boolean entryExists(RandomAccessFile dictFile, long hashValue) {
        Entry entry = readEntry(dictFile, hashValue);
        return entry != null && entry.ptr != 0 && entry.size != 0;
    }

    /**
     * Write index to file after indexing is done.
     */

    public void cleanup() {
        System.err.println(index.keySet().size() + " unique words");
        System.err.print("Writing index to disk...");

        Instant startTime = Instant.now();
        writeIndex();
        Instant endTime = Instant.now();

        System.err.println("done!");

        Duration duration = Duration.between(startTime, endTime);
        System.err.println("Use " + duration.getSeconds() + "." + duration.getNano() + " seconds.");
    }

}