package com.clarkparsia.rpi.clipper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.Collection;
import java.util.Set;
import java.util.Map;
import java.util.Map.Entry;
import java.util.HashMap;
import java.util.List;

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

	public void run(int warms, int times) {
		System.out.println("[- Running experiment -]");
		int totRuns = warms + times;
		Map<String, ClipperReport> theResultsMap = new HashMap<String, ClipperReport>();

		for (int i=1; i <= totRuns; i++) {
			for (Entry<File, File[]> entry : mFiles.entrySet()) {
				theResultsMap = new HashMap<String, ClipperReport>();
				File aOntoFile = entry.getKey();
				File[] aQueryFiles = entry.getValue();

				for (File aQueryFile : aQueryFiles) {
					System.out.println("Rewriting for query: " + aQueryFile.getName());

					ClipperReport aReport = query(aOntoFile.getAbsolutePath(), aQueryFile.getAbsolutePath());
					theResultsMap.put(aQueryFile.getName(), aReport);
				}
				
				if (i > totRuns - times) {
					TimeReporter.getInstance().setActive(true);
					System.out.println("Experiment: "+ (i-warms) +" #####################");

					System.out.println("Ontology file: "+ aOntoFile.getName());
					System.out.println("-------------------------------");
					System.out.format("query\t\trw size\treasoning time\trw time\ttot rw time\teval time%n");

					for (Entry<String, ClipperReport> aResEntry : theResultsMap.entrySet()) {
						String aQueryName = aResEntry.getKey();
						ClipperReport aRes = aResEntry.getValue();

						TimeReporter.getInstance().setCurrentQuery(aQueryName);

						long rwTime = aRes.getReasoningTime() + aRes.getQueryRewritingTime();
						long evalTime = aRes.getDatalogRunTime() + aRes.getOutputAnswerTime();
						int rwSize = aRes.getNumberOfRewrittenQueriesAndRules();

						TimeReporter.getInstance().addRwTime(rwTime);
						TimeReporter.getInstance().addEvalTime(evalTime);
						TimeReporter.getInstance().setRwSize(rwSize);

						System.out.format(aQueryName + "\t" +
							aRes.getNumberOfRewrittenQueriesAndRules() + "\t"+
							aRes.getReasoningTime() + "\t" + 
							aRes.getQueryRewritingTime() + "\t" +
							rwTime + "\t" +
							evalTime + "%n");
					}
				}
				else {
					TimeReporter.getInstance().setActive(false);
					System.out.println("Warmup: "+ i +" Finished #####################");
				}
			}
		}

		System.out.println("\n\n");
		System.out.println("Printing Experiment Summary");
		System.out.format("query\trw size\ttot rw time\teval time%n");
		for (String qName : theResultsMap.keySet()) {
			System.out.println("-----------------------------------------------------");
			System.out.format(qName +"\t"+ 
				TimeReporter.getInstance().getRwSize(qName) +"\t"+
				TimeReporter.getInstance().getAverageRwTime(qName) +"\t"+
				TimeReporter.getInstance().getAverageEvalTime(qName) + "%n"
			);
		}
	}

	public static void main(String[] args) {
		ClipperRunner aRunner = new ClipperRunner(args);
		aRunner.run(5, 10);
	}

	// private ClipperReport rewrite(File theOntologyFile, File theQueryFile) {
	// 	System.setProperty("entityExpansionLimit", "512000");

	// 	SparqlParser sparqlParser = null;
	// 	try {
	// 		sparqlParser = new SparqlParser(theQueryFile.getAbsolutePath());
	// 	} catch (IOException e) {
	// 		e.printStackTrace();
	// 	}

	// 	CQ cq = null;
	// 	try {
	// 		cq = sparqlParser.query();
	// 	} catch (RecognitionException e) {
	// 		e.printStackTrace();
	// 	}

	// 	QAHornSHIQ qaHornSHIQ = new QAHornSHIQ();
	// 	// note that naming strategy shoud be set after create new QAHornSHIQ
	// 	ClipperManager.getInstance().setNamingStrategy(NamingStrategy.LowerCaseFragment);
	// 	ClipperManager.getInstance().setVerboseLevel(debugLevel);

	// 	String ontologyFileName = theOntologyFile.getAbsolutePath();
	// 	qaHornSHIQ.setOntologyName(ontologyFileName);

	// 	qaHornSHIQ.setDataLogName(ontologyFileName + ".dl");
	// 	qaHornSHIQ.setCq(cq);
	// 	qaHornSHIQ.setQueryRewriter("new");

	// 	qaHornSHIQ.computeClipperRewritings();
		
	// 	// long totalTime = qaHornSHIQ.getClipperReport().getReasoningTime()
	// 	// 		+ qaHornSHIQ.getClipperReport().getQueryRewritingTime();
	// 	// System.out.println(qaHornSHIQ.getClipperReport().getNumberOfRewrittenQueries() + " "
	// 	// 		+ qaHornSHIQ.getClipperReport().getNumberOfRewrittenQueriesAndRules() + " " + totalTime);

	// 	return qaHornSHIQ.getClipperReport();
	// }

	private ClipperReport query(String theOntoPath, String theQueryPath) {
		ClipperManager.getInstance().setNamingStrategy(NamingStrategy.LowerCaseFragment);
		System.setProperty("entityExpansionLimit", "512000");
		String ontologyFileName = theOntoPath;
		String sparqlFileName = theQueryPath;
		SparqlParser sparqlParser = null;
		try {
			sparqlParser = new SparqlParser(sparqlFileName);
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

//		File ontologyFile = new File(ontologyFileName);
//		String ontologyDir = ontologyFile.getParent();
//		String name = ontologyFile.getName();
//		String string = Files.getFileExtension(ontologyFileName);
//		
		String sparqlName = new File(sparqlFileName).getName();
		
		qaHornSHIQ.setOntologyName(ontologyFileName);
		qaHornSHIQ.setDataLogName(ontologyFileName + "-" + sparqlName + ".dl");
		qaHornSHIQ.setCq(cq);
		qaHornSHIQ.setQueryRewriter("new");

		qaHornSHIQ.setDlvPath("/usr/bin/dlv");

		long startTime = System.currentTimeMillis();
		List<List<String>> answers = qaHornSHIQ.runDatalogEngine();
		long endTime = System.currentTimeMillis();

		

		// QueryResultPrinter printer = createQueryResultPrinter(cmd.getOutputFormat());

		// printer.print(cq.getHead(), answers);

		statistics(qaHornSHIQ.getClipperReport(), startTime, endTime);

		return qaHornSHIQ.getClipperReport();
	}

	private void statistics(ClipperReport clipperReport, long startTime, long endTime) {
		System.out.println("Ontology parsing and normalization time:                      "
				+ clipperReport.getNormalizationTime() + "  milliseconds");
		System.out.println("Reasoning time:                                               "
				+ clipperReport.getReasoningTime() + "  milliseconds");
		System.out.println("Query rewriting time:                                         "
				+ clipperReport.getQueryRewritingTime() + "  milliseconds");
		long totalTime = clipperReport.getReasoningTime() + clipperReport.getQueryRewritingTime();
		System.out.println("Total time for query rewriting (reasoning + rewriting time):  " + totalTime
				+ "  milliseconds");
		System.out.println("Total rules/rewritten queries: " + clipperReport.getNumberOfRewrittenQueriesAndRules());
		System.out.println("Time of running datalog program:                              "
				+ clipperReport.getDatalogRunTime() + "  milliseconds");
		System.out.println("Time for output answer  :                                     "
				+ clipperReport.getOutputAnswerTime() + "  milliseconds");
		System.out.println("Time for counting queries realted rules (just for benchmark): "
				+ clipperReport.getCoutingRealtedRulesTime() + "  milliseconds");
		long runningTime = endTime - startTime - clipperReport.getCoutingRealtedRulesTime();
		System.out.println("Total running time of the whole system:                       " + runningTime
				+ "  milliseconds");
	}
}