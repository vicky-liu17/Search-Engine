/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Johan Boye, 2017
 */

package ir;

import java.util.*;
import java.util.stream.Collectors;

public class PostingsList implements Iterable<PostingsEntry> {
    /**
     * The postings list
     */
    private ArrayList<PostingsEntry> list = new ArrayList<PostingsEntry>();

    /**
     * Number of postings in this list.
     */
    public int size() {
        return list.size();
    }

    /**
     * Returns the ith posting.
     */
    public PostingsEntry get(int i) {
        return list.get(i);
    }

    /**
     * If the entry with the same docID is found in the list, it merges their
     * positions.
     * If the entry is not found, it inserts the entry at the calculated index.
     */
    public void insert(PostingsEntry entry) {
        int low = 0; // Initialize the low index for binary search
        int high = list.size() - 1; // Initialize the high index for binary search
        int index = -1; // Initialize the index to store the insertion point

        // Binary search to find the insertion point
        while (low <= high) {
            int mid = low + (high - low) / 2; // Calculate the middle index
            if (list.get(mid).getDocID() == entry.getDocID()) { // If entry with same docID found
                index = mid; // Store the index of found entry
                break; // Exit the loop
            } else if (list.get(mid).getDocID() < entry.getDocID()) { // If entry's docID is greater
                low = mid + 1; // Move low to the right
            } else { // If entry's docID is smaller
                high = mid - 1; // Move high to the left
            }
        }

        // Not in the list
        if (index == -1) {
            index = low; // Set the index to the insertion point
        }

        // Add or merge entry
        if (index < list.size() && list.get(index).getDocID() == entry.getDocID()) { // If entry already exists
            list.get(index).addPositions(entry.getPositions()); // Merge positions
        } else { // If entry does not exist
            list.add(index, entry); // Insert entry at calculated index
        }
    }

    public String toString() {
        return list.stream()
                .map(PostingsEntry::toString)
                .collect(Collectors.joining(";"));
    }

    public PostingsList(String data) {
        list = Arrays.stream(data.split(";"))
                .map(PostingsEntry::new)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public PostingsList() {
    }

    @Override
    public Iterator<PostingsEntry> iterator() {
        return list.iterator();
    }

}
