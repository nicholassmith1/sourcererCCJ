package models;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import org.apache.lucene.document.Document;

import indexbased.CustomCollectorFwdIndex;
import indexbased.SearchManager;
import indexbased.TermSearcher;

public class CandidateProcessor implements IListener, Runnable {
	
	private final SearchManager searchManager;
	
	public CandidateProcessor(SearchManager searchManager) {
		this.searchManager = searchManager;
	}

    @Override
    public void run() {
    	searchManager.incrementStayAlive();
        try {
            // System.out.println( "QCQ size: "+
            // SearchManager.queryCandidatesQueue.size() + Util.debug_thread());
            QueryCandidates qc = searchManager.queryCandidatesQueue.remove();
            this.processResultWithFilter(qc.termSearcher, qc.queryBlock);
        } catch (NoSuchElementException e) {
            // e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        searchManager.decrementStayAlive();
    }

    private void processResultWithFilter(TermSearcher result, QueryBlock queryBlock) throws InterruptedException {	
        // System.out.println("HERE, thread_id: " + Util.debug_thread() +
        // ", query_id "+ queryBlock.getId());
        Map<Long, CandidateSimInfo> codeBlockIds = result.getSimMap();
        if (searchManager.isGenCandidateStats) {
            searchManager.updateNumCandidates(codeBlockIds.size());
        }
        for (Entry<Long, CandidateSimInfo> entry : codeBlockIds.entrySet()) {
            Document doc = null;
            try {
                doc = searchManager.searcher.getDocument(entry.getKey());
                CandidateSimInfo simInfo = entry.getValue();
                String candidateId = doc.get("id"); // changed to String from long
                String functionIdCandidate = candidateId;
                
                if ((candidateId.equals(queryBlock.getId()))) {
                    // || (functionIdCandidate == queryBlock.getFunctionId())) {
                    continue; // we reject the candidate
                }
                int newCt = -1;
                int candidateSize = Integer.parseInt(doc.get("size"));
                if (candidateSize > queryBlock.getSize()) {
                    newCt = Integer.parseInt(doc.get("ct"));
                }
                CustomCollectorFwdIndex collector = searchManager.fwdSearcher
                        .search(doc);
                List<Integer> blocks = collector.getBlocks();
                
                if (!blocks.isEmpty()) {
                    if (blocks.size() == 1) {
                        Document document = searchManager.fwdSearcher
                                .getDocument(blocks.get(0));
                        String tokens = document.get("tokens");
                        CandidatePair candidatePair = null;
                        if (newCt != -1) {
                            candidatePair = new CandidatePair(queryBlock, tokens, simInfo, newCt, candidateSize, candidateId);
                        } else {
                            candidatePair = new CandidatePair(queryBlock, tokens, simInfo, queryBlock.getComputedThreshold(), candidateSize, candidateId);
                        }
                        searchManager.verifyCandidateQueue.put(candidatePair);
                        entry = null;
                    } else {
                        System.out
                                .println("ERROR: more that one doc found. some error here.");
                    }

                } else {
                    System.out.println("document not found in fwd index");
                }
            } catch (NumberFormatException e) {
                System.out.println(e.getMessage() + ", cant parse id for "
                        + doc.get("id"));
            } catch (IOException e) {
                System.out.println(e.getMessage()
                        + ", can't find document from searcher"
                        + entry.getKey());
            }
        }
        result = null;
    }

}
