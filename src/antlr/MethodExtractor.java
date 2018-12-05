/*
 * See https://github.com/adamsiemion/antlr-java-ast/tree/master/src/main
 */

package antlr;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MethodExtractor {
	
	static Map<String, MethodExtractorI> parsers;
	protected static MethodExtractor instance = null;
	
	static {
		Map<String, MethodExtractorI> map = new HashMap<String, MethodExtractorI>();
		map.put("JAVA", new JavaMethodExtractor());
		map.put("java", new JavaMethodExtractor());
		map.put("m", new MatlabMethodExtractor());
		map.put("c", new CMethodExtractor());
		map.put("C", new CMethodExtractor());
		
		parsers = Collections.unmodifiableMap(map);
	}
	
	private MethodExtractor() {
		;
	}
	
	public static synchronized MethodExtractor getInstance() {
		if (null == instance) {
			instance = new MethodExtractor();
		}
		return instance;
	}
	
	public String[] getSupportedExtensions() {
		String[] rtn = new String[parsers.keySet().size()];
		
		parsers.keySet().toArray(rtn);
		return rtn;
	}
	
	public Map<String, String> getMethods(String inputFile) {
		HashMap<String, String> methods = new HashMap<>();
		
		/* Chain or responsibility iterator */
		for (Map.Entry<String, MethodExtractorI> entry : parsers.entrySet()) {
			if (inputFile.endsWith("." + entry.getKey())) {
				methods.putAll(entry.getValue().getMethods(inputFile));
				break;
			}
		}
		
		return methods;
	}
}
