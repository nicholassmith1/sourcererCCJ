package sourcerercc;

import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.*;
import java.text.ParseException;

import java.util.*;

import antlr.MethodExtractor;
import indexbased.SearchManager;
import cyclone.core.spi.CloneDetectorService;
import cyclone.core.spi.CloneListener;
import cyclone.core.spi.CloneSearch;

public class SourcererCC implements CloneDetectorService {
    
    private String work_dir = "/tmp/rawData/";
    private String top_work_dir = "/tmp/"; // anything other than partial index files in "/tmp/rawData" causes crashes
    private boolean debug = false;
    
    private final static Logger LOGGER = Logger.getLogger(SourcererCC.class.getName());
    static {
    	LOGGER.setLevel(Level.WARNING);
    }
    
    /* TODO - relocate */
	public static File[] get_cached_files(String work_dir, Path file) {
		final String s = Parser.sterilizePath(file.toString());
		
		File f = new File(work_dir);
		return f.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.split("#")[0].equals(s);
			}
		});
	}
    
    private Set<String> get_prev_sources() throws Exception {
    	FileInputStream fin = new FileInputStream(this.top_work_dir + File.separator + "prev_sources");
    	ObjectInputStream ois = new ObjectInputStream(fin);
    	return (Set<String>)ois.readObject();
    }
    
    private void set_prev_sources(Set<String> sources) {
    	try (FileOutputStream fout = new FileOutputStream(this.top_work_dir
    				+ File.separator + "prev_sources", false);
    			ObjectOutputStream oos = new ObjectOutputStream(fout)) {

    	    oos.writeObject(sources);
    	} catch (Exception ex) {
    	    ex.printStackTrace();
    	}
    }
    
    /**
     * Replaces the partial index for the specified file, but only if
     * the source file is newer than the partial index (indicating that
     * the partial index is stale).
     * @param file
     * @param attrs
     */
	public void updatePartialIndex(String source_file, boolean force_update) {
		Path file = new File(source_file).toPath();
		
		/* Check if the file type is something we can handle */
		MethodExtractor extractor = MethodExtractor.getInstance();
		String[] supportedExtensions = extractor.getSupportedExtensions();
		boolean canHandle = false;
		
		for (String suffix : supportedExtensions) {
			if (source_file.endsWith("." + suffix)) {
				canHandle = true;
				break;
			}
		}
		if (!canHandle) {
			return;
		}
		
		/* Add partial index of all interesting files to the working directory */
		LOGGER.fine("generating partial index for "
				+ file.toAbsolutePath().toString());
		
		boolean should_update = false;
		
		/*
		 * Update the cached files if the source file has
		 * changed more recently.
		 */
		long last_cache_touch = 0;
		File[] cached_files = SourcererCC.get_cached_files(this.work_dir, file);
		for (File b : cached_files) {
//			System.out.println("++++ " + b.getAbsolutePath());
			if (b.lastModified() > last_cache_touch) {
				last_cache_touch = b.lastModified();
			}
		}
		
		if (file.toFile().lastModified() > last_cache_touch) {
			LOGGER.fine("Need to update " + file.toString());
			should_update = true;
		}
		should_update = should_update || force_update;
		
		if (should_update) {
			/* 
			 * Remove all cached files. They could represent
			 * functions that have since been deleted.
			 */
			for (File b : cached_files) {
				b.delete();
			}
			
			try {
				Parser.printMethods(file.toString(), this.work_dir);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public String[] getSupportedExtensions() {
		MethodExtractor extractor = MethodExtractor.getInstance();
		return extractor.getSupportedExtensions();
	}
	
	@Override
	public void updateCache(String source_file) {
		updatePartialIndex(source_file, true);		
	}
    
	@Override
	public void cancel(CloneSearch spec) {
		// TODO Auto-generated method stub
		
	}
	
	private void trySearch(CloneSearch spec, CloneListener listener) throws IOException,
			ParseException, InterruptedException {

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
		Parser.printMethod2("NaN", "NaN", buffer.toString(), prt_index);
		
		if (this.debug) {
			System.out.println("");
			System.out.println("Generate partial index for search:");
			System.out.println("\t" + prt_index.toString());
		}
		
		
		/*
		 * Get all files that were previously visited searched. Any
		 * that were searched last time that weren't searched this time
		 * are stale and need to be removed to prevent false positives.
		 */
		Set<String> prev_visited;
		try {
			prev_visited = this.get_prev_sources();
			for (String ss : prev_visited) {
				LOGGER.finer("prev_visited = " + ss);
			}
		} catch (Exception ex) {
			LOGGER.fine("Unable to determine prior execution source: recreating");
			FileUtils.cleanDirectory(new File(this.work_dir));
			prev_visited = new HashSet<String>();
		}
		
		/* Generate partial indices for source files */
		for (String s : spec.source_files) {			
	        updatePartialIndex(s, false);
		}
		
		/* Clean stale partial indices. */
		Set<String> visited = new HashSet<>(spec.source_files);
		prev_visited.removeAll(visited);
		prev_visited.forEach(new Consumer<String>() {
			@Override
			public void accept(String filename) {
				File[] stale_files = SourcererCC.get_cached_files(work_dir,
						Paths.get(filename));
				
				for (File f : stale_files) {
					LOGGER.finer("TODO - MUST REMOVE FILE " + f.toString());
					f.delete();
				}
			}
		});
		
		/* Record all sources discovered this execution */
		set_prev_sources(visited);
    	
		/* Now, actually perform the search */
    	SearchManager sm = new SearchManager("/tmp/");
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
    	sm.genReportV2();		
	}
	

	@Override
	public void search(CloneSearch spec, CloneListener listener) {
		try {
			trySearch(spec, listener);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
