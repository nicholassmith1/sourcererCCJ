package models;

public class ClonePair {
	String qid; // changed from long to string - Sarah
	String cid; // changed from long to string - Sarah
	
	double confidence;
	double time;
	
	public ClonePair(String qid, String candidateId,
			double confidence, double time) {
		super();
		this.qid = qid;
		this.cid = candidateId;
		this.confidence = confidence;
		this.time = time;
	}
}
