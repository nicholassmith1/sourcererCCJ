package sourcerercc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import antlr.MethodExtractor;

public abstract class Parser {

	
	/* ------------------------------------------------------------------
	 * -- Private Methods
	 * --------------------------------------------------------------- */

	private static Map<String, Integer> makeTokenToFrequencyMap(List<String> tokens) {
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
	
	private static boolean isNotNullNotEmptyNotWhiteSpaceOnlyByJava(final String string) {
		return string != null && !string.isEmpty() && !string.trim().isEmpty();
	}

	private static String getFileName(String classPath, String functionKey) {
		functionKey = functionKey.replace('(', '+');
		functionKey = functionKey.replace(')', '=');
		String baseString = classPath + '#' + functionKey;
		String retString = "";

//		for (int i = 1; i < baseString.length(); i++) {
		for (int i = 0; i < baseString.length(); i++) {
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
	
	/**
	 * Create SourcererCC partial index for a single method.
	 * @param classPath
	 * @param methodKey
	 * @param methodBody
	 * @param outputLocation
	 */
	private static void printMethod(String classPath, String methodKey, String methodBody, String outputLocation) {
		
		String dataFileName = getFileName(Parser.sterilizePath(classPath), methodKey);
		File file = new File(outputLocation + dataFileName);
		
//		System.out.println("write to " + outputLocation + dataFileName + ":: " + classPath + " :: " + methodKey);
		
		/* Ensure path and file exist */
		if (file.getParentFile() != null) {
			file.getParentFile().mkdirs();
		}
		
		if (!file.exists()) {
			try {
//				System.out.println("CREATE FILE");
				file.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
//		System.out.println(">>>>" + file.getAbsolutePath());
		
		/* Generate partial index */
		try (FileWriter dataFile = new FileWriter(file);
			BufferedWriter out = new BufferedWriter(dataFile);) {
			Parser.printMethod2(classPath, methodKey, methodBody, out);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	/* ------------------------------------------------------------------
	 * -- Public Methods
	 * --------------------------------------------------------------- */
	
	public static String sterilizePath(String path) {
    	return path.replace(File.separatorChar, '$');
    }
	
	public static void printMethod2(String classPath, String methodKey, String methodBody, Writer out) {
		final int MIN_TOKEN_IN_METHOD = 25;

		if (isNotNullNotEmptyNotWhiteSpaceOnlyByJava(methodBody)) {
			List<String> tokens = Tokenizer.processMethodBody(methodBody);
			if (tokens.size() > MIN_TOKEN_IN_METHOD) {
				Map<String, Integer> tokenToFrequencyMap = Parser.makeTokenToFrequencyMap(tokens);
				StringBuilder sb = new StringBuilder();
				String dataFileName = getFileName(classPath, methodKey);
				sb.append(dataFileName + "@#@");
				for (String token : tokenToFrequencyMap.keySet()) {
					sb.append(token + "@@::@@" + tokenToFrequencyMap.get(token) + ",");
				}

				if (sb.length() > 0) {
//					FileWriter dataFile;
					try {
						out.write(sb.substring(0, sb.length() - 1));
//						dataFile = new FileWriter(outputLocation + dataFileName);
//						dataFile.write(sb.substring(0, sb.length() - 1));
//						dataFile.flush();
//						dataFile.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}	
	
	/**
	 * Create sourcererCC partial index for the specified file
	 * @param file
	 * @param outputLocation
	 * @throws Exception 
	 * @throws JavaModelException
	 */
	public static void printMethods(String file, String outputLocation) throws Exception {
		MethodExtractor extractor = MethodExtractor.getInstance();
		HashMap<String, String> methods = new HashMap<>(extractor.getMethods(file));
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
}
