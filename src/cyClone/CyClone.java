package cyClone;

import java.util.concurrent.Callable;

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

    public static void main(String[] args) throws Exception {
        CommandLine.call(new CyClone(), args);
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

    @Override
    public Void call() throws Exception {
    	System.out.println("Hello world");
//        byte[] fileContents = Files.readAllBytes(file.toPath());
//        byte[] digest = MessageDigest.getInstance(algorithm).digest(fileContents);
//        System.out.println(javax.xml.bind.DatatypeConverter.printHexBinary(digest));
    	
    	
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
    	Parser p = new Parser();
    	p.printMethod("com.cyclone.test", "test_method_key", testMethodBody, "/tmp/out.txt");
    	
//    	System.out.println(testMethodBody);
    	
//    	// TEST #2
////    	p.printMethods("/home/nism/Documents/git/SourcererCCI/src/cyClone/Parser.java", "/tmp/rawData");
//    	Utils.getMethods("/home/nism/Documents/git/SourcererCCI/src/cyClone/Parser.java");
    	
    	// TEST #3
//    	p.printMethods("/home/nism/Documents/git/SourcererCCI/src/cyClone/Parser.java", "/tmp/rawData");
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
                System.out.println("shutting down QBQ, "
                        + (System.currentTimeMillis()));
                SearchManager.queryBlockQueue.shutdown();
                System.out.println("shutting down QCQ, "
                        + System.currentTimeMillis());
                SearchManager.queryCandidatesQueue.shutdown();
                System.out.println("shutting down VCQ, "
                        + System.currentTimeMillis());
                SearchManager.verifyCandidateQueue.shutdown();
                System.out.println("shutting down RCQ, "
                        + System.currentTimeMillis());
                SearchManager.reportCloneQueue.shutdown();
                break;
            } else {
                Thread.sleep(2 * 1000);
            }
        }
    	sm.genReportV2();
    	
    	System.out.println("++++++ FINISHED?");
//    	sm.pr
    	
    	
    	
        return null;
    }
}
