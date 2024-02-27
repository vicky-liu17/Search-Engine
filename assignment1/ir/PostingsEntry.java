/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Johan Boye, 2017
 */

package ir;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class PostingsEntry implements Comparable<PostingsEntry>, Serializable {
    public int docID;

    public double score = 0;

    public ArrayList<Integer> positions = new ArrayList<>();

    /**
     * PostingsEntries are compared by their score (only relevant
     * in ranked retrieval).
     *
     * The comparison is defined so that entries will be put in
     * descending order.
     */
    public int compareTo(PostingsEntry other) {
        return Double.compare(other.score, score);
    }

    public PostingsEntry() {
    }

    public PostingsEntry(int docID) {
        this.docID = docID;
    }

    public PostingsEntry(int docID, int position) {
        this.docID = docID;
        addPosition(position);
    }

    public void addPosition(int position) {
        positions.add(position);
    }

    public void setPositions(ArrayList<Integer> positions) {
        this.positions = positions;
    }

    public PostingsEntry(int docID, ArrayList<Integer> positions) {
        this.docID = docID;
        this.positions.addAll(positions);
    }

    public void addPositions(ArrayList<Integer> positions) {
        this.positions.addAll(positions);
    }

    public ArrayList<Integer> getPositions() {
        return positions;
    }

    public int getDocID() {
        return docID;
    }

    public double getScore() {
        return score;
    }

    public String toString() {
        String positionsString = positions.stream()
                .map(Object::toString)
                .collect(Collectors.joining(","));
        return docID + ":" + positionsString;
    }

    public PostingsEntry(String data) {
        String[] fields = data.split(":");
        this.docID = Integer.parseInt(fields[0]);

        String[] wordStringPositions = fields[1].split(",");
        this.positions = new ArrayList<>(wordStringPositions.length);
        for (String pos : wordStringPositions) {
            this.positions.add(Integer.parseInt(pos));
        }
    }

}
