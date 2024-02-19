/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.io.Serializable;

public class PostingsEntry implements Comparable<PostingsEntry>, Serializable {

    public int docID;
    public double score = 0;
    public final ArrayList<Integer> positions = new ArrayList<>();

    /**
     *  PostingsEntries are compared by their score (only relevant
     *  in ranked retrieval).
     *
     *  The comparison is defined so that entries will be put in 
     *  descending order.
     */
    public int compareTo( PostingsEntry other ) {
       return Double.compare( other.score, score );
    }

    public PostingsEntry(int docID) {
        this.docID = docID;
    }

    public PostingsEntry(int docID, int position){
        this.docID = docID;
        addPosition(position);
    }

    public void addPosition(int position) {
        positions.add(position);
    }

    public ArrayList<Integer> getPositions() {
        return positions;
    }

    public int getDocID(){
        return docID;
    }

    public double getScore(){ 
        return score;
    }
    
}

