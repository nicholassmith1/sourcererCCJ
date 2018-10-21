package models;

public class ClonePair {
	String qid; // changed from long to string - Sarah
	String cid; // changed from long to string - Sarah
	public ClonePair(String qid, String candidateId) {
		super();
		this.qid = qid;
		this.cid = candidateId;
	}
}
