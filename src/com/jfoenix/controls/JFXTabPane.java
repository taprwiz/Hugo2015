/*
 * JFoenix
 * Copyright (c) 2015, JFoenix and/or its affiliates., All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */

package com.jfoenix.controls;

import javafx.scene.control.Skin;
import javafx.scene.control.TabPane;
import javafx.scene.input.MouseEvent;

import com.jfoenix.skins.JFXTabPaneSkin;

public class JFXTabPane extends TabPane {

	private static final String DEFAULT_STYLE_CLASS = "jfx-tab-pane";

	public JFXTabPane() {
		super();
		initialize();
	}

	@Override
	protected Skin<?> createDefaultSkin() {
		return new JFXTabPaneSkin(this);
	}

	private void initialize() {
		this.getStyleClass().setAll(DEFAULT_STYLE_CLASS);
	}
	
	public void propagateMouseEventsToParent(){
		this.addEventHandler(MouseEvent.ANY, (e)->{
			e.consume();
			this.getParent().fireEvent(e);
		});
	}
}
