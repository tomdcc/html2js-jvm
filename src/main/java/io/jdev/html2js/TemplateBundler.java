package io.jdev.html2js;

import static java.lang.System.lineSeparator;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.regex.Pattern;

public class TemplateBundler {

	private String module;
	private File base;
	private String quoteChar = "\"";
	private Pattern quoteCharRegex = Pattern.compile("\\" + quoteChar);
	private String indentString = "  ";
	private boolean useStrict = false;
	private String fileHeader = null;
	
	public void bundleTemplates(File targetFile, File... files) throws IOException {
		bundleTemplates(targetFile, Arrays.asList(files));
	}
	
	private String relativePath(File srcFile) {
		if(base != null) {
			// find relative to the given base
			return base.toURI().relativize(srcFile.toURI()).getPath();
		} else {
			// who knows? just give filename
			return srcFile.getName();
		}
	}
	
	public void bundleTemplates(Map<File,Collection<File>> files) throws IOException {
		for(Map.Entry<File,Collection<File>> entry : files.entrySet()) {
			bundleTemplates(entry.getKey(), entry.getValue());
		}
	}
	
	public void bundleTemplates(File targetFile, Collection<File> src) throws IOException {
		// take copy in case was passed immutable list
		List<File> files = new ArrayList<>(src);
		
		// remove any non-existant files
		for(ListIterator<File> lit = files.listIterator(); lit.hasNext();) {
			File file = lit.next();
			if(!file.isFile()) {
				lit.remove();
			}
		}
		
		if(files.isEmpty()) {
			return;
		}
		
		StringBuilder moduleNames = new StringBuilder();
		StringBuilder modules = new StringBuilder();
		for(File srcFile : files) {
			String moduleName = relativePath(srcFile);
			if(moduleNames.length() != 0) {
				moduleNames.append(", ");
			}
			moduleNames.append("'");
			moduleNames.append(moduleName);
			moduleNames.append("'");
			
			if(modules.length() != 0) {
				modules.append(lineSeparator());
			}
			compileTemplate(modules, moduleName, srcFile);
		}
		
		
		String bundle = "";
		if(module != null) {
		    bundle = "angular.module('" + module + "', [" + moduleNames + "]);"  + lineSeparator() + lineSeparator();
		}

        try(PrintWriter writer = new PrintWriter(targetFile)) {
        	if(fileHeader != null) {
        		writer.print(fixNewlines(fileHeader + "\n"));
        	}
        	writer.print(bundle);
        	writer.print(fixNewlines(modules.toString()));
        }
	}
	
	private static final Pattern NL_REGEXP = Pattern.compile("\\r?\\n");
	private String fixNewlines(String src) {
		return NL_REGEXP.matcher(src).replaceAll(lineSeparator());
	}

	private void compileTemplate(StringBuilder modulesBuf, String moduleName, File srcFile) throws IOException {
		String content = getContent(srcFile);
		String doubleIndent = indentString + indentString;
		String strict = (useStrict) ? indentString + quoteChar + "use strict" + quoteChar + ";\n" : "";

		modulesBuf.append("angular.module(").append(quoteChar).append(moduleName).append(quoteChar).append(", []).run([").append(quoteChar).append("$templateCache").append(quoteChar).append(", function($templateCache) ").append("{\n");
		modulesBuf.append(strict);
		modulesBuf.append(indentString).append("$templateCache.put(").append(quoteChar).append(moduleName).append(quoteChar).append(",\n");
		modulesBuf.append(doubleIndent).append(quoteChar).append(content).append(quoteChar).append(");\n}]);\n");
	}

	private static String readFile(File file, Charset encoding) throws IOException {
		byte[] encoded = Files.readAllBytes(file.toPath());
		return encoding == null ? new String(encoded) : new String(encoded, encoding);
	}
	
	private static final Pattern WS_REGEXP = Pattern.compile("(^\\s*)");
	private String getContent(File file) throws IOException {
		String content = readFile(file, null);
		content = WS_REGEXP.matcher(content).replaceAll("");
		return escapeContent(content);
	}

	private static final Pattern BS_REGEXP = Pattern.compile("\\\\");
	  
	private String escapeContent(String content) {
		String nlReplace = "\\\\n" + quoteChar + " +\n" + indentString + indentString + quoteChar;
		content = BS_REGEXP.matcher(content).replaceAll("\\\\\\\\");
		content = quoteCharRegex.matcher(content).replaceAll("\\\\" + quoteChar);
		return NL_REGEXP.matcher(content).replaceAll(nlReplace);
	}
		  
	public String getModule() {
		return module;
	}

	public void setModule(String module) {
		this.module = module;
	}

	public File getBase() {
		return base;
	}

	public void setBase(File base) {
		this.base = base;
	}

	public String getQuoteChar() {
		return quoteChar;
	}

	public void setQuoteChar(String quoteChar) {
		this.quoteChar = quoteChar;
		quoteCharRegex = Pattern.compile("\\" + quoteChar);
	}

	public String getIndentString() {
		return indentString;
	}

	public void setIndentString(String indentString) {
		this.indentString = indentString;
	}

	public boolean isUseStrict() {
		return useStrict;
	}

	public void setUseStrict(boolean useStrict) {
		this.useStrict = useStrict;
	}

	public String getFileHeader() {
		return fileHeader;
	}

	public void setFileHeader(String fileHeader) {
		this.fileHeader = fileHeader;
	}
}
