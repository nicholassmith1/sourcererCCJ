package models;

import java.io.File;
import java.util.NoSuchElementException;

import cyclone.core.spi.CloneListener;
import cyclone.core.spi.CloneSearch;
import indexbased.SearchManager;

public class CloneReporter implements IListener, Runnable {
	private final CloneSearch spec;
	private final CloneListener cloneListener;
	private final SearchManager searchManager;
	
	public CloneReporter(SearchManager searchManager, CloneSearch spec, CloneListener cloneListener) {
		this.searchManager = searchManager;
		this.cloneListener = cloneListener;
		this.spec = spec;
	}
	
	@Override
	public void run() {
		try {
			ClonePair cp = searchManager.reportCloneQueue.remove();
			this.reportClone(cp);
		} catch (NoSuchElementException e) {
		}
		
	}
	
	/* Tis very brittle. Expecting a serialized absolute file path
	 * + "#" + "start_line,end_line" for the query and clone ids.
	 */
	private String idToFilePath(String id) {
		String filePath = id.split("#")[0];	
		filePath = filePath.replaceAll("\\$", File.separator);
		
		return filePath;
	}
	
	private int idToStartLine(String id) {
		String lines = id.split("#")[1];
	
		return Integer.parseInt(lines.split(",")[0]);
	}
	
	private int idToEndLine(String id) {
		String lines = id.split("#")[1];
		lines = lines.split(".txt")[0];
		
		return Integer.parseInt(lines.split(",")[1]);
	}

	private void reportClone(ClonePair cp) {
//		System.out.println("$$$$$$$$$$ qid=" + cp.qid + " cid=" + cp.cid);
		
		/* Extract the filePath and line numbers of the detect clone */
		try {		
			String filePath = idToFilePath(cp.cid);
			int start = idToStartLine(cp.cid);
			int end = idToEndLine(cp.cid);
			
			/*
			 * If the candidate clone is in the same file, and one exists
			 * purely within the bounds of the other, we consider them
			 * to be the same code.
			 */
			if (spec.target_file.equals(filePath) &&
					(((spec.start_line >= start) && spec.end_line <= end) ||
					((start >= spec.start_line) && (end <= spec.end_line)))) {
				return;
			}
			
			/* TODO - fix the time calculations */
			cloneListener.notifyCloneDetected(spec, filePath, start, end,
					cp.confidence, "sourcererCC", 0);
		} catch (Exception e) {
			/* fatal error. Did the MethodExtractor conform to specs? */
			e.printStackTrace();
			return;
		}
		
		searchManager.updateClonePairsCount(1);
		
		String text = cp.cid;
		searchManager.searchResult.add(text); //added by - sarah
	}

}
