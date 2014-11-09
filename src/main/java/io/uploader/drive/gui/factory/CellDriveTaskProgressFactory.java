/*
 * Copyright 2014 Loic Merckel
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package io.uploader.drive.gui.factory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.ProgressBarTableCell;
import javafx.util.Callback;

public class CellDriveTaskProgressFactory <S, T> implements Callback<TableColumn<S, T>, TableCell<S, T>> {

	private static final Logger logger = LoggerFactory.getLogger(CellDriveTaskProgressFactory.class);
	
	// http://stackoverflow.com/questions/16721380/javafx-update-progressbar-in-tableview-from-task
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public TableCell<S, T> call(final TableColumn<S, T> arg0) {

		logger.info("Cell factory");
		return new ProgressBarTableCell () ;
	}
	
	

}
