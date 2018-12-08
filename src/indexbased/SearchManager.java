/**
 * 
 */
package indexbased;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.TieredMergePolicy;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import cyclone.core.spi.CloneListener;
import cyclone.core.spi.CloneSearch;
import models.Bag;
import models.CandidatePair;
import models.CandidateProcessor;
import models.CandidateSearcher;
import models.ClonePair;
import models.CloneReporter;
import models.CloneValidator;
import models.IListener;
import models.QueryBlock;
import models.QueryCandidates;
import models.Queue;
import models.TokenInfo;
import noindex.CloneHelper;
import utility.Util;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author vaibhavsaini
 * 
 */
public class SearchManager {
    private long clonePairsCount;
    public CodeSearcher searcher;
    public CodeSearcher fwdSearcher;
    private CloneHelper cloneHelper;
    private static String QUERY_DIR_PATH;
    public static String DATASET_DIR;
    public static Writer clonesWriter; // writer to write the output
    public static float th; // args[2]
                            // search
    private final static String ACTION_INDEX = "index";
    private final static String ACTION_SEARCH = "search";
    private final static int CANDIDATE_SEARCHER = 1;
    private final static int CANDIDATE_PROCESSOR = 2;
    private final static int CLONE_VALIDATOR = 3;
    private final static int CLONE_REPORTER = 4;
    
    /* added all of the below arguments */ 
    private final static float threshold = 7;
    private final static int QBQ_THREADS = 1;
    private final static int QCQ_THREADS = 1;
    private final static int VCQ_THREADS = 1;
    private final static int RCQ_THREADS = 1;
    private final static int QBQ_SIZE=5;
    private final static int QCQ_SIZE=5;
    private final static int VCQ_SIZE=5;
    private final static int RCQ_SIZE=5;
    public List<String> searchResult = new ArrayList<String>(); //added by - sarah
    private String rawDataDir = "";
    private String indexDir = "";
    private String fwdIndexDir = "";

    private CodeIndexer indexer;
    private long timeSpentInProcessResult;
    public long timeSpentInSearchingCandidates;
    private long timeIndexing;
    private long timeGlobalTokenPositionCreation;
    private long timeSearch;
    private long numCandidates;
    private Writer outputWriter;
    private long timeTotal;
    private String action;
    private boolean appendToExistingFile;
    private Writer cloneSiblingCountWriter;
    public static final Integer MUL_FACTOR = 100;
    int deletemeCounter = 0;
    private double ramBufferSizeMB;
    private long bagsSortTime;
    public Queue<QueryCandidates> queryCandidatesQueue;
    public Queue<QueryBlock> queryBlockQueue;
    public Queue<ClonePair> reportCloneQueue;
    public Queue<CandidatePair> verifyCandidateQueue;
    public Object lock = new Object();
    private int qbq_thread_count;
    private int qcq_thread_count;
    private int vcq_thread_count;
    private int rcq_thread_count;
    private int qbq_size;
    private int qcq_size;
    private int vcq_size;
    private int rcq_size;
    public boolean isGenCandidateStats;
    public int statusCounter;
    public boolean isStatusCounterOn;
    public int printAfterEveryXQueries;
    public static String loggingMode;
    
    private CloneSearch spec;
    private CloneListener cloneListener;
    
    private final static Logger LOGGER = Logger.getLogger("sourcererCC");

    public SearchManager(String baseDir, CloneSearch spec, CloneListener cloneListener) throws IOException {
        this.clonePairsCount = 0;
        this.cloneHelper = new CloneHelper();
        this.timeSpentInProcessResult = 0;
        this.timeSpentInSearchingCandidates = 0;
        this.timeIndexing = 0;
        this.timeGlobalTokenPositionCreation = 0;
        this.timeSearch = 0;
        this.numCandidates = 0;
        this.timeTotal = 0;
        this.appendToExistingFile = true;
        this.ramBufferSizeMB = 1024 * 1;
        this.bagsSortTime = 0;
        this.action = ACTION_INDEX;
        statusCounter = 0;
        this.th = (threshold * SearchManager.MUL_FACTOR);
        this.qbq_thread_count = QBQ_THREADS;
        this.qcq_thread_count = QCQ_THREADS;
        this.vcq_thread_count = VCQ_THREADS;
        this.rcq_thread_count = RCQ_THREADS;
        this.qbq_size = QBQ_SIZE;
        this.qcq_size = QCQ_SIZE;
        this.vcq_size = VCQ_SIZE;
        this.rcq_size = RCQ_SIZE;
        
        this.spec = spec;
        this.cloneListener = cloneListener;
        
        LOGGER.info("acton: " + this.action + System.lineSeparator()
                + "threshold: " + threshold + System.lineSeparator()
                + "QBQ_THREADS: " + this.qbq_thread_count + ", QBQ_SIZE: "
                + this.qbq_size + System.lineSeparator() + "QCQ_THREADS: "
                + this.qcq_thread_count + ", QCQ_SIZE: " + this.qcq_size
                + System.lineSeparator() + "VCQ_THREADS: "
                + this.vcq_thread_count + ", VCQ_SIZE: " + this.vcq_size
                + System.lineSeparator() + "RCQ_THREADS: "
                + this.rcq_thread_count + ", RCQ_SIZE: " + this.rcq_size
                + System.lineSeparator());
        queryBlockQueue = new Queue<QueryBlock>(
                this.qbq_thread_count, this.qbq_size);
        queryCandidatesQueue = new Queue<QueryCandidates>(
                this.qcq_thread_count, this.qcq_size);
        verifyCandidateQueue = new Queue<CandidatePair>(
                this.vcq_thread_count, this.vcq_size);
        reportCloneQueue = new Queue<ClonePair>(
                this.rcq_thread_count, this.rcq_size);

        this.registerListeners(this.qbq_thread_count,
                queryBlockQueue, CANDIDATE_SEARCHER);
        this.registerListeners(this.qcq_thread_count,
                queryCandidatesQueue, CANDIDATE_PROCESSOR);
        this.registerListeners(this.vcq_thread_count,
                verifyCandidateQueue, CLONE_VALIDATOR);
        this.registerListeners(this.rcq_thread_count,
                reportCloneQueue, CLONE_REPORTER);
        
        //initialized the parameters below:
        
        
        isGenCandidateStats = false;
        isStatusCounterOn = false;
        printAfterEveryXQueries = 250;
        loggingMode = "N";
        
        this.rawDataDir = baseDir + "rawData" + "/";
        this.indexDir = baseDir + Util.INDEX_DIR + "/";
        LOGGER.info("indexDir is:" + this.indexDir);
        this.fwdIndexDir = baseDir + Util.FWD_INDEX_DIR + "/"; 
        LOGGER.info("fwdIndexDir is:" + this.fwdIndexDir);
        this.DATASET_DIR = this.rawDataDir;
        
    }

    private void registerListeners(int nListeners, Queue<?> queue,
            int ListenerType) {
        List<IListener> listeners = new ArrayList<IListener>();
        for (int i = 0; i < nListeners; i++) {
            IListener listener = null;
            if (ListenerType == CANDIDATE_PROCESSOR) {
                listener = new CandidateProcessor(this);
            } else if (ListenerType == CANDIDATE_SEARCHER) {
                listener = new CandidateSearcher(this);
            } else if (ListenerType == CLONE_REPORTER) {
                listener = new CloneReporter(this, spec, cloneListener);
            } else if (ListenerType == CLONE_VALIDATOR) {
                listener = new CloneValidator(this);
            }
            listeners.add(listener);
        }
        queue.setListeners(listeners);
    }

    // added by - sarah
    public File getFwdIndex() {
    	File datasetDir = new File(this.rawDataDir);
    	return datasetDir;
    }
    
    // added by - sarah
    public String getRawDataPath() {
    	return this.rawDataDir;
    }
    
    // added by - sarah
    public List<String> getResultFileList() {
    	return this.searchResult;
    }
    
    // changed to public - sarah
    public void initIndexEnv() throws IOException, ParseException {
        TermSorter termSorter = new TermSorter();
        String dir = this.getRawDataPath();
        long timeGlobalPositionStart = System.currentTimeMillis();
        
        try {
            FileUtils.deleteDirectory(new File(Util.GTPM_DIR));
        } catch (IOException e) {
            e.printStackTrace();
        }
        Util.createDirs(Util.GTPM_DIR);
        
        termSorter.populateGlobalPositionMap(dir);
        
        this.timeGlobalTokenPositionCreation = System.currentTimeMillis() - timeGlobalPositionStart;
    }

    private void genReport() {
        String header = "";
        if (!this.appendToExistingFile) {
            header = "index_time, "
                    + "globalTokenPositionCreationTime,num_candidates, "
                    + "num_clonePairs, total_run_time, searchTime,"
                    + "timeSpentInSearchingCandidates,timeSpentInProcessResult,"
                    + "operation,sortTime_during_indexing\n";
        }
        header += this.timeIndexing + ",";
        header += this.timeGlobalTokenPositionCreation + ",";
        header += this.numCandidates + ",";
        header += this.clonePairsCount + ",";
        header += this.timeTotal + ",";
        header += this.timeSearch + ",";
        header += this.timeSpentInSearchingCandidates + ",";
        header += this.timeSpentInProcessResult + ",";
        if (this.action.equalsIgnoreCase("index")) {
            header += this.action + ",";
            header += this.bagsSortTime;
        } else {
            header += this.action;
        }

        Util.writeToFile(this.outputWriter, header, true);
    }
    
    public void genReportV2() {  // NISM -added to gain visability
        String header = "";
        if (!this.appendToExistingFile) {
            header = "index_time, "
                    + "globalTokenPositionCreationTime,num_candidates, "
                    + "num_clonePairs, total_run_time, searchTime,"
                    + "timeSpentInSearchingCandidates,timeSpentInProcessResult,"
                    + "operation,sortTime_during_indexing\n";
        }
        header += this.timeIndexing + ",";
        header += this.timeGlobalTokenPositionCreation + ",";
        header += numCandidates + ",";
        header += this.clonePairsCount + ",";
        header += this.timeTotal + ",";
        header += this.timeSearch + ",";
        header += this.timeSpentInSearchingCandidates + ",";
        header += this.timeSpentInProcessResult + ",";
        if (this.action.equalsIgnoreCase("index")) {
            header += this.action + ",";
            header += this.bagsSortTime;
        } else {
            header += this.action;
        }

        LOGGER.info(header);

        System.out.println("Discovered (" + this.clonePairsCount + ") clones:");
        for (String s : this.searchResult) {
        	System.out.println("\t" + s);
        }
        
    }
    
    /* Hack to fix lucene lock corruption issue in 4.6 */
    private static final Object INDEX_LOCK = new Object();
    
    //changed to public - sarah
    public void doIndex() throws IOException, ParseException {
        KeywordAnalyzer keywordAnalyzer = new KeywordAnalyzer();
        WhitespaceAnalyzer whitespaceAnalyzer = new WhitespaceAnalyzer(
                Version.LUCENE_46);
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(
                Version.LUCENE_46, whitespaceAnalyzer);
        IndexWriterConfig fwdIndexWriterConfig = new IndexWriterConfig(
                Version.LUCENE_46, keywordAnalyzer);
        indexWriterConfig.setOpenMode(OpenMode.CREATE);// add new
                                                       // docs to
                                                       // exisiting
                                                       // index
        TieredMergePolicy mergePolicy = (TieredMergePolicy) indexWriterConfig
                .getMergePolicy();

        mergePolicy.setNoCFSRatio(0);// what was this for?
        mergePolicy.setMaxCFSSegmentSizeMB(0); // what was this for?
        // indexWriterConfig.setMergePolicy(mergePolicy);
        indexWriterConfig.setRAMBufferSizeMB(this.ramBufferSizeMB);
        fwdIndexWriterConfig.setOpenMode(OpenMode.CREATE);
        fwdIndexWriterConfig.setRAMBufferSizeMB(this.ramBufferSizeMB);
        // fwdIndexWriterConfig.setMergePolicy(mergePolicy);
        IndexWriter indexWriter;
        IndexWriter fwdIndexWriter = null;
        CodeIndexer fwdIndexer = null;
        synchronized (INDEX_LOCK) {
        try {
            indexWriter = new IndexWriter(FSDirectory.open(new File(this.indexDir)), indexWriterConfig);
            this.indexer = new CodeIndexer(this.indexDir, indexWriter,cloneHelper, SearchManager.th);
            fwdIndexWriter = new IndexWriter(FSDirectory.open(new File(this.fwdIndexDir)), fwdIndexWriterConfig);
            fwdIndexer = new CodeIndexer(this.fwdIndexDir, fwdIndexWriter,cloneHelper, SearchManager.th);
            File datasetDir = new File(this.rawDataDir);

            if (datasetDir.isDirectory()) {
                BufferedReader br = null;
                for (File inputFile : datasetDir.listFiles()) {
                    try {
                        br = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), "UTF-8"));
                        String line;
                        while ((line = br.readLine()) != null && line.trim().length() > 0) {
                            Bag bag = cloneHelper.deserialise(line);
                            long startTime = System.currentTimeMillis();
                            Util.sortBag(bag);
                            this.bagsSortTime += System.currentTimeMillis() - startTime;
                            fwdIndexer.fwdIndexCodeBlock(bag);
                            this.indexer.indexCodeBlock(bag);
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            br.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    // fwdIndexer.createFwdIndex(inputFile);
                }
            } else {
                LOGGER.severe("File: " + datasetDir.getName() + " is not a direcory. exiting now");
                System.exit(1);
            }
        } catch (IOException e) {
        	e.printStackTrace();
            LOGGER.severe(e.getMessage() + ", exiting now.");
            System.exit(1);
        } finally {
            fwdIndexer.closeIndexWriter();
            this.indexer.closeIndexWriter();
        }
        }
    }
    
    // changed to public and changed it to pass "query" as a parameter
    
    public void findCandidates(String query) throws InterruptedException{
    	long start_time = System.currentTimeMillis();
        try {
        	QueryBlock queryBlock = this.getNextQueryBlock(query);
            if (isStatusCounterOn) {
                statusCounter += 1;
                if ((statusCounter % printAfterEveryXQueries) == 0) {
                    long end_time = System.currentTimeMillis();
                    Duration duration;
                    try {
                        duration = DatatypeFactory.newInstance().newDuration(end_time - start_time);
                        LOGGER.info("queries processed: " + statusCounter +
                        		String.format(" time taken: %02dh:%02dm:%02ds", duration.getDays()* 24
                        		+ duration.getHours(), duration.getMinutes(), duration.getSeconds()));
                        start_time = end_time;
                    } catch (DatatypeConfigurationException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
            queryBlockQueue.put(queryBlock);
        } catch (ParseException e) {
            LOGGER.severe(e.getMessage()
                    + " skiping to next bag");
//            e.printStackTrace();
        }
    }

    public void findCandidates() throws InterruptedException {
        long start_time = System.currentTimeMillis();
        try {
            File queryDirectory = this.getQueryDirectory();
            File[] queryFiles = this.getQueryFiles(queryDirectory);
            for (File queryFile : queryFiles) {
            	LOGGER.info("Query File: " + queryFile);
                String filename = queryFile.getName().replaceFirst("[.][^.]+$",
                        "");
                try {

                    SearchManager.clonesWriter = Util.openFile("output"
                            + SearchManager.th / SearchManager.MUL_FACTOR + "/"
                            + filename + "clones_index_WITH_FILTER.txt", false);
                } catch (IOException e) {
                    LOGGER.severe(e.getMessage() + " exiting");
                    System.exit(1);
                }
                BufferedReader br = this.getReader(queryFile);
                String line = null;
                try {
                    QueryBlock queryBlock = null;
                    while ((line = br.readLine()) != null
                            && line.trim().length() > 0) {
                        try {
                            queryBlock = this.getNextQueryBlock(line);
                            if (isStatusCounterOn) {
                                statusCounter += 1;
                                if ((statusCounter % printAfterEveryXQueries) == 0) {
                                    long end_time = System.currentTimeMillis();
                                    Duration duration;
                                    try {
                                        duration = DatatypeFactory
                                                .newInstance().newDuration(
                                                        end_time - start_time);
                                        LOGGER.info("queries processed: "
                                                        + statusCounter
                                                        + String.format(" time taken: %02dh:%02dm:%02ds",
                                                        duration.getDays() * 24  + duration.getHours(),
                                                        duration.getMinutes(),
                                                        duration.getSeconds()));
                                        start_time = end_time;
                                    } catch (DatatypeConfigurationException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                    }
                                }
                            }
                            queryBlockQueue.put(queryBlock);
                        } catch (ParseException e) {
                            LOGGER.severe(e.getMessage()
                                    + " skiping to next bag");
//                            e.printStackTrace();
                        }
                    }
                } catch (IOException e) {
                    System.out
                            .println(e.getMessage() + " skiping to next file");
                } finally {
                    try {
                        br.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (FileNotFoundException e) {
            LOGGER.severe(e.getMessage() + "exiting");
            System.exit(1);
        }
    }
    
    // chnaged to public - sarah
    public void initSearchEnv() {
        // testGson = new TestGson(); // remove this line later. for
        // validation only.
        // testGson.populateMap(); // this is for validation only, remove this
        // line.
        Util.createDirs("output" + SearchManager.th / SearchManager.MUL_FACTOR + "/cloneGroups/");
        try {
            this.cloneSiblingCountWriter = Util.openFile("output" + SearchManager.th / SearchManager.MUL_FACTOR+ "/cloneGroups/siblings_count.csv", false);
            Util.writeToFile(this.cloneSiblingCountWriter,"query_block_id,siblings", true);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        String dir = this.getRawDataPath();
        TermSorter termSorter = new TermSorter();
        try {

            long timeGlobalPositionStart = System.currentTimeMillis();
            termSorter.populateGlobalPositionMap(dir);
            this.timeGlobalTokenPositionCreation = System.currentTimeMillis()
                    - timeGlobalPositionStart;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            LOGGER.severe("Error in Parsing: " + e.getMessage());
            e.printStackTrace();
        }
        this.fwdSearcher = new CodeSearcher(this.fwdIndexDir, "id"); //changed this.fwdIndexDir from Util.FWD_INDEX_DIR // searches
                                                                                // on
                                                                                // fwd
                                                                                // index
        this.searcher = new CodeSearcher(this.indexDir, "tokens");

    }

    public synchronized void updateNumCandidates(int num) {
        numCandidates += num;
    }

    public synchronized void updateClonePairsCount(int num) {
        clonePairsCount += num;
    }
    
    /*
     * We we grabbed this code, it SearchManager was kept alive
     * by the amount of work in each of the process queues. However,
     * each of the processors will pop off of the queue, process for
     * some finite amount of time, then place onto the next queue
     * if appropriate. In this finite amount of time, the SearchManager
     * could be judge to be idle and shut down. This keep alive should
     * prevent this.
     * 
     * I'm under no false illusions that this is a 'good' fix. It is
     * however the fix that time allows me.
     */
    private int stayAliveCount = 0;
    
    public synchronized void incrementStayAlive() {
    	stayAliveCount += 1;
    }
    
    public synchronized void decrementStayAlive() {
    	stayAliveCount -= 1;
    }

    private BufferedReader getReader(File queryFile)
            throws FileNotFoundException {
        BufferedReader br = null;
        br = new BufferedReader(new FileReader(queryFile));
        return br;
    }

    private File getQueryDirectory() throws FileNotFoundException {
        File queryDir = new File(QUERY_DIR_PATH);
        if (!queryDir.isDirectory()) {
            throw new FileNotFoundException("directory not found.");
        } else {
        	LOGGER.info("Directory: " + queryDir.getName());
            return queryDir;
        }
    }

    private File[] getQueryFiles(File queryDirectory) {
        return queryDirectory.listFiles();
    }

    private QueryBlock getNextQueryBlock(String line) throws ParseException {
        List<Entry<String, TokenInfo>> listOfTokens = new ArrayList<Entry<String, TokenInfo>>();
        QueryBlock queryBlock = this.cloneHelper.deserialiseToQueryBlock(line,
                listOfTokens);
        Collections.sort(listOfTokens,
                new Comparator<Entry<String, TokenInfo>>() {
                    public int compare(Entry<String, TokenInfo> tfFirst,
                            Entry<String, TokenInfo> tfSecond) {
                        long position1 = 0;
                        try {
                            position1 = TermSorter.globalTokenPositionMap
                                    .get(tfFirst.getKey());
                        } catch (Exception e) {
                            position1 = -1;
                        }
                        long position2 = 0;
                        try {
                            position2 = TermSorter.globalTokenPositionMap
                                    .get(tfSecond.getKey());
                        } catch (Exception e) {
                            position2 = -1;
                        }
                        if (position1 - position2 != 0) {
                            return (int) (position1 - position2);
                        } else {
                            return 1;
                        }
                    }
                });
        int position = 0;
        for (Entry<String, TokenInfo> entry : listOfTokens) {
            TokenInfo tokenInfo = entry.getValue();
            if (position < queryBlock.getPrefixSize()) {
                queryBlock.getPrefixMap().put(entry.getKey(), tokenInfo);
                position += tokenInfo.getFrequency();
                queryBlock.setPrefixMapSize(position);
            } else {
                queryBlock.getSuffixMap().put(entry.getKey(), tokenInfo);
                position += tokenInfo.getFrequency();
            }
            tokenInfo.setPosition(position);
        }
        return queryBlock;
    }

    public void waitForCompletion() throws InterruptedException {
    	while (true) {
    		if (queryBlockQueue.size() == 0
    				&& queryCandidatesQueue.size() == 0
    				&& verifyCandidateQueue.size() == 0
    				&& reportCloneQueue.size() == 0
    				&& stayAliveCount <= 0) {
//    			System.out.println("shutting down QBQ, " + (System.currentTimeMillis()));
    			queryBlockQueue.shutdown();
//    			System.out.println("shutting down QCQ, " + System.currentTimeMillis());
    			queryCandidatesQueue.shutdown();
//    			System.out.println("shutting down VCQ, " + System.currentTimeMillis());
    			verifyCandidateQueue.shutdown();
//    			System.out.println("shutting down RCQ, " + System.currentTimeMillis());
    			reportCloneQueue.shutdown();
    			break;
    		} else {
    			Thread.sleep(2 * 1000);
    		}
    	}

	}
}
