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
import org.semanticweb.clipper.hornshiq.queryanswering.ClipperManager;
import org.semanticweb.clipper.hornshiq.queryanswering.ClipperReport;
import org.semanticweb.clipper.hornshiq.queryanswering.QAHornSHIQ;
import org.semanticweb.clipper.hornshiq.queryanswering.ReductionToDatalogOpt.NamingStrategy;
import org.semanticweb.clipper.hornshiq.rule.Atom;
import org.semanticweb.clipper.hornshiq.rule.CQ;
import org.semanticweb.clipper.hornshiq.rule.DLPredicate;
import org.semanticweb.clipper.hornshiq.rule.Variable;
import org.semanticweb.clipper.hornshiq.sparql.SparqlParser;
import org.semanticweb.clipper.util.SymbolEncoder;
import org.semanticweb.clipper.hornshiq.cli.QueryResultPrinter;
import org.semanticweb.clipper.hornshiq.cli.TableQueryResultPrinter;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;

public class RunDatalog {

//    private static String getFullName(String prefixedName){
//        String original = prefixedName.toString();
//        int i = original.indexOf(":");
//        String prefix = original.substring(0,i);
//        String full = namespaces.get(prefix) + original.substring(i+1);
//        return full;
//      }
	private Map<File, File[]> mFiles;

	public RunDatalog(String[] args) {

		mFiles = processArguments(args);
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
				aOntoFile = (args[0] != null) ? new File(args[0]) : null;

				if (aOntoFile != null && !aOntoFile.exists()) {
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

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		RunDatalog aRunner = new RunDatalog(args);
		aRunner.run(5, 10);
	}

	public void run(int warms, int times) {
		System.out.println("[- Running experiment -]");
		int totRuns = warms + times;
		Map<String, ClipperReport> theResultsMap = new HashMap<String, ClipperReport>();

		for (int i=1; i <= totRuns; i++) {
			for (Entry<File, File[]> entry : mFiles.entrySet()) {
				theResultsMap = new HashMap<String, ClipperReport>();
				File aOntoFile = entry.getKey();
				File[] aDatalogFiles = entry.getValue();

				for (File aDatalogFile : aDatalogFiles) {
					System.out.println("Evaluating for datalog: " + aDatalogFile.getName());

					ClipperReport aReport = evaluate(aDatalogFile.getAbsolutePath());
					theResultsMap.put(aDatalogFile.getName(), aReport);
				}
				
				if (i > totRuns - times) {
					TimeReporter.getInstance().setActive(true);
					System.out.println("Experiment: "+ (i-warms) +" #####################");

					if (aOntoFile != null) {
						System.out.println("Ontology file: "+ aOntoFile.getName());
					}
					System.out.println("-------------------------------");
					System.out.format("query\t\teval time%n");

					for (Entry<String, ClipperReport> aResEntry : theResultsMap.entrySet()) {
						String aDatalogName = aResEntry.getKey();
						ClipperReport aRes = aResEntry.getValue();

						TimeReporter.getInstance().setCurrentQuery(aDatalogName);

						long evalTime = aRes.getDatalogRunTime();

						TimeReporter.getInstance().addEvalTime(evalTime);

						System.out.format(aDatalogName + "\t\t" +
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
		System.out.format("query\teval time%n");
		for (String qName : theResultsMap.keySet()) {
			System.out.println("-----------------------------------------------------");
			System.out.format(qName +"\t"+ 
				TimeReporter.getInstance().getAverageEvalTime(qName) + "%n"
			);
		}
	}

	public ClipperReport evaluate(String theFilePath) {
	
		ClipperManager.getInstance().setVerboseLevel(0);

		QAHornSHIQ qaHornSHIQ = new QAHornSHIQ();

//		File ontologyFile = new File(ontologyFileName);
//		String ontologyDir = ontologyFile.getParent();
//		String name = ontologyFile.getName();
//		String string = Files.getFileExtension(ontologyFileName);
//		
//		String sparqlName = new File(sparqlFileName).getName();
		
		//qaHornSHIQ.setOntologyName(theOntologyPath);
//		qaHornSHIQ.setDataLogName(ontologyFileName + "-" + sparqlName + ".dl");
		qaHornSHIQ.setDataLogName(theFilePath);
		
		
//		qaHornSHIQ.setCq(cq);
		qaHornSHIQ.setQueryRewriter("new");

		qaHornSHIQ.setDlvPath("/usr/bin/dlv");

		long startTime = System.currentTimeMillis();
		List<List<String>> answers = qaHornSHIQ.runDatalogEngineWithoutRewriting();
		long endTime = System.currentTimeMillis();

		QueryResultPrinter printer = new TableQueryResultPrinter();//createQueryResultPrinter(cmd.getOutputFormat());

//		printer.print(cq.getHead(), answers);

		if (ClipperManager.getInstance().getVerboseLevel() > 0) {
			statistics(qaHornSHIQ.getClipperReport(), startTime, endTime);
		}

		return qaHornSHIQ.getClipperReport();
	}

	/**
	 * @param qaHornSHIQ
	 * @param startTime
	 * @param endTime
	 */
	private static void statistics(ClipperReport clipperReport, long startTime, long endTime) {
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