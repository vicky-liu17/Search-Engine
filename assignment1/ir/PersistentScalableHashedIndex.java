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
public class PersistentScalableHashedIndex implements Index {

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
    public static final long TABLESIZE = 3_500_017L;
    // public static final long TABLESIZE = 611953L;

    /** The dictionary hash table is stored in this file. */
    RandomAccessFile dictionaryFile;

    /** The data (the PostingsLists) are stored in this file. */
    RandomAccessFile dataFile;

    /** Pointer to the first free memory cell in the data file. */
    long free = 0L;

    /** The cache as a main-memory hash map. */
    HashMap<String,PostingsList> index = new HashMap<String,PostingsList>();


    // ===================================================================

    /**
     *   A helper class representing one entry in the dictionary hashtable.
    */ 
    public class Entry {
        long ptr;
        int size; 

        public Entry(long ptr, int size) {
            this.ptr = ptr;
            this.size = size;
        }
    }

    int ENTRY_SIZE = 12;        // 12 bytes per entry (long + int)
    int total_tokens = 0;
    // Total tokens (guardian): 57_663_287
    int token_count = 0;
    int TOKEN_LIMIT = 10_000_000;
    // int TOKEN_LIMIT = 1_000_000;
    int previous_docID = -1;

    int intermediary_number = 0;
    ArrayList<String> intermediate_dict = new ArrayList<>();
    ArrayList<String> intermediate_data = new ArrayList<>();
    ArrayList<String> intermediate_info = new ArrayList<>();
    HashMap<String, Boolean> merging = new HashMap<>();
    ArrayList<Thread> threads = new ArrayList<>();

    String foutName;


    // ==================================================================

    
    /**
     *  Constructor. Opens the dictionary file and the data file.
    *  If these files don't exist, they will be created. 
    */
    public PersistentScalableHashedIndex() {
        intermediate_dict.add(INDEXDIR + "/" + DICTIONARY_FNAME);
        intermediate_data.add(INDEXDIR + "/" + DATA_FNAME);
        foutName = INDEXDIR + "/" + DOCINFO_FNAME;
        merging.put(intermediate_dict.get(intermediate_dict.size()-1), true);
        try {
            dictionaryFile = new RandomAccessFile(intermediate_dict.get(0), "rw" );
            dataFile = new RandomAccessFile(intermediate_data.get(0), "rw" );
        } catch ( IOException e ) {
            e.printStackTrace();
        }

        try {
            readDocInfo();
        } catch ( FileNotFoundException e ) {
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    public void createNewIntermediaryFiles() {
        ++intermediary_number;
        intermediate_dict.add(INDEXDIR + "/" + DICTIONARY_FNAME + intermediary_number);
        intermediate_data.add(INDEXDIR + "/" + DATA_FNAME + intermediary_number);
        merging.put(intermediate_dict.get(intermediate_dict.size()-1), true);
        foutName = INDEXDIR + "/" + DOCINFO_FNAME + intermediary_number;
        try {
            dictionaryFile.close();
            dataFile.close();
            dictionaryFile = new RandomAccessFile(intermediate_dict.get(intermediate_dict.size()-1), "rw" );
            dataFile = new RandomAccessFile(intermediate_data.get(intermediate_data.size()-1), "rw" );
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    /**
     *  Writes data to the data file at a specified place.
    *
    *  @return The number of bytes written.
    */ 
    int writeData( String dataString, long ptr ) {
        try {
            dataFile.seek( ptr ); 
            byte[] data = dataString.getBytes();
            dataFile.write( data );
            return data.length;
        } catch ( IOException e ) {
            e.printStackTrace();
            return -1;
        }
    }


    /**
     *  Reads data from the data file
    */ 
    String readData( long ptr, int size ) {
        try {
            dataFile.seek( ptr );
            byte[] data = new byte[size];
            dataFile.readFully( data );
            return new String(data);
        } catch ( IOException e ) {
            e.printStackTrace();
            return null;
        }
    }


    // ==================================================================
    //
    //  Reading and writing to the dictionary file.

    /*
    *  Writes an entry to the dictionary hash table file. 
    *
    *  @param entry The key of this entry is assumed to have a fixed length
    *  @param ptr   The place in the dictionary file to store the entry
    */
    void writeEntry( Entry entry, long ptr ) {
        try {
            dictionaryFile.seek(ptr);
            dictionaryFile.writeLong(entry.ptr);
            dictionaryFile.seek(ptr + 8);
            dictionaryFile.writeInt(entry.size);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *  Reads an entry from the dictionary file.
    *
    *  @param ptr The place in the dictionary file where to start reading.
    */
    Entry readEntry( RandomAccessFile dict, long ptr ) {   
        try {
            dict.seek(ptr);
            long entry_ptr = dict.readLong();
            dict.seek(ptr + 8);
            int entry_size = dict.readInt();
            Entry entry = new Entry(entry_ptr, entry_size);
            return entry;
        } catch (IOException e) {
        }
        return null;
    }
    Entry readEntry( long ptr ) {   
        return readEntry(dictionaryFile, ptr);
    }


    // ==================================================================

    /**
     *  Writes the document names and document lengths to file.
    *
    * @throws IOException  { exception_description }
    */
    private void writeDocInfo() throws IOException {
        intermediate_info.add(foutName);
        FileOutputStream fout = new FileOutputStream(intermediate_info.get(intermediate_info.size()-1));
        for ( Map.Entry<Integer,String> entry : docNames.entrySet() ) {
            Integer key = entry.getKey();
            String docInfoEntry = key + ";" + entry.getValue() + ";" + docLengths.get(key) + "\n";
            fout.write( docInfoEntry.getBytes() );
        }
        fout.close();
    }


    /**
     *  Reads the document names and document lengths from file, and
    *  put them in the appropriate data structures.
    *
    * @throws     IOException  { exception_description }
    */
    private void readDocInfo() throws IOException {
        File file = new File( INDEXDIR + "/docInfo" );
        FileReader freader = new FileReader(file);
        try ( BufferedReader br = new BufferedReader(freader) ) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(";");
                docNames.put( new Integer(data[0]), data[1] );
                docLengths.put( new Integer(data[0]), new Integer(data[2]) );
            }
        }
        freader.close();
    }


    /**
     *  Write the index to files.
    */
    public void writeIndex() {
        int collisions = 0;
        try {
            // Write the 'docNames' and 'docLengths' hash maps to a file
            writeDocInfo();

            // Write the dictionary and the postings list

            // Sort keyset
            List<String> keys = new ArrayList<>(index.keySet());
            Collections.sort(keys);
            int total_keys = keys.size();
            int counter = 0;
            for (String key : keys) {
                ++counter;
                if (counter % 1000 == 0) {
                    System.err.print("\r" + String.format("%d%%", (100*counter)/total_keys));
                }

                // Find empty slot in dictionary
                long hash = hash(key);
                while (entryExists(hash)) {
                    hash = (hash + 1) % TABLESIZE;
                    ++collisions;
                }

                // Write to dataFile
                String data = key + " " + index.get(key).toString() + "\n";
                int size = writeData(data, free);

                // Write to dictionaryFile
                Entry entry = new Entry(free, size);
                writeEntry(entry, hash * ENTRY_SIZE);
                
                free += size;
            }
            System.err.println("\r100%");
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        System.err.println( collisions + " collisions." );

        merging.put(intermediate_dict.get(intermediate_dict.size()-1), false);
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

        public Merger() { }

        public void run() {
            findIndexesToMerge();
        }

        public void findIndexesToMerge() {
            for (int i = 0; i < intermediate_dict.size()-1; ++i) {
                // Only next one is allowed to merge
                if (!merging.get(intermediate_dict.get(i)) && !merging.get(intermediate_dict.get(i+1))) {
                    dictionaryFile1 = intermediate_dict.get(i);
                    dataFile1 = intermediate_data.get(i);
                    docInfoFile1 = intermediate_info.get(i);

                    dictionaryFile2 = intermediate_dict.get(i+1);
                    dataFile2 = intermediate_data.get(i+1);
                    docInfoFile2 = intermediate_info.get(i+1);

                    merging.put(dictionaryFile1, true);
                    merging.put(dictionaryFile2, true);
                    mergeIndexes();
                    break;
                }
            }
        }

        public void mergeIndexes() {
            System.err.println("Merging indexes " + dataFile1 + " and " + dataFile2);
            
            // Merge dictionaries and data
            String merge_dict = INDEXDIR + "/" + "merger_" + dictionaryFile1.split("/")[2] + "_" + dictionaryFile2.split("/")[2];
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
                    local_free = writeDataAndEntry(data, dict, dataString, local_free);
                }
                // Read remainder
                while ((line1 = br1.readLine()) != null) {
                    String dataString = line1 + "\n";
                    local_free = writeDataAndEntry(data, dict, dataString, local_free);
                }
                while ((line2 = br2.readLine()) != null) {
                    String dataString = line2 + "\n";
                    local_free = writeDataAndEntry(data, dict, dataString, local_free);
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
            for (int i = 0; i < intermediate_dict.size(); ++i) {
                if (intermediate_dict.get(i).equals(dictionaryFile2)) {
                    intermediate_dict.remove(i);
                    intermediate_data.remove(i);
                    intermediate_info.remove(i);
                    break;
                }
            }
            merging.put(dictionaryFile1, false);
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

    public long writeDataAndEntry(
        RandomAccessFile data, RandomAccessFile dict, String str, long ptr
    ) {
        // Write data
        try {
            data.seek(ptr);
            byte[] bytes = str.getBytes();
            data.write(bytes);
    
            // Write entry in dict
            long hash = hash(str.split(" ")[0]);
            while (entryExists(dict, hash)) hash = (hash + 1) % TABLESIZE;
            dict.seek(hash * ENTRY_SIZE);
            dict.writeLong(ptr);
            dict.seek(hash * ENTRY_SIZE + 8);
            dict.writeInt(bytes.length);
    
            ptr += bytes.length;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return ptr;
    }


    // ==================================================================


    /**
     *  Returns the postings for a specific term, or null
    *  if the term is not in the index.
    */
    public PostingsList getPostings( String token ) {
        //
        //  REPLACE THE STATEMENT BELOW WITH YOUR CODE
        //
        long startTime = System.currentTimeMillis();
        int collisions = 0;

        long hash = hash(token);
        while (entryExists(hash)) {
            Entry entry = readEntry(hash * ENTRY_SIZE);
            String[] data = readData(entry.ptr, entry.size).split(" ");
            if (data[0].equals(token)) {
                long startTime2 = System.currentTimeMillis();
                PostingsList list = new PostingsList(data[1].trim());
                long elapsedTime = System.currentTimeMillis() - startTime;
                long elapsedTime2 = System.currentTimeMillis() - startTime2;
                System.out.println("Found list for '" + token + "': " + elapsedTime + " ms");
                System.out.println("Collisions: " + collisions);
                System.out.println("Parsing time: " + elapsedTime2 + " ms");
                return list;
            }
            collisions++;
            // Try next slot
            ++hash;
        }
        
        // No match
        return null;
    }


    /**
     *  Inserts this token in the main-memory hashtable.
    */
    public void insert( String token, int docID, int offset ) {
        //
        //  YOUR CODE HERE
        //
        
        ++total_tokens;
        ++token_count;
        if (previous_docID != docID && token_count > TOKEN_LIMIT) {
            System.err.println("Writing block and resetting...");
            writeIndex();

            index.clear();
            docNames.clear();
            docLengths.clear();
            free = 0;
            createNewIntermediaryFiles();
            token_count = 1;
        }

        // If first occurrence of this word
        if (!index.containsKey(token)) index.put(token, new PostingsList());

        // Assume in-order insertions, current doc is last doc if previously seen
        // Doc not previously inserted
        if (index.get(token).size() == 0 || index.get(token).get(index.get(token).size()-1).docID != docID) index.get(token).insert(new PostingsEntry(docID));
        // Add position of token
        index.get(token).get(index.get(token).size()-1).addPosition(offset);

        previous_docID = docID;
    }

    public long hash(String token) {
        long hash = 0;
        for (char c : token.toCharArray()) {
            hash = (hash*50 + c) % TABLESIZE;
        }
        return hash;
    }

    public boolean entryExists(long hash) {
        return entryExists(dictionaryFile, hash);
    }

    public boolean entryExists(RandomAccessFile dict, long hash) {
        Entry entry = readEntry(dict, hash * ENTRY_SIZE);
        if (entry == null) return false;
        if (entry.ptr == 0 && entry.size == 0) return false;
        return true;
    }


    /**
     *  Write index to file after indexing is done.
    */
    public void cleanup() {
        System.err.println("Indexing finished");
        writeIndex();
        try {
            dictionaryFile.close();
            dataFile.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.err.println(index.keySet().size() + " unique words" );
        System.err.println(total_tokens + " total tokens");
        System.err.println("Waiting for all merger-threads to finish...");
        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        try {
            dictionaryFile = new RandomAccessFile(intermediate_dict.get(0), "rw" );
            dataFile = new RandomAccessFile(intermediate_data.get(0), "rw" );
        } catch ( IOException e ) {
            e.printStackTrace();
        }

        try {
            readDocInfo();
        } catch ( FileNotFoundException e ) {
        } catch ( IOException e ) {
            e.printStackTrace();
        }

        System.err.println( "done!" );
    }
}
