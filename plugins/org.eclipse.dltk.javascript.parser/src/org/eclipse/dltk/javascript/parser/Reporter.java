package org.eclipse.dltk.javascript.parser;

import org.eclipse.dltk.compiler.problem.DefaultProblem;
import org.eclipse.dltk.compiler.problem.IProblem;
import org.eclipse.dltk.compiler.problem.IProblemIdentifier;
import org.eclipse.dltk.compiler.problem.IProblemReporter;
import org.eclipse.dltk.compiler.problem.ProblemSeverities;
import org.eclipse.dltk.core.builder.ISourceLineTracker;

public class Reporter extends LineTracker implements IProblemReporter {

	public enum Severity {
		WARNING, ERROR
	}

	private final IProblemReporter problemReporter;

	private IProblemIdentifier id;
	private String message;
	private int line;
	private int start;
	private int end;
	private Severity severity;

	private int problemCount;

	private final ISeverityReporter severityReporter;

	public Reporter(ISourceLineTracker lineTracker,
			IProblemReporter problemReporter, ISeverityReporter severityReporter) {
		super(lineTracker);
		this.problemReporter = problemReporter;
		this.severityReporter = severityReporter;
		reset();
	}

	private void reset() {
		severity = Severity.WARNING;
		id = null;
		message = null;
		line = -1;
		start = -1;
		end = -1;
	}

	public void report() {
		if (problemReporter != null && message != null) {
			reportProblem(createProblem());
		}
		reset();
	}

	private IProblem createProblem() {
		if (line == -1 && start != -1) {
			line = getLineNumberOfOffset(start);
		}
		if (line > getNumberOfLines() && start >= 0 && start <= getLength()) {
			line = getLineNumberOfOffset(start);
		}

		if (severityReporter != null) {
			severity = severityReporter.getSeverity(id, severity);
			if (severity == null)
				return null;
		}

		return new DefaultProblem(message, id, null,
				severity == Severity.ERROR ? ProblemSeverities.Error
						: ProblemSeverities.Warning, start, end, line);
	}

	public void setMessage(IProblemIdentifier id, String message) {
		this.id = id;
		this.message = message;
	}

	public IProblemIdentifier getId() {
		return id;
	}

	public void setId(IProblemIdentifier id) {
		this.id = id;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public int getLine() {
		return line;
	}

	public void setLine(int line) {
		this.line = line;
	}

	public int getStart() {
		return start;
	}

	public void setStart(int start) {
		this.start = start;
	}

	public int getEnd() {
		return end;
	}

	public void setEnd(int end) {
		this.end = end;
	}

	public Severity getSeverity() {
		return severity;
	}

	public void setSeverity(Severity severity) {
		this.severity = severity;
	}

	public void setRange(int start, int end) {
		this.start = start;
		this.end = end;
	}

	public void reportProblem(IProblem problem) {
		if (problemReporter != null && problem != null) {
			problemCount++;
			problemReporter.reportProblem(problem);
		}
	}

	public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
		return null;
	}

	public void reportProblem(IProblemIdentifier id, String message, int start, int end) {
		Severity sev = severity;
		if (severityReporter != null) {
			sev = severityReporter.getSeverity(id, sev);
			if (sev == null)
				return;
		}

		reportProblem(new DefaultProblem(message, id, null,
				sev == Severity.ERROR ? ProblemSeverities.Error
						: ProblemSeverities.Warning, start, end,
				getLineNumberOfOffset(start)));
	}

	public int getProblemCount() {
		return problemCount;
	}

}
