package astParser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.ui.wizards.JavaCapabilityConfigurationPage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import utility.Util;

public class ASTMethodParser {
    private String clonePath = "clones";
    private String rawDataPath = "rawData";
    public IWorkspace workspace;
    String projectName;
    Gson gson = null;

    public ASTMethodParser() {
    }

    public Object execute(HashMap<String, String> projectNameToPathMap)
            throws ExecutionException {
        // Get the root of the workspace
        gson = new GsonBuilder().create();

        this.workspace = ResourcesPlugin.getWorkspace();
        IWorkspaceRoot root = workspace.getRoot();
        @SuppressWarnings("unused")
        Runnable runnable = new Runnable() {
            public List<File> getSubfolders(String directoryName) {
                File directory = new File(directoryName);
                List<File> dirs = new ArrayList<File>();

                // get all the files from a directory
                File[] fList = directory.listFiles();
                for (File file : fList) {
                    if (file.isDirectory()) {
                        dirs.add(file);
                    }
                }
                return dirs;
            }

            public void run() {
                try {
                    String rootDirPath = ResourcesPlugin.getWorkspace()
                            .getRoot().getLocation().toString()
                            + "/";
                    List<File> dirs = this.getSubfolders(rootDirPath);
                    for (File projectRoot : dirs) {
                        String projectPath = projectRoot.getAbsolutePath();
                        String dotProjectFilePath = projectPath + "/.project";
                        IPath projectDotProjectFile = new org.eclipse.core.runtime.Path(
                                dotProjectFilePath);
                        IProjectDescription projectDescription = workspace
                                .loadProjectDescription(projectDotProjectFile);
                        IProject project = workspace.getRoot().getProject(
                                projectDescription.getName());
                        JavaCapabilityConfigurationPage.createProject(project,
                                projectDescription.getLocationURI(), null);
                    }
                } catch (CoreException e) {
                    e.printStackTrace();
                }
            }
        };

        // Get all projects in the workspace
        IProject[] projects = root.getProjects();
        // Loop over all projects
        try {
            for (IProject project : projects) {
                if (project.isOpen()) {
                    try {
                        this.projectName = project.getName();
                        System.out.println("PROJECT NAME:" + this.projectName);
                        // String clonePath =
                        // project.getWorkspace().getRoot().getLocation().toString()
                        // + "/" + project.getName().toString() + "/" +
                        // this.clonePath + "/";
                        String clonePath = project.getName().toString() + "/"
                                + this.clonePath + "/";
                        String rawFilePath = clonePath + this.rawDataPath + "/";
                        this.createDirs(rawFilePath);
                        printProjectInfo(project, rawFilePath);
                        projectNameToPathMap.put(projectName, clonePath);
                    } catch (Exception e) { // change to CoreException
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("ERROR:" + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("finished");
        return null;
    }

    private boolean createDirs(String dirname) {
        File dir = new File(dirname);
        if (!dir.exists()) {
            return dir.mkdirs();
        } else {
            return true;
        }
    }

    private void printProjectInfo(IProject project, String outputLocation)
            throws CoreException, JavaModelException {
        if (project.isNatureEnabled("org.eclipse.jdt.core.javanature")) {
            IJavaProject javaProject = JavaCore.create(project);
            printPackageInfos(javaProject, outputLocation);
        }
    }

    private void printPackageInfos(IJavaProject javaProject,
            String outputLocation) throws JavaModelException {
        IPackageFragment[] packages = javaProject.getPackageFragments();
        for (IPackageFragment mypackage : packages) {
            if (mypackage.getKind() == IPackageFragmentRoot.K_SOURCE) {
                printICompilationUnitInfo(mypackage, outputLocation);
            }
        }
    }

    private void printICompilationUnitInfo(IPackageFragment mypackage,
            String outputLocation) throws JavaModelException {
        for (ICompilationUnit unit : mypackage.getCompilationUnits()) {
            printIMethods(unit, outputLocation);
        }
    }

    @SuppressWarnings("unused")
    private static CompilationUnit parse(ICompilationUnit unit) {
        ASTParser parser = ASTParser.newParser(AST.JLS4);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(unit);
        parser.setResolveBindings(true);
        return (CompilationUnit) parser.createAST(null); // parse
    }

    public static boolean isNotNullNotEmptyNotWhiteSpaceOnlyByJava(
            final String string) {
        return string != null && !string.isEmpty() && !string.trim().isEmpty();
    }

    private String getFileName(String classPath, String functionKey) {
        functionKey = functionKey.replace('(', '+');
        functionKey = functionKey.replace(')', '=');
        String baseString = classPath + '#' + functionKey;
        String retString = "";

        for (int i = 1; i < baseString.length(); i++) {
            if (baseString.charAt(i) == '\r')
                continue;
            if (baseString.charAt(i) == '/')
                retString += '$';
            else
                retString += baseString.charAt(i);
        }

        retString += ".txt";
        return retString;
    }

    public void printSpecificIMethods(String fileName, String outputLocation) {
        this.workspace = ResourcesPlugin.getWorkspace();
        IWorkspaceRoot root = workspace.getRoot();
        IProject[] projects = root.getProjects();

        for (IProject project : projects) {
            if (project.isOpen()) {
                try {
                    IJavaProject javaProject = JavaCore.create(project);
                    IPackageFragment[] packages = javaProject
                            .getPackageFragments();
                    for (IPackageFragment mypackage : packages) {
                        if (mypackage.getKind() == IPackageFragmentRoot.K_SOURCE) {
                            for (ICompilationUnit unit : mypackage
                                    .getCompilationUnits()) {
                                IType[] allTypes = unit.getAllTypes();
                                for (IType type : allTypes) {
                                    String filepath = "L"
                                            + type.getPath().toString();
                                    System.out.println("filepath is : "+ filepath + ", fileName is "+ fileName);
                                    if (filepath.contains(fileName)) {
                                        this.printIMethods(unit, outputLocation);
                                        return;
                                    }
                                }
                            }
                        }
                    }
                } catch (JavaModelException e) {
                    return;
                }
            }
        }
    }

    private void printIMethods(ICompilationUnit unit, String outputLocation)
            throws JavaModelException {
        final int MIN_TOKEN_IN_METHOD = 25;
        IType[] allTypes = unit.getAllTypes();
        for (IType type : allTypes) {
            IMethod[] methods = type.getMethods();
            for (IMethod method : methods) {
                String methodBody = method.getSource();
                if (isNotNullNotEmptyNotWhiteSpaceOnlyByJava(methodBody)) {
                    List<String> tokens = Tokenizer
                            .processMethodBody(methodBody);
                    if (tokens.size() > MIN_TOKEN_IN_METHOD) {
                        Map<String, Integer> tokenToFrequencyMap = this
                                .makeTokenToFrequencyMap(tokens);
                        StringBuilder sb = new StringBuilder();
                        String dataFileName = getFileName(unit.getPath()
                                .toString(), method.getKey());
                        sb.append(dataFileName + "@#@");
                        for (String token : tokenToFrequencyMap.keySet()) {
                            sb.append(token + "@@::@@"
                                    + tokenToFrequencyMap.get(token) + ",");
                        }

                        if (sb.length() > 0) {
                            FileWriter dataFile;
                            try {
                                dataFile = new FileWriter(outputLocation
                                        + dataFileName);
                                dataFile.write(sb.substring(0, sb.length() - 1));
                                dataFile.flush();
                                dataFile.close();
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
    }

    private Map<String, Integer> makeTokenToFrequencyMap(List<String> tokens) {
        Map<String, Integer> tokenToFrequencyMap = new HashMap<String, Integer>();
        for (String token : tokens) {
            if (token.trim().length() > 0) {
                if (tokenToFrequencyMap.containsKey(token)) {
                    tokenToFrequencyMap.put(token,
                            tokenToFrequencyMap.get(token) + 1);
                } else {
                    tokenToFrequencyMap.put(token, 1);
                }
            }
        }
        return tokenToFrequencyMap;
    }
}