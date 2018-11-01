package cyClone;

import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import static java.nio.file.FileVisitResult.*;
import java.util.*;

import antlr.Utils;
import indexbased.SearchManager;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class CyClone implements Callable<Void> {

//    @Parameters(index = "0", description = "The file whose checksum to calculate.")
//    private File file;

    @Option(names = {"-S", "--sources"}, split=":", description = "source directories to investigate")
    private String[] src_dir = { "." };
    
    @Option(names = {"-R", "--recursive"}, description = "search source directories recursively")
    private boolean recursive;
    
    @Option(names = {"-v", "--verbose"}, description = "verbosity")
    private boolean verbose;
    
    @Option(names = {"-F"}, description = "force a refresh of partial file indices")
    private boolean force_refresh;
    
    @Option(names = {"-d", "--debug"}, description = "debug")
    private boolean debug;
    
    @Parameters(index = "0", description = "target code chunk (filename:start_line:end_line")
    private String target;
    
    
    private String work_dir = "/tmp/rawData/";
    private String top_work_dir = "/tmp/"; // anything other than partial index files in "/tmp/rawData" causes crashes
    
    private final static Logger LOGGER = Logger.getLogger(CyClone.class.getName());
    static {
    	LOGGER.setLevel(Level.WARNING);
    }
    

    public static void main(String[] args) throws Exception {
        CommandLine.call(new CyClone(), args);
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
    
	static class SourceWalker extends SimpleFileVisitor<Path> {
		private final PathMatcher matcher;
		private final Parser parser;
		private final String work_dir;
		private final boolean force_update;
		private final boolean debug;
		
		
		private Set<String> visited = new HashSet<String>();
		
		public SourceWalker(Parser parser, String work_dir, 
				boolean force_update, boolean debug) {
			this.parser = parser;
			this.work_dir = work_dir;
			this.force_update = force_update;
			this.debug = debug;
			
			/*
			 * FIXME - I only support java files!
			 */
			matcher = FileSystems.getDefault()
                    .getPathMatcher("glob:" + "**.java");
		}
		
		public Set<String> get_visited() {
			return visited;
		}
		
		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
			/* Early return for files that aren't processable */
			if (!this.matcher.matches(file)) {
				LOGGER.warning(file.toAbsolutePath().toString() + " does not match");
				return CONTINUE;
			}
			
			/* Marked as visited */
			visited.add(file.toString());
			
			/* Add partial index of all interesting files to the working directory */
			LOGGER.fine("generating partial index for "
					+ file.toAbsolutePath().toString());
			
			String s = Parser.sterilizePath(file.toString());
			
			boolean should_update = false;
			
//			Parser.getFileName(classPath, functionKey)
//			System.out.println("<<< " + s);
//			System.out.println("<<< " + file.toString());
			
			/*
			 * Update the cached files if the source file has
			 * changed more recently.
			 */
			long last_cache_touch = 0;
			File[] cached_files = CyClone.get_cached_files(this.work_dir, file);
			for (File b : cached_files) {
//				System.out.println("++++ " + b.getAbsolutePath());
				if (b.lastModified() > last_cache_touch) {
					last_cache_touch = b.lastModified();
				}
			}
			
			if (file.toFile().lastModified() > last_cache_touch) {
				LOGGER.fine("Need to update " + file.toString());
				should_update = true;
			}
			should_update = should_update || this.force_update;
			
			if (should_update) {
				/* 
				 * Remove all cached files. They could represent
				 * functions that have since been deleted.
				 */
				for (File b : cached_files) {
					b.delete();
				}
				
				try {
					this.parser.printMethods(file.toString(), this.work_dir);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			return CONTINUE;
		}
	}

    /* NISM - try to find this clone */
    private int add(int a, int b) {
    	int c = a + b;
		int d = a + b;
		int e = a + b;
		int f = a + b;
		int g = a + b;
		int h = a + b;
		int i = a + b;
		
		return c + d + e + f + g + h + i;
	}

    private void run_tests() throws Exception {
    	Parser p = new Parser();
    	
       	// TEST #1
    		String testMethodBody = "private int add(int a, int b) {\n\r"
    				+ "int c = a + b;\n\r"
    				+ "int d = a + b;\n\r"
    				+ "int e = a + b;\n\r"
    				+ "int f = a + b;\n\r"
    				+ "int g = a + b;\n\r"
    				+ "int h = a + b;\n\r"
    				+ "int i = a + b;\n\r"
    				+ "return c + d + e + f + g + h + i;\n\r"
    				+ "}";
        	p.printMethod("com.cyclone.test", "test_method_key", testMethodBody, "/tmp/out.txt");
        	
//        	System.out.println(testMethodBody);
        	
        	// TEST #2
//        	p.printMethods("/home/nism/Documents/git/SourcererCCI/src/cyClone/Parser.java", "/tmp/rawData");
        	Utils.getMethods("/home/nism/Documents/git/SourcererCCI/src/cyClone/Parser.java");
        	
        	// TEST #3
//        	p.printMethods("/home/nism/Documents/git/SourcererCCI/src/cyClone/Parser.java", "/tmp/rawData");
        	p.printMethods("/home/nism/Documents/git/SourcererCCI/src/cyClone/CyClone.java", "/tmp/rawData/");
        	
        	
        	// TEST #3b
        	
        	SearchManager sm = new SearchManager("/tmp/");
        	sm.initIndexEnv();
        	sm.doIndex();
        	sm.initSearchEnv();
        	sm.findCandidates("om.cyclone.test#test_method_key.txt@#@add@@::@@1,a@@::@@8,private@@::@@1,b@@::@@8,c@@::@@2,d@@::@@2,e@@::@@2,f@@::@@2,g@@::@@2,h@@::@@2,i@@::@@2,int@@::@@10,return@@::@@1");
        	
        	
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
        	
        	System.out.println("++++++ FINISHED?");	
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
    
    @Override
    public Void call() throws Exception {
//    	run_tests();

    	String t_file;
    	int t_start;
    	int t_end;
    	
    	String[] tgt_strings = target.split(":");
    	if (tgt_strings.length == 3) {
    		t_file = tgt_strings[0];
    		t_start = Integer.parseInt(tgt_strings[1]);
    		t_end = Integer.parseInt(tgt_strings[2]);
    	} else {
    		throw new Exception("not supported");
		}
    	
    	LOGGER.fine("Searching for " + t_file + " start="
    			+ t_start + " end=" + t_end + ":");

		StringBuffer buffer = new StringBuffer();	
		
		try (Stream<String> lines = Files.lines(Paths.get(t_file))) {
			t_start = Integer.max(t_start - 1, 0);
		    
		    Iterator<String> iter = lines.skip(t_start).iterator();
		    String line;
		    for (int i = t_start; i < t_end && iter.hasNext(); i++) {
		    	line = iter.next();
			    buffer.append(line);
				if (this.debug) {
					System.out.println("\t" + line);
				}
		    }
		}
		
		
		Parser p = new Parser();
		
		/* Generate the partial index for the target code chunk. */
		StringWriter prt_index = new StringWriter();
		p.printMethod2("NaN", "NaN", buffer.toString(), prt_index);
		
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
		
		/*
		 * Generate or update the partial indices for all the files on the
		 * search path.
		 */
		SourceWalker finder = new SourceWalker(p, this.work_dir, 
				this.force_refresh, this.debug);
		Set<FileVisitOption> options = new HashSet<FileVisitOption>();
		int depth;
		
		if (this.recursive) {
			depth = Integer.MAX_VALUE;
		} else {
			depth = 1;
		}
		
		/* Generate partial indices for source files */
		for (String s : this.src_dir) {			
	        Files.walkFileTree(Paths.get(s), options, depth, finder);
		}
		
		/* Clean stale partial indices. */
		Set<String> visited = finder.get_visited();
		prev_visited.removeAll(visited);
		prev_visited.forEach(new Consumer<String>() {
			@Override
			public void accept(String filename) {
				File[] stale_files = CyClone.get_cached_files(work_dir,
						Paths.get(filename));
				
				for (File f : stale_files) {
					LOGGER.finer("TODO - MUST REMOVE FILE " + f.toString());
					f.delete();
				}
			}
		});
		
		/* Record all sources discovered this execution */
		set_prev_sources(visited);
    	
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
    	
        return null;
    }
}
