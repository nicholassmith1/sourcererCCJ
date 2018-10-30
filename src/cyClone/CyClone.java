package cyClone;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringWriter;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.concurrent.Callable;
import java.util.stream.Stream;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import static java.nio.file.FileVisitResult.*;
import static java.nio.file.FileVisitOption.*;
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
    
    @Option(names = {"-d", "--debug"}, description = "debug")
    private boolean debug;
    
    @Parameters(index = "0", description = "target code chunk (filename:start_line:end_line")
    private String target;
    
    
    private String work_dir = "/tmp/rawData/";
    
    

    public static void main(String[] args) throws Exception {
        CommandLine.call(new CyClone(), args);
    }
    
	static class SourceWalker extends SimpleFileVisitor<Path> {
		private final PathMatcher matcher;
		private final Parser parser;
		private final String work_dir;
		private final boolean debug;
		
		public SourceWalker(Parser parser, String work_dir, boolean debug) {
			this.parser = parser;
			this.work_dir = work_dir;
			this.debug = debug;
			
			/*
			 * FIXME - I only support java files!
			 */
			matcher = FileSystems.getDefault()
                    .getPathMatcher("glob:" + "**.java");
		}
		
		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
			/* Early return for files that aren't processable */
			if (!this.matcher.matches(file)) {
				System.out.println(file.toAbsolutePath().toString() + " does not match");
				return CONTINUE;
			}
			
			/* Add partial index of all interesting files to the working directory */
			if (this.debug) {
				System.out.println("generating partial index for "
						+ file.toAbsolutePath().toString());
			}
			
			try {
				this.parser.printMethods(file.toString(), this.work_dir);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return CONTINUE;
		}

//		// Invoke the pattern matching
//		// method on each directory.
//		@Override
//		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
////			find(dir);
//			return CONTINUE;
//		}
//
//		@Override
//		public FileVisitResult visitFileFailed(Path file, IOException exc) {
//			System.err.println(exc);
//			return CONTINUE;
//		}
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
    	
    	if (this.debug) {
    		System.out.println("Searching for " + t_file + " start="
    				+ t_start + " end=" + t_end + ":");
    	}

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
		 * Generate or update the partial indices for all the files on the
		 * search path.
		 */
		SourceWalker finder = new SourceWalker(p, this.work_dir, this.debug);
		Set<FileVisitOption> options = new HashSet<FileVisitOption>();
		int depth;
		
		if (this.recursive) {
			depth = Integer.MAX_VALUE;
		} else {
			depth = 1;
		}
		
		for (String s : this.src_dir) {			
	        Files.walkFileTree(Paths.get(s), options, depth, finder);
		}
    	
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
