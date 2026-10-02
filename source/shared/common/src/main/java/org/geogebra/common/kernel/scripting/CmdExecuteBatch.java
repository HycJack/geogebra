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

package org.geogebra.common.kernel.scripting;

import org.geogebra.common.gui.dialog.BatchCommandExecutor;
import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.kernel.arithmetic.Command;
import org.geogebra.common.kernel.commands.CommandProcessor;
import org.geogebra.common.kernel.commands.EvalInfo;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoText;
import org.geogebra.common.main.MyError;

/**
 * ExecuteBatch[commands]
 * 批量执行 GeoGebra 指令
 * 
 * @author GeoGebra Team
 */
public class CmdExecuteBatch extends CommandProcessor {

	/**
	 * Create new command processor
	 * 
	 * @param kernel
	 *            kernel
	 */
	public CmdExecuteBatch(Kernel kernel) {
		super(kernel);
	}

	@Override
	final public GeoElement[] process(Command c, EvalInfo info) throws MyError {
		int n = c.getArgumentNumber();
		GeoElement[] arg;

		switch (n) {
		case 1:
			arg = resArgs(c, info);
			if (arg[0].isGeoText()) {
				String commands = ((GeoText) arg[0]).getTextString();
				
				BatchCommandExecutor executor = new BatchCommandExecutor(app);
				executor.setStopOnError(false);
				executor.executeBatch(commands);
				
				// 返回最后创建的对象
				GeoElement lastGeo = app.getKernel().getConstruction().getLastGeoElement();
				if (lastGeo != null) {
					return new GeoElement[] { lastGeo };
				}
				return new GeoElement[0];
			}
			throw argErr(c, arg[0]);

		default:
			throw argNumErr(c);
		}
	}
}