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
    } else if (queryType == QueryType.RANKED_QUERY) {
        result = rankedRetrieval(query.queryterm);
    }

    return result;
}

 /* --------------------------------------------- */

    // 2.2 and 2.1 and 2.5.2 tf_idf (everything between the lines)
    // changed for 3.1

    private PostingsList rankedRetrieval(List<Query.QueryTerm> queryTerms) {

        // Get all the searched terms
        List<String> terms = queryTerms.stream().map(queryTerm -> queryTerm.term).toList();

        // map: <doc, score>
        // Create empty dictionary to hold document scores
        Map<PostingsEntry, Double> docScores = new HashMap<>();

        // Loop through all search query terms, for each term,
        // changed the following 2 lines for 3.1
        // used to be:
        // for ( term : terms ) {
        for (Query.QueryTerm queryTerm : queryTerms) {
            String term = queryTerm.term;
            // Retrieve documents containing the search term
            // "allDocuments" is a list that has all the documents that have that term
            PostingsList allDocuments = index.getPostings(term);

            // Loop through all retrieved documents
            // for each document in this list (of documents that have the term)
            for (PostingsEntry document : allDocuments.getList()) {

                // Calculate the score for the document
                // 3.1: multiply by queryTerm.weight
                double score = calculateTfIdfScore(document, term) * queryTerm.weight;

                // if the document already exists in the list of scores, add the SCORE to it
                // only
                if (docScores.containsKey(document)) {
                    docScores.put(document, docScores.get(document) + score);
                }
                // otherwise, put the document in the list of score and its score
                else {
                    docScores.put(document, score);
                }
            }
        }

        // a list called results which will be the documents that have the term and
        // ranked
        PostingsList results = new PostingsList();

        for (String term : terms) {

            // Retrieve documents containing the search term
            PostingsList allDocuments = index.getPostings(term);

            // for every doc in this list of doc, if it's not in results list, add it
            for (PostingsEntry document : allDocuments.getList()) {
                    results.insert(document);
            }
        }

        // for every doc in this list of doc ==> doc Score / doc length
        for (PostingsEntry document : docScores.keySet()) {
            document.score = docScores.get(document) / index.docLengths.get(document.docID);
        }

        // sort the docs by score and put them in results list
        results.sortByScore();
        return results;
    }

    // tf * idf
    // later, it will be divided by length of the doc (in rankedTfIdf method)
    private double calculateTfIdfScore(PostingsEntry document, String term) {
        double tf = getTF(document, term);
        double idf = getIDF(term);
        return tf * idf;
    }

    // calculating the term frequency : number of occurrences of term in doc
    // how many positions that term has in the doc aka tf
    public double getTF(PostingsEntry document, String term) {
        return document.positions.size();
    }

    // calculating Inverse Document Frequency : idf = log(N/df)
    private double getIDF(String term) {
        double N = index.docNames.size(); // number of ALL the documents in the corpus
        double df = index.getPostings(term).size(); // number of documents in the corpus that contain the term
        double idf = Math.log(N / df);

        // this print is for task 2.3
        // System.out.printf(" idf - %s: %f\n", term, Math.round(idf * 10000.0) /
        // 10000.0);

        return idf; // calculating the idf
    }

    /* --------------------------------------------- */



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