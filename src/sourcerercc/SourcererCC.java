package sourcerercc;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.*;
import antlr.MethodExtractor;
import cyclone.core.spi.CloneDetectorService;
import cyclone.core.spi.CloneListener;
import cyclone.core.spi.CloneSearch;
import cyclone.core.spi.CloneSearchStatusListener;

public class SourcererCC implements CloneDetectorService {
	private static WeakHashMap<CloneSearch, Thread> activeSearches = new WeakHashMap<>();
    
    private final static Logger LOGGER = Logger.getLogger("sourcererCC");
    static {
    	LOGGER.setLevel(Level.FINE);
    	
    	ConsoleHandler handler = new ConsoleHandler();
        // PUBLISH this level
        handler.setLevel(Level.FINE);
        LOGGER.addHandler(handler);
    }
    
    /* ------------------------------------------------------------------
	 * -- Private Methods
	 * --------------------------------------------------------------- */
    


	
	/* ------------------------------------------------------------------
	 * -- Public Methods
	 * --------------------------------------------------------------- */

	@Override
	public String[] getSupportedExtensions() {
		MethodExtractor extractor = MethodExtractor.getInstance();
		return extractor.getSupportedExtensions();
	}
	
	@Override
	public void updateCache(String source_file) {
		CacheManager.getInstance().updatePartialIndex(source_file, true);		
	}
    
	@Override
	public void cancel(CloneSearch spec) {
		Thread thread = activeSearches.get(spec);
		if (null != thread) {
			thread.interrupt();
		}
	}
	
	@Override
	public void search(CloneSearch spec, CloneListener listener,
			CloneSearchStatusListener statusListener) {
		Thread thread = new Thread(new SearchHandler(spec, listener, statusListener));
		activeSearches.put(spec, thread);
		thread.start();
	}

}
