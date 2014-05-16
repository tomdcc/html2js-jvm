package io.jdev.html2js

import org.codehaus.groovy.runtime.InvokerHelper;

import spock.lang.Specification
import spock.lang.Unroll

class TemplateBundlerSpec extends Specification {

	private static File destDir
	private TemplateBundler bundler

	void setupSpec() {
		String destDirProp = System.getProperty("template.dest.dir")
		if(destDirProp) {
			destDir = new File(destDirProp)
			if(!destDir.isDirectory()) {
				assert destDir.mkdirs(), "couldn't create dest dir $destDirProp"
			}
		} else {
			destDir = File.createTempDir()
			destDir.deleteOnExit()
		}
	}

	void setup() {
		bundler = new TemplateBundler()
		bundler.base = getGrunt2HtmlFile("test/")
	}


	private File getSourceFile(String name) {
		getGrunt2HtmlFile("test/fixtures/${name}.tpl.html")
	}
	
	private File getExpectedFile(String name) {
		getGrunt2HtmlFile("test/expected/${name}.js")
	}
	
	private File getGrunt2HtmlFile(String path) {
		new File(Thread.currentThread().contextClassLoader.getResource("grunt-html2js/$path").toURI())
	}
	
	private String getExpectedOutput(String expectedFileName) {
		getExpectedFile(expectedFileName).text.replaceAll(/..\/test\//, "").replaceAll(/\r?\n/, System.lineSeparator())
	}
	
	private boolean assertFileContentsEqual(File actualFile, String expectedFileName) {
		assert actualFile.isFile(), "No file $actualFile created"
		assert actualFile.text == getExpectedOutput(expectedFileName)
		true
	}

	//	private static final List<String> TEST_NAMES = ['regex_in_template']

	@spock.lang.IgnoreRest
	void "no base"() {
		given:
		bundler.module = "templates-nobase"
		bundler.base = null
		File destFile = new File(destDir, "no_base.js")
		
		when:
		bundler.bundleTemplates(destFile, getSourceFile("pattern"))
		
		then:
//		destFile.text.indexOf(')
		true
	}
	
	@Unroll
	void "single dest #testName works"() {
		given:
		bundler.module = "templates-$testName"
		InvokerHelper.setProperties(bundler, options)
		File destFile = new File(destDir, "${testName}.js")
		
		when:
		bundler.bundleTemplates(destFile, inputFiles.collect { getSourceFile(it) })
		
		then:
		assertFileContentsEqual(destFile, testName)
		
		where:
		testName                         | inputFiles           | options
		'regex_in_template'              | ['pattern']          | [:]
		'compact_format_default_options' | ['one', 'two']       | [:]
		'compact_format_custom_options'  | ['one', 'two']       | [module: 'my-custom-template-module']
		'multi_lines'                    | ['three']            | [:]
		'double_quotes'                  | ['four']             | [:]
		'single_quotes'                  | ['four']             | [quoteChar:"'"]
		'multi_lines_tabs'               | ['three']            | [indentString:"\t"]
		'multi_lines_4spaces'            | ['three']            | [module: 'templates-multi_lines_4space', indentString:"    "]
		'file_header'                    | ['three']            | [fileHeader: '/* global angular: false */\n']
//		'rename'                         | ['one', 'two']       | [rename: { src -> src.replaceAll(/\.html, '')}] NOT SUPPORTED YET
//		'module_as_function'             | ['one', 'two']       | [module: { file -> NAME_FROM_FUNCTION }] NOT SUPPORTED YET
//		'coffee'                         | ['one', 'two']       | [target: 'coffee' ] NOT SUPPORTED YET
		'strict_mode'                    | ['one']              | [useStrict: true]
//		'htmlmin'                        | ['five']             | [htmlmin: [various:options]] NOT SUPPORTED YET
//		'process_template'               | ['process_template'] | NOT SUPPORTED YET
//		'process_function'               | ['process_function'] | NOT SUPPORTED YET
//		'process_jade'                   | ['process_jade']     | NOT SUPPORTED YET
	}
	
	@Unroll
	void "mapped files #testName works"() {
		given:
		bundler.module = "templates-$testName"
		InvokerHelper.setProperties(bundler, options)
		def src = inputFilesMap.collectEntries { targetName, inputFiles ->
			[new File(destDir, "${targetName}.js"), inputFiles.collect { getSourceFile(it) }]
		}
		
		when:
		bundler.bundleTemplates(src)
		
		then:
		inputFilesMap.each { targetName, inputFiles ->
			assertFileContentsEqual(new File(destDir, "${targetName}.js"), targetName)
		}
		
		where:
		testName                       | inputFilesMap                                                                      | options
		'files_object_default_options' | [files_object_default_options_1: ['one'], files_object_default_options_2: ['two']] | [:]
		'files_array_default_options'  | [files_array_default_options_1: ['one'], files_array_default_options_2: ['two']]   | [:]
		'files_object_custom_options'  | [files_object_custom_options_1: ['one'], files_object_custom_options_2: ['two']]   | [module: 'my-custom-template-module']
		// this one doesn't work yet, we don't support module-per-file yet
		//'files_array_custom_options'   | [files_array_custom_options_1: ['one'], files_array_custom_options_2: ['two']]     | [module: 'my-custom-template-module']
	}

}
