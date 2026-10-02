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

package org.geogebra.common.gui.dialog;

import java.util.ArrayList;
import java.util.List;

import org.geogebra.common.kernel.commands.EvalInfo;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.main.App;
import org.geogebra.common.util.debug.Log;

/**
 * 批量命令执行器
 * 支持执行多条 GeoGebra 指令，并提供执行结果反馈
 */
public class BatchCommandExecutor {

	private final App app;
	private List<ExecutionResult> results;
	private boolean stopOnError = false;
	private boolean running = false;

	/**
	 * 执行结果
	 */
	public static class ExecutionResult {
		private final String command;
		private final boolean success;
		private final String errorMessage;
		private final GeoElement[] createdElements;

		public ExecutionResult(String command, boolean success, String errorMessage, GeoElement[] createdElements) {
			this.command = command;
			this.success = success;
			this.errorMessage = errorMessage;
			this.createdElements = createdElements;
		}

		public String getCommand() {
			return command;
		}

		public boolean isSuccess() {
			return success;
		}

		public String getErrorMessage() {
			return errorMessage;
		}

		public GeoElement[] getCreatedElements() {
			return createdElements;
		}
	}

	/**
	 * 构造函数
	 * @param app 应用实例
	 */
	public BatchCommandExecutor(App app) {
		this.app = app;
		this.results = new ArrayList<>();
	}

	/**
	 * 设置是否遇到错误时停止执行
	 * @param stopOnError true表示遇到错误停止，false表示继续执行
	 */
	public void setStopOnError(boolean stopOnError) {
		this.stopOnError = stopOnError;
	}

	/**
	 * 检查是否正在执行
	 * @return true表示正在执行
	 */
	public boolean isRunning() {
		return running;
	}

	/**
	 * 执行单行命令
	 * @param command 命令字符串
	 * @return 执行结果
	 */
	public ExecutionResult executeCommand(String command) {
		if (command == null || command.trim().isEmpty()) {
			return new ExecutionResult(command, true, null, new GeoElement[0]);
		}

		String trimmedCommand = command.trim();
		
		// 跳过注释行
		if (trimmedCommand.startsWith("//") || trimmedCommand.startsWith("#")) {
			return new ExecutionResult(command, true, null, new GeoElement[0]);
		}

		try {
			app.getKernel().getAlgebraProcessor().processAlgebraCommand(
				trimmedCommand, 
				false
			);
			
			GeoElement lastGeo = app.getKernel().getConstruction().getLastGeoElement();
			GeoElement[] geos = lastGeo != null ? new GeoElement[] { lastGeo } : new GeoElement[0];
			return new ExecutionResult(command, true, null, geos);
			
		} catch (Exception e) {
			Log.error("Command execution error: " + e.getMessage());
			return new ExecutionResult(command, false, e.getMessage(), new GeoElement[0]);
		}
	}

	/**
	 * 批量执行命令（按换行分割）
	 * @param commands 多行命令字符串
	 * @return 执行结果列表
	 */
	public List<ExecutionResult> executeBatch(String commands) {
		if (commands == null || commands.isEmpty()) {
			return new ArrayList<>();
		}

		running = true;
		results.clear();

		String[] commandLines = commands.split("\n");
		
		for (String line : commandLines) {
			ExecutionResult result = executeCommand(line);
			results.add(result);

			if (!result.isSuccess() && stopOnError) {
				break;
			}
		}

		running = false;
		return new ArrayList<>(results);
	}

	/**
	 * 批量执行命令（从列表执行）
	 * @param commandList 命令列表
	 * @return 执行结果列表
	 */
	public List<ExecutionResult> executeBatch(List<String> commandList) {
		running = true;
		results.clear();

		for (String command : commandList) {
			ExecutionResult result = executeCommand(command);
			results.add(result);

			if (!result.isSuccess() && stopOnError) {
				break;
			}
		}

		running = false;
		return new ArrayList<>(results);
	}

	/**
	 * 获取最后一次执行的结果
	 * @return 执行结果列表
	 */
	public List<ExecutionResult> getLastResults() {
		return new ArrayList<>(results);
	}

	/**
	 * 获取成功执行的命令数
	 * @return 成功数
	 */
	public int getSuccessCount() {
		return (int) results.stream().filter(ExecutionResult::isSuccess).count();
	}

	/**
	 * 获取失败的命令数
	 * @return 失败数
	 */
	public int getErrorCount() {
		return (int) results.stream().filter(r -> !r.isSuccess()).count();
	}

	/**
	 * 获取总命令数
	 * @return 总命令数
	 */
	public int getTotalCount() {
		return results.size();
	}

	/**
	 * 重置执行结果
	 */
	public void reset() {
		results.clear();
		running = false;
	}
}