package org.semanticweb.clipper.hornshiq.cli;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.semanticweb.clipper.hornshiq.rule.Atom;
import org.semanticweb.clipper.hornshiq.rule.Term;
import org.semanticweb.clipper.hornshiq.rule.Variable;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

public abstract class QueryResultPrinter {

	protected Appendable out;

	public QueryResultPrinter() {
		this(System.out);
	}

	public QueryResultPrinter(Appendable out) {
		this.out = out;
	}

	public void print(Atom head, List<List<String>> answers) {
		List<Term> answerVars = head.getTerms();
		init(answerVars, answers);
		printHead(answerVars);
		for (List<String> answer : answers) {
			printAnswer(answer);
		}
		printFoot();
	}

	abstract void printFoot();

	abstract void printAnswer(List<String> answer);

	abstract void printHead(List<Term> answerVars);

	abstract void init(List<Term> answerVars, List<List<String>> answers);
}

class CsvQueryResultPrinter extends QueryResultPrinter {

	@Override
	void printFoot() {

	}

	@Override
	void printAnswer(List<String> answer) {
		try {
			Joiner.on(", ").appendTo(System.out, answer);
			System.out.println();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	void printHead(List<Term> answerVars) {
		try {
			Joiner.on(", ").appendTo(System.out, answerVars);
			System.out.println();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	void init(List<Term> answerVars, List<List<String>> answers) {

	}

}

class HtmlQueryResultPrinter extends QueryResultPrinter {

	@Override
	void init(List<Term> answerVars, List<List<String>> answers) {

	}

	@Override
	void printHead(List<Term> answerVars) {
		System.out.println("<table><tr>");

		for (Term ansVar : answerVars) {
			System.out.print("<td>");
			System.out.print(encodeHTML(ansVar.toString()));
			System.out.println("</td>");
		}

		System.out.println("</tr>");

	}

	@Override
	void printAnswer(List<String> answer) {
		System.out.println("<tr>");
		for (String term : answer) {
			System.out.print("<td>");
			System.out.print(encodeHTML(term));
			System.out.println("</td>");
		}
		System.out.println("</tr>");
	}

	@Override
	void printFoot() {
		System.out.println("</table>");
	}

	public static String encodeHTML(String s) {
		StringBuffer out = new StringBuffer();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c > 127 || c == '"' || c == '<' || c == '>') {
				out.append("&#" + (int) c + ";");
			} else {
				out.append(c);
			}
		}
		return out.toString();
	}
}
