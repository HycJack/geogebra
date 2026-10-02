/*
 * GeoGebra - Dynamic Mathematics for Everyone
 * Copyright (c) GeoGebra GmbH, Altenbergerstr. 69, 4040 Linz, Austria
 * https://www.geogebra.org
 *
 * This file is licensed by GeoGebra GmbH under the EUPL 1.2 licence and
 * may be used under the EUPL 1.2 in compatible projects (see Article 5
 * and the Appendix of EUPL 1.2 for details).
 * You may obtain a copy of the licence at:
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Note: The overall GeoGebra software package is free to use for
 * non-commercial purposes only.
 * See https://www.geogebra.org/license for full licensing details
 */

package org.geogebra.web.full.gui.dialog;

import java.util.List;
import java.util.TreeSet;

import org.geogebra.common.gui.dialog.BatchCommandExecutor;
import org.geogebra.common.gui.dialog.BatchCommandExecutor.ExecutionResult;
import org.geogebra.common.kernel.StringTemplate;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.web.html5.main.AppW;
import org.gwtproject.user.client.ui.TextArea;
import org.geogebra.web.shared.components.dialog.ComponentDialog;
import org.geogebra.web.shared.components.dialog.DialogData;
import org.gwtproject.user.client.ui.CheckBox;
import org.gwtproject.user.client.ui.FlowPanel;
import org.gwtproject.user.client.ui.Label;
import org.gwtproject.user.client.ui.ScrollPanel;

/**
 * Web implementation of Batch Command Execution Dialog
 */
public class BatchCommandDialogW extends ComponentDialog {

	private final AppW app;
	private final BatchCommandExecutor executor;

	private TextArea commandArea;
	private TextArea resultArea;
	private CheckBox chkStopOnError;

	/**
	 * @param app2 application
	 */
	public BatchCommandDialogW(AppW app2) {
		super(app2, new DialogData(app2.getLocalization().getMenuDefault("BatchCommand", "Batch Command Execution"),
				"Cancel", "Execute"), true, true);
		this.app = app2;
		this.executor = new BatchCommandExecutor(app2);
		createGUI();
		// 打开时默认填充当前项目所有指令（扩展窗口）
		prefillCommands();
		setOnPositiveAction(this::executeCommands);
		addStyleName("BatchCommandDialog");
	}

	/**
	 * 打开时默认显示当前 ggb 项目的所有 GGB 指令（按构造顺序），
	 * 若项目为空则保留示例说明。
	 */
	private void prefillCommands() {
		String commands = getAllCommands();
		if (commands != null && !commands.trim().isEmpty()) {
			commandArea.setText(commands);
		}
	}

	/**
	 * 收集当前构造中所有已标记的元素的指令（标签 = 定义），每行一条，按构造顺序排列。
	 * @return 全部指令文本；空项目时返回空字符串
	 */
	private String getAllCommands() {
		StringBuilder sb = new StringBuilder();
		TreeSet<GeoElement> geos = app.getKernel().getConstruction()
				.getGeoSetConstructionOrder();
		if (geos == null) {
			return "";
		}
		for (GeoElement geo : geos) {
			if (geo == null || !geo.isLabelSet() || geo.isAuxiliaryObject()) {
				continue;
			}
			String def = geo.getAlgebraDescription(StringTemplate.defaultTemplate);
			if (def != null && !def.trim().isEmpty()) {
				sb.append(def.trim()).append("\n");
			}
		}
		return sb.toString();
	}

	private void createGUI() {
		FlowPanel mainPanel = new FlowPanel();
		mainPanel.addStyleName("batchCommandMainPanel");

		// Command input section
		FlowPanel inputSection = new FlowPanel();
		inputSection.addStyleName("batchCommandSection");

		Label inputLabel = new Label(app.getLocalization().getMenuDefault("Commands", "Commands") + ":");
		inputLabel.addStyleName("batchCommandLabel");
		inputSection.add(inputLabel);

		commandArea = new TextArea();
		commandArea.addStyleName("batchCommandTextArea");
		commandArea.setText("# Enter GeoGebra commands, one per line\n# Lines starting with # or // are comments\n\n# Examples:\na = 1\nb = 2\nc = a + b\nButton(\"Click Me\", \"SetValue(a, a+1)\")");
		commandArea.setCharacterWidth(60);
		commandArea.setVisibleLines(10);

		ScrollPanel inputScroll = new ScrollPanel(commandArea);
		inputScroll.addStyleName("batchCommandScroll");
		inputSection.add(inputScroll);

		// Options
		FlowPanel optionsPanel = new FlowPanel();
		optionsPanel.addStyleName("batchCommandOptions");

		chkStopOnError = new CheckBox(app.getLocalization().getMenuDefault("StopOnError", "Stop on error"));
		chkStopOnError.addStyleName("batchCommandCheckbox");
		optionsPanel.add(chkStopOnError);

		inputSection.add(optionsPanel);
		mainPanel.add(inputSection);

		// Result section
		FlowPanel resultSection = new FlowPanel();
		resultSection.addStyleName("batchCommandSection");

		Label resultLabel = new Label(app.getLocalization().getMenuDefault("Results", "Results") + ":");
		resultLabel.addStyleName("batchCommandLabel");
		resultSection.add(resultLabel);

		resultArea = new TextArea();
		resultArea.addStyleName("batchCommandResultArea");
		resultArea.setReadOnly(true);
		resultArea.setCharacterWidth(60);
		resultArea.setVisibleLines(8);

		ScrollPanel resultScroll = new ScrollPanel(resultArea);
		resultScroll.addStyleName("batchCommandScroll");
		resultSection.add(resultScroll);

		mainPanel.add(resultSection);

		setDialogContent(mainPanel);
	}

	private void executeCommands() {
		String commands = commandArea.getText();

		if (commands == null || commands.trim().isEmpty()) {
			resultArea.setText(app.getLocalization().getMenuDefault("EmptyInput", "Please enter commands"));
			return;
		}

		executor.setStopOnError(chkStopOnError.getValue());
		List<ExecutionResult> results = executor.executeBatch(commands);

		StringBuilder sb = new StringBuilder();
		sb.append(app.getLocalization().getMenuDefault("ExecutionResults", "Execution Results")).append("\n");
		sb.append("=====================================\n\n");

		for (int i = 0; i < results.size(); i++) {
			ExecutionResult result = results.get(i);
			sb.append((i + 1)).append(". ");

			if (result.isSuccess()) {
				sb.append("\u2714 ");
				sb.append(app.getLocalization().getMenuDefault("Success", "Success"));
				if (result.getCommand() != null && !result.getCommand().trim().isEmpty()) {
					sb.append(": ").append(result.getCommand().trim());
				}
			} else {
				sb.append("\u2718 ");
				sb.append(app.getLocalization().getMenuDefault("Error", "Error"));
				if (result.getCommand() != null && !result.getCommand().trim().isEmpty()) {
					sb.append(": ").append(result.getCommand().trim());
				}
				if (result.getErrorMessage() != null) {
					sb.append("\n   ").append(app.getLocalization().getMenuDefault("ErrorMessage", "Error message"))
							.append(": ").append(result.getErrorMessage());
				}
			}
			sb.append("\n");
		}

		sb.append("\n");
		sb.append(app.getLocalization().getMenuDefault("Summary", "Summary")).append(":\n");
		sb.append(app.getLocalization().getMenuDefault("Total", "Total")).append(": ").append(executor.getTotalCount())
				.append("\n");
		sb.append(app.getLocalization().getMenuDefault("Success", "Success")).append(": ")
				.append(executor.getSuccessCount()).append("\n");
		sb.append(app.getLocalization().getMenuDefault("Errors", "Errors")).append(": ")
				.append(executor.getErrorCount()).append("\n");

		resultArea.setText(sb.toString());
	}
}