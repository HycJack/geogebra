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

package org.geogebra.desktop.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;

import org.geogebra.common.gui.dialog.BatchCommandExecutor;
import org.geogebra.common.gui.dialog.BatchCommandExecutor.ExecutionResult;
import org.geogebra.common.kernel.StringTemplate;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.main.App;
import org.geogebra.desktop.main.AppD;
import org.geogebra.desktop.main.LocalizationD;

/**
 * 批量命令执行对话框
 * 支持用户输入多行 GeoGebra 指令并批量执行
 */
public class BatchCommandDialog extends Dialog implements ActionListener {

	private final AppD app;
	private final LocalizationD loc;

	private JTextArea commandArea;
	private JTextArea resultArea;
	private JButton btExecute;
	private JButton btClear;
	private JButton btCopy;
	private JButton btClose;
	private JCheckBox chkStopOnError;

	private BatchCommandExecutor executor;

	/**
	 * @param app 应用实例
	 */
	public BatchCommandDialog(AppD app) {
		super(app.getFrame(), false);
		this.app = app;
		this.loc = app.getLocalization();
		this.executor = new BatchCommandExecutor(app);

		setTitle(loc.getMenuDefault("BatchCommand", "Batch Command Execution"));
		createGUI();
		// 扩展窗口：打开时默认填充当前项目所有指令，并放大窗口以完整展示
		prefillCommands();
		setPreferredSize(new Dimension(760, 640));
		pack();
		setLocationRelativeTo(app.getMainComponent());
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
		JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// 命令输入区域
		JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
		JLabel inputLabel = new JLabel(loc.getMenuDefault("Commands", "Commands") + ":");
		inputPanel.add(inputLabel, BorderLayout.NORTH);

		commandArea = new JTextArea(10, 60);
		commandArea.setFont(app.getPlainFont());
		commandArea.setLineWrap(false);
		commandArea.setWrapStyleWord(false);
		commandArea.setText("# Enter GeoGebra commands, one per line\n# Lines starting with # or // are comments\n\n# Examples:\na = 1\nb = 2\nc = a + b\nButton(\"Click Me\", \"SetValue(a, a+1)\")");

		JScrollPane inputScrollPane = new JScrollPane(commandArea);
		inputScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		inputScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		inputPanel.add(inputScrollPane, BorderLayout.CENTER);

		// 选项面板
		JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
		chkStopOnError = new JCheckBox(loc.getMenuDefault("StopOnError", "Stop on error"));
		optionsPanel.add(chkStopOnError);
		inputPanel.add(optionsPanel, BorderLayout.SOUTH);

		mainPanel.add(inputPanel, BorderLayout.NORTH);

		// 结果显示区域
		JPanel resultPanel = new JPanel(new BorderLayout(5, 5));
		JLabel resultLabel = new JLabel(loc.getMenuDefault("Results", "Results") + ":");
		resultPanel.add(resultLabel, BorderLayout.NORTH);

		resultArea = new JTextArea(8, 60);
		resultArea.setFont(app.getPlainFont());
		resultArea.setEditable(false);
		resultArea.setLineWrap(true);
		resultArea.setWrapStyleWord(true);
		resultArea.setBackground(Color.WHITE);

		JScrollPane resultScrollPane = new JScrollPane(resultArea);
		resultScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		resultScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		resultPanel.add(resultScrollPane, BorderLayout.CENTER);

		mainPanel.add(resultPanel, BorderLayout.CENTER);

		// 按钮面板
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));

		btExecute = new JButton(loc.getMenuDefault("Execute", "Execute"));
		btExecute.setActionCommand("Execute");
		btExecute.addActionListener(this);
		btExecute.setPreferredSize(new Dimension(100, 25));

		btClear = new JButton(loc.getMenuDefault("Clear", "Clear"));
		btClear.setActionCommand("Clear");
		btClear.addActionListener(this);
		btClear.setPreferredSize(new Dimension(100, 25));

		btCopy = new JButton(loc.getMenuDefault("Copy", "Copy"));
		btCopy.setActionCommand("Copy");
		btCopy.addActionListener(this);
		btCopy.setPreferredSize(new Dimension(100, 25));

		btClose = new JButton(loc.getMenuDefault("Close", "Close"));
		btClose.setActionCommand("Close");
		btClose.addActionListener(this);
		btClose.setPreferredSize(new Dimension(100, 25));

		buttonPanel.add(btExecute);
		buttonPanel.add(btCopy);
		buttonPanel.add(btClear);
		buttonPanel.add(btClose);

		mainPanel.add(buttonPanel, BorderLayout.SOUTH);

		setContentPane(mainPanel);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();

		switch (command) {
		case "Execute":
			executeCommands();
			break;
		case "Copy":
			copyCommands();
			break;
		case "Clear":
			clearAll();
			break;
		case "Close":
			setVisible(false);
			break;
		}
	}

	/**
	 * 将指令文本复制到系统剪贴板。
	 */
	private void copyCommands() {
		String text = commandArea.getText();
		if (text == null) {
			text = "";
		}
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		if (toolkit != null) {
			Clipboard clipboard = toolkit.getSystemClipboard();
			clipboard.setContents(new StringSelection(text), null);
			resultArea.setText(loc.getMenuDefault("Copied", "Commands copied to clipboard"));
		}
	}

	private void executeCommands() {
		String commands = commandArea.getText();
		
		if (commands == null || commands.trim().isEmpty()) {
			resultArea.setText(loc.getMenuDefault("EmptyInput", "Please enter commands"));
			return;
		}

		executor.setStopOnError(chkStopOnError.isSelected());
		List<ExecutionResult> results = executor.executeBatch(commands);

		StringBuilder sb = new StringBuilder();
		sb.append(loc.getMenuDefault("ExecutionResults", "Execution Results") + "\n");
		sb.append("=====================================\n\n");

		for (int i = 0; i < results.size(); i++) {
			ExecutionResult result = results.get(i);
			sb.append((i + 1) + ". ");

			if (result.isSuccess()) {
				sb.append("\u2714 ");
				sb.append(loc.getMenuDefault("Success", "Success"));
				if (result.getCommand() != null && !result.getCommand().trim().isEmpty()) {
					sb.append(": " + result.getCommand().trim());
				}
			} else {
				sb.append("\u2718 ");
				sb.append(loc.getMenuDefault("Error", "Error"));
				if (result.getCommand() != null && !result.getCommand().trim().isEmpty()) {
					sb.append(": " + result.getCommand().trim());
				}
				if (result.getErrorMessage() != null) {
					sb.append("\n   " + loc.getMenuDefault("ErrorMessage", "Error message") + ": " + result.getErrorMessage());
				}
			}
			sb.append("\n");
		}

		sb.append("\n");
		sb.append(loc.getMenuDefault("Summary", "Summary") + ":\n");
		sb.append(loc.getMenuDefault("Total", "Total") + ": " + executor.getTotalCount() + "\n");
		sb.append(loc.getMenuDefault("Success", "Success") + ": " + executor.getSuccessCount() + "\n");
		sb.append(loc.getMenuDefault("Errors", "Errors") + ": " + executor.getErrorCount() + "\n");

		resultArea.setText(sb.toString());
	}

	private void clearAll() {
		commandArea.setText("");
		resultArea.setText("");
	}

	public void updateFonts() {
		Font font = app.getPlainFont();

		commandArea.setFont(font);
		resultArea.setFont(font);
		btExecute.setFont(font);
		btCopy.setFont(font);
		btClear.setFont(font);
		btClose.setFont(font);
	}
}