package sourcerercc;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.Iterator;
import java.util.logging.Logger;
import java.util.stream.Stream;

import cyclone.core.spi.CloneListener;
import cyclone.core.spi.CloneSearch;
import cyclone.core.spi.CloneSearchStatusListener;
import indexbased.SearchManager;

public class SearchHandler implements Runnable {
	private boolean debug = false;
	private CloneSearch spec;
	private CloneListener listener;
	private CloneSearchStatusListener statusListener;
	
	public SearchHandler(CloneSearch spec, CloneListener listener,
			CloneSearchStatusListener statusListener) {
		this.spec = spec;
		this.listener = listener;
		this.statusListener = statusListener;
	}
	
	private void trySearch(CloneSearch spec, CloneListener listener) throws IOException,
			ParseException, InterruptedException {
		CacheManager cacheManager = CacheManager.getInstance();
		StringBuffer buffer = new StringBuffer();
		
		/* Get the text from the specified target file */
		try (Stream<String> lines = Files.lines(Paths.get(spec.target_file))) {
			int t_start = Integer.max((int)spec.start_line - 1, 0);
		    
		    Iterator<String> iter = lines.skip(t_start).iterator();
		    String line;
		    for (int i = t_start; i < (int)spec.end_line && iter.hasNext(); i++) {
		    	line = iter.next();
			    buffer.append(line);
				if (this.debug) {
					System.out.println("\t" + line);
				}
		    }
		}
		
		/* Generate the partial index for the target code chunk. */
		StringWriter prt_index = new StringWriter();
		String target = new File(spec.target_file).toPath().toAbsolutePath().normalize().toString();
		target = Parser.sterilizePath(target);
//		Parser.printMethod2(target, spec.start_line + "," + spec.end_line, buffer.toString(), prt_index);
		Parser.printMethod2("NaN", "NaN", buffer.toString(), prt_index);
		
		if (this.debug) {
			System.out.println("");
			System.out.println("Generate partial index for search:");
			System.out.println("\t" + prt_index.toString());
		}
		
		/*
		 * Generate the partial index for all files in source files set.
		 * The CacheManager helps do this optimally, but ignoring files
		 * if they have a corresponding partial index that is newer than
		 * that file.
		 */
		cacheManager.updatePartialIndices(spec.source_files);
    	
		/* Now, actually perform the search */
    	SearchManager sm = new SearchManager("/tmp/", spec, listener);
    	sm.initIndexEnv();
    	sm.doIndex();
    	sm.initSearchEnv();
    	sm.findCandidates(prt_index.toString());
    	
    	while (true) {
            if (SearchManager.queryBlockQueue.size() == 0
                    && SearchManager.queryCandidatesQueue.size() == 0
                    && SearchManager.verifyCandidateQueue.size() == 0
                    && SearchManager.reportCloneQueue.size() == 0) {
            	if (debug)
            		System.out.println("shutting down QBQ, " + (System.currentTimeMillis()));
                SearchManager.queryBlockQueue.shutdown();
                
                if (debug)
                	System.out.println("shutting down QCQ, " + System.currentTimeMillis());
                SearchManager.queryCandidatesQueue.shutdown();
                
                if (debug)
                	System.out.println("shutting down VCQ, " + System.currentTimeMillis());
                SearchManager.verifyCandidateQueue.shutdown();
                
                if (debug)
                	System.out.println("shutting down RCQ, " + System.currentTimeMillis());
                SearchManager.reportCloneQueue.shutdown();
                break;
            } else {
                Thread.sleep(2 * 1000);
            }
        }
    	/* Debug only */
//    	sm.genReportV2();
	}
	
	@Override
	public void run() {
		try {
			trySearch(spec, listener);
		} catch (IOException | ParseException | InterruptedException e) {
			/* 
			 * We can't do anything about the IO or Parse exceptions,
			 * and the interrupt exception is expected (for cancelling
			 * the search)
			 */
			;
		} finally {
			/* Regardless of outcome (interrupted, etc), notify completion */
			statusListener.notifyComplete(spec);
		}
	}

}
