package sourcerercc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;

import antlr.MethodExtractor;

public class CacheManager {
	private final static Logger LOGGER = Logger.getLogger("sourcererCC");
    private final static String work_dir = "/tmp/rawData/";
    private final static String top_work_dir = "/tmp/"; // anything other than partial index files in "/tmp/rawData" causes crashes
    private static CacheManager instance = null;
    
    private CacheManager() {
    	;
    }
    
    public static synchronized CacheManager getInstance() {
    	if (null == instance) {
    		instance = new CacheManager();
    	}
    	return instance;
    }
    
	private static File[] get_cached_files(String work_dir, Path file) {
		final String s = Parser.sterilizePath(file.toAbsolutePath().normalize().toString());
		
		File f = new File(work_dir);
		if (!f.exists()){
			f.mkdir();
		}
		
		return f.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.split("#")[0].equals(s);
			}
		});
	}
    
    private Set<String> get_prev_sources() throws Exception {
    	FileInputStream fin = new FileInputStream(top_work_dir + File.separator + "prev_sources");
    	ObjectInputStream ois = new ObjectInputStream(fin);
    	return (Set<String>)ois.readObject();
    }
    
    private void set_prev_sources(Set<String> sources) {
    	try (FileOutputStream fout = new FileOutputStream(top_work_dir
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
	public synchronized void updatePartialIndex(String source_file, boolean force_update) {
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
			LOGGER.fine("Ignoring unhandled file "
					+ file.toAbsolutePath().toString());
			return;
		}
		
		/* Add partial index of all interesting files to the working directory */
		boolean should_update = false;
		
		/*
		 * Update the cached files if the source file has
		 * changed more recently.
		 */
		long last_cache_touch = 0;
		File[] cached_files = get_cached_files(work_dir, file);
		for (File b : cached_files) {
			if (b.lastModified() > last_cache_touch) {
				last_cache_touch = b.lastModified();
			}
		}
		
		if (file.toFile().lastModified() > last_cache_touch) {
			LOGGER.fine(file.toAbsolutePath().toString() + " is stale (" +
					file.toFile().lastModified() + ", " +
					last_cache_touch + ")");
			should_update = true;
		} else {
			LOGGER.fine(file.toAbsolutePath().toString() + " is current (" +
					file.toFile().lastModified() + ", " +
					last_cache_touch + ")");
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
				Parser.printMethods(file.toAbsolutePath().toString(), work_dir);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public synchronized void updatePartialIndices(Collection<String> source_files) throws IOException {
		LOGGER.info("begin updatePartialIndices for " + source_files);
		
		/*
		 * Get all files that were previously visited searched. Any
		 * that were searched last time that weren't searched this time
		 * are stale and need to be removed to prevent false positives.
		 */
		Set<String> prev_visited;
		try {
			prev_visited = get_prev_sources();
			for (String ss : prev_visited) {
				LOGGER.finer("prev_visited = " + ss);
			}
		} catch (Exception ex) {
			LOGGER.warning("Unable to determine prior execution source: recreating");
			FileUtils.cleanDirectory(new File(work_dir));
			prev_visited = new HashSet<String>();
		}
		
		/* Generate partial indices for source files */
		for (String s : source_files) {
			LOGGER.fine("checking if " + s + " needs an update");
			updatePartialIndex(s, false);
		}
		
		/* Clean stale partial indices. */
		Set<String> visited = new HashSet<>(source_files);
		prev_visited.removeAll(visited);
		prev_visited.forEach(new Consumer<String>() {
			@Override
			public void accept(String filename) {
				File[] stale_files = get_cached_files(work_dir,
						Paths.get(filename));
				
				for (File f : stale_files) {
					LOGGER.fine(f.toString() +
							" no longer part of search set, removing");
					f.delete();
				}
			}
		});
		
		/* Record all sources discovered this execution */
		set_prev_sources(visited);
	}
	
}
