/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Johan Boye, 2017
 */

package ir;

import java.util.*;

public class Searcher {
    /** The index to be searched by this Searcher. */
    final Index index;

    /** The k-gram index to be searched by this Searcher */
    final KGramIndex kgIndex;

    /** Constructor */
    public Searcher(Index index, KGramIndex kgIndex) {
        this.index = index;
        this.kgIndex = kgIndex;
    }

    /**
     * Searches the index for postings matching the query.
     * 
     * @return A postings list representing the result of the query.
     */
    public PostingsList search(Query query, QueryType queryType, RankingType rankingType, NormalizationType normType) {

        List<PostingsList> postings = getPostingsLists(query);

        if (postings == null || postings.isEmpty()) {
            return new PostingsList();
        }

        PostingsList result = null;

        if (queryType == QueryType.INTERSECTION_QUERY) {
            result = query(postings, true);
        } else if (queryType == QueryType.PHRASE_QUERY) {
            result = query(postings, false);
        }
        return result;
    }

    public List<PostingsList> getPostingsLists(Query query) {
        List<PostingsList> postings = new ArrayList<>();
        for (int i = 0; i < query.size(); ++i) {
            PostingsList list = index.getPostings(query.queryterm.get(i).term);
            if (list == null) {
                list = new PostingsList();
            }
            postings.add(list);
        }
        return postings;
    }

    public PostingsList query(List<PostingsList> postings, boolean isIntersectionQuery) {
        if (postings == null || postings.isEmpty()) {
            return new PostingsList();
        }
        Iterator<PostingsList> iterator = postings.iterator();
        PostingsList result = iterator.next();
        while (iterator.hasNext()) {
            result = isIntersectionQuery ? intersect(result, iterator.next())
                    : positionalIntersect(result, iterator.next());
        }
        return result;
    }

    public PostingsList positionalIntersect(PostingsList postingsList1, PostingsList postingsList2) {
        // Create a new PostingsList to store the intersection results.
        final PostingsList answer = new PostingsList();

        // Initialize iterators for both input postings lists.
        Iterator<PostingsEntry> iterator1 = postingsList1.iterator();
        Iterator<PostingsEntry> iterator2 = postingsList2.iterator();

        // Initialize variables to hold the current entry from each iterator.
        PostingsEntry entry1 = null;
        PostingsEntry entry2 = null;
        // Advance each iterator to its first element, if possible.
        if (iterator1.hasNext()) {
            entry1 = iterator1.next();
        }
        if (iterator2.hasNext()) {
            entry2 = iterator2.next();
        }

        // Continue as long as both postings lists have elements.
        while (entry1 != null && entry2 != null) {
            // Compare the document IDs of the current entries.
            int docId1 = entry1.docID;
            int docId2 = entry2.docID;

            // If the document IDs match, we need to check the positions for a phrase match.
            if (docId1 == docId2) {
                // Temporary list to store positions where the terms appear next to each other.
                ArrayList<Integer> l = new ArrayList<>();
                // Initialize position iterators for the current entries.
                Iterator<Integer> positionIterator1 = entry1.positions.iterator();
                Iterator<Integer> positionIterator2 = entry2.positions.iterator();
                Integer pos1 = null;
                Integer pos2 = null;
                // Advance each position iterator to its first element, if possible.
                if (positionIterator1.hasNext()) {
                    pos1 = positionIterator1.next();
                }
                if (positionIterator2.hasNext()) {
                    pos2 = positionIterator2.next();
                }

                // Continue as long as both position lists have elements.
                while (pos1 != null && pos2 != null) {
                    // Check if the positions are consecutive (i.e., form a phrase).
                    if (pos2 - pos1 == 1) {
                        // If consecutive, add the position from the second list to the result.
                        l.add(pos2);
                        // Advance both position iterators.
                        pos1 = positionIterator1.hasNext() ? positionIterator1.next() : null;
                        pos2 = positionIterator2.hasNext() ? positionIterator2.next() : null;
                    } else if (pos2 > pos1) {
                        // If the position in the first list is before the second, advance the first
                        // iterator.
                        pos1 = positionIterator1.hasNext() ? positionIterator1.next() : null;
                    } else {
                        // If the position in the second list is before the first, advance the second
                        // iterator.
                        pos2 = positionIterator2.hasNext() ? positionIterator2.next() : null;
                    }
                }

                // If any consecutive positions were found, add a new entry to the result list.
                if (!l.isEmpty()) {
                    answer.insert(new PostingsEntry(docId1, l));
                }
                // Move to the next entries in both postings lists.
                entry1 = iterator1.hasNext() ? iterator1.next() : null;
                entry2 = iterator2.hasNext() ? iterator2.next() : null;
            } else if (docId1 < docId2) {
                // If the first document ID is less than the second, move to the next entry in
                // the first list.
                entry1 = iterator1.hasNext() ? iterator1.next() : null;
            } else {
                // If the second document ID is less than the first, move to the next entry in
                // the second list.
                entry2 = iterator2.hasNext() ? iterator2.next() : null;
            }
        }
        return answer;
    }

    public PostingsList intersect(PostingsList list1, PostingsList list2) {
        // Create a new PostingsList to store the intersection result
        PostingsList answer = new PostingsList();

        // Initialize two pointers to iterate over list1 and list2
        int index1 = 0, index2 = 0;

        // Iterate through both lists until one of them is fully processed
        while (index1 < list1.size() && index2 < list2.size()) {
            // If the document IDs at the current positions are equal, add the document ID
            // to the result and move both pointers forward
            if (list1.get(index1).docID == list2.get(index2).docID) {
                answer.insert(new PostingsEntry(list1.get(index1).docID));
                index1++;
                index2++;
            } else {
                // If the document ID in list1 is smaller than in list2, move the pointer of
                // list1 forward
                // Otherwise, move the pointer of list2 forward
                if (list1.get(index1).docID < list2.get(index2).docID) {
                    index1++;
                } else {
                    index2++;
                }
            }
        }

        // Return the intersection result
        return answer;
    }

}