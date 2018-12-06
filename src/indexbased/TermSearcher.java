/**
 * 
 */
package indexbased;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import models.CandidateSimInfo;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;

import utility.Util;

/**
 * @author vaibhavsaini
 * 
 */
public class TermSearcher {
	private String searchTerm;
	private int freqTerm;
	private IndexReader reader;
	private Map<Long, CandidateSimInfo> simMap;
	private int querySize;
	private int computedThreshold;
	private final SearchManager searchManager;
	
	private final static Logger LOGGER = Logger.getLogger(SearchManager.class.getName());
    static {
    	LOGGER.setLevel(Level.WARNING);
    }

	public TermSearcher(SearchManager searchManager) {
		this.simMap = new HashMap<Long, CandidateSimInfo>();
		this.searchManager = searchManager;
	}

	public synchronized void searchWithPosition(int queryTermsSeen) {
		if(null!=this.reader){
			if(null!= this.reader.getContext()){
				if(null!=this.reader.getContext().leaves()){
					Term term = new Term("tokens", this.searchTerm);
					for (AtomicReaderContext ctx : this.reader.getContext().leaves()) {
						int base = ctx.docBase;
						// SpanTermQuery spanQ = new SpanTermQuery(term);
						try {
							DocsAndPositionsEnum docEnum = MultiFields
									.getTermPositionsEnum(ctx.reader(),
											MultiFields.getLiveDocs(ctx.reader()),
											"tokens", term.bytes());
							if (null != docEnum) {
								int doc = DocsEnum.NO_MORE_DOCS;
								while ((doc = docEnum.nextDoc()) != DocsEnum.NO_MORE_DOCS) {
									long docId = doc + base;
									CandidateSimInfo simInfo = null;
									if (this.simMap.containsKey(docId)) {
										simInfo = this.simMap.get(docId);
										simInfo.similarity = simInfo.similarity
												+ Math.min(freqTerm, docEnum.freq());

									} else {
										simInfo = new CandidateSimInfo();
										simInfo.similarity = Math.min(freqTerm,
												docEnum.freq());
										//System.out.println("before putting in simmap "+ Util.debug_thread());
										this.simMap.put(docId, simInfo);
										//System.out.println("after putting in simmap "+ Util.debug_thread());
									}
									simInfo.queryMatchPosition = queryTermsSeen;
									int candidatePos = docEnum.nextPosition();
									simInfo.candidateMatchPosition = candidatePos
											+ docEnum.freq();
									if (simInfo.candidateSize == 0) {
										simInfo.candidateSize = Integer
												.parseInt(searchManager.searcher
														.getDocument(docId).get("size"));
									}
									
//									System.out.println("TermSearcher " + this.simMap.get(docId).similarity
//											+ " " + this.querySize + " " + queryTermsSeen + " "
//											+ simInfo.candidateSize + " " + simInfo.candidateMatchPosition
//											+ " " + this.computedThreshold);
									
									if (!Util.isSatisfyPosFilter(
											this.simMap.get(docId).similarity,
											this.querySize, queryTermsSeen,
											simInfo.candidateSize,
											simInfo.candidateMatchPosition,
											this.computedThreshold)) {
									//	System.out.println("before removing in simmap "+ Util.debug_thread());
										this.simMap.remove(docId);
										//System.out.println("after removing in simmap "+ Util.debug_thread());
									}
//									else {
//										System.out.println("+++++++ success!");
//									}
								}
							}else{
								if(SearchManager.loggingMode.equals("D")){
									LOGGER.severe("docEnum is null, " + base + ", term: "+ this.searchTerm + Util.debug_thread());
								}
							}
						} catch (Exception e) {
							e.printStackTrace();
							LOGGER.severe("exception caught " + e.getMessage() + Util.debug_thread() + " search term:" + this.searchTerm);
						}

					}
				}else{
					if(SearchManager.loggingMode.equals("D")){
						LOGGER.severe("leaves are null, "+ this.searchTerm + Util.debug_thread());
					}
				}
			}else{
				if(SearchManager.loggingMode.equals("D")){
					LOGGER.severe("getContext is null, "+ this.searchTerm + Util.debug_thread());
				}
			}
		}else{
			if(SearchManager.loggingMode.equals("D")){
				LOGGER.severe("this.reader is null, "+ this.searchTerm + Util.debug_thread());
			}
		}
	}

	/**
	 * @return the searchTerm
	 */
	public String getSearchTerm() {
		return searchTerm;
	}

	/**
	 * @param searchTerm
	 *            the searchTerm to set
	 */
	public void setSearchTerm(String searchTerm) {
		//System.out.println(Util.debug_thread() + "setting searchTerm: "+ searchTerm);
		this.searchTerm = searchTerm;
	}

	/**
	 * @return the freqTerm
	 */
	public int getFreqTerm() {
		return freqTerm;
	}

	/**
	 * @param freqTerm
	 *            the freqTerm to set
	 */
	public void setFreqTerm(int freqTerm) {
		this.freqTerm = freqTerm;
	}

	/**
	 * @return the reader
	 */
	public IndexReader getReader() {
		return reader;
	}

	/**
	 * @param reader
	 *            the reader to set
	 */
	public void setReader(IndexReader reader) {
		this.reader = reader;
	}

	/**
	 * @return the simMap
	 */
	public Map<Long, CandidateSimInfo> getSimMap() {
		return simMap;
	}

	/**
	 * @param simMap
	 *            the simMap to set
	 */
	public void setSimMap(ConcurrentMap<Long, CandidateSimInfo> simMap) {
		this.simMap = simMap;
	}

	public void setQuerySize(int size) {
		this.querySize = size;

	}

	public void setComputedThreshold(int computedThreshold) {
		this.computedThreshold = computedThreshold;

	}

}
