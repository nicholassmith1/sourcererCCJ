/**
 * 
 */
/**
 * @author nism
 *
 */
module SourcererCCI {
	exports models;
	exports clonedetector;
	exports noindex;
	exports postprocessing;
	exports astParser;
	exports utility;
	exports indexbased;
	exports userInterface;
	exports validation;

	requires gson;
	requires guava;
	requires java.desktop;
	requires java.xml;
	requires lucene.analyzers.common;
	requires lucene.core;
	requires lucene.queryparser;
	requires org.apache.commons.io;
}