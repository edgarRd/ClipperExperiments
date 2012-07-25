package com.clarkparsia.rpi.clipper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.Collection;
import java.util.Set;
import java.util.Map;
import java.util.Map.Entry;
import java.util.HashMap;

import org.antlr.runtime.RecognitionException;

import org.semanticweb.clipper.hornshiq.queryanswering.QAHornSHIQ;
import org.semanticweb.clipper.hornshiq.queryanswering.ClipperManager;
import org.semanticweb.clipper.hornshiq.queryanswering.ClipperReport;
import org.semanticweb.clipper.hornshiq.rule.CQ;
import org.semanticweb.clipper.hornshiq.queryanswering.ReductionToDatalogOpt.NamingStrategy;
import org.semanticweb.clipper.hornshiq.sparql.SparqlParser;

public class ClipperRunner {

	private QAHornSHIQ mHornSHIQ;

	private final String queryPrefix;
	private String headPredicate;

	private final String queryRewriter = "new";
	private Collection<CQ> rewrittenQueries;

	private int debugLevel;

	private Map<File, File[]> mFiles;

	public ClipperRunner(String[] args) {
		mHornSHIQ = new QAHornSHIQ();

		queryPrefix = null;

		mFiles = processArguments(args);
	}

	public int getDebugLevel() {
		return this.debugLevel;
	}

	public void setDebug(int theDebugLevel) {
		this.debugLevel = theDebugLevel;
	}

	private Map<File, File[]> processArguments(String[] args) {
		File aOntoFile = null;
		File[] aQueryFileSet = null;
		String queryDirStr = args[1];
		Map<File, File[]> theFilesMap = new HashMap<File, File[]>();

		if (args.length == 2) {

			System.out.println("args[0] --> "+ args[0]);
			System.out.println("args[1] --> "+ args[1]);

			try {
				aOntoFile = new File(args[0]);

				if (!aOntoFile.exists()) {
					throw new FileNotFoundException("File "+ args[0] + " was not found!");
				}

				// Get queries files
				File queryDir = new File(queryDirStr);

				aQueryFileSet = queryDir.listFiles();
				
				theFilesMap.put(aOntoFile, aQueryFileSet);
			}
			catch (FileNotFoundException fnfe) {
				System.err.println("File not found: "+ fnfe.getMessage());
				fnfe.printStackTrace();
				System.exit(1);
			}
		}
		else {
			System.out.println("Missing input files.");
			System.exit(1);
		}

		return theFilesMap;
	}

	public void run(int times) {
		System.out.println("[- Running experiment -]");

		for (int i=0; i < times; i++) {
			for (Entry<File, File[]> entry : mFiles.entrySet()) {
				Map<String, ClipperReport> theResultsMap = new HashMap<String, ClipperReport>();
				File aOntoFile = entry.getKey();
				File[] aQueryFiles = entry.getValue();

				for (File aQueryFile : aQueryFiles) {
					System.out.println("Rewriting for query: " + aQueryFile.getName());
					ClipperReport aReport = rewrite(aOntoFile, aQueryFile);
					theResultsMap.put(aQueryFile.getName(), aReport);
				}
				
				System.out.println("Ontology file: "+ aOntoFile.getName());
				System.out.println("-------------------------------");
				System.out.format("query\t\t\treasoning time\trewriting time\trewriting size\ttotal%n");

				for (Entry<String, ClipperReport> aResEntry : theResultsMap.entrySet()) {
					String aQueryName = aResEntry.getKey();
					ClipperReport aRes = aResEntry.getValue();

					long totalTime = aRes.getReasoningTime() + aRes.getQueryRewritingTime();

					System.out.println(aQueryName + "\t" +
						aRes.getReasoningTime() + "\t\t" + 
						aRes.getQueryRewritingTime() + "\t\t" +
						aRes.getNumberOfRewrittenQueriesAndRules() + "\t\t" +
						totalTime + "\n");
				}
			}
		}
	}

	public static void main(String[] args) {
		ClipperRunner aRunner = new ClipperRunner(args);
		aRunner.run(1);
	}

	private ClipperReport rewrite(File theOntologyFile, File theQueryFile) {
		System.setProperty("entityExpansionLimit", "512000");

		SparqlParser sparqlParser = null;
		try {
			sparqlParser = new SparqlParser(theQueryFile.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}

		CQ cq = null;
		try {
			cq = sparqlParser.query();
		} catch (RecognitionException e) {
			e.printStackTrace();
		}

		QAHornSHIQ qaHornSHIQ = new QAHornSHIQ();
		// note that naming strategy shoud be set after create new QAHornSHIQ
		ClipperManager.getInstance().setNamingStrategy(NamingStrategy.LowerCaseFragment);
		ClipperManager.getInstance().setVerboseLevel(debugLevel);

		String ontologyFileName = theOntologyFile.getAbsolutePath();
		qaHornSHIQ.setOntologyName(ontologyFileName);

		qaHornSHIQ.setDataLogName(ontologyFileName + ".dl");
		qaHornSHIQ.setCq(cq);
		qaHornSHIQ.setQueryRewriter("new");

		qaHornSHIQ.computeClipperRewritings();
		
		// long totalTime = qaHornSHIQ.getClipperReport().getReasoningTime()
		// 		+ qaHornSHIQ.getClipperReport().getQueryRewritingTime();
		// System.out.println(qaHornSHIQ.getClipperReport().getNumberOfRewrittenQueries() + " "
		// 		+ qaHornSHIQ.getClipperReport().getNumberOfRewrittenQueriesAndRules() + " " + totalTime);

		return qaHornSHIQ.getClipperReport();
	}
}