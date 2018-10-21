package userInterface;

import indexbased.SearchManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.SimpleMarkerAnnotation;

import userInterface.UtilASTNode.MethodInfo;
import astParser.ASTMethodParser;
import astParser.MethodVisitor;

public class WatchUserBehavior {

    private Thread t;
    private int lastCloneDetectedPos = 0;
    private int lastCloneDetectedStartLine = 0;
    public List<String> lastSearchedFileList;
    private String lastParsedFileName = "";
    private int currentFunctionLineNumber = -1;
    private HashMap<IEditorPart, Integer> unsavedDocs;
    private boolean cloneListAlreadyClicked = false;

    private ASTMethodParser astMethodParser;
    private HashMap<String, String> mapAST;
    private HashMap<String, SearchManager> mapSearchManager;
    private HashMap<ITextEditor, SimpleMarkerAnnotation> curAnnotation;

    private List<MethodInfo> currnetMethodKeyList;
    WatchUserBehavior currentWatchUserBehaviorInstance = null;

    //
    // Initialize
    //
    public WatchUserBehavior(ASTMethodParser ast,
            HashMap<String, String> mapAST,
            HashMap<String, SearchManager> mapSearchManager) {
        this.lastSearchedFileList = new ArrayList<String>();
        this.unsavedDocs = new HashMap<IEditorPart, Integer>();
        this.curAnnotation = new HashMap<ITextEditor, SimpleMarkerAnnotation>();

        this.astMethodParser = ast;
        this.mapAST = mapAST;
        this.mapSearchManager = mapSearchManager;
        this.currnetMethodKeyList = new ArrayList<MethodInfo>();
        currentWatchUserBehaviorInstance = this;
    }

    // Clear all current markers
    //
    public void clearAllMarkers() {
        try {
            lastSearchedFileList.clear();
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            root.deleteMarkers("edu.uci.ics.mondego.clonedetection.marker",
                    true, IResource.DEPTH_INFINITE);

            Display.getDefault().asyncExec(new Runnable() {

                IWorkbench workbench = null;
                IWorkbenchWindow workbenchWindow = null;
                IWorkbenchPage page = null;
                IEditorPart editorPart = null;

                //
                // Check if there is new project
                //

                public void run() {
                    workbench = PlatformUI.getWorkbench();
                    workbenchWindow = workbench.getActiveWorkbenchWindow();
                    page = workbenchWindow.getActivePage();
                    editorPart = page.getActiveEditor();
                    removeAnnotation(getEditor());
                }

                //
                // Get Text editor
                //
                public ITextEditor getEditor() {
                    if (editorPart == null)
                        return null;

                    ITextEditor editor = (ITextEditor) editorPart
                            .getAdapter(ITextEditor.class);
                    return editor;
                }
            });

        } catch (CoreException e1) {
            return;
        }
    }

    //
    // Run new thread
    //
    // "CurrentMethodDetector" function iterates until eclipse closed.
    //
    public void run() {
        t = new Thread(new CurrentMethodDetector(), "CurrentMethodDetector");
        t.start();
    }

    public void stopCurrentMethodIndicator() {
        t.interrupt();
    }

    public void resetParsingHistory() {
        lastParsedFileName = "";
        currentFunctionLineNumber = -1;
    }

    /********************************************************************************************************************/
    // MARKER
    //
    // 1. Add or clear markers
    // 2. Set marker color
    // 3. Detect whether the marker was clicked
    //

    //
    // Clear previous marker's setting (color)
    //
    public void removeAnnotation(ITextEditor editor) {
        IDocumentProvider idp = editor.getDocumentProvider();
        AnnotationModel amf = (AnnotationModel) idp.getAnnotationModel(editor
                .getEditorInput());
        if (curAnnotation.containsKey(editor)) {
            SimpleMarkerAnnotation sma = curAnnotation.get(editor);
            amf.removeAnnotation(sma);
            curAnnotation.remove(editor);
        }
    }

    /********************************************************************************************************************/

    /********************************************************************************************************************/

    /********************************************************************************************************************/

    /********************************************************************************************************************/
    // ROUTINELY WORKS
    //
    // 1. Looking for files are created/changed/deleted
    // If it found something, update AST
    // 2. Update AST
    // This work includes update indexes
    // 3. Parsing current document
    // This work is triggered every iteration except no documents are opened or
    // the document the user is now working on is not saved.
    //
    public String getProjectNameFromCurrentMethodKey(String methodKey) {
        return methodKey.substring(1, methodKey.indexOf('/', 1));
    }

    public SearchManager getSearchManager(String projectName) {
        if (mapSearchManager.containsKey(projectName))
            return mapSearchManager.get(projectName);
        return null;
    }

    /* - END - ROUTINELY WORKS */
    /********************************************************************************************************************/

    public class CurrentMethodDetector implements Runnable {

        public CurrentMethodDetector() {
        }

        //
        // Return the path of term-freq file
        //
        // "currnetMethodKeyList" contains the information of the methods in
        // current documents.
        // This method is only called when user changed the location of cursor.
        // (And, the document must be saved.)
        //
        public String getTermFreqLocation(int index) {
            String projectName = getProjectNameFromCurrentMethodKey(currnetMethodKeyList
                    .get(index).path.toString());
            SearchManager searchManager = getSearchManager(projectName);
            if (searchManager == null) {
                return "";
            }

            File datasetDir = searchManager.getFwdIndex();
            String fwdIndex = currnetMethodKeyList.get(index).path.toString()
                    + "#" + currnetMethodKeyList.get(index).methodKey + ".txt";
            fwdIndex = fwdIndex.substring(1);
            fwdIndex = fwdIndex.replace('/', '$');
            fwdIndex = fwdIndex.replace('(', '+');
            fwdIndex = fwdIndex.replace(')', '=');

            boolean isFound = false;
            for (File inputFile : datasetDir.listFiles()) {
                if (inputFile.getName().equals(fwdIndex)) {
                    isFound = true;
                    break;
                }
            }

            if (isFound == false)
                return "";

            return searchManager.getRawDataPath() + fwdIndex;
        }

        //
        // Read and return the contents of term-freq file
        // The contents of the file is used as query
        //
        @SuppressWarnings("resource")
        public String getQuery(String target) {
            String query = "";
            BufferedReader br;
            try {
                br = new BufferedReader(new FileReader(target));
                String line = "";
                String temp = "";

                while (line != null) {
                    line = br.readLine();
                    temp = temp + line;
                }
                query = temp;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return query;
        }

        @Override
        public void run() {
            while (true) {
                //
                // If there is no files and projects in workspace, we don't need
                // to continue process
                //
                Display.getDefault().syncExec(new Runnable() {
                    //
                    // This part is main procedure of UI part in this plugin
                    // "run" method iterates until eclipse closed
                    //
                    IWorkbench workbench = null;
                    IWorkbenchWindow workbenchWindow = null;
                    IWorkbenchPage page = null;
                    IEditorPart editorPart = null;

                    //
                    // Check if there is new project
                    //

                    public void run() {
                        workbench = PlatformUI.getWorkbench();
                        if (null != workbench) {
                            workbenchWindow = workbench
                                    .getActiveWorkbenchWindow();
                            if (null != workbenchWindow) {
                                page = workbenchWindow.getActivePage();
                                if (null != page) {
                                    editorPart = page.getActiveEditor();
                                }
                            }
                        }
                        if (mapAST.isEmpty()) {
                            try {
                                if (checkNewProject()) {
                                    parseCurrentDocument();
                                    watchUpdatedFiles();
                                }
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }

                        } else {
                            // If a new project is found, clear all markers and
                            // clone list
                            try {
                                if (checkNewProject()) {
                                    lastSearchedFileList.clear();
                                    // clearAllMarkers();
                                    clearCloneList();
                                    resetParsingHistory();
                                    return;
                                }
                            } catch (IOException e1) {
                                // TODO Auto-generated catch block
                                e1.printStackTrace();
                            }

                            // Check if there is a modified file
                            // If it found something, update AST and indexes.
                            watchUpdatedFiles();

                            // If there is no opened documents, we don't need to
                            // continue the process
                            if (isDocumentOpened() == false)
                                return;

                            // If current document is not saved, we don't
                            // continue the process
                            if (isSaved() == false) {
                                // clearAllMarkers();
                                clearCloneList();
                                return;
                            }

                            // check whether the marker is clicked
                            checkMarkerClicked();

                            /*************************************************************************************/
                            //
                            // PARSING CURRENT DOCUMENT AND EXTRACT LINENUMBER
                            //
                            // 1. Parsing current document
                            // 2. Get line number that user is working
                            //
                            parseCurrentDocument();
                            ITextSelection textSelection = getTextSelection();
                            if (textSelection == null)
                                return;
                            int lineNumber = textSelection.getOffset() + 10;
                            IWorkbenchPart workbenchPart = null;
                            IFile file = null;
                            try {
                                workbenchPart = page.getActivePart();
                                file = (IFile) workbenchPart.getSite()
                                        .getPage().getActiveEditor()
                                        .getEditorInput()
                                        .getAdapter(IFile.class);
                                if (file == null) {
                                    return;
                                }
                            } catch (Exception e) {
                                return;
                            }
                            /*
                             * - END - PARSING CURRENT DOCUMENT AND EXTRACT
                             * LINENUMBER
                             */
                            /*************************************************************************************/

                            /*************************************************************************************/
                            //
                            // FIND CLONES
                            //
                            // step 1. Find method that the user is working on
                            // We need to compare the beginning and end of the
                            // methods in the documents to the position of the
                            // cursor
                            // step 2. Parsing the method
                            // step 3. Search
                            // This is done by searchManager and get the list of
                            // clones
                            // step 4. If there are clones, show marker
                            //
                            for (int i = 0; i < currnetMethodKeyList.size(); i++) {

                                // STEP1
                                if (lineNumber >= currnetMethodKeyList.get(i).start
                                        && lineNumber <= currnetMethodKeyList
                                                .get(i).end) {
                                    if (currentFunctionLineNumber != currnetMethodKeyList
                                            .get(i).start) {
                                        currentFunctionLineNumber = currnetMethodKeyList
                                                .get(i).start;

                                        String termFreqLocation = getTermFreqLocation(i);
                                        if (termFreqLocation == "") {
                                            return;
                                        }

                                        // STEP2
                                        String query = getQuery(termFreqLocation);
                                        query = query.substring(0,
                                                query.length() - 4);
                                        System.out
                                                .println("Query is:*" + query);
                                        // STEP3
                                        List<String> listFile = new ArrayList<String>();
                                        for (String key : mapSearchManager
                                                .keySet()) {
                                            SearchManager searchManager = getSearchManager(key);
                                            if (searchManager == null)
                                                return;
                                            /*
                                             * earlier part - sarah
                                             * searchManager.doSearch(query);
                                             * listFile.addAll(searchManager.
                                             * getResultFileList());
                                             */

                                            // added part - sarah
                                            try {
                                                searchManager.initSearchEnv();
                                                searchManager
                                                        .findCandidates(query);

                                                Thread.sleep(1 * 1000);

                                                // Set<String> set = new
                                                // HashSet<String>(searchManager.getResultFileList());
                                                // searchManager.searchResult.clear();
                                                // searchManager.searchResult.addAll(set);
                                                listFile.addAll(searchManager
                                                        .getResultFileList());
                                                searchManager.searchResult
                                                        .clear();
                                            } catch (InterruptedException e) {
                                                // TODO Auto-generated catch
                                                // block
                                                e.printStackTrace();
                                            }
                                        }

                                        // STEP4
                                        if (listFile.size() > 0) {

                                            // clearAllMarkers();
                                            try {
                                                if (lastCloneDetectedPos != currnetMethodKeyList
                                                        .get(i).start + 1)
                                                    clearCloneList();
                                                lastCloneDetectedPos = currnetMethodKeyList
                                                        .get(i).start + 1;
                                                lastCloneDetectedStartLine = currnetMethodKeyList
                                                        .get(i).lineStart + 1;
                                                createMarker(file,
                                                        listFile.size());
                                                lastSearchedFileList = listFile;
                                                cloneListAlreadyClicked = false;

                                            } catch (CoreException e) {
                                                return;
                                            }
                                        } else {
                                            clearCloneList();
                                        }
                                        break;
                                    }
                                }
                            }

                            /* - END - FIND CLONES */
                            /*************************************************************************************/
                        }
                    }

                    // HELPER METHODS
                    //
                    // In order to handle the documents opened in eclipse, we
                    // need to make use of "IWorkbench" class which makes the
                    // code lengthy.
                    // Therefore, I made split the code depending on the
                    // function.
                    //
                    public boolean checkNewProject() throws IOException {
                        HashMap<String, String> mapCurrentProject = new HashMap<String, String>();
                        IWorkspaceRoot root = ResourcesPlugin.getWorkspace()
                                .getRoot();
                        IProject[] projects = root.getProjects();
                        try {
                            for (IProject project : projects) {
                                if (project.isOpen()) {
                                    try {
                                        mapCurrentProject.put(
                                                project.getName(), "");
                                    } catch (Exception e) { // change to
                                                            // CoreException
                                        e.printStackTrace();
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        boolean foundRemovedProject = false;
                        if (mapCurrentProject.size() < mapAST.size())
                            foundRemovedProject = true;

                        for (String projectName : mapAST.keySet()) {
                            if (mapCurrentProject.containsKey(projectName))
                                mapCurrentProject.remove(projectName);
                        }

                        if (mapCurrentProject.size() > 0 || foundRemovedProject) {
                            // Set<String> newProjects =
                            // mapCurrentProject.keySet();
                            clearCloneList();
                            try {
                                mapAST.clear();
                                astMethodParser.execute(mapAST);
                            } catch (ExecutionException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }

                            mapSearchManager.clear();
                            for (String key : mapAST.keySet()) {
                                String path = mapAST.get(key);

                                SearchManager searchManager = new SearchManager(
                                        path);
                                try {
                                    searchManager.initIndexEnv();
                                    searchManager.doIndex();
                                } catch (IOException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                } catch (ParseException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }

                                mapSearchManager.put(key, searchManager);
                            }

                            return true;
                        }

                        return false;
                    }

                    //
                    // Create new marker.
                    //
                    public void createMarker(IFile file, int numOfClones)
                            throws CoreException {
                        IMarker marker = file
                                .createMarker("edu.uci.ics.mondego.clonedetection.marker");
                        addAnnotation(marker, getEditor(),
                                lastCloneDetectedPos, numOfClones);
                    }

                    //
                    // Set Marker Color
                    //
                    public void addAnnotation(IMarker marker,
                            ITextEditor editor, int lineNumber, int numOfClones) {

                        removeAnnotation(getEditor());
                        IDocumentProvider idp = editor.getDocumentProvider();
                        IDocument document = idp.getDocument(editor
                                .getEditorInput());
                        IAnnotationModel iamf = idp.getAnnotationModel(editor
                                .getEditorInput());

                        String annType = "edu.uci.ics.mondego.clonedetection.";
                        if (numOfClones <= 5)
                            annType += "green";
                        else if (numOfClones <= 9)
                            annType += "orange";
                        else
                            annType += "red";

                        SimpleMarkerAnnotation ma = new SimpleMarkerAnnotation(
                                annType, marker);
                        iamf.connect(document);
                        iamf.addAnnotation(ma, new Position(lineNumber - 1, 1));
                        iamf.disconnect(document);

                        curAnnotation.put(getEditor(), ma);
                    }

                    //
                    // Check if there are at least one document opened in editor
                    //
                    //
                    public boolean isDocumentOpened() {
                        // editorPart = page.getActiveEditor();
                        if (editorPart == null) {
                            return false;
                        }
                        return true;
                    }

                    //
                    // Check if current working document is saved
                    //
                    public boolean isSaved() {
                        // editorPart = page.getActiveEditor();
                        if (editorPart.isDirty()) {
                            return false;
                        }
                        return true;
                    }

                    //
                    // Check if there are unsaved documents
                    //
                    public IEditorPart[] getDirtyEditors() {

                        if (workbench == null)
                            return null;
                        if (workbenchWindow == null)
                            return null;
                        if (page == null)
                            return null;
                        return page.getDirtyEditors();
                    }

                    //
                    // Check if there are documents opened in editor
                    //
                    /*
                     * public IEditorPart[] getOpenEditors() { if (workbench ==
                     * null) return null; if (workbenchWindow == null) return
                     * null; if (page == null) return null;
                     * 
                     * return (IEditorPart[]) page.getEditorReferences(); }
                     */

                    //
                    // Get current working editor
                    //
                    public IEditorPart getActiveEditorPart() {
                        return editorPart;
                    }

                    //
                    // Get current working document
                    //
                    public IDocument getDocument() {
                        if (editorPart == null)
                            return null;

                        ITextEditor editor = (ITextEditor) editorPart
                                .getAdapter(ITextEditor.class);
                        if (editor == null)
                            return null;
                        IDocumentProvider provider = editor
                                .getDocumentProvider();
                        if (provider == null)
                            return null;
                        IDocument document = provider.getDocument(editorPart
                                .getEditorInput());
                        return document;
                    }

                    //
                    // Get Text editor
                    //
                    public ITextEditor getEditor() {
                        if (editorPart == null)
                            return null;

                        ITextEditor editor = (ITextEditor) editorPart
                                .getAdapter(ITextEditor.class);
                        return editor;
                    }

                    //
                    // Get the information of the function that a user is
                    // working on
                    // This is used to look for the line number that the user is
                    // working on
                    //
                    public ITextSelection getTextSelection() {
                        IDocument document = getDocument();
                        if (document == null)
                            return null;

                        if (editorPart == null)
                            return null;
                        ITextSelection textSelection = (ITextSelection) editorPart
                                .getSite().getSelectionProvider()
                                .getSelection();
                        return textSelection;
                    }

                    //
                    // Clear all clones in the list
                    //
                    public void clearCloneList() {
                        if (workbench == null || workbenchWindow == null
                                || page == null) {
                            return;
                        }
                        IViewReference viewReferences[] = page
                                .getViewReferences();
                        List<String> nullList = new ArrayList<String>();
                        for (int k = 0; k < viewReferences.length; k++) {
                            if (viewReferences[k].getId().equals(
                                    "CloneDetector.resultView")) {
                                ResultView rv = (ResultView) viewReferences[k]
                                        .getView(true);
                                rv.setContent(nullList,
                                        currentWatchUserBehaviorInstance);
                                break;
                            }
                        }
                    }

                    //
                    // Detect whether the marker is clicked
                    //
                    public void checkMarkerClicked() {
                        if (cloneListAlreadyClicked)
                            return;
                        if (lastSearchedFileList.size() == 0)
                            return;
                        IWorkspaceRoot root = ResourcesPlugin.getWorkspace()
                                .getRoot();
                        try {
                            IEditorPart editorPart = getEditor();
                            if (editorPart == null)
                                return;
                            IVerticalRulerInfo info = (IVerticalRulerInfo) editorPart
                                    .getAdapter(IVerticalRulerInfo.class);
                            IMarker[] markers = root
                                    .findMarkers(
                                            "edu.uci.ics.mondego.clonedetection.marker",
                                            true, IResource.DEPTH_INFINITE);
                            for (int j = 0; j < markers.length; j++) {
                                if (lastCloneDetectedStartLine == (info
                                        .getLineOfLastMouseButtonActivity() + 1)) {
                                    IViewReference viewReferences[] = page
                                            .getViewReferences();
                                    /*
                                     * try{
                                     * page.showView("CloneDetector.resultView"
                                     * ); }catch(PartInitException pe){
                                     * System.out.println("caught, "+
                                     * pe.getMessage()); }
                                     */
                                    for (int k = 0; k < viewReferences.length; k++) {
                                        if (viewReferences[k].getId().equals(
                                                "CloneDetector.resultView")) {
                                            ResultView rv = (ResultView) viewReferences[k]
                                                    .getView(true);
                                            rv.setContent(lastSearchedFileList,
                                                    currentWatchUserBehaviorInstance);
                                            cloneListAlreadyClicked = true;
                                            break;
                                        }
                                    }
                                }
                            }
                        } catch (CoreException e1) {
                            return;
                        }
                    }

                    //
                    // Looking for files that are created/changed/deleted
                    //
                    public void watchUpdatedFiles() {
                        IEditorPart[] editorParts = getDirtyEditors();
                        if (editorParts == null) {
                            System.out
                                    .println("no dirty editor found, returning");
                            return;
                        }

                        HashMap<IEditorPart, Integer> currentUnsavedDocs = new HashMap<IEditorPart, Integer>();
                        for (IEditorPart editorPart : editorParts) {
                            currentUnsavedDocs.put(editorPart, (Integer) 0);
                        }

                        if (unsavedDocs.size() > currentUnsavedDocs.size()) {
                            Set<IEditorPart> currentSavedEditorParts = currentUnsavedDocs
                                    .keySet();
                            System.out
                                    .println("removing unsaved editors to get saved editors, "
                                            + unsavedDocs.size());
                            for (IEditorPart editorPart : currentSavedEditorParts) {
                                if (unsavedDocs.containsKey(editorPart))
                                    unsavedDocs.remove(editorPart);
                            }
                            System.out
                                    .println("removed unsaved editors to get saved editors, "
                                            + unsavedDocs.size());
                            if (unsavedDocs.size() > 0) {
                                System.out
                                        .println("found a recently saved editor");
                                Set<IEditorPart> savedEditorPart = unsavedDocs
                                        .keySet();

                                for (IEditorPart editorPart : savedEditorPart) {
                                    ITextEditor editor = (ITextEditor) editorPart
                                            .getAdapter(ITextEditor.class);
                                    if (editor == null) {
                                        System.out
                                                .println("returning from watchupdatedFiles, editor is null");
                                        return;
                                    }
                                    IEditorInput input = editor
                                            .getEditorInput();
                                    String filename = ((IEditorInput) input)
                                            .getName();

                                    String projectName = input.toString();
                                    int pos1 = projectName.indexOf('(', 0);
                                    int pos2 = projectName.indexOf('/',
                                            pos1 + 2);
                                    projectName = projectName.substring(
                                            pos1 + 2, pos2);
                                    System.out.println("calling updateAST");
                                    updateAST(projectName, filename);

                                    unsavedDocs.remove(editorPart);
                                }
                            }
                        }

                        for (IEditorPart editorPart : editorParts) {
                            unsavedDocs.put(editorPart, (Integer) 0);
                        }
                    }

                    //
                    // Update AST (term-frequency) and save them in rawData/
                    // folder
                    //
                    public boolean updateAST(String projectName, String fileName) {
                        System.out.println("inside updateAST: " + projectName
                                + ", " + fileName);
                        if (fileName == "")
                            return false;

                        String path = "";
                        if (mapAST.containsKey(projectName))
                            path = mapAST.get(projectName);
                        if (path == "")
                            return false;

                        File directory = new File(path + "rawData/");
                        File[] fList = directory.listFiles();
                        for (File file : fList) {
                            if (!file.isDirectory()) {
                                if (file.toString().contains(fileName)) {
                                    file.delete();
                                }
                            }
                        }

                        astMethodParser.printSpecificIMethods(fileName, path
                                + "rawData/");
                        try {
                            SearchManager searchManager = null;
                            System.out.println("mapSearchManager is "+ mapSearchManager);
                            if (mapSearchManager.containsKey(projectName)) {
                                searchManager = mapSearchManager
                                        .get(projectName);
                                System.out.println("indexing this project, "+ projectName);
                                searchManager.initIndexEnv();
                                searchManager.doIndex();
                            }else{
                                System.out.println("searchManager not found, so creating a new one and updating it in mapSearchManager");
                                searchManager = new SearchManager(path);
                                searchManager.initIndexEnv();
                                searchManager.doIndex();
                                mapSearchManager.put(projectName, searchManager);
                            }
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (ParseException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }

                        parseCurrentDocument();
                        resetParsingHistory();

                        return true;
                    }

                    //
                    // Parse current document
                    //
                    public boolean parseCurrentDocument() {
                        IFile file = null;
                        String currentFile;
                        try {
                            file = (IFile) editorPart.getEditorInput()
                                    .getAdapter(IFile.class);

                            if (file == null)
                                return false;

                            currentFile = file.toString();
                            if (currentFile.equals(lastParsedFileName))
                                return false;
                            lastParsedFileName = currentFile;

                        } catch (Exception e) {
                            return false;
                        }

                        currentWatchUserBehaviorInstance.currnetMethodKeyList
                                .clear();
                        IProject project = file.getProject();

                        IJavaProject javaProject = JavaCore.create(project);
                        IPackageFragment[] packages;
                        try {
                            packages = javaProject.getPackageFragments();
                            System.out.println("PACKAGES WHILE INDEXING ARE:"
                                    + packages);
                            for (IPackageFragment mypackage : packages) {
                                for (ICompilationUnit unit : mypackage
                                        .getCompilationUnits()) {
                                    IType[] allTypes = unit.getAllTypes();
                                    for (IType type : allTypes) {
                                        String filepath = "L"
                                                + type.getPath().toString();
                                        if (currentFile.equals(filepath)) {
                                            // ASTParser parser =
                                            // ASTParser.newParser(AST.JLS8);
                                            ASTParser parser = ASTParser
                                                    .newParser(AST.JLS4);
                                            parser.setKind(ASTParser.K_COMPILATION_UNIT);
                                            parser.setSource(unit);
                                            parser.setResolveBindings(true);

                                            CompilationUnit parse = (CompilationUnit) parser
                                                    .createAST(null);
                                            MethodVisitor visitor = new MethodVisitor(
                                                    project.getName());
                                            parse.accept(visitor);

                                            IMethod[] methods = type
                                                    .getMethods();
                                            for (IMethod method : methods) {
                                                ASTNode node = UtilASTNode
                                                        .astNode(method);
                                                if (node != null) {
                                                    MethodInfo methodInfo = new MethodInfo();
                                                    methodInfo.path = method
                                                            .getPath();
                                                    methodInfo.methodKey = method
                                                            .getKey();
                                                    UtilASTNode
                                                            .setStartAndEndLine(
                                                                    method,
                                                                    methodInfo);
                                                    // System.out.println("adding currentMethodKeyList"
                                                    // + methodInfo.methodKey);
                                                    currnetMethodKeyList
                                                            .add(methodInfo);
                                                }
                                            }

                                            return true;
                                        }
                                    }
                                }
                            }
                        } catch (JavaModelException e) {
                            return false;
                        }

                        return true;
                    }
                    /* - END - MARKER */
                    /* - END - HELPER METHODS */

                });

                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }
}
