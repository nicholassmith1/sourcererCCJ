package models;

public class CandidatePair {
	QueryBlock queryBlock;
	String candidateTokens;
	CandidateSimInfo simInfo;
	int computedThreshold;
	int candidateSize;
	String candidateId; // changed from long to string - Sarah
//	long
	
	public CandidatePair(QueryBlock queryBlock, String candidateTokens,
			CandidateSimInfo simInfo, int computedThreshold, int candidateSize, String candidateId) {
		super();
		this.queryBlock = queryBlock;
		this.candidateTokens = candidateTokens;
		this.simInfo = simInfo;
		this.computedThreshold = computedThreshold;
		this.candidateSize = candidateSize;
		this.candidateId = candidateId;
	}
}
