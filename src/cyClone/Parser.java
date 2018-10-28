package cyClone;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.antlr.v4.runtime.tree.ParseTreeListener;

import antlr.Utils;
import astParser.Tokenizer;
import utility.Util;

public class Parser {
	private String clonePath = "clones";
	private String rawDataPath = "rawData";
	String projectName;

	public Parser() {
	}

//    public Object execute(HashMap<String, String> projectNameToPathMap)
//            throws ExecutionException {
//        // Get the root of the workspace
//        gson = new GsonBuilder().create();
//
//        this.workspace = ResourcesPlugin.getWorkspace();
//        IWorkspaceRoot root = workspace.getRoot();
//        @SuppressWarnings("unused")
//        Runnable runnable = new Runnable() {
//            public List<File> getSubfolders(String directoryName) {
//                File directory = new File(directoryName);
//                List<File> dirs = new ArrayList<File>();
//
//                // get all the files from a directory
//                File[] fList = directory.listFiles();
//                for (File file : fList) {
//                    if (file.isDirectory()) {
//                        dirs.add(file);
//                    }
//                }
//                return dirs;
//            }
//
//            public void run() {
//                try {
//                    String rootDirPath = ResourcesPlugin.getWorkspace()
//                            .getRoot().getLocation().toString()
//                            + "/";
//                    List<File> dirs = this.getSubfolders(rootDirPath);
//                    for (File projectRoot : dirs) {
//                        String projectPath = projectRoot.getAbsolutePath();
//                        String dotProjectFilePath = projectPath + "/.project";
//                        IPath projectDotProjectFile = new org.eclipse.core.runtime.Path(
//                                dotProjectFilePath);
//                        IProjectDescription projectDescription = workspace
//                                .loadProjectDescription(projectDotProjectFile);
//                        IProject project = workspace.getRoot().getProject(
//                                projectDescription.getName());
//                        JavaCapabilityConfigurationPage.createProject(project,
//                                projectDescription.getLocationURI(), null);
//                    }
//                } catch (CoreException e) {
//                    e.printStackTrace();
//                }
//            }
//        };
//
//        // Get all projects in the workspace
//        IProject[] projects = root.getProjects();
//        // Loop over all projects
//        try {
//            for (IProject project : projects) {
//                if (project.isOpen()) {
//                    try {
//                        this.projectName = project.getName();
//                        System.out.println("PROJECT NAME:" + this.projectName);
//                        // String clonePath =
//                        // project.getWorkspace().getRoot().getLocation().toString()
//                        // + "/" + project.getName().toString() + "/" +
//                        // this.clonePath + "/";
//                        String clonePath = project.getName().toString() + "/"
//                                + this.clonePath + "/";
//                        String rawFilePath = clonePath + this.rawDataPath + "/";
//                        this.createDirs(rawFilePath);
//                        printProjectInfo(project, rawFilePath);
//                        projectNameToPathMap.put(projectName, clonePath);
//                    } catch (Exception e) { // change to CoreException
//                        e.printStackTrace();
//                    }
//                }
//            }
//        } catch (Exception e) {
//            System.out.println("ERROR:" + e.getMessage());
//            e.printStackTrace();
//        }
//
//        System.out.println("finished");
//        return null;
//    }

	private boolean createDirs(String dirname) {
		File dir = new File(dirname);
		if (!dir.exists()) {
			return dir.mkdirs();
		} else {
			return true;
		}
	}

//	private void printProjectInfo(IProject project, String outputLocation) throws CoreException, JavaModelException {
//		if (project.isNatureEnabled("org.eclipse.jdt.core.javanature")) {
//			IJavaProject javaProject = JavaCore.create(project);
//			printPackageInfos(javaProject, outputLocation);
//		}
//	}
//
//	private void printPackageInfos(IJavaProject javaProject, String outputLocation) throws JavaModelException {
//		IPackageFragment[] packages = javaProject.getPackageFragments();
//		for (IPackageFragment mypackage : packages) {
//			if (mypackage.getKind() == IPackageFragmentRoot.K_SOURCE) {
//				printICompilationUnitInfo(mypackage, outputLocation);
//			}
//		}
//	}
//
//	private void printICompilationUnitInfo(IPackageFragment mypackage, String outputLocation)
//			throws JavaModelException {
//		for (ICompilationUnit unit : mypackage.getCompilationUnits()) {
//			printIMethods(unit, outputLocation);
//		}
//	}

//	@SuppressWarnings("unused")
//	private static CompilationUnit parse(ICompilationUnit unit) {
//		ASTParser parser = ASTParser.newParser(AST.JLS4);
//		parser.setKind(ASTParser.K_COMPILATION_UNIT);
//		parser.setSource(unit);
//		parser.setResolveBindings(true);
//		return (CompilationUnit) parser.createAST(null); // parse
//	}

	public static boolean isNotNullNotEmptyNotWhiteSpaceOnlyByJava(final String string) {
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

//    public void printSpecificIMethods(String fileName, String outputLocation) {
//        this.workspace = ResourcesPlugin.getWorkspace();
//        IWorkspaceRoot root = workspace.getRoot();
//        IProject[] projects = root.getProjects();
//
//        for (IProject project : projects) {
//            if (project.isOpen()) {
//                try {
//                    IJavaProject javaProject = JavaCore.create(project);
//                    IPackageFragment[] packages = javaProject
//                            .getPackageFragments();
//                    for (IPackageFragment mypackage : packages) {
//                        if (mypackage.getKind() == IPackageFragmentRoot.K_SOURCE) {
//                            for (ICompilationUnit unit : mypackage
//                                    .getCompilationUnits()) {
//                                IType[] allTypes = unit.getAllTypes();
//                                for (IType type : allTypes) {
//                                    String filepath = "L"
//                                            + type.getPath().toString();
//                                    System.out.println("filepath is : "+ filepath + ", fileName is "+ fileName);
//                                    if (filepath.contains(fileName)) {
//                                        this.printIMethods(unit, outputLocation);
//                                        return;
//                                    }
//                                }
//                            }
//                        }
//                    }
//                } catch (JavaModelException e) {
//                    return;
//                }
//            }
//        }
//    }

//	public void printSpecificIMethods(String fileName, String outputLocation) {
//		this.workspace = ResourcesPlugin.getWorkspace();
//		IWorkspaceRoot root = workspace.getRoot();
//		IProject[] projects = root.getProjects();
//
//		for (IProject project : projects) {
//			if (project.isOpen()) {
//				try {
//					IJavaProject javaProject = JavaCore.create(project);
//					IPackageFragment[] packages = javaProject.getPackageFragments();
//					for (IPackageFragment mypackage : packages) {
//						if (mypackage.getKind() == IPackageFragmentRoot.K_SOURCE) {
//							for (ICompilationUnit unit : mypackage.getCompilationUnits()) {
//								IType[] allTypes = unit.getAllTypes();
//								for (IType type : allTypes) {
//									String filepath = "L" + type.getPath().toString();
//									System.out.println("filepath is : " + filepath + ", fileName is " + fileName);
//									if (filepath.contains(fileName)) {
//										this.printIMethods(unit, outputLocation);
//										return;
//									}
//								}
//							}
//						}
//					}
//				} catch (JavaModelException e) {
//					return;
//				}
//			}
//		}
//	}

	
	/**
	 * Create SourcererCC partial index for a single method.
	 * @param classPath
	 * @param methodKey
	 * @param methodBody
	 * @param outputLocation
	 */
	public void printMethod(String classPath, String methodKey, String methodBody, String outputLocation) {
		final int MIN_TOKEN_IN_METHOD = 25;

		if (isNotNullNotEmptyNotWhiteSpaceOnlyByJava(methodBody)) {
			List<String> tokens = Tokenizer.processMethodBody(methodBody);
			if (tokens.size() > MIN_TOKEN_IN_METHOD) {
				Map<String, Integer> tokenToFrequencyMap = this.makeTokenToFrequencyMap(tokens);
				StringBuilder sb = new StringBuilder();
				String dataFileName = getFileName(classPath, methodKey);
				sb.append(dataFileName + "@#@");
				for (String token : tokenToFrequencyMap.keySet()) {
					sb.append(token + "@@::@@" + tokenToFrequencyMap.get(token) + ",");
				}

				if (sb.length() > 0) {
					FileWriter dataFile;
					try {
						dataFile = new FileWriter(outputLocation + dataFileName);
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
	
	
//	public interface JavaListener extends ParseTreeListener<Token> {
//		  void enterClassDeclaration(JavaParser.ClassDeclarationContext ctx);
//		  void exitClassDeclaration(JavaParser.ClassDeclarationContext ctx);
//		  void enterMethodDeclaration(JavaParser.MethodDeclarationContext ctx);
//		}
	
	
//	public interface JavaListener extends ParseTreeListener {
//		  void enterClassDeclaration(JavaParser.ClassDeclarationContext ctx);
//		  void exitClassDeclaration(JavaParser.ClassDeclarationContext ctx);
//		  void enterMethodDeclaration(JavaParser.MethodDeclarationContext ctx);
//		}
	
	
//    public Python3Parser.File_inputContext parse(File file) throws IOException {
//        String code = readFile(file, Charset.forName("UTF-8"));
//        Python3Lexer lexer = new Python3Lexer(new ANTLRInputStream(code));
//        CommonTokenStream tokens = new CommonTokenStream(lexer);
//        Python3Parser parser = new Python3Parser(tokens);
//        return parser.file_input();
//    }
	
	
	/**
	 * Create sourcererCC partial index for the specified file
	 * @param file
	 * @param outputLocation
	 * @throws Exception 
	 * @throws JavaModelException
	 */
	public void printMethods(String file, String outputLocation) throws Exception {
		HashMap<String, String> methods = Utils.getMethods(file);
//		String classpath;
//		String methodBody;
//
//		IType[] allTypes = unit.getAllTypes();
//		for (IType type : allTypes) {
//			IMethod[] methods = type.getMethods();
//			for (IMethod method : methods) {
//				printMethod(classPath, methodBody, outputLocation);
//			}
//		}
		
		for (Map.Entry<String, String> entry : methods.entrySet()) {
			printMethod(file, entry.getKey(), entry.getValue(), outputLocation);
		}
	}
	

//	private void printIMethods(ICompilationUnit unit, String outputLocation) throws JavaModelException {
//
//		String classpath;
//		String methodBody;
//
//		IType[] allTypes = unit.getAllTypes();
//		for (IType type : allTypes) {
//			IMethod[] methods = type.getMethods();
//			for (IMethod method : methods) {
//				printMethod(classPath, methodBody, outputLocation);
//			}
//		}
//	}

	private Map<String, Integer> makeTokenToFrequencyMap(List<String> tokens) {
		Map<String, Integer> tokenToFrequencyMap = new HashMap<String, Integer>();
		for (String token : tokens) {
			if (token.trim().length() > 0) {
				if (tokenToFrequencyMap.containsKey(token)) {
					tokenToFrequencyMap.put(token, tokenToFrequencyMap.get(token) + 1);
				} else {
					tokenToFrequencyMap.put(token, 1);
				}
			}
		}
		return tokenToFrequencyMap;
	}
}
