/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Johan Boye, 2017
 *
 *   Edited by Hasti Mohebali Zadeh, 2023
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
     *  Searches the index for postings matching the query.
     *  @return A postings list representing the result of the query.
     */
    public PostingsList search(Query query, QueryType queryType, RankingType rankingType, NormalizationType normType) {

        List<PostingsList> postings = getPostingsLists(query);

        if (postings == null || postings.isEmpty()) {
            return new PostingsList(); 
        }
        
        PostingsList result = null;

        if (queryType == QueryType.INTERSECTION_QUERY) {
            result = query(postings,true);
        } else if (queryType == QueryType.PHRASE_QUERY) {
            result = query(postings,false);
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
            result = isIntersectionQuery ? intersect(result, iterator.next()) : positionalIntersect(result, iterator.next());
        }
        return result;
    }

    public PostingsList positionalIntersect(PostingsList postingsList1, PostingsList postingsList2) {
        final PostingsList answer = new PostingsList();
    
        Iterator<PostingsEntry> iterator1 = postingsList1.iterator();
        Iterator<PostingsEntry> iterator2 = postingsList2.iterator();
    
        // Initialize iterators and entries
        PostingsEntry entry1 = null;
        PostingsEntry entry2 = null;
        if (iterator1.hasNext()) {
            entry1 = iterator1.next();
        }
        if (iterator2.hasNext()) {
            entry2 = iterator2.next();
        }
    
        // Iterate through both postings lists
        while (entry1 != null && entry2 != null) {
            int docId1 = entry1.docID;
            int docId2 = entry2.docID;
    
            if (docId1 == docId2) {
                ArrayList<Integer> l = new ArrayList<>();
                Iterator<Integer> positionIterator1 = entry1.positions.iterator();
                Iterator<Integer> positionIterator2 = entry2.positions.iterator();
                Integer pos1 = null;
                Integer pos2 = null;
                if (positionIterator1.hasNext()) {
                    pos1 = positionIterator1.next();
                }
                if (positionIterator2.hasNext()) {
                    pos2 = positionIterator2.next();
                }
    
                // Iterate through positions of both entries
                while (pos1 != null && pos2 != null) {
                    if (pos2 - pos1 == 1) {
                        l.add(pos2);
                        if (positionIterator1.hasNext()) {
                            pos1 = positionIterator1.next();
                        } else {
                            pos1 = null;
                        }
                        if (positionIterator2.hasNext()) {
                            pos2 = positionIterator2.next();
                        } else {
                            pos2 = null;
                        }
                    } else if (pos2 > pos1) {
                        if (positionIterator1.hasNext()) {
                            pos1 = positionIterator1.next();
                        } else {
                            pos1 = null;
                        }
                    } else {
                        if (positionIterator2.hasNext()) {
                            pos2 = positionIterator2.next();
                        } else {
                            pos2 = null;
                        }
                    }
                }
    
                // Add to answer if adjacent positions found
                if (!l.isEmpty()) {
                    answer.insert(new PostingsEntry(docId1, l));
                }
                // Move to next entries
                if (iterator1.hasNext()) {
                    entry1 = iterator1.next();
                } else {
                    entry1 = null;
                }
                if (iterator2.hasNext()) {
                    entry2 = iterator2.next();
                } else {
                    entry2 = null;
                }
            } else if (docId1 < docId2) {
                if (iterator1.hasNext()) {
                    entry1 = iterator1.next();
                } else {
                    entry1 = null;
                }
            } else {
                if (iterator2.hasNext()) {
                    entry2 = iterator2.next();
                } else {
                    entry2 = null;
                }
            }
        }
        return answer;
    }  
    
    public PostingsList intersect(PostingsList p1, PostingsList p2) {
        final PostingsList answer = new PostingsList();
        
        Iterator<PostingsEntry> iterator1 = p1.iterator();
        Iterator<PostingsEntry> iterator2 = p2.iterator();
        
        PostingsEntry e1 = iterator1.next();
        PostingsEntry e2 = iterator2.next();
        
        while (iterator1.hasNext() && iterator2.hasNext()) {
            int docId1 = e1.docID;
            int docId2 = e2.docID;
            
            if (docId1 == docId2) {
                answer.insert(e1);
                e1 = iterator1.next();
                e2 = iterator2.next();
            } else if (docId1 < docId2) {
                e1 = iterator1.next();
            } else {
                e2 = iterator2.next();
            }
        }
        return answer;
    }

    
}