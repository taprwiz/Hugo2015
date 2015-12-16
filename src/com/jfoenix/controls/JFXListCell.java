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

import com.jfoenix.skins.JFXListCellSkin;
import com.jfoenix.svg.SVGGlyph;

import javafx.animation.Animation.Status;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Skin;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class JFXListCell<T> extends ListCell<T> {

	private StackPane cellContainer = new StackPane();
	private StackPane mainContainer = new StackPane();
	private JFXRippler cellRippler;

	private Node cellContent;
	private Timeline animateGap;
	private Timeline expandAnimation;
	private double animatedHeight = 0;	
	
	public JFXListCell() {
		super();
		initialize();
	}

	@Override
	protected Skin<?> createDefaultSkin()	{
		return new JFXListCellSkin<T>(this);
	}

	@Override
	public void updateItem(T item, boolean empty){
		super.updateItem(item,empty);
		if(empty){
			setText(null);
			setGraphic(null);
			// remove empty (Trailing cells)
			setMouseTransparent(true);
			setStyle("-fx-background-color:TRANSPARENT;");			
			
		}else{			
			if(item != null) {				
				// if cell is not a trailing cell then show it
				setStyle(null);
				setMouseTransparent(false);				
				
				Node currentNode = getGraphic();
				
				Node newNode;
				if((item instanceof Region || item instanceof Control))  newNode = (Node) item;
				else newNode = new Label(item.toString());

				boolean bindRippler = false;
				boolean addCellRippler = true;
				boolean isJFXListView = getListView() instanceof JFXListView;
				
				// show cell tooltip if its toggled in JFXListView
				if(isJFXListView && ((JFXListView<?>)getListView()).isShowTooltip() && newNode instanceof Label){
					setTooltip(new Tooltip(((Label)newNode).getText()));
				}
				

				if (currentNode == null || !currentNode.equals(newNode)) {
					// clear nodes
					cellContainer.getChildren().clear();
					cellContent = newNode;

					// build the Cell node and its rippler					
					// RIPPLER ITEM : in case if the list item has its own rippler bind the list rippler and item rippler properties
					if(newNode instanceof JFXRippler){
						bindRippler = true;
						// build cell container from exisiting rippler
						cellContent = ((JFXRippler)newNode).getControl();						
						cellContainer.getChildren().add(cellContent);
					}

					// SUBLIST ITEM : build the Cell node as sublist the sublist
					else if(newNode instanceof JFXListView<?>){
						// add the sublist to the parent and style the cell as sublist item
						((JFXListView<?>)getListView()).addSublist((JFXListView<?>) newNode, this.getIndex());						
						this.getStyleClass().add("sublist-item");
						addCellRippler = false;

						// First build the group item used to expand / hide the sublist
						StackPane group = new StackPane();						
						group.getStyleClass().add("sublist-header");
						group.getChildren().add(((JFXListView<?>)newNode).getGroupnode());
						
						SVGGlyph dropIcon = new SVGGlyph(0, "ANGLE_RIGHT", "M340 548.571q0 7.429-5.714 13.143l-266.286 266.286q-5.714 5.714-13.143 5.714t-13.143-5.714l-28.571-28.571q-5.714-5.714-5.714-13.143t5.714-13.143l224.571-224.571-224.571-224.571q-5.714-5.714-5.714-13.143t5.714-13.143l28.571-28.571q5.714-5.714 13.143-5.714t13.143 5.714l266.286 266.286q5.714 5.714 5.714 13.143z", Color.BLACK);
						dropIcon.setStyle("-fx-min-width:0.4em;-fx-max-width:0.4em;-fx-min-height:0.6em;-fx-max-height:0.6em;");
						dropIcon.getStyleClass().add("drop-icon");
						
						group.getChildren().add(dropIcon);
						// the margin is needed when rotating the angle
						StackPane.setMargin(dropIcon, new Insets(0,7,0,0));
						StackPane.setAlignment(dropIcon, Pos.CENTER_RIGHT);						
						group.paddingProperty().bind(Bindings.createObjectBinding(()->{
							double cellInsetHgap = ((JFXListView<T>)getListView()).getCellHorizontalMargin().doubleValue() + cellContainer.getPadding().getLeft();
							double cellInsetVgap = ((JFXListView<T>)getListView()).getCellVerticalMargin().doubleValue() + cellContainer.getPadding().getTop();
							return new Insets(cellInsetVgap, cellInsetHgap, cellInsetVgap, cellInsetHgap);
						}, ((JFXListView<T>)getListView()).cellHorizontalMarginProperty(), ((JFXListView<T>)getListView()).cellVerticalMarginProperty()));
						// add group item rippler
						JFXRippler groupRippler = new JFXRippler(group);

						// FIXME : NEED TO CHECK THE HEIGHT OF THE CELLS (as it's being changed )
						cellContainer.setPadding(new Insets(0));

						// Second build the sublist container
						StackPane sublistContainer = new StackPane();
						sublistContainer.getStyleClass().add("sublist-container");
						sublistContainer.setMaxWidth(getListView().getWidth()-2);
						sublistContainer.getChildren().add(cellContent);
						sublistContainer.setTranslateY(1);
						sublistContainer.setOpacity(0);	
						
						sublistContainer.heightProperty().addListener((o,oldVal,newVal)->{
							// store the hieght of the sublist and resize it to 0 to make it hidden
							if(subListHeight == -1){
								subListHeight = newVal.doubleValue();
								totalSubListsHeight += subListHeight;
								// set the parent list 
								Platform.runLater(()->{
									sublistContainer.setMinHeight(0);
									sublistContainer.setPrefHeight(0);
									sublistContainer.setMaxHeight(0);
									double currentHeight = ((JFXListView<T>)getListView()).getHeight();
									// FIXME : THIS SHOULD ONLY CALLED ONCE ( NOW ITS BEING CALLED FOR EVERY SUBLIST)
									updateListViewHeight(currentHeight - totalSubListsHeight);
								});	
							}
						});


						// Third, create container of group title and the sublist
						VBox contentHolder = new VBox();
						contentHolder.getChildren().add(groupRippler);
						contentHolder.getChildren().add(sublistContainer);
						cellContainer.getChildren().add(contentHolder);
						cellContainer.addEventHandler(MouseEvent.ANY, (e)-> contentHolder.fireEvent(e));
						contentHolder.addEventHandler(MouseEvent.ANY, (e)-> e.consume());


						// Finally, add sublist animation						
						group.setOnMouseClicked((click)->{
							JFXListView<T> listview = ((JFXListView<T>)getListView());
							// invert the expand property 
							expandedProperty.set(!expandedProperty.get());

							// change the list height
							animatedHeight = subListHeight;
							if(!expandedProperty.get()) animatedHeight = -animatedHeight;

							// stop the animation or change the list height 
							if(expandAnimation!=null && expandAnimation.getStatus().equals(Status.RUNNING)) expandAnimation.stop();								
							else if(expandedProperty.get()) updateListViewHeight(listview.getHeight() + animatedHeight);


							// animate showing/hiding the sublist
							double initMin,initMax;
							initMin = initMax = !expandedProperty.get()? subListHeight : 0.0;
							int opacity = !expandedProperty.get()? 0 : 1;
							expandAnimation = new Timeline(new KeyFrame(Duration.millis(320),
									new KeyValue( sublistContainer.minHeightProperty(), initMin + animatedHeight ,Interpolator.EASE_BOTH),																
									new KeyValue( sublistContainer.maxHeightProperty(), initMax + animatedHeight ,Interpolator.EASE_BOTH),
									new KeyValue( sublistContainer.opacityProperty(), opacity ,Interpolator.EASE_BOTH)));							
							if(!expandedProperty.get()) expandAnimation.setOnFinished((finish)->updateListViewHeight(listview.getHeight() + animatedHeight));
							expandAnimation.play();
						});

						// animate arrow
						expandedProperty.addListener((o,oldVal,newVal)->{
							if(newVal) new Timeline(new KeyFrame(Duration.millis(160),new KeyValue( dropIcon.rotateProperty(),90 ,Interpolator.EASE_BOTH))).play();
							else new Timeline(new KeyFrame(Duration.millis(160),new KeyValue( dropIcon.rotateProperty(), 0 ,Interpolator.EASE_BOTH))).play();
						});
					}

					// DEFAULT BUILD  : build cell container and rippler if the cell has no rippler
					else{
						cellContainer.getChildren().clear();
						cellContainer.getChildren().add(newNode);						
					}

					if(addCellRippler){
						// initialize the gaps between cells
						double cellInsetHgap = isJFXListView?((JFXListView<T>)getListView()).getCellHorizontalMargin().doubleValue():0;
						double cellInsetVgap = isJFXListView?((JFXListView<T>)getListView()).getCellVerticalMargin().doubleValue():4;
						StackPane.setMargin(cellContainer, new Insets(cellInsetVgap, cellInsetHgap, cellInsetVgap, cellInsetHgap));

						// add listeners to gaps properties 
						if(isJFXListView){
							((JFXListView<T>)getListView()).cellHorizontalMarginProperty().addListener((o,oldVal,newVal)-> {
								// fit the rippler into the cell bounds
								double newCellInsetHgap = newVal.doubleValue();
								double oldCellInsetVgap = ((JFXListView<T>)getListView()).getCellVerticalMargin().doubleValue();
								StackPane.setMargin(cellContainer, new Insets(oldCellInsetVgap, newCellInsetHgap, oldCellInsetVgap, newCellInsetHgap));
							});
							((JFXListView<T>)getListView()).cellVerticalMarginProperty().addListener((o,oldVal,newVal)-> {
								// fit the rippler into the cell bounds
								double oldCellInsetHgap = ((JFXListView<T>)getListView()).getCellHorizontalMargin().doubleValue();
								double newCellInsetVgap = newVal.doubleValue();						
								StackPane.setMargin(cellContainer, new Insets(newCellInsetVgap, oldCellInsetHgap, newCellInsetVgap, oldCellInsetHgap));
							});
						}
					}

					// check if the list is in expanded mode 
					if(isJFXListView && this.getIndex() > 0 && ((JFXListView<T>)getListView()).isExpanded()) 
						this.translateYProperty().set(((JFXListView<T>)getListView()).getVerticalGap()*this.getIndex());

					if(isJFXListView){
						((JFXListView<T>)getListView()).currentVerticalGapProperty().addListener((o,oldVal,newVal)->{
							// validate changing gap operation
							JFXListView<T> listview = ((JFXListView<T>)getListView());
							double borderWidth = 0;
							if(listview.getPadding()!=null){
								borderWidth += listview.getPadding().getTop();
								borderWidth += listview.getPadding().getBottom();
							}
							double newHeight = (this.getHeight() + listview.currentVerticalGapProperty().get()) * listview.getItems().size() + borderWidth - listview.currentVerticalGapProperty().get();
							if(listview.getMaxHeight() == -1 || (listview.getMaxHeight() > 0 && newHeight <= listview.getMaxHeight())){
								if(this.getIndex() > 0 && this.getIndex() < listview.getItems().size()){
									// stop the previous animation 
									if(animateGap!=null) animateGap.stop();
									// create new animation
									animateGap = new Timeline(
											new KeyFrame( Duration.ZERO, new KeyValue( this.translateYProperty(), this.translateYProperty().get() ,Interpolator.EASE_BOTH)),
											new KeyFrame(Duration.millis(500), new KeyValue( this.translateYProperty(), newVal.doubleValue()*this.getIndex()  ,Interpolator.EASE_BOTH))
											);	
									// change the height of the list view
									if(oldVal.doubleValue()<newVal.doubleValue())
										listview.setPrefHeight(newHeight);
									else
										animateGap.setOnFinished((e)->{
											listview.setPrefHeight(newHeight);
										});

									animateGap.play();	
								}
							}
						});
					}

					// set the content of the cell
					mainContainer.getChildren().setAll(cellContainer);
					// creating rippler/calling set graphic must be executed once for each visible cell in the list
					if(cellRippler == null){
						if(addCellRippler){
							cellRippler = new JFXRippler(mainContainer);
							// if the item passed to the list is JFXRippler then we bind its color mask and position properties to the cell rippler
							if(bindRippler){
								cellRippler.ripplerFillProperty().bind(((JFXRippler)newNode).ripplerFillProperty());
								cellRippler.maskTypeProperty().bind(((JFXRippler)newNode).maskTypeProperty());
								cellRippler.positionProperty().bind(((JFXRippler)newNode).positionProperty());
							}
							setGraphic(cellRippler);
						}else{
							setGraphic(mainContainer);	
						}
						setText(null);
					}

					// propagate mouse events to all children
					mainContainer.addEventHandler(MouseEvent.ANY, (e)-> {
						if(!e.isConsumed()){
							cellContent.fireEvent(e);
							e.consume();
						}
					});					
					cellContent.addEventHandler(MouseEvent.ANY, (e)->e.consume());
				}
			}
		}
	}


	private void updateListViewHeight(double newHeight){
		((JFXListView<T>)getListView()).setPrefHeight(newHeight);
		((JFXListView<T>)getListView()).setMaxHeight(newHeight);
		((JFXListView<T>)getListView()).setMinHeight(newHeight);
	}


	/***************************************************************************
	 *                                                                         *
	 * Properties                                                              *
	 *                                                                         *
	 **************************************************************************/

	// indicate whether the sub list is expanded or not
	private BooleanProperty expandedProperty = new SimpleBooleanProperty(false);	

	public BooleanProperty expandedProperty(){
		return expandedProperty;
	}
	public void setExpanded(boolean expand){
		expandedProperty.set(expand);
	}	
	public boolean isExpanded(){
		return expandedProperty.get();
	}


	// hold the height of the sub list if existed
	private double subListHeight = -1;

	// FIXME : this value must be computed instead of fixed
	private static double totalSubListsHeight = -34;



	/***************************************************************************
	 *                                                                         *
	 * Stylesheet Handling                                                     *
	 *                                                                         *
	 **************************************************************************/

	private static final String DEFAULT_STYLE_CLASS = "jfx-list-cell";

	private void initialize() {
		this.getStyleClass().add(DEFAULT_STYLE_CLASS);
		// set default classes
		mainContainer.getStyleClass().add("jfx-list-cell-container");
		cellContainer.getStyleClass().add("jfx-list-cell-content-container");
		cellContainer.setPadding(new Insets(4,8,4,8));
		this.setPadding(new Insets(0));
		totalSubListsHeight = -34;
	}

}
