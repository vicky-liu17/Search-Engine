/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  


package ir;
import java.util.HashMap;


/**
 *   Implements an inverted index as a Hashtable from words to PostingsLists.
 */
public class HashedIndex implements Index {


    /** The index as a hashtable. */
    private HashMap<String,PostingsList> index = new HashMap<String,PostingsList>();


    /**
     * * Inserts this token in the hashtable.
     * */
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
     *  Returns the postings for a specific term, or null
     *  if the term is not in the index.
     */
    public PostingsList getPostings( String token ) {
        return index.get(token);
    }

    /**
     *  No need for cleanup in a HashedIndex.
     */
    public void cleanup() {
    }
}
