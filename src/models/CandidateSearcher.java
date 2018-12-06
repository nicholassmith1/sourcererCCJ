package models;

import java.io.IOException;
import java.util.NoSuchElementException;

import indexbased.SearchManager;
import indexbased.TermSearcher;

public class CandidateSearcher implements IListener, Runnable {
	
	private final SearchManager searchManager;
	
	public CandidateSearcher(SearchManager searchManager) {
		this.searchManager = searchManager;
	}
	@Override
	public void run() {
		try {
			QueryBlock queryBlock = searchManager.queryBlockQueue.remove();
			this.searchCandidates(queryBlock);
		} catch (NoSuchElementException e) {
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void searchCandidates(QueryBlock queryBlock) throws IOException,
			InterruptedException {
//		System.out.println("spin up searchCandidates");
		
		TermSearcher termSearcher = new TermSearcher(searchManager);
		searchManager.searcher.search(queryBlock, termSearcher);
		QueryCandidates qc = new QueryCandidates();
		qc.queryBlock = queryBlock;
		qc.termSearcher = termSearcher;
		searchManager.queryCandidatesQueue.put(qc);
		
//		System.out.println("spin down searchCandidates");
	}

}
