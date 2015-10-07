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


import javafx.animation.Transition;
import javafx.beans.DefaultProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import com.jfoenix.transitions.hamburger.HamburgerTransition;

@DefaultProperty(value="animation")
public class JFXHamburger extends VBox {

	private static final String DEFAULT_STYLE_CLASS = "jfx-hamburger";
	
	private Transition animation;
	
	public JFXHamburger() {
				
		StackPane line1 = new StackPane();
		StackPane line2 = new StackPane();
		StackPane line3 = new StackPane();
		
		initStyle(line1);
		initStyle(line2);
		initStyle(line3);
		
		this.getChildren().add(line1);
		this.getChildren().add(line2);
		this.getChildren().add(line3);
		
		this.getStyleClass().add(DEFAULT_STYLE_CLASS);
		this.setAlignment(Pos.CENTER);
		this.setFillWidth(false);
		this.setSpacing(4);
	}

	public Transition getAnimation() {
		return animation;
	}

	public void setAnimation(Transition animation) {
		this.animation = ((HamburgerTransition)animation).getAnimation(this);
		this.animation.setRate(-1);
	}

	private void initStyle(StackPane pane){
		pane.setOpacity(1);
		pane.setPrefSize(30, 4);
		pane.setBackground(new Background(new BackgroundFill(Color.BLACK, new CornerRadii(5), Insets.EMPTY)));
	}

}
