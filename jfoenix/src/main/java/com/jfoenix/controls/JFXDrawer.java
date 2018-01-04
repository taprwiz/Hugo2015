/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.jfoenix.controls;

import com.jfoenix.controls.events.JFXDrawerEvent;
import com.jfoenix.transitions.JFXAnimationTimer;
import com.jfoenix.transitions.JFXKeyFrame;
import com.jfoenix.transitions.JFXKeyValue;
import javafx.animation.Interpolator;
import javafx.animation.PauseTransition;
import javafx.animation.Transition;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import javafx.util.Duration;

import java.util.ArrayList;

/**
 * JFXDrawer is material design implementation of drawer.
 * the drawer has two main nodes, the content and side pane.
 * <ul>
 * <li><b>content pane:</b> is a stack pane that holds the nodes inside the drawer</li>
 * <li><b>side pane:</b> is a stack pane that holds the nodes inside the drawer side area (Drawable node)</li>
 * </ul>
 *
 * @author Shadi Shaheen
 * @version 1.0
 * @since 2016-03-09
 */
public class JFXDrawer extends StackPane {

    /**
     * Defines the directions that a {@link JFXDrawer} can originate from.
     */
    public enum DrawerDirection {
        LEFT(1), RIGHT(-1), TOP(1), BOTTOM(-1);
        private double numVal;

        DrawerDirection(double numVal) {
            this.numVal = numVal;
        }

        public double doubleValue() {
            return numVal;
        }
    }

    // nodes
    private StackPane overlayPane = new StackPane();
    StackPane sidePane = new StackPane();
    private StackPane content = new StackPane();
    private StackPane contentHolder = new StackPane();
    private Region paddingPane = new Region();

    // animation
    private Transition partialTransition;
    private Duration holdTime = Duration.seconds(0.2);
    private PauseTransition holdTimer = new PauseTransition(holdTime);
    private double initOffset = 30;
    private DoubleProperty initTranslate = new SimpleDoubleProperty();

    private double activeOffset = 20;
    private double startMouse = -1;
    private double startTranslate = -1;
    private double startSize = -1;

    // used to trigger the drawer events
    private boolean openCalled = false;
    private boolean closeCalled = true;

    // side pane size properties
    private DoubleProperty translateProperty = sidePane.translateXProperty();
    private DoubleProperty defaultSizeProperty = new SimpleDoubleProperty();
    private DoubleProperty maxSizeProperty = sidePane.maxWidthProperty();
    private DoubleProperty prefSizeProperty = sidePane.prefWidthProperty();
    private ReadOnlyDoubleProperty sizeProperty = sidePane.widthProperty();

    // this property is used to reference the parent width/height property
    private ReadOnlyDoubleProperty parentSizeProperty = null;

    // this is used to allow resizing the content of the drawer
    private DoubleProperty paddingSizeProperty = paddingPane.minWidthProperty();

    /***************************************************************************
     *                                                                         *
     * Animations                                                                *
     *                                                                         *
     **************************************************************************/

    // used to hold the new translation value during the animation
    private double translateTo = 0;

    // used to cache the drawer size
    private double tempDrawerSize = getDefaultDrawerSize();

    private JFXAnimationTimer translateTimer = new JFXAnimationTimer(
        new JFXKeyFrame(Duration.millis(420),
            JFXKeyValue.builder()
                .setTargetSupplier(() -> overlayPane.opacityProperty())
                .setEndValueSupplier(() -> 1 - translateTo / initTranslate.get())
                .setInterpolator(Interpolator.EASE_BOTH).build()),
        new JFXKeyFrame(Duration.millis(420),
            JFXKeyValue.builder()
                .setTargetSupplier(() -> translateProperty)
                .setEndValueSupplier(() -> translateTo)
                .setInterpolator(Interpolator.EASE_BOTH).build()),
        new JFXKeyFrame(Duration.millis(420),
            JFXKeyValue.builder()
                .setTargetSupplier(() -> prefSizeProperty)
                .setEndValueSupplier(() -> getDefaultDrawerSize())
                .setAnimateCondition(() -> translateTo == initTranslate.get())
                .setInterpolator(Interpolator.EASE_BOTH).build()),
        new JFXKeyFrame(Duration.millis(420),
            JFXKeyValue.builder()
                .setTargetSupplier(() -> maxSizeProperty)
                .setEndValueSupplier(() -> getDefaultDrawerSize())
                .setAnimateCondition(() -> translateTo == initTranslate.get())
                .setInterpolator(Interpolator.EASE_BOTH).build()),
        new JFXKeyFrame(Duration.millis(420),
            JFXKeyValue.builder()
                .setTargetSupplier(() -> prefSizeProperty)
                .setEndValueSupplier(() -> tempDrawerSize)
                .setAnimateCondition(() -> translateTo == 0 && tempDrawerSize > getDefaultDrawerSize())
                .setInterpolator(Interpolator.EASE_BOTH).build()),
        new JFXKeyFrame(Duration.millis(420),
            JFXKeyValue.builder()
                .setTargetSupplier(() -> maxSizeProperty)
                .setEndValueSupplier(() -> tempDrawerSize)
                .setAnimateCondition(() -> translateTo == 0 && tempDrawerSize > getDefaultDrawerSize())
                .setInterpolator(Interpolator.EASE_BOTH).build()),
        // padding animation
        new JFXKeyFrame(Duration.millis(420),
            JFXKeyValue.builder()
                .setTargetSupplier(() -> paddingSizeProperty)
                .setEndValueSupplier(this::computePaddingSize)
                .setInterpolator(Interpolator.EASE_BOTH).build())

    );

    /**
     * creates empty drawer node
     */
    public JFXDrawer() {
        initialize();

        contentHolder.setPickOnBounds(false);

        overlayPane.setBackground(new Background(new BackgroundFill(Color.rgb(0, 0, 0, 0.1),
            CornerRadii.EMPTY,
            Insets.EMPTY)));
        overlayPane.getStyleClass().add("jfx-drawer-overlay-pane");
        overlayPane.setOpacity(0);
        overlayPane.setMouseTransparent(true);

        sidePane.getStyleClass().add("jfx-drawer-side-pane");
        sidePane.setBackground(new Background(new BackgroundFill(Color.rgb(255, 255, 255, 1),
            CornerRadii.EMPTY,
            Insets.EMPTY)));
        sidePane.setPickOnBounds(false);

        translateTimer.setCacheNodes(sidePane);

        // add listeners
        initListeners();
        //  init size value
        setDefaultDrawerSize(100);
        getChildren().setAll(contentHolder, overlayPane, sidePane);
    }

    private void initialize() {
        this.getStyleClass().add(DEFAULT_STYLE_CLASS);
    }

    private void initListeners() {
        updateDirection(directionProperty.get());
        initTranslate.bind(Bindings.createDoubleBinding(() ->
                -1 * directionProperty.get().doubleValue() * defaultSizeProperty.getValue()
                - initOffset * directionProperty.get().doubleValue(),
            defaultSizeProperty, directionProperty));

        // add listeners to update drawer properties
        overLayVisibleProperty().addListener(observable -> {
            final boolean overLayVisible = isOverLayVisible();
            overlayPane.setStyle(!overLayVisible ? "-fx-background-color : transparent;" : "");
            overlayPane.setPickOnBounds(overLayVisible);
        });

        directionProperty.addListener(observable -> updateDirection(directionProperty.get()));
        initTranslate.addListener(observable -> updateDrawerAnimation(initTranslate.get()));

        // mouse drag handler
//        translateProperty.addListener(observable -> overlayPane.setOpacity(1 - translateProperty.doubleValue() / initTranslate.get()));

        // add opening/closing action listeners
        translateProperty.addListener((o, oldVal, newVal) -> {
            if (!openCalled && closeCalled
                && directionProperty.get().doubleValue() * newVal.doubleValue() >
                   directionProperty.get().doubleValue() * initTranslate.get() / 2) {
                openCalled = true;
                closeCalled = false;
                fireEvent(new JFXDrawerEvent(JFXDrawerEvent.OPENING));
            }
        });
        translateProperty.addListener((o, oldVal, newVal) -> {
            if (openCalled && !closeCalled
                && directionProperty.get().doubleValue() * newVal.doubleValue() <
                   directionProperty.get().doubleValue() * initTranslate.get() / 2) {
                closeCalled = true;
                openCalled = false;
                fireEvent(new JFXDrawerEvent(JFXDrawerEvent.CLOSING));
            }
        });

        overlayPane.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> close());

        sidePane.addEventHandler(MouseEvent.MOUSE_DRAGGED, mouseDragHandler);
        sidePane.addEventHandler(MouseEvent.MOUSE_RELEASED, mouseReleasedHandler);
        sidePane.addEventHandler(MouseEvent.MOUSE_PRESSED, mousePressedHandler);

        // content listener for mouse hold on a side
        this.content.addEventHandler(MouseEvent.MOUSE_PRESSED, (e) -> {
            if (!e.isConsumed()) {
                double size = 0;
                long valid = 0;
                for (int i = 0; i < callBacks.size(); i++) {
                    if (!callBacks.get(i).call(null)) {
                        valid++;
                    }
                }
                if (directionProperty.get() == DrawerDirection.RIGHT) {
                    size = content.getWidth();
                } else if (directionProperty.get() == DrawerDirection.BOTTOM) {
                    size = content.getHeight();
                }

                double eventPoint = 0;
                if (directionProperty.get() == DrawerDirection.RIGHT
                    || directionProperty.get() == DrawerDirection.LEFT) {
                    eventPoint = e.getX();
                } else {
                    eventPoint = e.getY();
                }

                if (size + directionProperty.get().doubleValue() * eventPoint < activeOffset
                    && (content.getCursor() == Cursor.DEFAULT || content.getCursor() == null)
                    && valid == 0
                    && !isShown()) {
                    holdTimer.play();
                    e.consume();
                }
            }
        });

        this.content.addEventHandler(MouseEvent.MOUSE_RELEASED, (e) -> {
            holdTimer.stop();
            this.content.removeEventFilter(MouseEvent.MOUSE_DRAGGED, mouseDragHandler);
        });

        holdTimer.setOnFinished(e -> {
            translateTo = initTranslate.get()
                          + initOffset * directionProperty.get().doubleValue()
                          + activeOffset * directionProperty.get().doubleValue();
            overlayPane.setMouseTransparent(!isOverLayVisible());
            translateTimer.setOnFinished(null);
            translateTimer.start();
        });
    }


    ChangeListener<? super Node> widthListener = (o, oldVal, newVal) -> {
        if (newVal != null && newVal instanceof Region) {
            parentSizeProperty = ((Region) newVal).widthProperty();
        }
    };
    ChangeListener<? super Node> heightListener = (o, oldVal, newVal) -> {
        if (newVal != null && newVal instanceof Region) {
            parentSizeProperty = ((Region) newVal).heightProperty();
        }
    };
    ChangeListener<? super Scene> sceneWidthListener = (o, oldVal, newVal) -> {
        if (newVal != null && this.getParent() == null) {
            parentSizeProperty = newVal.widthProperty();
        }
    };
    ChangeListener<? super Scene> sceneHeightListener = (o, oldVal, newVal) -> {
        if (newVal != null && this.getParent() == null) {
            parentSizeProperty = newVal.heightProperty();
        }
    };

    /**
     * This method will change the drawer behavior according to the argument direction.
     *
     * @param dir - The direction that the drawer will enter the screen from.
     */
    private void updateDirection(DrawerDirection dir) {
        maxSizeProperty.set(-1);
        prefSizeProperty.set(-1);
        // reset old translation
        translateProperty.set(0);

        // update properties
        if (dir == DrawerDirection.LEFT || dir == DrawerDirection.RIGHT) {
            // set the new translation property
            translateProperty = sidePane.translateXProperty();
            // change the size property
            maxSizeProperty = sidePane.maxWidthProperty();
            prefSizeProperty = sidePane.prefWidthProperty();
            sizeProperty = sidePane.widthProperty();
            paddingSizeProperty = paddingPane.prefWidthProperty();

            this.boundedNodeProperty().removeListener(heightListener);
            this.boundedNodeProperty().addListener(widthListener);
            if (getBoundedNode() == null) {
                this.boundedNodeProperty().bind(this.parentProperty());
            }
            this.sceneProperty().removeListener(sceneHeightListener);
            this.sceneProperty().removeListener(sceneWidthListener);
            this.sceneProperty().addListener(sceneWidthListener);

        } else if (dir == DrawerDirection.TOP || dir == DrawerDirection.BOTTOM) {
            // set the new translation property
            translateProperty = sidePane.translateYProperty();
            // change the size property
            maxSizeProperty = sidePane.maxHeightProperty();
            prefSizeProperty = sidePane.prefHeightProperty();
            sizeProperty = sidePane.heightProperty();
            paddingSizeProperty = paddingPane.prefHeightProperty();

            this.boundedNodeProperty().removeListener(widthListener);
            this.boundedNodeProperty().addListener(heightListener);
            if (getBoundedNode() == null) {
                this.boundedNodeProperty().bind(this.parentProperty());
            }
            this.sceneProperty().removeListener(sceneHeightListener);
            this.sceneProperty().removeListener(sceneWidthListener);
            this.sceneProperty().addListener(sceneHeightListener);
        }
        // update pane alignment
        if (dir == DrawerDirection.LEFT) {
            StackPane.setAlignment(sidePane, Pos.CENTER_LEFT);
        } else if (dir == DrawerDirection.RIGHT) {
            StackPane.setAlignment(sidePane, Pos.CENTER_RIGHT);
        } else if (dir == DrawerDirection.TOP) {
            StackPane.setAlignment(sidePane, Pos.TOP_CENTER);
        } else if (dir == DrawerDirection.BOTTOM) {
            StackPane.setAlignment(sidePane, Pos.BOTTOM_CENTER);
        }

        setDefaultDrawerSize(defaultSizeProperty.get());
        updateDrawerAnimation(initTranslate.get());
        updateContent();
    }

    private void updateDrawerAnimation(double translation) {
        translateProperty.set(translation);
        translateTo = translation;
    }

    private double computePaddingSize() {
        if (!isResizeContent()) {
            return 0;
        }
        if (translateTo == 0 && tempDrawerSize > getDefaultDrawerSize()) {
            return tempDrawerSize;
        } else if (translateTo == 0) {
            return getDefaultDrawerSize();
        } else if (translateTo == initTranslate.get()) {
            return 0;
        } else {
            return defaultSizeProperty.get() + getDirection().doubleValue() * translateTo;
        }
    }


    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    private ArrayList<Callback<Void, Boolean>> callBacks = new ArrayList<>();

    /**
     * the callbacks are used to add conditions to allow
     * starting the drawer when holding on the side part of the content
     */
    public void addInitDrawerCallback(Callback<Void, Boolean> callBack) {
        callBacks.add(callBack);
    }

    /**
     * this method is only used in drawers stack component
     *
     * @param callback
     */
    void bringToFront(Callback<Void, Void> callback) {
        EventHandler<? super MouseEvent> eventFilter = Event::consume;
        final boolean bindSize = prefSizeProperty.isBound();
        prefSizeProperty.unbind();
        maxSizeProperty.unbind();
        // disable mouse events
        this.addEventFilter(MouseEvent.ANY, eventFilter);

        Runnable onFinished = () -> {
            callback.call(null);
            translateTo = 0;
            translateTimer.setOnFinished(() -> {
                if (bindSize) {
                    prefSizeProperty.bind(parentSizeProperty);
                    maxSizeProperty.bind(parentSizeProperty);
                }
                // enable mouse events
                this.removeEventFilter(MouseEvent.ANY, eventFilter);
            });
            translateTimer.start();
        };

        if (sizeProperty.get() > getDefaultDrawerSize()) {
            tempDrawerSize = sizeProperty.get();
        } else {
            tempDrawerSize = getDefaultDrawerSize();
        }
        translateTo = initTranslate.get();
        translateTimer.setOnFinished(onFinished);
        translateTimer.start();
    }

    /**
     * This indicates whether or not the drawer is completely shown.
     *
     * @return True if the drawer is totally visible and not transitioning, otherwise false.
     */
    public boolean isShown() {
        return (translateTo == 0 || translateProperty.get() == 0) && !translateTimer.isRunning();
    }

    /**
     * This indicates whether or not the drawer is in the process of being shown.
     *
     * @return True if the drawer is transitioning from closed to open, otherwise false.
     */
    public boolean isShowing() {
        return translateTo == 0 && translateTimer.isRunning();
    }

    /**
     * This indicates whether or not the drawer is in the process of being hidden.
     *
     * @return True if the drawer is transitioning from open to closed, otherwise false.
     */
    public boolean isHiding() {
        return translateTo == initTranslate.get() && translateTimer.isRunning();
    }

    /**
     * This indicates whether or not the drawer is completely hidden.
     *
     * @return True if the drawer is hidden and not in the process of transitioning, otherwise false.
     */
    public boolean isHidden() {
        return translateTo == initTranslate.get() && !translateTimer.isRunning();
    }

    /**
     * Toggles the drawer between open and closed. The drawer will be closed if it is shown or transitioning between
     * closed and open. Likewise, it will be opened if it is open or transitioning from open to closed.
     */
    public void toggle() {
        if (isShown() || isShowing()) {
            close();
        } else {
            open();
        }
    }

    /**
     * Starts the animation to transition this drawer to open.
     */
    public void open() {
        translateTo = 0;
        overlayPane.setMouseTransparent(!isOverLayVisible());
        translateTimer.setOnFinished(() -> fireEvent(new JFXDrawerEvent(JFXDrawerEvent.OPENED)));
        translateTimer.reverseAndContinue();
    }

    /**
     * Starts the animation to transition this drawer to closed.
     */
    public void close() {
        // unbind properties as the drawer size might be bound to stage size
        maxSizeProperty.unbind();
        prefSizeProperty.unbind();
        if (sizeProperty.get() > getDefaultDrawerSize()) {
            tempDrawerSize = prefSizeProperty.get();
        } else {
            tempDrawerSize = getDefaultDrawerSize();
        }
        if (translateTo != initTranslate.get()) {
            translateTo = initTranslate.get();
            translateTimer.reverseAndContinue();
        }
        translateTimer.setOnFinished(() -> {
            overlayPane.setMouseTransparent(true);
            fireEvent(new JFXDrawerEvent(JFXDrawerEvent.CLOSED));
        });
    }

    /***************************************************************************
     *                                                                         *
     * Setters / Getters                                                       *
     *                                                                         *
     **************************************************************************/

    public ObservableList<Node> getSidePane() {
        return sidePane.getChildren();
    }

    public void setSidePane(Node... sidePane) {
        this.sidePane.getChildren().setAll(sidePane);
    }

    public ObservableList<Node> getContent() {
        if(contentHolder.getChildren().isEmpty()) updateContent();
        return content.getChildren();
    }

    public void setContent(Node... content) {
        this.content.getChildren().setAll(content);
        if(contentHolder.getChildren().isEmpty()) updateContent();
    }

    private void updateContent() {
        paddingPane.setPrefSize(0, 0);
        paddingPane.setMinSize(0, 0);
        Node contentNode = content;
        switch (getDirection()) {
            case TOP:
                contentNode = new VBox(paddingPane, content);
                VBox.setVgrow(content, Priority.ALWAYS);
                break;
            case BOTTOM:
                contentNode = new VBox(content, paddingPane);
                VBox.setVgrow(content, Priority.ALWAYS);
                break;
            case LEFT:
                contentNode = new HBox(paddingPane, content);
                HBox.setHgrow(content, Priority.ALWAYS);
                break;
            case RIGHT:
                contentNode = new HBox(content, paddingPane);
                HBox.setHgrow(content, Priority.ALWAYS);
                break;
        }
        contentNode.setPickOnBounds(false);
        if (isShown()) {
            paddingSizeProperty.set(computePaddingSize());
        }
        contentHolder.getChildren().setAll(contentNode);
    }

    /***************************************************************************
     *                                                                         *
     * public properties                                                       *
     *                                                                         *
     **************************************************************************/

    public double getDefaultDrawerSize() {
        return defaultSizeProperty.get();
    }

    public void setDefaultDrawerSize(double drawerWidth) {
        defaultSizeProperty.set(drawerWidth);
        maxSizeProperty.set(drawerWidth);
        prefSizeProperty.set(drawerWidth);
    }


    private SimpleObjectProperty<DrawerDirection> directionProperty =
        new SimpleObjectProperty<>(DrawerDirection.LEFT);

    public DrawerDirection getDirection() {
        return directionProperty.get();
    }

    public SimpleObjectProperty<DrawerDirection> directionProperty() {
        return directionProperty;
    }

    public void setDirection(DrawerDirection direction) {
        this.directionProperty.set(direction);
    }


    private BooleanProperty overLayVisible = new SimpleBooleanProperty(true);

    public final BooleanProperty overLayVisibleProperty() {
        return this.overLayVisible;
    }

    public final boolean isOverLayVisible() {
        return this.overLayVisibleProperty().get();
    }

    public final void setOverLayVisible(final boolean overLayVisible) {
        this.overLayVisibleProperty().set(overLayVisible);
    }


    private boolean resizable = false;

    public boolean isResizableOnDrag() {
        return resizable;
    }

    public void setResizableOnDrag(boolean resizable) {
        this.resizable = resizable;
    }


    private boolean resizeContent = false;

    public boolean isResizeContent() {
        return resizeContent;
    }

    public void setResizeContent(boolean resizeContent) {
        this.resizeContent = resizeContent;
        // animate content size
        translateTimer.reverseAndContinue();
    }

    private ObjectProperty<Node> boundedNode = new SimpleObjectProperty<>();

    private final ObjectProperty<Node> boundedNodeProperty() {
        return this.boundedNode;
    }

    private final Node getBoundedNode() {
        return this.boundedNodeProperty().get();
    }

    private final void setBoundedNode(final Node boundedNode) {
        this.boundedNodeProperty().unbind();
        this.boundedNodeProperty().set(boundedNode);
    }

    /***************************************************************************
     *                                                                         *
     * Custom Events                                                           *
     *                                                                         *
     **************************************************************************/

    public EventHandler<JFXDrawerEvent> getOnDrawerClosed() {
        return onDrawerClosedProperty().get();
    }

    public ObjectProperty<EventHandler<JFXDrawerEvent>> onDrawerClosedProperty() {
        return onDrawerClosed;
    }

    public void setOnDrawerClosed(EventHandler<JFXDrawerEvent> onDrawerClosed) {
        onDrawerClosedProperty().set(onDrawerClosed);
    }

    private ObjectProperty<EventHandler<JFXDrawerEvent>> onDrawerClosed = new ObjectPropertyBase<EventHandler<JFXDrawerEvent>>() {
        @Override
        protected void invalidated() {
            setEventHandler(JFXDrawerEvent.CLOSED, get());
        }

        @Override
        public Object getBean() {
            return JFXDrawer.this;
        }

        @Override
        public String getName() {
            return "onClosed";
        }
    };


    public EventHandler<JFXDrawerEvent> getOnDrawerClosing() {
        return onDrawerClosing.get();
    }

    public ObjectProperty<EventHandler<JFXDrawerEvent>> onDrawerClosingProperty() {
        return onDrawerClosing;
    }

    public void setOnDrawerClosing(EventHandler<JFXDrawerEvent> onDrawerClosing) {
        this.onDrawerClosing.set(onDrawerClosing);
    }

    private ObjectProperty<EventHandler<JFXDrawerEvent>> onDrawerClosing = new ObjectPropertyBase<EventHandler<JFXDrawerEvent>>() {
        @Override
        protected void invalidated() {
            setEventHandler(JFXDrawerEvent.CLOSING, get());
        }

        @Override
        public Object getBean() {
            return JFXDrawer.this;
        }

        @Override
        public String getName() {
            return "onClosing";
        }
    };


    public EventHandler<JFXDrawerEvent> getOnDrawerOpened() {
        return onDrawerOpened.get();
    }

    public ObjectProperty<EventHandler<JFXDrawerEvent>> onDrawerOpenedProperty() {
        return onDrawerOpened;
    }

    public void setOnDrawerOpened(EventHandler<JFXDrawerEvent> onDrawerOpened) {
        this.onDrawerOpened.set(onDrawerOpened);
    }

    private ObjectProperty<EventHandler<JFXDrawerEvent>> onDrawerOpened = new ObjectPropertyBase<EventHandler<JFXDrawerEvent>>() {
        @Override
        protected void invalidated() {
            setEventHandler(JFXDrawerEvent.OPENED, get());
        }

        @Override
        public Object getBean() {
            return JFXDrawer.this;
        }

        @Override
        public String getName() {
            return "onOpened";
        }
    };


    public EventHandler<JFXDrawerEvent> getOnDrawerOpening() {
        return onDrawerOpening.get();
    }

    public ObjectProperty<EventHandler<JFXDrawerEvent>> onDrawerOpeningProperty() {
        return onDrawerOpening;
    }

    public void setOnDrawerOpening(EventHandler<JFXDrawerEvent> onDrawerOpening) {
        this.onDrawerOpening.set(onDrawerOpening);
    }

    private ObjectProperty<EventHandler<JFXDrawerEvent>> onDrawerOpening = new ObjectPropertyBase<EventHandler<JFXDrawerEvent>>() {
        @Override
        protected void invalidated() {
            setEventHandler(JFXDrawerEvent.OPENING, get());
        }

        @Override
        public Object getBean() {
            return JFXDrawer.this;
        }

        @Override
        public String getName() {
            return "onOpening";
        }
    };

    /***************************************************************************
     *                                                                         *
     * Action Handlers                                                         *
     *                                                                         *
     **************************************************************************/

    private EventHandler<MouseEvent> mouseDragHandler = (mouseEvent) -> {
        if (!mouseEvent.isConsumed()) {
            mouseEvent.consume();
            double size = 0;
            Bounds sceneBounds = content.localToScene(content.getLayoutBounds());
            if (directionProperty.get() == DrawerDirection.RIGHT) {
                size = sceneBounds.getMinX() + sceneBounds.getWidth();
            } else if (directionProperty.get() == DrawerDirection.BOTTOM) {
                size = sceneBounds.getMinY() + sceneBounds.getHeight();
            }

            if (startSize == -1) {
                startSize = sizeProperty.get();
            }

            double eventPoint;
            double directionValue = getDirection().doubleValue();

            if (getDirection() == DrawerDirection.RIGHT
                || getDirection() == DrawerDirection.LEFT) {
                eventPoint = mouseEvent.getSceneX();
            } else {
                eventPoint = mouseEvent.getSceneY();
            }

            if (size + (directionValue * eventPoint) >= activeOffset
                && partialTransition != null) {
                partialTransition = null;
            } else if (partialTransition == null) {
                double currentTranslate = startTranslate + eventPoint - startMouse;

                if (directionValue * currentTranslate <= 0) {
                    // the drawer is hidden
                    if (resizable) {
                        maxSizeProperty.unbind();
                        prefSizeProperty.unbind();
                        if (startSize - getDefaultDrawerSize() + directionValue * currentTranslate > 0) {
                            // change the side drawer size if dragging from hidden
                            maxSizeProperty.set(startSize + directionValue * currentTranslate);
                            prefSizeProperty.set(startSize + directionValue * currentTranslate);
                            if (isResizeContent()) {
                                paddingSizeProperty.set(startSize + directionValue * currentTranslate);
                            }
                        } else {
                            // if the side drawer is not fully shown perform translation to show it , and set its default size
                            maxSizeProperty.set(getDefaultDrawerSize());
                            double translation = directionValue * (startSize - getDefaultDrawerSize()) + currentTranslate;
                            translateProperty.set(translation);
                            overlayPane.setOpacity(1 - translateProperty.get() / initTranslate.get());
                            if (isResizeContent()) {
                                paddingSizeProperty.set(defaultSizeProperty.get() + directionValue * translation);
                            }
                        }
                    } else {
                        translateProperty.set(currentTranslate);
                        overlayPane.setOpacity(1 - translateProperty.get() / initTranslate.get());
                    }
                } else {
                    // the drawer is already shown
                    if (resizable) {
                        if (startSize + directionValue * currentTranslate <= parentSizeProperty.get()) {
                            // change the side drawer size after being shown
                            maxSizeProperty.unbind();
                            prefSizeProperty.unbind();
                            maxSizeProperty.set(startSize + directionValue * currentTranslate);
                            prefSizeProperty.set(startSize + directionValue * currentTranslate);
                            if (isResizeContent()) {
                                paddingSizeProperty.set(startSize + directionValue * currentTranslate);
                            }
                        } else {
                            // bind the drawer size to its parent
                            maxSizeProperty.bind(parentSizeProperty);
                            prefSizeProperty.bind(parentSizeProperty);
                        }
                    }
                    translateProperty.set(0);
                    overlayPane.setOpacity(1 - translateProperty.get() / initTranslate.get());
                }
            }
        }
    };

    private EventHandler<MouseEvent> mousePressedHandler = (mouseEvent) -> {
        translateTimer.setOnFinished(null);
        translateTimer.stop();
        if (directionProperty.get() == DrawerDirection.RIGHT
            || directionProperty.get() == DrawerDirection.LEFT) {
            startMouse = mouseEvent.getSceneX();
        } else {
            startMouse = mouseEvent.getSceneY();
        }
        startTranslate = translateProperty.get();
        startSize = sizeProperty.get();

    };

    private EventHandler<MouseEvent> mouseReleasedHandler = (mouseEvent) -> {
        if (directionProperty.get().doubleValue() * translateProperty.get()
            > directionProperty.get().doubleValue() * initTranslate.get() / 2) {
            // show side pane
            if (translateProperty.get() != 0.0) {
                partialOpen();
            }
        } else {
            // hide the sidePane
            if (translateProperty.get() != initTranslate.get()) {
                partialClose();
            }
        }
        if (sizeProperty.get() > getDefaultDrawerSize()) {
            tempDrawerSize = prefSizeProperty.get();
        } else {
            tempDrawerSize = getDefaultDrawerSize();
        }
        // reset drawer animation properties
        startMouse = -1;
        startTranslate = -1;
        startSize = sizeProperty.get();
    };

    private void partialClose() {
        translateTo = initTranslate.get();
        translateTimer.setOnFinished(() -> {
            overlayPane.setMouseTransparent(true);
            fireEvent(new JFXDrawerEvent(JFXDrawerEvent.CLOSED));
        });
        translateTimer.start();
    }

    private void partialOpen() {
        translateTo = 0;
        overlayPane.setMouseTransparent(!isOverLayVisible());
        translateTimer.setOnFinished(() -> fireEvent(new JFXDrawerEvent(JFXDrawerEvent.OPENED)));
        translateTimer.start();
    }

    /***************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    /**
     * Initialize the style class to 'jfx-drawer'.
     * <p>
     * This is the selector class from which CSS can be used to style
     * this control.
     */
    private static final String DEFAULT_STYLE_CLASS = "jfx-drawer";

}

